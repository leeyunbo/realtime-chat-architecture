import { useEffect, useState, type MutableRefObject } from 'react';
import MessageList from './MessageList';
import MessageInput from './MessageInput';
import MessageSearch from './MessageSearch';
import type { WSMessage } from '../types';

interface Props {
  roomId: number | null;
  onSend: (roomId: number, content: string) => void;
  onSendFile: (roomId: number, file: File) => void;
  uploading: boolean;
  onReadMessages: (roomId: number) => void;
  messageReceivedRef: MutableRefObject<((msg: WSMessage) => void) | null>;
  messageUpdatedRef: MutableRefObject<((msg: WSMessage) => void) | null>;
  messagesReadRef: MutableRefObject<((msg: WSMessage) => void) | null>;
}

export default function ChatView({
  roomId,
  onSend,
  onSendFile,
  uploading,
  onReadMessages,
  messageReceivedRef,
  messageUpdatedRef,
  messagesReadRef,
}: Props) {
  const [showSearch, setShowSearch] = useState(false);

  // Mark as read when entering a room
  useEffect(() => {
    if (roomId) {
      onReadMessages(roomId);
    }
  }, [roomId, onReadMessages]);

  // Close search when switching rooms
  useEffect(() => {
    setShowSearch(false);
  }, [roomId]);

  if (!roomId) {
    return (
      <div className="flex-1 flex items-center justify-center bg-gray-50">
        <p className="text-gray-400">Select a chat room to start messaging</p>
      </div>
    );
  }

  return (
    <div className="flex-1 flex flex-col bg-white">
      {/* Header with search toggle */}
      <div className="px-4 py-2 border-b flex items-center justify-end">
        <button
          onClick={() => setShowSearch((v) => !v)}
          className={`p-1.5 rounded-lg text-sm ${
            showSearch
              ? 'bg-blue-100 text-blue-600'
              : 'text-gray-500 hover:bg-gray-100'
          }`}
          title="메시지 검색"
        >
          <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
        </button>
      </div>

      {showSearch ? (
        <MessageSearch roomId={roomId} onClose={() => setShowSearch(false)} />
      ) : (
        <>
          <MessageList
            roomId={roomId}
            messageReceivedRef={messageReceivedRef}
            messageUpdatedRef={messageUpdatedRef}
            messagesReadRef={messagesReadRef}
          />
          <MessageInput
            roomId={roomId}
            onSend={(content) => onSend(roomId, content)}
            onSendFile={(file) => onSendFile(roomId, file)}
            uploading={uploading}
          />
        </>
      )}
    </div>
  );
}
