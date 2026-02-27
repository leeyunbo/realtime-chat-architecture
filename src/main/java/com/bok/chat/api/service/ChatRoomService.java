package com.bok.chat.api.service;

import com.bok.chat.api.dto.ChatRoomResponse;
import com.bok.chat.api.dto.CreateChatRoomRequest;
import com.bok.chat.api.dto.InviteResult;
import com.bok.chat.api.dto.LeaveResult;
import com.bok.chat.entity.ChatRoom;
import com.bok.chat.entity.ChatRoomUser;
import com.bok.chat.entity.Message;
import com.bok.chat.entity.User;
import com.bok.chat.repository.ChatRoomRepository;
import com.bok.chat.repository.ChatRoomUserRepository;
import com.bok.chat.repository.FriendshipRepository;
import com.bok.chat.repository.MessageRepository;
import com.bok.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final MessageRepository messageRepository;

    @Transactional
    public ChatRoomResponse create(Long currentUserId, CreateChatRoomRequest request) {
        List<Long> allUserIds = new ArrayList<>(request.userIds().stream().distinct().toList());

        for (Long targetUserId : allUserIds) {
            if (!friendshipRepository.existsFriendship(currentUserId, targetUserId)) {
                throw new IllegalArgumentException("친구가 아닌 사용자가 포함되어 있습니다: " + targetUserId);
            }
        }

        allUserIds.add(currentUserId);

        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.create(allUserIds.size()));

        List<String> memberNames = new ArrayList<>();
        for (Long userId : allUserIds) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + userId));
            chatRoomUserRepository.save(ChatRoomUser.builder()
                    .chatRoom(chatRoom)
                    .user(user)
                    .build());
            memberNames.add(user.getUsername());
        }

        return new ChatRoomResponse(chatRoom.getId(), chatRoom.getType(), memberNames, 0, chatRoom.getCreatedAt());
    }

    public List<ChatRoomResponse> getMyChatRooms(Long userId) {
        List<ChatRoomUser> myRooms = chatRoomUserRepository.findByUserIdAndStatus(userId, ChatRoomUser.Status.ACTIVE);

        return myRooms.stream().map(cru -> {
            ChatRoom room = cru.getChatRoom();
            List<String> memberNames = chatRoomUserRepository
                    .findByChatRoomIdAndStatus(room.getId(), ChatRoomUser.Status.ACTIVE)
                    .stream()
                    .map(m -> m.getUser().getUsername())
                    .toList();

            return new ChatRoomResponse(room.getId(), room.getType(), memberNames,
                    getUnreadCount(cru), room.getCreatedAt());
        }).toList();
    }

    @Transactional
    public InviteResult inviteMembers(Long inviterId, Long chatRoomId, List<Long> userIds) {
        ChatRoomUser inviterMembership = chatRoomUserRepository.findByChatRoomIdAndUserId(chatRoomId, inviterId)
                .filter(cru -> cru.getStatus() == ChatRoomUser.Status.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("채팅방의 활성 멤버가 아닙니다."));

        ChatRoom chatRoom = inviterMembership.getChatRoom();

        List<Long> invitedUserIds = new ArrayList<>();
        List<String> invitedNames = new ArrayList<>();

        for (Long userId : userIds) {
            if (userId.equals(inviterId)) {
                throw new IllegalArgumentException("자기 자신은 초대할 수 없습니다.");
            }

            if (!friendshipRepository.existsFriendship(inviterId, userId)) {
                throw new IllegalArgumentException("친구가 아닌 사용자입니다: " + userId);
            }

            User user = addOrRejoinMember(chatRoomId, userId, chatRoom);
            if (user == null) continue;

            invitedUserIds.add(userId);
            invitedNames.add(user.getUsername());
        }

        List<ChatRoomUser> allMembers = chatRoomUserRepository
                .findByChatRoomIdAndStatus(chatRoomId, ChatRoomUser.Status.ACTIVE);

        Message systemMessage = null;
        if (!invitedNames.isEmpty()) {
            String names = String.join(", ", invitedNames);
            systemMessage = messageRepository.save(
                    Message.createSystemMessage(chatRoom, names + "님이 입장하셨습니다.", allMembers.size()));
        }

        return new InviteResult(invitedUserIds, allMembers, systemMessage);
    }

    @Transactional
    public LeaveResult leaveRoom(Long userId, Long chatRoomId) {
        ChatRoomUser membership = chatRoomUserRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .filter(cru -> cru.getStatus() == ChatRoomUser.Status.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("채팅방의 활성 멤버가 아닙니다."));

        String username = membership.getUser().getUsername();
        membership.leave();

        List<ChatRoomUser> remainingMembers = chatRoomUserRepository
                .findByChatRoomIdAndStatus(chatRoomId, ChatRoomUser.Status.ACTIVE);

        Message systemMessage = null;
        if (!remainingMembers.isEmpty()) {
            ChatRoom chatRoom = membership.getChatRoom();
            systemMessage = messageRepository.save(
                    Message.createSystemMessage(chatRoom, username + "님이 퇴장하셨습니다.", remainingMembers.size()));
        }

        return new LeaveResult(systemMessage, remainingMembers);
    }

    private User addOrRejoinMember(Long chatRoomId, Long userId, ChatRoom chatRoom) {
        var existing = chatRoomUserRepository.findByChatRoomIdAndUserId(chatRoomId, userId);

        if (existing.isPresent()) {
            ChatRoomUser cru = existing.get();
            if (cru.getStatus() == ChatRoomUser.Status.ACTIVE) {
                return null;
            }
            cru.rejoin();
            return cru.getUser();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + userId));
        chatRoomUserRepository.save(ChatRoomUser.builder()
                .chatRoom(chatRoom)
                .user(user)
                .build());
        return user;
    }

    private long getUnreadCount(ChatRoomUser chatRoomUser) {
        long lastReadId = chatRoomUser.getLastReadMessageIdOrDefault();
        return messageRepository.countUnreadMessages(
                chatRoomUser.getChatRoom().getId(), lastReadId);
    }
}
