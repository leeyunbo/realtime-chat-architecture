package com.bok.chat.integration;

import com.bok.chat.api.dto.*;
import com.bok.chat.api.service.ChatMessageService;
import com.bok.chat.api.service.ChatRoomService;
import com.bok.chat.api.service.FriendService;
import com.bok.chat.api.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("채팅 플로우 통합 테스트")
class ChatFlowIntegrationTest extends IntegrationTestBase {

    @Autowired
    private UserService userService;

    @Autowired
    private FriendService friendService;

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private ChatMessageService chatMessageService;

    @Test
    @DisplayName("회원가입 → 친구추가 → 채팅방생성 → 메시지전송 → 읽음처리 전체 플로우가 정상 동작한다")
    void fullFlow_register_friend_chatRoom_message_read() {
        // 1. 회원가입
        Long userId1 = userService.register(new RegisterRequest("alice", "password1234"));
        Long userId2 = userService.register(new RegisterRequest("bob", "password1234"));

        // 2. 로그인
        LoginResponse login1 = userService.login(new LoginRequest("alice", "password1234"));
        assertThat(login1.token()).isNotBlank();

        // 3. 친구 추가
        friendService.addFriend(userId1, "bob");

        List<FriendResponse> friends = friendService.getFriends(userId1);
        assertThat(friends).hasSize(1);
        assertThat(friends.get(0).username()).isEqualTo("bob");

        // 4. 채팅방 생성
        ChatRoomResponse room = chatRoomService.create(userId1,
                new CreateChatRoomRequest(List.of(userId2)));
        assertThat(room.type()).isEqualTo(com.bok.chat.entity.ChatRoom.ChatRoomType.DIRECT);
        assertThat(room.members()).containsExactlyInAnyOrder("alice", "bob");

        // 5. 메시지 전송
        ChatMessageService.SendResult sendResult = chatMessageService.sendMessage(
                userId1, room.id(), "Hello Bob!");
        assertThat(sendResult.message().getContent()).isEqualTo("Hello Bob!");
        assertThat(sendResult.message().getUnreadCount()).isEqualTo(1);

        // 6. 읽음 처리
        ChatMessageService.BulkReadResult readResult = chatMessageService.readMessages(userId2, room.id());
        assertThat(readResult.success()).isTrue();
        assertThat(readResult.lastReadMessageId()).isEqualTo(sendResult.message().getId());
    }

    @Test
    @DisplayName("메시지 전송 후 채팅방 목록에서 안읽은 메시지 수를 확인하고 읽음 처리하면 0이 된다")
    void chatRoomList_shouldShowUnreadCount() {
        // 회원가입 + 친구
        Long userId1 = userService.register(new RegisterRequest("charlie", "password1234"));
        Long userId2 = userService.register(new RegisterRequest("diana", "password1234"));
        friendService.addFriend(userId1, "diana");

        // 채팅방 생성
        ChatRoomResponse room = chatRoomService.create(userId1,
                new CreateChatRoomRequest(List.of(userId2)));

        // 메시지 3개 전송
        chatMessageService.sendMessage(userId1, room.id(), "msg1");
        chatMessageService.sendMessage(userId1, room.id(), "msg2");
        chatMessageService.sendMessage(userId1, room.id(), "msg3");

        // diana 기준 unreadCount 확인
        List<ChatRoomResponse> rooms = chatRoomService.getMyChatRooms(userId2);
        assertThat(rooms).hasSize(1);
        assertThat(rooms.get(0).unreadCount()).isEqualTo(3);

        // diana 읽음 처리 후 unreadCount 확인
        chatMessageService.readMessages(userId2, room.id());

        rooms = chatRoomService.getMyChatRooms(userId2);
        assertThat(rooms.get(0).unreadCount()).isEqualTo(0);
    }
}
