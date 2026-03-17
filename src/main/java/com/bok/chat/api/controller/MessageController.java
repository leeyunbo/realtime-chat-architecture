package com.bok.chat.api.controller;

import com.bok.chat.api.dto.MessageResponse;
import com.bok.chat.api.dto.MessageSearchResponse;
import com.bok.chat.api.service.MessageSearchService;
import com.bok.chat.api.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final MessageSearchService messageSearchService;

    @GetMapping("/chatrooms/{roomId}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(
            Authentication authentication,
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(messageService.getMessages(userId, roomId, page, size));
    }

    @GetMapping("/chatrooms/{roomId}/messages/search")
    public ResponseEntity<MessageSearchResponse> searchInRoom(
            Authentication authentication,
            @PathVariable Long roomId,
            @RequestParam("q") String query,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(messageSearchService.searchInRoom(userId, roomId, query, cursor, size));
    }

    @GetMapping("/messages/search")
    public ResponseEntity<MessageSearchResponse> searchAll(
            Authentication authentication,
            @RequestParam("q") String query,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(messageSearchService.searchAll(userId, query, cursor, size));
    }
}
