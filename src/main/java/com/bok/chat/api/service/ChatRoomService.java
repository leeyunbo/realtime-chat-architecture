package com.bok.chat.api.service;

import com.bok.chat.api.dto.ChatRoomResponse;
import com.bok.chat.api.dto.CreateChatRoomRequest;
import com.bok.chat.entity.ChatRoom;
import com.bok.chat.entity.ChatRoomUser;
import com.bok.chat.entity.User;
import com.bok.chat.repository.ChatRoomRepository;
import com.bok.chat.repository.ChatRoomUserRepository;
import com.bok.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatRoomResponse create(Long currentUserId, CreateChatRoomRequest request) {
        List<Long> allUserIds = new ArrayList<>(request.userIds().stream().distinct().toList());
        allUserIds.add(currentUserId);

        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.create(allUserIds.size()));

        List<String> memberNames = new ArrayList<>();
        for (Long userId : allUserIds) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + userId));
            chatRoomUserRepository.save(ChatRoomUser.builder()
                    .chatRoom(chatRoom)
                    .user(user)
                    .build());
            memberNames.add(user.getUsername());
        }

        return new ChatRoomResponse(chatRoom.getId(), chatRoom.getType(), memberNames, chatRoom.getCreatedAt());
    }

    public List<ChatRoomResponse> getMyChatRooms(Long userId) {
        List<ChatRoomUser> myRooms = chatRoomUserRepository.findByUserIdAndStatus(userId, ChatRoomUser.Status.ACTIVE);

        return myRooms.stream().map(cru -> {
            ChatRoom room = cru.getChatRoom();
            List<String> memberNames = chatRoomUserRepository
                    .findByChatRoomIdAndStatus(room.getId(), ChatRoomUser.Status.ACTIVE)
                    .stream()
                    .map(m -> m.getUser().getUsername())
                    .toList();

            return new ChatRoomResponse(room.getId(), room.getType(), memberNames, room.getCreatedAt());
        }).toList();
    }
}
