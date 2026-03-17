package com.bok.chat.api.service;

import com.bok.chat.api.dto.MessageDocument;
import com.bok.chat.entity.Message;
import com.bok.chat.entity.OutboxEvent;
import com.bok.chat.event.MessageDomainEvent;
import com.bok.chat.event.OutboxEventCreatedEvent;
import com.bok.chat.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @EventListener
    public void handle(MessageDomainEvent domainEvent) {
        Message message = domainEvent.message();
        String payload = toJson(MessageDocument.from(message));

        OutboxEvent outboxEvent = switch (domainEvent.eventType()) {
            case CREATED -> OutboxEvent.messageCreated(message.getId(), payload);
            case UPDATED -> OutboxEvent.messageUpdated(message.getId(), payload);
            case DELETED -> OutboxEvent.messageDeleted(message.getId(), payload);
        };

        outboxEventRepository.save(outboxEvent);
        eventPublisher.publishEvent(new OutboxEventCreatedEvent(outboxEvent.getId()));
    }

    private String toJson(MessageDocument document) {
        try {
            return objectMapper.writeValueAsString(document);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize MessageDocument", e);
        }
    }
}
