package com.bok.chat.api.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.bok.chat.api.dto.CursorPage;
import com.bok.chat.api.dto.MessageDocument;
import com.bok.chat.api.dto.MessageResponse;
import com.bok.chat.api.dto.MessageSearchResponse;
import com.bok.chat.config.ElasticsearchIndexInitializer;
import com.bok.chat.entity.ChatRoomUser;
import com.bok.chat.entity.Message;
import com.bok.chat.repository.ChatRoomUserRepository;
import com.bok.chat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageSearchService {

    private final ElasticsearchClient esClient;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final MessageRepository messageRepository;

    private static final String INDEX = ElasticsearchIndexInitializer.MESSAGE_INDEX;

    /**
     * 채팅방 단위 검색.
     */
    @Transactional(readOnly = true)
    public MessageSearchResponse searchInRoom(Long userId, Long chatRoomId,
                                               String keyword, String cursor, int size) {
        ChatRoomUser membership = chatRoomUserRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방 멤버가 아닙니다."));

        BoolQuery.Builder bool = buildBaseQuery(keyword)
                .filter(f -> f.term(t -> t.field("chatRoomId").value(chatRoomId)))
                .filter(f -> f.range(r -> r.date(d -> d
                        .field("createdAt")
                        .gte(formatDateTime(membership.getJoinedAt())))));

        return executeSearch(bool.build(), cursor, size);
    }

    /**
     * 전체 채팅방 통합 검색.
     */
    @Transactional(readOnly = true)
    public MessageSearchResponse searchAll(Long userId, String keyword, String cursor, int size) {
        List<ChatRoomUser> memberships = chatRoomUserRepository
                .findByUserIdAndStatus(userId, ChatRoomUser.Status.ACTIVE);

        if (memberships.isEmpty()) {
            return new MessageSearchResponse(List.of(), null, false);
        }

        List<Long> roomIds = memberships.stream()
                .map(m -> m.getChatRoom().getId())
                .toList();

        BoolQuery.Builder bool = buildBaseQuery(keyword)
                .filter(f -> f.terms(t -> t
                        .field("chatRoomId")
                        .terms(tv -> tv.value(roomIds.stream()
                                .map(id -> co.elastic.clients.elasticsearch._types.FieldValue.of(id))
                                .toList()))));

        return executeSearch(bool.build(), cursor, size);
    }

    private BoolQuery.Builder buildBaseQuery(String keyword) {
        return new BoolQuery.Builder()
                .must(m -> m.multiMatch(mm -> mm
                        .query(keyword)
                        .fields("content", "originalFilename")))
                .filter(f -> f.term(t -> t.field("deleted").value(false)));
    }

    private MessageSearchResponse executeSearch(BoolQuery boolQuery, String cursor, int size) {
        try {
            SearchRequest.Builder request = new SearchRequest.Builder()
                    .index(INDEX)
                    .query(Query.of(q -> q.bool(boolQuery)))
                    .sort(s -> s.field(f -> f.field("messageId").order(SortOrder.Desc)))
                    .size(size + 1);

            Long decodedCursor = CursorPage.decodeCursor(cursor);
            if (decodedCursor != null) {
                request.searchAfter(sa -> sa.longValue(decodedCursor));
            }

            SearchResponse<MessageDocument> response = esClient.search(
                    request.build(), MessageDocument.class);

            List<Long> messageIds = response.hits().hits().stream()
                    .map(Hit::source)
                    .map(MessageDocument::messageId)
                    .toList();

            CursorPage<Long> page = CursorPage.of(new ArrayList<>(messageIds), size, id -> id);
            if (page.isEmpty()) {
                return new MessageSearchResponse(List.of(), null, false);
            }

            List<Message> messages = messageRepository.findAllByIdWithSenderAndFile(page.items());
            List<MessageResponse> responses = messages.stream()
                    .map(MessageResponse::from)
                    .toList();

            return new MessageSearchResponse(responses, page.nextCursor(), page.hasNext());
        } catch (IOException e) {
            throw new RuntimeException("Elasticsearch search failed", e);
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
