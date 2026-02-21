import { useState, useCallback, useRef } from 'react';
import { useAuth } from '../contexts/AuthContext';
import useWebSocket from '../hooks/useWebSocket';
import useChatRooms from '../hooks/useChatRooms';
import Sidebar from '../components/Sidebar';
import ChatView from '../components/ChatView';
import type { WSMessage } from '../types';

export default function ChatPage() {
  const { token, logout } = useAuth();
  const [selectedRoomId, setSelectedRoomId] = useState<number | null>(null);
  const selectedRoomIdRef = useRef(selectedRoomId);
  selectedRoomIdRef.current = selectedRoomId;

  const {
    rooms,
    loadRooms,
    handleMessageReceived: updateRoomOnMessage,
    handleMessagesRead: updateRoomOnRead,
    clearUnread,
  } = useChatRooms(selectedRoomId);

  // Callback refs for ChatView to subscribe to realtime events
  const messageReceivedRef = useRef<((msg: WSMessage) => void) | null>(null);
  const messageUpdatedRef = useRef<((msg: WSMessage) => void) | null>(null);
  const messagesReadRef = useRef<((msg: WSMessage) => void) | null>(null);
  const userStatusRef = useRef<((msg: WSMessage) => void) | null>(null);

  const { send } = useWebSocket(token, {
    onMessageReceived: useCallback(
      (msg: WSMessage) => {
        updateRoomOnMessage(msg);
        messageReceivedRef.current?.(msg);
      },
      [updateRoomOnMessage],
    ),
    onMessageUpdated: useCallback((msg: WSMessage) => {
      messageUpdatedRef.current?.(msg);
    }, []),
    onMessagesRead: useCallback(
      (msg: WSMessage) => {
        updateRoomOnRead(msg);
        messagesReadRef.current?.(msg);
      },
      [updateRoomOnRead],
    ),
    onUserStatus: useCallback((msg: WSMessage) => {
      userStatusRef.current?.(msg);
    }, []),
  });

  const handleSend = useCallback(
    (roomId: number, content: string) => {
      send({
        type: 'message.send',
        chatRoomId: roomId,
        content,
      });
    },
    [send],
  );

  const handleReadMessages = useCallback(
    (roomId: number) => {
      send({ type: 'message.read', chatRoomId: roomId });
      clearUnread(roomId);
    },
    [send, clearUnread],
  );

  return (
    <div className="h-screen flex bg-gray-100">
      <Sidebar
        rooms={rooms}
        selectedRoomId={selectedRoomId}
        onSelectRoom={setSelectedRoomId}
        onLogout={logout}
        loadRooms={loadRooms}
        userStatusRef={userStatusRef}
      />
      <ChatView
        roomId={selectedRoomId}
        onSend={handleSend}
        onReadMessages={handleReadMessages}
        messageReceivedRef={messageReceivedRef}
        messageUpdatedRef={messageUpdatedRef}
        messagesReadRef={messagesReadRef}
      />
    </div>
  );
}
