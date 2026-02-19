package com.bok.chat.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AddFriendRequest(@NotBlank String username) {
}
