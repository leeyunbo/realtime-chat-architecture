package com.bok.chat.api.service;

import com.bok.chat.entity.ChatRoomUser;
import com.bok.chat.entity.Message;
import com.bok.chat.entity.User;
import com.bok.chat.repository.ChatRoomRepository;
import com.bok.chat.repository.ChatRoomUserRepository;
import com.bok.chat.repository.MessageRepository;
import com.bok.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final UserRepository userRepository;

    public record SendResult(Message message, User sender, List<ChatRoomUser> members) {}
    public record BulkReadResult(Long chatRoomId, Long readByUserId, Long lastReadMessageId,
                                 List<ChatRoomUser> members) {}

    @Transactional
    public SendResult sendMessage(Long senderId, Long chatRoomId, String content) {
        var chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방이 존재하지 않습니다."));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));

        List<ChatRoomUser> members = chatRoomUserRepository
                .findByChatRoomIdAndStatus(chatRoom.getId(), ChatRoomUser.Status.ACTIVE);

        Message saved = messageRepository.save(
                Message.create(chatRoom, sender, content, members.size()));

        return new SendResult(saved, sender, members);
    }

    /**
     * 채팅방 진입 시 일괄 읽음 처리.
     * lastReadMessageId 이후의 모든 메시지 unreadCount를 1 차감하고,
     * lastReadMessageId를 최신 메시지 ID로 갱신한다.
     *
     * @return 읽을 메시지가 없으면 null
     */
    @Transactional
    public BulkReadResult readMessages(Long userId, Long chatRoomId) {
        ChatRoomUser chatRoomUser = chatRoomUserRepository
                .findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방 멤버가 아닙니다."));

        Long prevLastRead = chatRoomUser.getLastReadMessageId() != null
                ? chatRoomUser.getLastReadMessageId() : 0L;

        Long latestMessageId = messageRepository.findMaxIdByChatRoomId(chatRoomId).orElse(null);
        if (latestMessageId == null || latestMessageId <= prevLastRead) {
            return null;
        }

        messageRepository.bulkDecrementUnreadCount(chatRoomId, prevLastRead);
        chatRoomUser.updateLastReadMessageId(latestMessageId);

        List<ChatRoomUser> members = chatRoomUserRepository
                .findByChatRoomIdAndStatus(chatRoomId, ChatRoomUser.Status.ACTIVE);

        return new BulkReadResult(chatRoomId, userId, latestMessageId, members);
    }
}
