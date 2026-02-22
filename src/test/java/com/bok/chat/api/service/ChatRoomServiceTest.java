package com.bok.chat.api.service;

import com.bok.chat.api.dto.ChatRoomResponse;
import com.bok.chat.api.dto.CreateChatRoomRequest;
import com.bok.chat.api.dto.InviteResult;
import com.bok.chat.api.dto.LeaveResult;
import com.bok.chat.entity.ChatRoom;
import com.bok.chat.entity.ChatRoomUser;
import com.bok.chat.entity.Message;
import com.bok.chat.entity.User;
import com.bok.chat.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.Mockito.verify;

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

    @Nested
    @DisplayName("멤버 초대")
    class InviteMembers {

        @Test
        @DisplayName("새 멤버를 초대하면 초대된 유저 ID와 시스템 메시지를 반환한다")
        void inviteMembers_shouldReturnInvitedUserIds() {
            ChatRoom chatRoom = createChatRoom(1L, 2);
            User inviter = createUser(1L, "inviter");
            User newUser = createUser(3L, "newbie");
            ChatRoomUser inviterMembership = createChatRoomUser(1L, chatRoom, inviter);
            ChatRoomUser newMembership = createChatRoomUser(3L, chatRoom, newUser);

            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L))
                    .willReturn(Optional.of(inviterMembership));
            given(friendshipRepository.existsFriendship(1L, 3L)).willReturn(true);
            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 3L))
                    .willReturn(Optional.empty());
            given(userRepository.findById(3L)).willReturn(Optional.of(newUser));
            given(chatRoomUserRepository.save(any(ChatRoomUser.class))).willAnswer(inv -> inv.getArgument(0));
            given(chatRoomUserRepository.findByChatRoomIdAndStatus(1L, ChatRoomUser.Status.ACTIVE))
                    .willReturn(List.of(inviterMembership, newMembership));
            given(messageRepository.save(any(Message.class))).willAnswer(inv -> inv.getArgument(0));

            InviteResult result = chatRoomService.inviteMembers(1L, 1L, List.of(3L));

            assertThat(result.invitedUserIds()).containsExactly(3L);
            assertThat(result.systemMessage()).isNotNull();
            assertThat(result.systemMessage().getContent()).contains("newbie");
        }

        @Test
        @DisplayName("자기 자신을 초대하면 예외가 발생한다")
        void inviteMembers_selfInvite_shouldThrow() {
            ChatRoom chatRoom = createChatRoom(1L, 2);
            User inviter = createUser(1L, "inviter");
            ChatRoomUser inviterMembership = createChatRoomUser(1L, chatRoom, inviter);

            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L))
                    .willReturn(Optional.of(inviterMembership));

            assertThatThrownBy(() -> chatRoomService.inviteMembers(1L, 1L, List.of(1L)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("자기 자신은 초대할 수 없습니다");
        }

        @Test
        @DisplayName("친구가 아닌 사용자를 초대하면 예외가 발생한다")
        void inviteMembers_notFriend_shouldThrow() {
            ChatRoom chatRoom = createChatRoom(1L, 2);
            User inviter = createUser(1L, "inviter");
            ChatRoomUser inviterMembership = createChatRoomUser(1L, chatRoom, inviter);

            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L))
                    .willReturn(Optional.of(inviterMembership));
            given(friendshipRepository.existsFriendship(1L, 3L)).willReturn(false);

            assertThatThrownBy(() -> chatRoomService.inviteMembers(1L, 1L, List.of(3L)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("친구가 아닌 사용자");
        }

        @Test
        @DisplayName("이미 ACTIVE인 멤버는 스킵된다")
        void inviteMembers_alreadyActive_shouldSkip() {
            ChatRoom chatRoom = createChatRoom(1L, 2);
            User inviter = createUser(1L, "inviter");
            User existing = createUser(2L, "existing");
            ChatRoomUser inviterMembership = createChatRoomUser(1L, chatRoom, inviter);
            ChatRoomUser existingMembership = createChatRoomUser(2L, chatRoom, existing);

            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L))
                    .willReturn(Optional.of(inviterMembership));
            given(friendshipRepository.existsFriendship(1L, 2L)).willReturn(true);
            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 2L))
                    .willReturn(Optional.of(existingMembership));
            given(chatRoomUserRepository.findByChatRoomIdAndStatus(1L, ChatRoomUser.Status.ACTIVE))
                    .willReturn(List.of(inviterMembership, existingMembership));

            InviteResult result = chatRoomService.inviteMembers(1L, 1L, List.of(2L));

            assertThat(result.invitedUserIds()).isEmpty();
            assertThat(result.systemMessage()).isNull();
        }

        @Test
        @DisplayName("LEFT 상태 멤버는 rejoin 처리된다")
        void inviteMembers_leftMember_shouldRejoin() {
            ChatRoom chatRoom = createChatRoom(1L, 2);
            User inviter = createUser(1L, "inviter");
            User leftUser = createUser(2L, "leftie");
            ChatRoomUser inviterMembership = createChatRoomUser(1L, chatRoom, inviter);
            ChatRoomUser leftMembership = createChatRoomUser(2L, chatRoom, leftUser);
            leftMembership.leave();

            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L))
                    .willReturn(Optional.of(inviterMembership));
            given(friendshipRepository.existsFriendship(1L, 2L)).willReturn(true);
            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 2L))
                    .willReturn(Optional.of(leftMembership));
            given(chatRoomUserRepository.findByChatRoomIdAndStatus(1L, ChatRoomUser.Status.ACTIVE))
                    .willReturn(List.of(inviterMembership, leftMembership));
            given(messageRepository.save(any(Message.class))).willAnswer(inv -> inv.getArgument(0));

            InviteResult result = chatRoomService.inviteMembers(1L, 1L, List.of(2L));

            assertThat(result.invitedUserIds()).containsExactly(2L);
            assertThat(leftMembership.getStatus()).isEqualTo(ChatRoomUser.Status.ACTIVE);
        }
    }

    @Nested
    @DisplayName("채팅방 퇴장")
    class LeaveRoom {

        @Test
        @DisplayName("퇴장하면 시스템 메시지와 남은 멤버를 반환한다")
        void leaveRoom_shouldReturnSystemMessageAndRemainingMembers() {
            ChatRoom chatRoom = createChatRoom(1L, 2);
            User user = createUser(1L, "alice");
            User other = createUser(2L, "bob");
            ChatRoomUser membership = createChatRoomUser(1L, chatRoom, user);
            ChatRoomUser otherMembership = createChatRoomUser(2L, chatRoom, other);

            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L))
                    .willReturn(Optional.of(membership));
            given(chatRoomUserRepository.findByChatRoomIdAndStatus(1L, ChatRoomUser.Status.ACTIVE))
                    .willReturn(List.of(otherMembership));
            given(messageRepository.save(any(Message.class))).willAnswer(inv -> inv.getArgument(0));

            LeaveResult result = chatRoomService.leaveRoom(1L, 1L);

            assertThat(membership.getStatus()).isEqualTo(ChatRoomUser.Status.LEFT);
            assertThat(result.systemMessage()).isNotNull();
            assertThat(result.systemMessage().getContent()).contains("alice");
            assertThat(result.remainingMembers()).hasSize(1);
        }

        @Test
        @DisplayName("활성 멤버가 아니면 예외가 발생한다")
        void leaveRoom_notActiveMember_shouldThrow() {
            ChatRoom chatRoom = createChatRoom(1L, 2);
            User user = createUser(1L, "alice");
            ChatRoomUser membership = createChatRoomUser(1L, chatRoom, user);
            membership.leave();

            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L))
                    .willReturn(Optional.of(membership));

            assertThatThrownBy(() -> chatRoomService.leaveRoom(1L, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("활성 멤버가 아닙니다");
        }

        @Test
        @DisplayName("마지막 멤버가 퇴장하면 시스템 메시지가 생성되지 않는다")
        void leaveRoom_lastMember_shouldNotCreateSystemMessage() {
            ChatRoom chatRoom = createChatRoom(1L, 2);
            User user = createUser(1L, "alice");
            ChatRoomUser membership = createChatRoomUser(1L, chatRoom, user);

            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L))
                    .willReturn(Optional.of(membership));
            given(chatRoomUserRepository.findByChatRoomIdAndStatus(1L, ChatRoomUser.Status.ACTIVE))
                    .willReturn(List.of());

            LeaveResult result = chatRoomService.leaveRoom(1L, 1L);

            assertThat(result.systemMessage()).isNull();
            assertThat(result.remainingMembers()).isEmpty();
        }
    }
}
