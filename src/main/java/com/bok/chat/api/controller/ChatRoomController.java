package com.bok.chat.api.controller;

import com.bok.chat.api.dto.ChatRoomResponse;
import com.bok.chat.api.dto.CreateChatRoomRequest;
import com.bok.chat.api.service.ChatRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chatrooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @PostMapping
    public ResponseEntity<ChatRoomResponse> create(
            Authentication authentication,
            @RequestBody @Valid CreateChatRoomRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(chatRoomService.create(userId, request));
    }

    @GetMapping
    public ResponseEntity<List<ChatRoomResponse>> getMyChatRooms(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(chatRoomService.getMyChatRooms(userId));
    }
}
