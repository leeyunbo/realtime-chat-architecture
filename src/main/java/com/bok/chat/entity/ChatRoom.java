package com.bok.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomType type;

    private ChatRoom(ChatRoomType type) {
        this.type = type;
    }

    public static ChatRoom create(int memberCount) {
        ChatRoomType type = memberCount == 2 ? ChatRoomType.DIRECT : ChatRoomType.GROUP;
        return new ChatRoom(type);
    }

    public enum ChatRoomType {
        DIRECT, GROUP
    }
}
