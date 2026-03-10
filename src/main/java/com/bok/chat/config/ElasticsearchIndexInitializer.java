package com.bok.chat.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.StringReader;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchIndexInitializer {

    public static final String MESSAGE_INDEX = "messages";

    private final ElasticsearchClient esClient;

    @EventListener(ApplicationReadyEvent.class)
    public void initIndex() {
        try {
            boolean exists = esClient.indices().exists(
                    ExistsRequest.of(e -> e.index(MESSAGE_INDEX))).value();

            if (!exists) {
                esClient.indices().create(CreateIndexRequest.of(c -> c
                        .index(MESSAGE_INDEX)
                        .withJson(new StringReader(INDEX_SETTINGS))));
                log.info("Elasticsearch index '{}' created", MESSAGE_INDEX);
            }
        } catch (Exception e) {
            log.warn("Failed to initialize Elasticsearch index: {}", e.getMessage());
        }
    }

    private static final String INDEX_SETTINGS = """
            {
              "settings": {
                "analysis": {
                  "analyzer": {
                    "nori_analyzer": {
                      "type": "custom",
                      "tokenizer": "nori_tokenizer",
                      "filter": ["lowercase", "nori_part_of_speech"]
                    }
                  },
                  "filter": {
                    "nori_part_of_speech": {
                      "type": "nori_part_of_speech",
                      "stoptags": ["E", "J", "SC", "SE", "SF", "SP", "SSC", "SSO", "SY", "VCN", "VCP", "VSV", "VX", "XPN", "XSA", "XSN", "XSV"]
                    }
                  }
                }
              },
              "mappings": {
                "properties": {
                  "messageId": { "type": "long" },
                  "chatRoomId": { "type": "long" },
                  "senderId": { "type": "long" },
                  "senderName": { "type": "keyword" },
                  "content": {
                    "type": "text",
                    "analyzer": "nori_analyzer"
                  },
                  "originalFilename": {
                    "type": "text",
                    "analyzer": "nori_analyzer"
                  },
                  "messageType": { "type": "keyword" },
                  "deleted": { "type": "boolean" },
                  "createdAt": { "type": "date" }
                }
              }
            }
            """;
}
