package com.bok.chat.repository;

import com.bok.chat.entity.ChatRoom;
import com.bok.chat.entity.Message;
import com.bok.chat.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MessageRepository")
class MessageRepositoryTest extends RepositoryTestBase {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private EntityManager em;

    private ChatRoom chatRoom;
    private User sender;

    @BeforeEach
    void setUp() {
        sender = User.builder().username("sender").password("pass").build();
        em.persist(sender);

        chatRoom = ChatRoom.create(2);
        em.persist(chatRoom);

        em.flush();
    }

    @Test
    @DisplayName("lastReadMessageId 이후 메시지의 unreadCount를 1씩 차감한다")
    void decrementUnreadCountAfter_shouldDecrementUnreadMessages() {
        Message msg1 = Message.create(chatRoom, sender, "msg1", 3);
        Message msg2 = Message.create(chatRoom, sender, "msg2", 3);
        Message msg3 = Message.create(chatRoom, sender, "msg3", 3);
        em.persist(msg1);
        em.persist(msg2);
        em.persist(msg3);
        em.flush();
        em.clear();

        int updated = messageRepository.decrementUnreadCountAfter(chatRoom.getId(), msg1.getId());

        assertThat(updated).isEqualTo(2);

        Message reloaded2 = em.find(Message.class, msg2.getId());
        Message reloaded3 = em.find(Message.class, msg3.getId());
        assertThat(reloaded2.getUnreadCount()).isEqualTo(1);
        assertThat(reloaded3.getUnreadCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("unreadCount가 0인 메시지는 차감하지 않는다")
    void decrementUnreadCountAfter_shouldNotGoBelowZero() {
        Message msg = Message.create(chatRoom, sender, "msg", 1); // unreadCount = 0
        em.persist(msg);
        em.flush();
        em.clear();

        int updated = messageRepository.decrementUnreadCountAfter(chatRoom.getId(), 0L);

        assertThat(updated).isEqualTo(0);

        Message reloaded = em.find(Message.class, msg.getId());
        assertThat(reloaded.getUnreadCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("채팅방의 최신 메시지 ID를 반환한다")
    void findLatestMessageIdByChatRoomId_shouldReturnMaxId() {
        Message msg1 = Message.create(chatRoom, sender, "msg1", 2);
        Message msg2 = Message.create(chatRoom, sender, "msg2", 2);
        em.persist(msg1);
        em.persist(msg2);
        em.flush();

        Long latestId = messageRepository.findLatestMessageIdByChatRoomId(chatRoom.getId())
                .orElse(null);

        assertThat(latestId).isEqualTo(msg2.getId());
    }

    @Test
    @DisplayName("메시지가 없는 채팅방은 빈 Optional을 반환한다")
    void findLatestMessageIdByChatRoomId_emptyRoom_shouldReturnEmpty() {
        var result = messageRepository.findLatestMessageIdByChatRoomId(chatRoom.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("lastReadMessageId 이후의 안 읽은 메시지를 시간순으로 반환한다")
    void findUnreadMessages_shouldReturnMessagesAfterLastRead() {
        Message msg1 = Message.create(chatRoom, sender, "msg1", 2);
        Message msg2 = Message.create(chatRoom, sender, "msg2", 2);
        Message msg3 = Message.create(chatRoom, sender, "msg3", 2);
        em.persist(msg1);
        em.persist(msg2);
        em.persist(msg3);
        em.flush();
        em.clear();

        List<Message> unread = messageRepository.findUnreadMessages(chatRoom.getId(), msg1.getId());

        assertThat(unread).hasSize(2);
        assertThat(unread.get(0).getContent()).isEqualTo("msg2");
        assertThat(unread.get(1).getContent()).isEqualTo("msg3");
        // JOIN FETCH로 sender가 로딩되어야 한다
        assertThat(unread.get(0).getSender().getUsername()).isEqualTo("sender");
    }

    @Test
    @DisplayName("lastReadMessageId가 0이면 모든 메시지를 반환한다")
    void findUnreadMessages_withZeroLastRead_shouldReturnAllMessages() {
        Message msg1 = Message.create(chatRoom, sender, "msg1", 2);
        Message msg2 = Message.create(chatRoom, sender, "msg2", 2);
        em.persist(msg1);
        em.persist(msg2);
        em.flush();
        em.clear();

        List<Message> unread = messageRepository.findUnreadMessages(chatRoom.getId(), 0L);

        assertThat(unread).hasSize(2);
    }

    @Test
    @DisplayName("안 읽은 메시지가 없으면 빈 리스트를 반환한다")
    void findUnreadMessages_noUnread_shouldReturnEmpty() {
        Message msg1 = Message.create(chatRoom, sender, "msg1", 2);
        em.persist(msg1);
        em.flush();
        em.clear();

        List<Message> unread = messageRepository.findUnreadMessages(chatRoom.getId(), msg1.getId());

        assertThat(unread).isEmpty();
    }

    @Test
    @DisplayName("lastReadMessageId 이후의 메시지 수를 반환한다")
    void countUnreadMessages_shouldCountMessagesAfterLastRead() {
        Message msg1 = Message.create(chatRoom, sender, "msg1", 2);
        Message msg2 = Message.create(chatRoom, sender, "msg2", 2);
        Message msg3 = Message.create(chatRoom, sender, "msg3", 2);
        em.persist(msg1);
        em.persist(msg2);
        em.persist(msg3);
        em.flush();

        long count = messageRepository.countUnreadMessages(chatRoom.getId(), msg1.getId());

        assertThat(count).isEqualTo(2);
    }
}
