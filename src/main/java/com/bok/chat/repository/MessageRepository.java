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

    @Query("SELECT m FROM Message m LEFT JOIN FETCH m.sender LEFT JOIN FETCH m.file " +
            "WHERE m.chatRoom.id = :chatRoomId AND m.createdAt >= :joinedAt " +
            "ORDER BY m.createdAt DESC")
    List<Message> findByChatRoomIdAndCreatedAtAfter(@Param("chatRoomId") Long chatRoomId,
                                                     @Param("joinedAt") LocalDateTime joinedAt,
                                                     Pageable pageable);

    @Query("SELECT MAX(m.id) FROM Message m WHERE m.chatRoom.id = :chatRoomId")
    Optional<Long> findLatestMessageIdByChatRoomId(@Param("chatRoomId") Long chatRoomId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatRoom.id = :chatRoomId AND m.id > :lastReadMessageId")
    long countUnreadMessages(@Param("chatRoomId") Long chatRoomId,
                             @Param("lastReadMessageId") Long lastReadMessageId);

    @Query("SELECT m FROM Message m LEFT JOIN FETCH m.sender LEFT JOIN FETCH m.file " +
            "WHERE m.chatRoom.id = :chatRoomId AND m.id > :lastReadMessageId ORDER BY m.id ASC")
    List<Message> findUnreadMessages(@Param("chatRoomId") Long chatRoomId,
                                     @Param("lastReadMessageId") Long lastReadMessageId);

    @Query(value = "SELECT m.id FROM messages m " +
            "WHERE m.chatroom_id = :chatRoomId " +
            "AND m.created_at >= :joinedAt " +
            "AND m.deleted = false " +
            "AND m.content_tsv @@ to_tsquery('simple', :query) " +
            "AND (:cursor IS NULL OR m.id < :cursor) " +
            "ORDER BY m.id DESC " +
            "LIMIT :limit",
            nativeQuery = true)
    List<Long> searchMessageIds(@Param("chatRoomId") Long chatRoomId,
                                @Param("joinedAt") LocalDateTime joinedAt,
                                @Param("query") String query,
                                @Param("cursor") Long cursor,
                                @Param("limit") int limit);

    @Query("SELECT m FROM Message m LEFT JOIN FETCH m.sender LEFT JOIN FETCH m.file " +
            "WHERE m.id IN :ids ORDER BY m.id DESC")
    List<Message> findAllByIdWithSenderAndFile(@Param("ids") List<Long> ids);

}
