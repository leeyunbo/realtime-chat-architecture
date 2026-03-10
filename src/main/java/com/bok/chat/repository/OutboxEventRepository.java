package com.bok.chat.repository;

import com.bok.chat.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByProcessedFalseAndRetryCountLessThanOrderByCreatedAtAsc(int maxRetry);
}
