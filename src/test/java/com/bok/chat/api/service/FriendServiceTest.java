package com.bok.chat.api.service;

import com.bok.chat.api.dto.FriendResponse;
import com.bok.chat.entity.Friendship;
import com.bok.chat.entity.User;
import com.bok.chat.redis.OnlineStatusService;
import com.bok.chat.repository.FriendshipRepository;
import com.bok.chat.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.bok.chat.support.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@DisplayName("FriendService")
@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    @InjectMocks
    private FriendService friendService;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OnlineStatusService onlineStatusService;

    @Test
    @DisplayName("친구 추가 성공 시 Friendship이 저장된다")
    void addFriend_shouldSaveFriendship() {
        User user = createUser(1L, "user1");
        User friend = createUser(2L, "user2");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.findByUsername("user2")).willReturn(Optional.of(friend));
        given(friendshipRepository.existsFriendship(1L, 2L)).willReturn(false);

        friendService.addFriend(1L, "user2");

        verify(friendshipRepository).save(any(Friendship.class));
    }

    @Test
    @DisplayName("자기 자신을 친구로 추가하면 예외가 발생한다")
    void addFriend_self_shouldThrow() {
        User user = createUser(1L, "user1");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.findByUsername("user1")).willReturn(Optional.of(user));

        assertThatThrownBy(() -> friendService.addFriend(1L, "user1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("자기 자신");
    }

    @Test
    @DisplayName("이미 친구인 사용자를 추가하면 예외가 발생한다")
    void addFriend_alreadyFriends_shouldThrow() {
        User user = createUser(1L, "user1");
        User friend = createUser(2L, "user2");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.findByUsername("user2")).willReturn(Optional.of(friend));
        given(friendshipRepository.existsFriendship(1L, 2L)).willReturn(true);

        assertThatThrownBy(() -> friendService.addFriend(1L, "user2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 친구");
    }

    @Test
    @DisplayName("존재하지 않는 사용자가 친구를 추가하면 예외가 발생한다")
    void addFriend_nonExistentUser_shouldThrow() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> friendService.addFriend(1L, "nobody"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용자가 존재하지 않습니다");
    }

    @Test
    @DisplayName("존재하지 않는 대상을 친구로 추가하면 예외가 발생한다")
    void addFriend_nonExistentFriend_shouldThrow() {
        User user = createUser(1L, "user1");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.findByUsername("nobody")).willReturn(Optional.empty());

        assertThatThrownBy(() -> friendService.addFriend(1L, "nobody"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 사용자");
    }

    @Test
    @DisplayName("친구 목록 조회 시 온라인 상태가 포함된다")
    void getFriends_shouldReturnFriendList() {
        User user = createUser(1L, "user1");
        User friend = createUser(2L, "user2");
        Friendship friendship = createFriendship(1L, user, friend);

        given(friendshipRepository.findAllByUserId(1L)).willReturn(List.of(friendship));
        given(onlineStatusService.isOnline(2L)).willReturn(true);

        List<FriendResponse> friends = friendService.getFriends(1L);

        assertThat(friends).hasSize(1);
        assertThat(friends.get(0).id()).isEqualTo(2L);
        assertThat(friends.get(0).username()).isEqualTo("user2");
        assertThat(friends.get(0).online()).isTrue();
    }

    @Test
    @DisplayName("친구 ID 목록을 반환한다")
    void getFriendIds_shouldReturnIds() {
        User user = createUser(1L, "user1");
        User friend = createUser(2L, "user2");
        Friendship friendship = createFriendship(1L, user, friend);

        given(friendshipRepository.findAllByUserId(1L)).willReturn(List.of(friendship));

        List<Long> friendIds = friendService.getFriendIds(1L);

        assertThat(friendIds).containsExactly(2L);
    }

    @Test
    @DisplayName("친구 관계 여부를 확인한다")
    void areFriends_shouldDelegateToRepository() {
        given(friendshipRepository.existsFriendship(1L, 2L)).willReturn(true);

        assertThat(friendService.areFriends(1L, 2L)).isTrue();
    }
}
