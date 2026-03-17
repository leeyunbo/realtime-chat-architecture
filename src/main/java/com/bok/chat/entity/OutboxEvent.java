package com.bok.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String aggregateType;

    @Column(nullable = false)
    private Long aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private boolean processed = false;

    @Column(nullable = false)
    private int retryCount = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    public OutboxEvent(String aggregateType, Long aggregateId, EventType eventType, String payload) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = LocalDateTime.now();
    }

    public static OutboxEvent messageCreated(Long messageId, String payload) {
        return new OutboxEvent("MESSAGE", messageId, EventType.CREATED, payload);
    }

    public static OutboxEvent messageUpdated(Long messageId, String payload) {
        return new OutboxEvent("MESSAGE", messageId, EventType.UPDATED, payload);
    }

    public static OutboxEvent messageDeleted(Long messageId, String payload) {
        return new OutboxEvent("MESSAGE", messageId, EventType.DELETED, payload);
    }

    public void markProcessed() {
        this.processed = true;
        this.processedAt = LocalDateTime.now();
    }

    public void incrementRetry() {
        this.retryCount++;
    }

    public enum EventType { CREATED, UPDATED, DELETED }
}
