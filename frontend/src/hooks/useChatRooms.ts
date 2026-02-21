import { useState, useCallback, useEffect } from 'react';
import { apiFetch } from '../api/client';
import type { ChatRoomResponse, WSMessage } from '../types';

export default function useChatRooms(currentRoomId: number | null) {
  const [rooms, setRooms] = useState<ChatRoomResponse[]>([]);

  const loadRooms = useCallback(() => {
    apiFetch<ChatRoomResponse[]>('/chatrooms').then(setRooms);
  }, []);

  useEffect(() => {
    loadRooms();
  }, [loadRooms]);

  const handleMessageReceived = useCallback(
    (msg: WSMessage) => {
      setRooms((prev) => {
        const idx = prev.findIndex((r) => r.id === msg.chatRoomId);
        if (idx === -1) {
          loadRooms();
          return prev;
        }
        const updated = [...prev];
        const room = { ...updated[idx] };
        if (msg.chatRoomId !== currentRoomId) {
          room.unreadCount += 1;
        }
        updated.splice(idx, 1);
        updated.unshift(room);
        return updated;
      });
    },
    [currentRoomId, loadRooms],
  );

  const handleMessagesRead = useCallback((msg: WSMessage) => {
    setRooms((prev) =>
      prev.map((r) =>
        r.id === msg.chatRoomId ? { ...r, unreadCount: 0 } : r,
      ),
    );
  }, []);

  const clearUnread = useCallback((roomId: number) => {
    setRooms((prev) =>
      prev.map((r) => (r.id === roomId ? { ...r, unreadCount: 0 } : r)),
    );
  }, []);

  return {
    rooms,
    loadRooms,
    handleMessageReceived,
    handleMessagesRead,
    clearUnread,
  };
}
