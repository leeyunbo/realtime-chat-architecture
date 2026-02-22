package com.bok.chat.websocket;

import com.bok.chat.api.service.ChatMessageService;
import com.bok.chat.api.service.ChatMessageService.BulkReadResult;
import com.bok.chat.api.service.ChatMessageService.SendResult;
import com.bok.chat.api.service.ChatMessageService.UndeliveredMessages;
import com.bok.chat.api.service.ChatRoomService;
import com.bok.chat.api.service.FriendService;
import com.bok.chat.config.ServerIdHolder;
import com.bok.chat.entity.ChatRoomUser;
import com.bok.chat.entity.Message;
import com.bok.chat.entity.User;
import com.bok.chat.redis.OnlineStatusService;
import com.bok.chat.redis.RedisMessageRelay;
import com.bok.chat.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.bok.chat.support.TestFixtures.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("ChatWebSocketHandler")
@ExtendWith(MockitoExtension.class)
class ChatWebSocketHandlerTest {

    @InjectMocks
    private ChatWebSocketHandler handler;

    @Mock
    private WebSocketSessionManager sessionManager;

    @Mock
    private ChatMessageService chatMessageService;

    @Mock
    private ChatRoomService chatRoomService;

    @Mock
    private FriendService friendService;

    @Mock
    private OnlineStatusService onlineStatusService;

    @Mock
    private RedisMessageRelay redisMessageRelay;

    @Mock
    private ServerIdHolder serverIdHolder;

    @Mock
    private UserRepository userRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private WebSocketSession session;

    @BeforeEach
    void setUp() {
        session = mock(WebSocketSession.class);
        given(session.getAttributes()).willReturn(Map.of("userId", 1L));
    }

    @Nested
    @DisplayName("연결/해제")
    class Connection {

        @Test
        @DisplayName("연결 시 세션 등록, 온라인 설정, 밀린 메시지 전달, 친구에게 상태 알림을 보낸다")
        void afterConnectionEstablished() {
            given(chatMessageService.getUndeliveredMessages(1L)).willReturn(List.of());
            given(userRepository.findById(1L)).willReturn(Optional.of(createUser(1L, "alice")));
            given(friendService.getFriendIds(1L)).willReturn(List.of(2L));

            handler.afterConnectionEstablished(session);

            verify(sessionManager).register(1L, session);
            verify(onlineStatusService).setOnline(1L);
            verify(chatMessageService).getUndeliveredMessages(1L);
            verify(friendService).getFriendIds(1L);
        }

        @Test
        @DisplayName("연결 시 밀린 메시지를 전달한다")
        void afterConnectionEstablished_sendsPendingMessages() throws Exception {
            var chatRoom = createChatRoom(1L, 2);
            var sender = createUser(2L, "bob");
            var msg1 = createMessage(10L, chatRoom, sender, "hello", 2);
            var msg2 = createMessage(11L, chatRoom, sender, "world", 2);

            given(chatMessageService.getUndeliveredMessages(1L))
                    .willReturn(List.of(new UndeliveredMessages(1L, List.of(msg1, msg2))));
            given(userRepository.findById(1L)).willReturn(Optional.of(createUser(1L, "alice")));
            given(friendService.getFriendIds(1L)).willReturn(List.of());

            handler.afterConnectionEstablished(session);

            verify(session, times(2)).sendMessage(any(TextMessage.class));
        }

        @Test
        @DisplayName("밀린 메시지 전송 중 IOException 발생 시 나머지 전송을 중단한다")
        void afterConnectionEstablished_ioException_stopsDelivery() throws Exception {
            var chatRoom = createChatRoom(1L, 2);
            var sender = createUser(2L, "bob");
            var msg1 = createMessage(10L, chatRoom, sender, "hello", 2);
            var msg2 = createMessage(11L, chatRoom, sender, "world", 2);

            given(chatMessageService.getUndeliveredMessages(1L))
                    .willReturn(List.of(new UndeliveredMessages(1L, List.of(msg1, msg2))));
            doThrow(new IOException("broken pipe")).when(session).sendMessage(any(TextMessage.class));
            given(userRepository.findById(1L)).willReturn(Optional.of(createUser(1L, "alice")));
            given(friendService.getFriendIds(1L)).willReturn(List.of());

            handler.afterConnectionEstablished(session);

            // IOException 발생 시 첫 메시지만 시도하고 중단
            verify(session, times(1)).sendMessage(any(TextMessage.class));
        }

