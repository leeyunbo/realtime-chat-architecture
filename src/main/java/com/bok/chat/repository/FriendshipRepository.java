package com.bok.chat.repository;

import com.bok.chat.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("SELECT f FROM Friendship f WHERE f.user.id = :userId OR f.friend.id = :userId")
    List<Friendship> findAllByUserId(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f " +
            "WHERE (f.user.id = :userId AND f.friend.id = :friendId) " +
            "OR (f.user.id = :friendId AND f.friend.id = :userId)")
    boolean existsFriendship(@Param("userId") Long userId, @Param("friendId") Long friendId);
}
