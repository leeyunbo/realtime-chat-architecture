package com.bok.chat.repository;

import com.bok.chat.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId, Pageable pageable);

    @Modifying
    @Query("UPDATE Message m SET m.unreadCount = m.unreadCount - 1 WHERE m.id = :id AND m.unreadCount > 0")
    int decrementUnreadCount(@Param("id") Long id);
}
