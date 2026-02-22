package com.bok.chat.api.controller;

import com.bok.chat.api.dto.MessageResponse;
import com.bok.chat.api.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chatrooms/{roomId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<List<MessageResponse>> getMessages(
            Authentication authentication,
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(messageService.getMessages(userId, roomId, page, size));
    }
}
