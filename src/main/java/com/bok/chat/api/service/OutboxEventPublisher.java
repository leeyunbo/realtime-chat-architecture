package com.bok.chat.api.service;

import com.bok.chat.api.dto.MessageDocument;
import com.bok.chat.entity.Message;
import com.bok.chat.entity.OutboxEvent;
import com.bok.chat.event.OutboxEventCreatedEvent;
import com.bok.chat.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public void publishMessageCreated(Message message) {
        OutboxEvent event = OutboxEvent.messageCreated(message.getId(), toJson(MessageDocument.from(message)));
        outboxEventRepository.save(event);
        eventPublisher.publishEvent(new OutboxEventCreatedEvent(event.getId()));
    }

    public void publishMessageUpdated(Message message) {
        OutboxEvent event = OutboxEvent.messageUpdated(message.getId(), toJson(MessageDocument.from(message)));
        outboxEventRepository.save(event);
        eventPublisher.publishEvent(new OutboxEventCreatedEvent(event.getId()));
    }

    public void publishMessageDeleted(Message message) {
        OutboxEvent event = OutboxEvent.messageDeleted(message.getId(), toJson(MessageDocument.from(message)));
        outboxEventRepository.save(event);
        eventPublisher.publishEvent(new OutboxEventCreatedEvent(event.getId()));
    }

    private String toJson(MessageDocument document) {
        try {
            return objectMapper.writeValueAsString(document);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize MessageDocument", e);
        }
    }
}
