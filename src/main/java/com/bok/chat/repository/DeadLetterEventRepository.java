package com.bok.chat.repository;

import com.bok.chat.entity.DeadLetterEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeadLetterEventRepository extends JpaRepository<DeadLetterEvent, Long> {
}
