import { useState, useCallback, useRef, type KeyboardEvent } from 'react';
import { searchAllMessages } from '../api/client';
import type { MessageResponse } from '../types';

interface Props {
  onClose: () => void;
  onNavigate: (roomId: number) => void;
}

function highlightKeyword(text: string, keyword: string) {
  if (!keyword.trim()) return text;
  const words = keyword.trim().split(/\s+/).filter(Boolean);
  const pattern = words.map((w) => w.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).join('|');
  const regex = new RegExp(`(${pattern})`, 'gi');
  const parts = text.split(regex);
  return parts.map((part, i) =>
    regex.test(part) ? (
      <mark key={i} className="bg-yellow-200 rounded px-0.5">
        {part}
      </mark>
    ) : (
      part
    ),
  );
}

interface SearchResult extends MessageResponse {
  chatRoomId?: number;
}

export default function GlobalSearch({ onClose, onNavigate }: Props) {
  const [query, setQuery] = useState('');
  const [messages, setMessages] = useState<SearchResult[]>([]);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [hasNext, setHasNext] = useState(false);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);
  const lastQuery = useRef('');

  const doSearch = useCallback(
    async (cursor?: string | null) => {
      const q = cursor ? lastQuery.current : query.trim();
      if (!q) return;
      lastQuery.current = q;
      setLoading(true);
      try {
        const result = await searchAllMessages(q, cursor);
        if (cursor) {
          setMessages((prev) => [...prev, ...result.messages]);
        } else {
          setMessages(result.messages);
        }
        setNextCursor(result.nextCursor);
        setHasNext(result.hasNext);
        setSearched(true);
      } catch {
        // ignore
      } finally {
        setLoading(false);
      }
    },
    [query],
  );

  const handleKeyDown = (e: KeyboardEvent) => {
    if (e.key === 'Enter') {
      doSearch();
    } else if (e.key === 'Escape') {
      onClose();
    }
  };

  const formatTime = (dateStr: string) => {
    const d = new Date(dateStr);
    return d.toLocaleString('ko-KR', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <div className="flex flex-col h-full">
      <div className="p-3 border-b flex items-center gap-2">
        <input
          type="text"
          className="flex-1 border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
          placeholder="전체 채팅방에서 검색..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={handleKeyDown}
          autoFocus
        />
        <button
          onClick={() => doSearch()}
          disabled={loading || !query.trim()}
          className="px-3 py-2 bg-blue-500 text-white text-sm rounded-lg hover:bg-blue-600 disabled:opacity-50"
        >
          검색
        </button>
        <button
          onClick={onClose}
          className="px-2 py-2 text-gray-500 hover:text-gray-700 text-sm"
        >
          닫기
        </button>
      </div>

      <div className="flex-1 overflow-y-auto p-3 space-y-2">
        {searched && messages.length === 0 && !loading && (
          <p className="text-center text-gray-400 text-sm mt-8">검색 결과가 없습니다.</p>
        )}
        {messages.map((msg) => (
          <div
            key={msg.id}
            className="p-3 bg-gray-50 rounded-lg hover:bg-gray-100 cursor-pointer"
            onClick={() => msg.chatRoomId && onNavigate(msg.chatRoomId)}
          >
            <div className="flex items-center justify-between mb-1">
              <span className="text-sm font-medium text-gray-700">
                {msg.senderName ?? '시스템'}
              </span>
              <span className="text-xs text-gray-400">{formatTime(msg.createdAt)}</span>
            </div>
            <p className="text-sm text-gray-600 break-words">
              {highlightKeyword(msg.content, lastQuery.current)}
            </p>
          </div>
        ))}
        {hasNext && (
          <button
            onClick={() => doSearch(nextCursor)}
            disabled={loading}
            className="w-full py-2 text-sm text-blue-500 hover:text-blue-700 disabled:opacity-50"
          >
            {loading ? '로딩 중...' : '더보기'}
          </button>
        )}
        {loading && !hasNext && messages.length === 0 && (
          <p className="text-center text-gray-400 text-sm mt-8">검색 중...</p>
        )}
      </div>
    </div>
  );
}
