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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private FileAttachment file;

    private Message(ChatRoom chatRoom, User sender, String content, int unreadCount,
                    MessageType type, FileAttachment file) {
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.content = content;
        this.unreadCount = unreadCount;
        this.type = type;
        this.file = file;
    }

    public static Message create(ChatRoom chatRoom, User sender, String content, int memberCount) {
        return new Message(chatRoom, sender, content, memberCount - 1, MessageType.CHAT, null);
    }

    public static Message createSystemMessage(ChatRoom chatRoom, String content, int memberCount) {
        return new Message(chatRoom, null, content, memberCount > 0 ? memberCount - 1 : 0, MessageType.SYSTEM, null);
    }

    public static Message createFileMessage(ChatRoom chatRoom, User sender, FileAttachment file, int memberCount) {
        return new Message(chatRoom, sender, file.getOriginalFilename(), memberCount - 1, MessageType.FILE, file);
    }

    public void edit(Long requestUserId, String newContent) {
        validateOwnership(requestUserId);
        if (this.deleted) {
            throw new IllegalArgumentException("삭제된 메시지는 수정할 수 없습니다.");
        }
        this.content = newContent;
        this.edited = true;
    }

    public void markDeleted(Long requestUserId) {
        validateOwnership(requestUserId);
        this.deleted = true;
    }

    private void validateOwnership(Long requestUserId) {
        if (this.sender == null || !this.sender.getId().equals(requestUserId)) {
            throw new IllegalArgumentException("본인의 메시지만 수정/삭제할 수 있습니다.");
        }
    }

    public enum MessageType { CHAT, SYSTEM, FILE }
}
