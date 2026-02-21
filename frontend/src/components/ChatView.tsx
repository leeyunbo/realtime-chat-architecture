import { useEffect, type MutableRefObject } from 'react';
import MessageList from './MessageList';
import MessageInput from './MessageInput';
import type { WSMessage } from '../types';

interface Props {
  roomId: number | null;
  onSend: (roomId: number, content: string) => void;
  onReadMessages: (roomId: number) => void;
  messageReceivedRef: MutableRefObject<((msg: WSMessage) => void) | null>;
  messageUpdatedRef: MutableRefObject<((msg: WSMessage) => void) | null>;
  messagesReadRef: MutableRefObject<((msg: WSMessage) => void) | null>;
}

export default function ChatView({
  roomId,
  onSend,
  onReadMessages,
  messageReceivedRef,
  messageUpdatedRef,
  messagesReadRef,
}: Props) {
  // Mark as read when entering a room
  useEffect(() => {
    if (roomId) {
      onReadMessages(roomId);
    }
  }, [roomId, onReadMessages]);

  if (!roomId) {
    return (
      <div className="flex-1 flex items-center justify-center bg-gray-50">
        <p className="text-gray-400">Select a chat room to start messaging</p>
      </div>
    );
  }

  return (
    <div className="flex-1 flex flex-col bg-white">
      <MessageList
        roomId={roomId}
        messageReceivedRef={messageReceivedRef}
        messageUpdatedRef={messageUpdatedRef}
        messagesReadRef={messagesReadRef}
      />
      <MessageInput
        roomId={roomId}
        onSend={(content) => onSend(roomId, content)}
      />
    </div>
  );
}
