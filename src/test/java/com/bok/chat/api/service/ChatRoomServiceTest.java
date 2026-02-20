package com.bok.chat.api.service;

import com.bok.chat.api.dto.ChatRoomResponse;
import com.bok.chat.api.dto.CreateChatRoomRequest;
import com.bok.chat.entity.ChatRoom;
import com.bok.chat.entity.ChatRoomUser;
import com.bok.chat.entity.User;
import com.bok.chat.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.bok.chat.support.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@DisplayName("ChatRoomService")
@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @InjectMocks
    private ChatRoomService chatRoomService;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomUserRepository chatRoomUserRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private MessageRepository messageRepository;

    @Test
    @DisplayName("1:1 채팅방 생성 시 DIRECT 타입으로 생성된다")
    void create_dm_shouldReturnDirectChatRoom() {
        User user1 = createUser(1L, "user1");
        User user2 = createUser(2L, "user2");
        ChatRoom chatRoom = createChatRoom(1L, 2);

        given(friendshipRepository.existsFriendship(1L, 2L)).willReturn(true);
        given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(chatRoom);
        given(userRepository.findById(2L)).willReturn(Optional.of(user2));
        given(userRepository.findById(1L)).willReturn(Optional.of(user1));
        given(chatRoomUserRepository.save(any(ChatRoomUser.class))).willAnswer(inv -> inv.getArgument(0));

        CreateChatRoomRequest request = new CreateChatRoomRequest(List.of(2L));
        ChatRoomResponse response = chatRoomService.create(1L, request);

        assertThat(response.type()).isEqualTo(ChatRoom.ChatRoomType.DIRECT);
        assertThat(response.members()).containsExactlyInAnyOrder("user2", "user1");
    }

    @Test
    @DisplayName("3명 이상이면 GROUP 타입으로 생성된다")
    void create_group_shouldReturnGroupChatRoom() {
        User user1 = createUser(1L, "user1");
        User user2 = createUser(2L, "user2");
        User user3 = createUser(3L, "user3");
        ChatRoom chatRoom = createChatRoom(1L, 3);

        given(friendshipRepository.existsFriendship(eq(1L), any())).willReturn(true);
        given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(chatRoom);
        given(userRepository.findById(2L)).willReturn(Optional.of(user2));
        given(userRepository.findById(3L)).willReturn(Optional.of(user3));
        given(userRepository.findById(1L)).willReturn(Optional.of(user1));
        given(chatRoomUserRepository.save(any(ChatRoomUser.class))).willAnswer(inv -> inv.getArgument(0));

        CreateChatRoomRequest request = new CreateChatRoomRequest(List.of(2L, 3L));
        ChatRoomResponse response = chatRoomService.create(1L, request);

        assertThat(response.type()).isEqualTo(ChatRoom.ChatRoomType.GROUP);
    }

    @Test
    @DisplayName("친구가 아닌 사용자가 포함되면 예외가 발생한다")
    void create_notFriends_shouldThrow() {
        given(friendshipRepository.existsFriendship(1L, 2L)).willReturn(false);

        CreateChatRoomRequest request = new CreateChatRoomRequest(List.of(2L));

        assertThatThrownBy(() -> chatRoomService.create(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("친구가 아닌 사용자");
    }

    @Test
    @DisplayName("존재하지 않는 사용자가 포함되면 예외가 발생한다")
    void create_nonExistentUser_shouldThrow() {
        given(friendshipRepository.existsFriendship(1L, 99L)).willReturn(true);
        given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(createChatRoom(1L, 2));
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        CreateChatRoomRequest request = new CreateChatRoomRequest(List.of(99L));

        assertThatThrownBy(() -> chatRoomService.create(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 사용자");
    }

    @Test
    @DisplayName("내 채팅방 목록 조회 시 안읽은 메시지 수가 포함된다")
    void getMyChatRooms_shouldReturnWithUnreadCount() {
        User user = createUser(1L, "user1");
        ChatRoom chatRoom = createChatRoom(1L, 2);
        ChatRoomUser chatRoomUser = createChatRoomUser(1L, chatRoom, user);
        chatRoomUser.updateLastReadMessageId(5L);

        given(chatRoomUserRepository.findByUserIdAndStatus(1L, ChatRoomUser.Status.ACTIVE))
                .willReturn(List.of(chatRoomUser));
        given(chatRoomUserRepository.findByChatRoomIdAndStatus(1L, ChatRoomUser.Status.ACTIVE))
                .willReturn(List.of(chatRoomUser));
        given(messageRepository.countUnreadMessages(1L, 5L)).willReturn(3L);

        List<ChatRoomResponse> rooms = chatRoomService.getMyChatRooms(1L);

        assertThat(rooms).hasSize(1);
        assertThat(rooms.get(0).unreadCount()).isEqualTo(3);
    }
}