        @Test
        @DisplayName("연결 해제 시 세션 제거, 오프라인 설정, 친구에게 상태 알림을 보낸다")
        void afterConnectionClosed() {
            given(userRepository.findById(1L)).willReturn(Optional.of(createUser(1L, "alice")));
            given(friendService.getFriendIds(1L)).willReturn(List.of(2L));

            handler.afterConnectionClosed(session, CloseStatus.NORMAL);

            verify(sessionManager).remove(1L);
            verify(onlineStatusService).setOffline(1L);
            verify(friendService).getFriendIds(1L);
        }
    }

    @Nested
    @DisplayName("메시지 전송 (MESSAGE_SEND)")
    class SendMessage {

        @Test
        @DisplayName("메시지 전송 시 채팅방 멤버에게 메시지를 전달한다")
        void handleSendMessage() throws Exception {
            var chatRoom = createChatRoom(1L, 2);
            var sender = createUser(1L, "alice");
            var receiver = createUser(2L, "bob");
            var message = createMessage(1L, chatRoom, sender, "hello", 2);
            var member1 = createChatRoomUser(1L, chatRoom, sender);
            var member2 = createChatRoomUser(2L, chatRoom, receiver);

            given(chatMessageService.sendMessage(1L, 1L, "hello"))
                    .willReturn(new SendResult(message, sender, List.of(member1, member2)));

            WebSocketSession senderSession = mock(WebSocketSession.class);
            given(senderSession.isOpen()).willReturn(true);
            WebSocketSession receiverSession = mock(WebSocketSession.class);
            given(receiverSession.isOpen()).willReturn(true);
            given(sessionManager.getSession(1L)).willReturn(senderSession);
            given(sessionManager.getSession(2L)).willReturn(receiverSession);

            String payload = "{\"type\":\"message.send\",\"chatRoomId\":1,\"content\":\"hello\"}";
            handler.handleTextMessage(session, new TextMessage(payload));

            verify(senderSession).sendMessage(any(TextMessage.class));
            verify(receiverSession).sendMessage(any(TextMessage.class));
        }

        @Test
        @DisplayName("로컬 세션 전송 중 IOException 발생 시 예외를 전파하지 않고 나머지 멤버에게 계속 전송한다")
        void handleSendMessage_ioException_continuesForOtherMembers() throws Exception {
            var chatRoom = createChatRoom(1L, 3);
            var sender = createUser(1L, "alice");
            var user2 = createUser(2L, "bob");
            var user3 = createUser(3L, "charlie");
            var message = createMessage(1L, chatRoom, sender, "hello", 3);
            var member1 = createChatRoomUser(1L, chatRoom, sender);
            var member2 = createChatRoomUser(2L, chatRoom, user2);
            var member3 = createChatRoomUser(3L, chatRoom, user3);

            given(chatMessageService.sendMessage(1L, 1L, "hello"))
                    .willReturn(new SendResult(message, sender, List.of(member1, member2, member3)));

            WebSocketSession senderSession = mock(WebSocketSession.class);
            given(senderSession.isOpen()).willReturn(true);
            given(sessionManager.getSession(1L)).willReturn(senderSession);

            // bob 세션은 IOException 발생
            WebSocketSession bobSession = mock(WebSocketSession.class);
            given(bobSession.isOpen()).willReturn(true);
            doThrow(new IOException("broken pipe")).when(bobSession).sendMessage(any());
            given(sessionManager.getSession(2L)).willReturn(bobSession);

            // charlie 세션은 정상
            WebSocketSession charlieSession = mock(WebSocketSession.class);
            given(charlieSession.isOpen()).willReturn(true);
            given(sessionManager.getSession(3L)).willReturn(charlieSession);

            String payload = "{\"type\":\"message.send\",\"chatRoomId\":1,\"content\":\"hello\"}";
            handler.handleTextMessage(session, new TextMessage(payload));

            // bob에게 전송 실패해도 charlie에게는 정상 전송된다
            verify(charlieSession).sendMessage(any(TextMessage.class));
        }

