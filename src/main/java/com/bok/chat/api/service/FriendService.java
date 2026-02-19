package com.bok.chat.api.service;

import com.bok.chat.api.dto.FriendResponse;
import com.bok.chat.entity.Friendship;
import com.bok.chat.entity.User;
import com.bok.chat.redis.OnlineStatusService;
import com.bok.chat.repository.FriendshipRepository;
import com.bok.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final OnlineStatusService onlineStatusService;

    @Transactional
    public void addFriend(Long userId, String friendUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));

        User friend = userRepository.findByUsername(friendUsername)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + friendUsername));

        if (user.getId().equals(friend.getId())) {
            throw new IllegalArgumentException("자기 자신을 친구로 추가할 수 없습니다.");
        }

        if (friendshipRepository.existsFriendship(userId, friend.getId())) {
            throw new IllegalArgumentException("이미 친구입니다.");
        }

        friendshipRepository.save(Friendship.create(user, friend));
    }

    public List<FriendResponse> getFriends(Long userId) {
        return friendshipRepository.findAllByUserId(userId).stream()
                .map(f -> {
                    User friendUser = f.getUser().getId().equals(userId) ? f.getFriend() : f.getUser();
                    return new FriendResponse(friendUser.getId(), friendUser.getUsername(),
                            onlineStatusService.isOnline(friendUser.getId()));
                })
                .toList();
    }

    public List<Long> getFriendIds(Long userId) {
        return friendshipRepository.findAllByUserId(userId).stream()
                .map(f -> f.getUser().getId().equals(userId) ? f.getFriend().getId() : f.getUser().getId())
                .toList();
    }

    public boolean areFriends(Long userId, Long friendId) {
        return friendshipRepository.existsFriendship(userId, friendId);
    }
}
