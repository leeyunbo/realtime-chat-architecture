package com.bok.chat.repository;

import com.bok.chat.entity.ChatRoomUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomUserRepository extends JpaRepository<ChatRoomUser, Long> {

    List<ChatRoomUser> findByUserIdAndStatus(Long userId, ChatRoomUser.Status status);

    Optional<ChatRoomUser> findByChatRoomIdAndUserId(Long chatRoomId, Long userId);

    List<ChatRoomUser> findByChatRoomIdAndStatus(Long chatRoomId, ChatRoomUser.Status status);

    int countByChatRoomIdAndStatus(Long chatRoomId, ChatRoomUser.Status status);
}
