package com.bok.chat.repository;

import com.bok.chat.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId, Pageable pageable);

    @Query("SELECT m FROM Message m LEFT JOIN FETCH m.sender " +
            "WHERE m.chatRoom.id = :chatRoomId AND m.createdAt >= :joinedAt " +
            "ORDER BY m.createdAt DESC")
    List<Message> findByChatRoomIdAndCreatedAtAfter(@Param("chatRoomId") Long chatRoomId,
                                                     @Param("joinedAt") LocalDateTime joinedAt,
                                                     Pageable pageable);

    @Modifying
    @Query("UPDATE Message m SET m.unreadCount = m.unreadCount - 1 " +
            "WHERE m.chatRoom.id = :chatRoomId AND m.id > :lastReadMessageId AND m.unreadCount > 0")
    int decrementUnreadCountAfter(@Param("chatRoomId") Long chatRoomId,
                                  @Param("lastReadMessageId") Long lastReadMessageId);

    @Query("SELECT MAX(m.id) FROM Message m WHERE m.chatRoom.id = :chatRoomId")
    Optional<Long> findLatestMessageIdByChatRoomId(@Param("chatRoomId") Long chatRoomId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatRoom.id = :chatRoomId AND m.id > :lastReadMessageId")
    long countUnreadMessages(@Param("chatRoomId") Long chatRoomId,
                             @Param("lastReadMessageId") Long lastReadMessageId);

    @Query("SELECT m FROM Message m LEFT JOIN FETCH m.sender " +
            "WHERE m.chatRoom.id = :chatRoomId AND m.id > :lastReadMessageId ORDER BY m.id ASC")
    List<Message> findUnreadMessages(@Param("chatRoomId") Long chatRoomId,
                                     @Param("lastReadMessageId") Long lastReadMessageId);
}
