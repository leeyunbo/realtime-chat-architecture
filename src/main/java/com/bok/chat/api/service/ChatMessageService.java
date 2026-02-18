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
    public record ReadResult(Message message, List<ChatRoomUser> members) {}

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

    @Transactional
    public ReadResult readMessage(Long userId, Long messageId) {
        messageRepository.decrementUnreadCount(messageId);

        Message msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("메시지가 존재하지 않습니다."));

        ChatRoomUser chatRoomUser = chatRoomUserRepository
                .findByChatRoomIdAndUserId(msg.getChatRoom().getId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방 멤버가 아닙니다."));
        chatRoomUser.updateLastReadMessageId(msg.getId());

        List<ChatRoomUser> members = chatRoomUserRepository
                .findByChatRoomIdAndStatus(msg.getChatRoom().getId(), ChatRoomUser.Status.ACTIVE);

        return new ReadResult(msg, members);
    }
}
