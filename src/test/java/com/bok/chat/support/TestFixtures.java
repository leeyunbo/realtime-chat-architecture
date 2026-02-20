package com.bok.chat.support;

import com.bok.chat.entity.*;
import org.springframework.test.util.ReflectionTestUtils;

public class TestFixtures {

    public static User createUser(Long id, String username) {
        User user = User.builder()
                .username(username)
                .password("encoded-password")
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    public static ChatRoom createChatRoom(Long id, int memberCount) {
        ChatRoom chatRoom = ChatRoom.create(memberCount);
        ReflectionTestUtils.setField(chatRoom, "id", id);
        return chatRoom;
    }

    public static ChatRoomUser createChatRoomUser(Long id, ChatRoom chatRoom, User user) {
        ChatRoomUser chatRoomUser = ChatRoomUser.builder()
                .chatRoom(chatRoom)
                .user(user)
                .build();
        ReflectionTestUtils.setField(chatRoomUser, "id", id);
        return chatRoomUser;
    }

    public static Message createMessage(Long id, ChatRoom chatRoom, User sender, String content, int memberCount) {
        Message message = Message.create(chatRoom, sender, content, memberCount);
        ReflectionTestUtils.setField(message, "id", id);
        return message;
    }

    public static Friendship createFriendship(Long id, User user, User friend) {
        Friendship friendship = Friendship.create(user, friend);
        ReflectionTestUtils.setField(friendship, "id", id);
        return friendship;
    }
}
