package com.bok.chat.api.controller;

import com.bok.chat.api.dto.AddFriendRequest;
import com.bok.chat.api.dto.FriendResponse;
import com.bok.chat.api.service.FriendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @PostMapping
    public ResponseEntity<Void> addFriend(Authentication auth, @Valid @RequestBody AddFriendRequest request) {
        Long userId = (Long) auth.getPrincipal();
        friendService.addFriend(userId, request.username());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<FriendResponse>> getFriends(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(friendService.getFriends(userId));
    }
}
