package com.bok.chat.event;

import com.bok.chat.entity.Message;
import com.bok.chat.entity.OutboxEvent;

public record MessageDomainEvent(OutboxEvent.EventType eventType, Message message) {
}
