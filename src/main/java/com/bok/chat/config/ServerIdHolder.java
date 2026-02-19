package com.bok.chat.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@Getter
public class ServerIdHolder {

    private String serverId;

    @PostConstruct
    public void init() {
        this.serverId = UUID.randomUUID().toString().substring(0, 8);
        log.info("Server ID generated: {}", serverId);
    }
}
