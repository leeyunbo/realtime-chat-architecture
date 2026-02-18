package com.bok.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatroom_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private int unreadCount;

    private Message(ChatRoom chatRoom, User sender, String content, int unreadCount) {
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.content = content;
        this.unreadCount = unreadCount;
    }

    public static Message create(ChatRoom chatRoom, User sender, String content, int memberCount) {
        return new Message(chatRoom, sender, content, memberCount - 1);
    }
}
