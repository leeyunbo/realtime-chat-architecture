package com.bok.chat.repository;

import com.bok.chat.entity.ChatRoomUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public interface ChatRoomUserRepository extends JpaRepository<ChatRoomUser, Long> {

    List<ChatRoomUser> findByUserIdAndStatus(Long userId, ChatRoomUser.Status status);

    Optional<ChatRoomUser> findByChatRoomIdAndUserId(Long chatRoomId, Long userId);

    List<ChatRoomUser> findByChatRoomIdAndStatus(Long chatRoomId, ChatRoomUser.Status status);

    int countByChatRoomIdAndStatus(Long chatRoomId, ChatRoomUser.Status status);

    @Query("SELECT m.id, COUNT(cru) FROM Message m " +
            "LEFT JOIN ChatRoomUser cru ON cru.chatRoom.id = m.chatRoom.id " +
            "AND cru.status = 'ACTIVE' AND cru.lastReadMessageId < m.id " +
            "WHERE m.id IN :messageIds GROUP BY m.id")
    List<Object[]> countUnreadPerMessageRaw(@Param("messageIds") List<Long> messageIds);

    default Map<Long, Long> countUnreadPerMessage(List<Long> messageIds) {
        return countUnreadPerMessageRaw(messageIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }
}
