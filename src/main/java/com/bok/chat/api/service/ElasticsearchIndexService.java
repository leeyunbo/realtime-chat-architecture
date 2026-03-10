package com.bok.chat.api.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.bok.chat.api.dto.MessageDocument;
import com.bok.chat.config.ElasticsearchIndexInitializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.StringReader;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchIndexService {

    private final ElasticsearchClient esClient;
    private final ObjectMapper objectMapper;

    private static final String INDEX = ElasticsearchIndexInitializer.MESSAGE_INDEX;

    public void index(String payload) {
        try {
            MessageDocument doc = objectMapper.readValue(payload, MessageDocument.class);
            esClient.index(i -> i
                    .index(INDEX)
                    .id(String.valueOf(doc.messageId()))
                    .withJson(new StringReader(payload)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to index message to ES", e);
        }
    }

    public void update(String payload) {
        try {
            MessageDocument doc = objectMapper.readValue(payload, MessageDocument.class);
            esClient.index(i -> i
                    .index(INDEX)
                    .id(String.valueOf(doc.messageId()))
                    .withJson(new StringReader(payload)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to update message in ES", e);
        }
    }

    public void delete(String payload) {
        try {
            MessageDocument doc = objectMapper.readValue(payload, MessageDocument.class);
            // soft delete: update the document with deleted=true
            esClient.index(i -> i
                    .index(INDEX)
                    .id(String.valueOf(doc.messageId()))
                    .withJson(new StringReader(payload)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete message from ES", e);
        }
    }
}
