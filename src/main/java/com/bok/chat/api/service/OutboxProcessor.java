package com.bok.chat.api.service;

import com.bok.chat.entity.OutboxEvent;
import com.bok.chat.event.OutboxEventCreatedEvent;
import com.bok.chat.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxProcessor {

    private static final int MAX_RETRY = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final ElasticsearchIndexService esIndexService;

    /**
     * 즉시 처리: 트랜잭션 커밋 후 비동기로 ES 인덱싱 시도.
     * 실패해도 폴링이 잡아줌.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOutboxCreated(OutboxEventCreatedEvent event) {
        outboxEventRepository.findById(event.outboxEventId()).ifPresent(this::processEvent);
    }

    /**
     * 폴링 안전망: 5초마다 미처리 이벤트를 재시도.
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void pollUnprocessedEvents() {
        List<OutboxEvent> events = outboxEventRepository
                .findByProcessedFalseAndRetryCountLessThanOrderByCreatedAtAsc(MAX_RETRY);

        for (OutboxEvent event : events) {
            processEvent(event);
        }
    }

    private void processEvent(OutboxEvent event) {
        try {
            switch (event.getEventType()) {
                case CREATED -> esIndexService.index(event.getPayload());
                case UPDATED -> esIndexService.update(event.getPayload());
                case DELETED -> esIndexService.delete(event.getPayload());
            }
            event.markProcessed();
        } catch (Exception e) {
            event.incrementRetry();
            log.warn("Failed to process outbox event {} (retry {}): {}",
                    event.getId(), event.getRetryCount(), e.getMessage());
        }
    }
}
