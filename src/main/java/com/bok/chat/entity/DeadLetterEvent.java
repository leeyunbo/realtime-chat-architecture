package com.bok.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "dead_letter_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeadLetterEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long outboxEventId;

    @Column(nullable = false)
    private String aggregateType;

    @Column(nullable = false)
    private Long aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxEvent.EventType eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public static DeadLetterEvent from(OutboxEvent outboxEvent) {
        DeadLetterEvent dlq = new DeadLetterEvent();
        dlq.outboxEventId = outboxEvent.getId();
        dlq.aggregateType = outboxEvent.getAggregateType();
        dlq.aggregateId = outboxEvent.getAggregateId();
        dlq.eventType = outboxEvent.getEventType();
        dlq.payload = outboxEvent.getPayload();
        return dlq;
    }
}