        @Test
        @DisplayName("대상이 같은 서버에 있지만 세션이 없으면 relay하지 않는다")
        void handleSendMessage_sameServer_noRelay() throws Exception {
            var chatRoom = createChatRoom(1L, 2);
            var sender = createUser(1L, "alice");
            var receiver = createUser(2L, "bob");
            var message = createMessage(1L, chatRoom, sender, "hello", 2);
            var member1 = createChatRoomUser(1L, chatRoom, sender);
            var member2 = createChatRoomUser(2L, chatRoom, receiver);

            given(chatMessageService.sendMessage(1L, 1L, "hello"))
                    .willReturn(new SendResult(message, sender, List.of(member1, member2)));

            WebSocketSession senderSession = mock(WebSocketSession.class);
            given(senderSession.isOpen()).willReturn(true);
            given(sessionManager.getSession(1L)).willReturn(senderSession);
            given(sessionManager.getSession(2L)).willReturn(null);
            given(onlineStatusService.getServerId(2L)).willReturn("my-server");
            given(serverIdHolder.getServerId()).willReturn("my-server");

            String payload = "{\"type\":\"message.send\",\"chatRoomId\":1,\"content\":\"hello\"}";
            handler.handleTextMessage(session, new TextMessage(payload));

            verify(redisMessageRelay, never()).relayToUser(anyLong(), anyString(), any());
        }

        @Test
        @DisplayName("로컬에 세션이 없으면 Redis를 통해 중계한다")
        void handleSendMessage_relayViaRedis() throws Exception {
            var chatRoom = createChatRoom(1L, 2);
            var sender = createUser(1L, "alice");
            var receiver = createUser(2L, "bob");
            var message = createMessage(1L, chatRoom, sender, "hello", 2);
            var member1 = createChatRoomUser(1L, chatRoom, sender);
            var member2 = createChatRoomUser(2L, chatRoom, receiver);

            given(chatMessageService.sendMessage(1L, 1L, "hello"))
                    .willReturn(new SendResult(message, sender, List.of(member1, member2)));

            WebSocketSession senderSession = mock(WebSocketSession.class);
            given(senderSession.isOpen()).willReturn(true);
            given(sessionManager.getSession(1L)).willReturn(senderSession);
            given(sessionManager.getSession(2L)).willReturn(null);
            given(onlineStatusService.getServerId(2L)).willReturn("other-server");
            given(serverIdHolder.getServerId()).willReturn("my-server");

            String payload = "{\"type\":\"message.send\",\"chatRoomId\":1,\"content\":\"hello\"}";
            handler.handleTextMessage(session, new TextMessage(payload));

            verify(redisMessageRelay).relayToUser(eq(2L), eq("other-server"), any(WebSocketMessage.class));
        }
    }

    @Nested
    @DisplayName("메시지 읽음 처리 (MESSAGE_READ)")
    class ReadMessage {

        @Test
        @DisplayName("읽음 처리 성공 시 다른 멤버에게 알림을 보낸다")
        void handleReadMessage() throws Exception {
            var chatRoom = createChatRoom(1L, 2);
            var user = createUser(1L, "alice");
            var other = createUser(2L, "bob");
            var member1 = createChatRoomUser(1L, chatRoom, user);
            var member2 = createChatRoomUser(2L, chatRoom, other);

            given(chatMessageService.readMessages(1L, 1L))
                    .willReturn(new BulkReadResult(true, 1L, 1L, 10L, List.of(member1, member2)));

            WebSocketSession otherSession = mock(WebSocketSession.class);
            given(otherSession.isOpen()).willReturn(true);
            given(sessionManager.getSession(2L)).willReturn(otherSession);

            String payload = "{\"type\":\"message.read\",\"chatRoomId\":1}";
            handler.handleTextMessage(session, new TextMessage(payload));

            verify(otherSession).sendMessage(any(TextMessage.class));
            verify(sessionManager, never()).getSession(1L);
        }

        @Test
        @DisplayName("읽을 메시지가 없으면 알림을 보내지 않는다")
        void handleReadMessage_nothingToRead() throws Exception {
            given(chatMessageService.readMessages(1L, 1L))
                    .willReturn(BulkReadResult.nothingToRead());

            String payload = "{\"type\":\"message.read\",\"chatRoomId\":1}";
            handler.handleTextMessage(session, new TextMessage(payload));

            verify(sessionManager, never()).getSession(anyLong());
        }
    }

    @Nested
    @DisplayName("하트비트")
    class Heartbeat {

        @Test
        @DisplayName("HEARTBEAT 메시지 수신 시 온라인 상태를 갱신한다")
        void handleHeartbeat() throws Exception {
            String payload = "{\"type\":\"heartbeat\"}";
            handler.handleTextMessage(session, new TextMessage(payload));

            verify(onlineStatusService).refreshOnline(1L);
        }
    }
}
