package com.bok.chat.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchIndexInitializer {

    public static final String MESSAGE_INDEX = "messages";
    private static final String INDEX_SETTINGS_PATH = "elasticsearch/messages-index.json";

    private final ElasticsearchClient esClient;

    @EventListener(ApplicationReadyEvent.class)
    public void initIndex() {
        try {
            boolean exists = esClient.indices().exists(
                    ExistsRequest.of(e -> e.index(MESSAGE_INDEX))).value();

            if (!exists) {
                InputStream settingsStream = new ClassPathResource(INDEX_SETTINGS_PATH).getInputStream();
                esClient.indices().create(CreateIndexRequest.of(c -> c
                        .index(MESSAGE_INDEX)
                        .withJson(settingsStream)));
                log.info("Elasticsearch index '{}' created", MESSAGE_INDEX);
            }
        } catch (Exception e) {
            log.error("Failed to initialize Elasticsearch index: {}", e.getMessage(), e);
        }
    }
}
