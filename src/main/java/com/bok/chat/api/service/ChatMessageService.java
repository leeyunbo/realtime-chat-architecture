package com.bok.chat.api.service;

import com.bok.chat.entity.ChatRoomUser;
import com.bok.chat.entity.Message;
import com.bok.chat.entity.User;
import com.bok.chat.repository.ChatRoomRepository;
import com.bok.chat.repository.ChatRoomUserRepository;
import com.bok.chat.repository.MessageRepository;
import com.bok.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final UserRepository userRepository;

    public record SendResult(Message message, User sender, List<ChatRoomUser> members) {}
    public record UndeliveredMessages(Long chatRoomId, List<Message> messages) {}
    public record EditResult(Message message, List<ChatRoomUser> members) {}
    public record DeleteResult(Message message, List<ChatRoomUser> members) {}
    public record BulkReadResult(boolean success, Long chatRoomId, Long readByUserId,
                                 Long lastReadMessageId, List<ChatRoomUser> members) {

        public static BulkReadResult nothingToRead() {
            return new BulkReadResult(false, null, null, null, List.of());
        }
    }

    @Transactional(readOnly = true)
    public List<UndeliveredMessages> getUndeliveredMessages(Long userId) {
        List<ChatRoomUser> rooms = chatRoomUserRepository.findByUserIdAndStatus(userId, ChatRoomUser.Status.ACTIVE);
        return rooms.stream()
                .map(room -> {
                    long lastRead = room.getLastReadMessageIdOrDefault();
                    List<Message> messages = messageRepository.findUnreadMessages(
                            room.getChatRoom().getId(), lastRead);
                    return new UndeliveredMessages(room.getChatRoom().getId(), messages);
                })
                .filter(um -> !um.messages().isEmpty())
                .toList();
    }

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
     */
    @Transactional
    public BulkReadResult readMessages(Long userId, Long chatRoomId) {
        ChatRoomUser chatRoomUser = chatRoomUserRepository
                .findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방 멤버가 아닙니다."));

        long prevLastRead = chatRoomUser.getLastReadMessageIdOrDefault();

        Long latestMessageId = messageRepository.findLatestMessageIdByChatRoomId(chatRoomId)
                .orElse(null);

        if (latestMessageId == null || latestMessageId.equals(prevLastRead)) {
            return BulkReadResult.nothingToRead();
        }

        if (latestMessageId < prevLastRead) {
            log.warn("데이터 정합성 문제: chatRoomId={}, latestMessageId={}, prevLastRead={}",
                    chatRoomId, latestMessageId, prevLastRead);
            return BulkReadResult.nothingToRead();
        }

        messageRepository.decrementUnreadCountAfter(chatRoomId, prevLastRead);
        chatRoomUser.updateLastReadMessageId(latestMessageId);

        List<ChatRoomUser> members = chatRoomUserRepository
                .findByChatRoomIdAndStatus(chatRoomId, ChatRoomUser.Status.ACTIVE);

        return new BulkReadResult(true, chatRoomId, userId, latestMessageId, members);
    }

    @Transactional
    public EditResult editMessage(Long userId, Long messageId, String newContent) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("메시지가 존재하지 않습니다."));

        if (message.getSender() == null || !message.getSender().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 메시지만 수정할 수 있습니다.");
        }

        if (message.isDeleted()) {
            throw new IllegalArgumentException("삭제된 메시지는 수정할 수 없습니다.");
        }

        message.edit(newContent);

        List<ChatRoomUser> members = chatRoomUserRepository
                .findByChatRoomIdAndStatus(message.getChatRoom().getId(), ChatRoomUser.Status.ACTIVE);

        return new EditResult(message, members);
    }

    @Transactional
    public DeleteResult deleteMessage(Long userId, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("메시지가 존재하지 않습니다."));

        if (message.getSender() == null || !message.getSender().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 메시지만 삭제할 수 있습니다.");
        }

        message.markDeleted();

        List<ChatRoomUser> members = chatRoomUserRepository
                .findByChatRoomIdAndStatus(message.getChatRoom().getId(), ChatRoomUser.Status.ACTIVE);

        return new DeleteResult(message, members);
    }

    public Message createSystemMessage(Long chatRoomId, String content) {
        var chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방이 존재하지 않습니다."));

        int activeCount = chatRoomUserRepository.countByChatRoomIdAndStatus(chatRoomId, ChatRoomUser.Status.ACTIVE);

        return messageRepository.save(Message.createSystem(chatRoom, content, activeCount));
    }
}
