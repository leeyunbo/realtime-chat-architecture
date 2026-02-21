import { useEffect, useRef, useState, type MutableRefObject } from 'react';
import { apiFetch } from '../api/client';
import { useAuth } from '../contexts/AuthContext';
import type { MessageResponse, WSMessage } from '../types';

interface Props {
  roomId: number;
  messageReceivedRef: MutableRefObject<((msg: WSMessage) => void) | null>;
  messageUpdatedRef: MutableRefObject<((msg: WSMessage) => void) | null>;
  messagesReadRef: MutableRefObject<((msg: WSMessage) => void) | null>;
}

export default function MessageList({
  roomId,
  messageReceivedRef,
  messageUpdatedRef,
  messagesReadRef,
}: Props) {
  const { userId } = useAuth();
  const [messages, setMessages] = useState<MessageResponse[]>([]);
  const bottomRef = useRef<HTMLDivElement>(null);
  const roomIdRef = useRef(roomId);
  roomIdRef.current = roomId;

  // Load messages on room change
  useEffect(() => {
    setMessages([]);
    apiFetch<MessageResponse[]>(`/chatrooms/${roomId}/messages`).then(
      setMessages,
    );
  }, [roomId]);

  // Subscribe to realtime events
  useEffect(() => {
    messageReceivedRef.current = (msg: WSMessage) => {
      if (msg.chatRoomId === roomIdRef.current) {
        setMessages((prev) => [
          ...prev,
          {
            id: msg.messageId!,
            senderId: msg.senderId!,
            senderName: msg.senderName!,
            content: msg.content!,
            unreadCount: msg.unreadCount ?? 0,
            createdAt: new Date().toISOString(),
          },
        ]);
      }
    };

    messageUpdatedRef.current = (msg: WSMessage) => {
      if (msg.chatRoomId === roomIdRef.current) {
        setMessages((prev) =>
          prev.map((m) =>
            m.id === msg.messageId
              ? { ...m, unreadCount: msg.unreadCount ?? m.unreadCount }
              : m,
          ),
        );
      }
    };

    messagesReadRef.current = (msg: WSMessage) => {
      if (msg.chatRoomId === roomIdRef.current && msg.messageId) {
        setMessages((prev) =>
          prev.map((m) =>
            m.id <= msg.messageId!
              ? { ...m, unreadCount: Math.max(0, m.unreadCount - 1) }
              : m,
          ),
        );
      }
    };

    return () => {
      messageReceivedRef.current = null;
      messageUpdatedRef.current = null;
      messagesReadRef.current = null;
    };
  }, [messageReceivedRef, messageUpdatedRef, messagesReadRef]);

  // Auto-scroll
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  function formatTime(iso: string) {
    const d = new Date(iso);
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  return (
    <div className="flex-1 overflow-y-auto p-4 space-y-2">
      {messages.map((msg) => {
        const isMine = msg.senderId === userId;
        return (
          <div
            key={msg.id}
            className={`flex ${isMine ? 'justify-end' : 'justify-start'}`}
          >
            <div
              className={`max-w-xs lg:max-w-md px-4 py-2 rounded-2xl ${
                isMine
                  ? 'bg-blue-500 text-white'
                  : 'bg-gray-200 text-gray-900'
              }`}
            >
              {!isMine && (
                <p className="text-xs font-semibold mb-1">{msg.senderName}</p>
              )}
              <p className="break-words">{msg.content}</p>
              <div
                className={`flex items-center gap-1 mt-1 text-xs ${
                  isMine ? 'text-blue-100 justify-end' : 'text-gray-500'
                }`}
              >
                <span>{formatTime(msg.createdAt)}</span>
                {isMine && msg.unreadCount > 0 && (
                  <span className="ml-1">{msg.unreadCount}</span>
                )}
              </div>
            </div>
          </div>
        );
      })}
      <div ref={bottomRef} />
    </div>
  );
}
