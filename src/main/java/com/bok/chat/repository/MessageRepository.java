package com.bok.chat.repository;

import com.bok.chat.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId, Pageable pageable);

    @Modifying
    @Query("UPDATE Message m SET m.unreadCount = m.unreadCount - 1 " +
            "WHERE m.chatRoom.id = :chatRoomId AND m.id > :lastReadMessageId AND m.unreadCount > 0")
    int bulkDecrementUnreadCount(@Param("chatRoomId") Long chatRoomId,
                                 @Param("lastReadMessageId") Long lastReadMessageId);

    @Query("SELECT MAX(m.id) FROM Message m WHERE m.chatRoom.id = :chatRoomId")
    Optional<Long> findMaxIdByChatRoomId(@Param("chatRoomId") Long chatRoomId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatRoom.id = :chatRoomId AND m.id > :lastReadMessageId")
    long countUnreadMessages(@Param("chatRoomId") Long chatRoomId,
                             @Param("lastReadMessageId") Long lastReadMessageId);
}
