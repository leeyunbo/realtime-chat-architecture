package com.bok.chat.repository;

import com.bok.chat.entity.Friendship;
import com.bok.chat.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FriendshipRepository")
class FriendshipRepositoryTest extends RepositoryTestBase {

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private EntityManager em;

    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        user1 = User.builder().username("user1").password("pass").build();
        user2 = User.builder().username("user2").password("pass").build();
        user3 = User.builder().username("user3").password("pass").build();
        em.persist(user1);
        em.persist(user2);
        em.persist(user3);

        em.persist(Friendship.create(user1, user2));
        em.flush();
    }

    @Test
    @DisplayName("정방향(user→friend)으로 친구 관계를 확인할 수 있다")
    void existsFriendship_shouldReturnTrue_forwardDirection() {
        boolean exists = friendshipRepository.existsFriendship(user1.getId(), user2.getId());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("역방향(friend→user)으로도 친구 관계를 확인할 수 있다")
    void existsFriendship_shouldReturnTrue_reverseDirection() {
        boolean exists = friendshipRepository.existsFriendship(user2.getId(), user1.getId());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("친구가 아닌 사용자는 false를 반환한다")
    void existsFriendship_shouldReturnFalse_notFriends() {
        boolean exists = friendshipRepository.existsFriendship(user1.getId(), user3.getId());

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("양방향 친구 관계를 모두 조회한다")
    void findAllByUserId_shouldReturnBothDirections() {
        em.persist(Friendship.create(user3, user1));
        em.flush();

        List<Friendship> friendships = friendshipRepository.findAllByUserId(user1.getId());

        assertThat(friendships).hasSize(2);
    }

    @Test
    @DisplayName("friend 측에서도 조회된다")
    void findAllByUserId_shouldReturnWhenUserIsOnFriendSide() {
        List<Friendship> friendships = friendshipRepository.findAllByUserId(user2.getId());

        assertThat(friendships).hasSize(1);
    }
}
