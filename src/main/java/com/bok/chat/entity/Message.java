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
    @JoinColumn(name = "sender_id")
    private User sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private int unreadCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type = MessageType.CHAT;

    @Column(nullable = false)
    private boolean edited = false;

    @Column(nullable = false)
    private boolean deleted = false;

    private Message(ChatRoom chatRoom, User sender, String content, int unreadCount, MessageType type) {
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.content = content;
        this.unreadCount = unreadCount;
        this.type = type;
    }

    public static Message create(ChatRoom chatRoom, User sender, String content, int memberCount) {
        return new Message(chatRoom, sender, content, memberCount - 1, MessageType.CHAT);
    }

    public static Message createSystem(ChatRoom chatRoom, String content, int memberCount) {
        return new Message(chatRoom, null, content, memberCount > 0 ? memberCount - 1 : 0, MessageType.SYSTEM);
    }

    public void edit(String newContent) {
        this.content = newContent;
        this.edited = true;
    }

    public void markDeleted() {
        this.deleted = true;
    }

    public enum MessageType { CHAT, SYSTEM }
}
