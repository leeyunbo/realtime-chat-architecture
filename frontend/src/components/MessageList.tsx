import { useEffect, useRef, useState, type MutableRefObject } from 'react';
import { apiFetch, getDownloadUrl, getThumbnailUrl } from '../api/client';
import { useAuth } from '../contexts/AuthContext';
import ImageModal from './ImageModal';
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
  const [thumbnails, setThumbnails] = useState<Record<number, string>>({});
  const [modalImage, setModalImage] = useState<{ url: string; filename: string } | null>(null);
  const bottomRef = useRef<HTMLDivElement>(null);
  const roomIdRef = useRef(roomId);
  roomIdRef.current = roomId;

  // Load messages on room change
  useEffect(() => {
    setMessages([]);
    setThumbnails({});
    apiFetch<MessageResponse[]>(`/chatrooms/${roomId}/messages`).then((msgs) => {
      setMessages(msgs);
      loadThumbnails(msgs);
    });
  }, [roomId]);

  // Load thumbnail with retry (async thumbnail generation may not be done yet)
  function loadThumbnail(fileId: number, retries = 3) {
    getThumbnailUrl(fileId).then((res) => {
      setThumbnails((prev) => ({ ...prev, [fileId]: res.downloadUrl }));
    }).catch(() => {
      if (retries > 0) {
        setTimeout(() => loadThumbnail(fileId, retries - 1), 2000);
      }
    });
  }

  // Load thumbnails for image file messages
  function loadThumbnails(msgs: MessageResponse[]) {
    const imageFiles = msgs.filter((m) => m.fileId && m.contentType?.startsWith('image/'));
    imageFiles.forEach((m) => loadThumbnail(m.fileId!));
  }

  // Subscribe to realtime events
  useEffect(() => {
    messageReceivedRef.current = (msg: WSMessage) => {
      if (msg.chatRoomId === roomIdRef.current) {
        const newMsg: MessageResponse = {
          id: msg.messageId!,
          senderId: msg.senderId!,
          senderName: msg.senderName!,
          content: msg.content!,
          unreadCount: msg.unreadCount ?? 0,
          createdAt: new Date().toISOString(),
          fileId: msg.fileId,
          originalFilename: msg.originalFilename,
          contentType: msg.contentType,
          fileSize: msg.fileSize,
        };
        setMessages((prev) => [...prev, newMsg]);

        // Load thumbnail for new image message (with retry for async generation)
        if (msg.fileId && msg.contentType?.startsWith('image/')) {
          loadThumbnail(msg.fileId);
        }
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

  function formatSize(bytes: number) {
    if (bytes < 1024) return `${bytes}B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)}KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)}MB`;
  }

  async function handleImageClick(fileId: number, filename: string) {
    try {
      const res = await getDownloadUrl(fileId);
      setModalImage({ url: res.downloadUrl, filename });
    } catch {
      alert('이미지를 불러올 수 없습니다.');
    }
  }

  async function handleFileDownload(fileId: number, filename: string) {
    try {
      const res = await getDownloadUrl(fileId);
      const a = document.createElement('a');
      a.href = res.downloadUrl;
      a.download = filename;
      a.target = '_blank';
      a.click();
    } catch {
      alert('다운로드에 실패했습니다.');
    }
  }

  function renderFileContent(msg: MessageResponse, isMine: boolean) {
    const isImage = msg.contentType?.startsWith('image/');
    const thumbUrl = msg.fileId ? thumbnails[msg.fileId] : null;

    if (isImage && thumbUrl) {
      return (
        <img
          src={thumbUrl}
          alt={msg.originalFilename}
          className="max-w-48 max-h-48 rounded-lg cursor-pointer hover:opacity-90"
          onClick={() => handleImageClick(msg.fileId!, msg.originalFilename!)}
        />
      );
    }

    if (isImage && !thumbUrl) {
      return (
        <div className="w-48 h-32 bg-gray-300 rounded-lg animate-pulse flex items-center justify-center">
          <span className="text-gray-500 text-sm">로딩 중...</span>
        </div>
      );
    }

    // Non-image file card
    return (
      <button
        onClick={() => handleFileDownload(msg.fileId!, msg.originalFilename!)}
        className={`flex items-center gap-3 px-3 py-2 rounded-lg hover:opacity-80 ${
          isMine ? 'bg-blue-400' : 'bg-gray-300'
        }`}
      >
        <span className="text-2xl">📄</span>
        <div className="text-left text-sm">
          <p className="font-medium truncate max-w-40">{msg.originalFilename}</p>
          <p className={`text-xs ${isMine ? 'text-blue-100' : 'text-gray-500'}`}>
            {formatSize(msg.fileSize!)}
          </p>
        </div>
      </button>
    );
  }

  return (
    <>
      <div className="flex-1 overflow-y-auto p-4 space-y-2">
        {messages.map((msg) => {
          const isMine = msg.senderId === userId;
          const isFile = !!msg.fileId;

          return (
            <div
              key={msg.id}
              className={`flex ${isMine ? 'justify-end' : 'justify-start'}`}
            >
              <div
                className={`max-w-xs lg:max-w-md rounded-2xl ${
                  isFile ? 'p-2' : 'px-4 py-2'
                } ${
                  isMine
                    ? 'bg-blue-500 text-white'
                    : 'bg-gray-200 text-gray-900'
                }`}
              >
                {!isMine && (
                  <p className={`text-xs font-semibold mb-1 ${isFile ? 'px-2' : ''}`}>
                    {msg.senderName}
                  </p>
                )}
                {isFile ? renderFileContent(msg, isMine) : (
                  <p className="break-words">{msg.content}</p>
                )}
                <div
                  className={`flex items-center gap-1 mt-1 text-xs ${
                    isFile ? 'px-2' : ''
                  } ${
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
      {modalImage && (
        <ImageModal
          imageUrl={modalImage.url}
          filename={modalImage.filename}
          onClose={() => setModalImage(null)}
        />
      )}
    </>
  );
}
