import { useState, type FormEvent } from 'react';

interface Props {
  roomId: number;
  onSend?: (content: string) => void;
}

export default function MessageInput({ roomId: _roomId, onSend }: Props) {
  const [content, setContent] = useState('');

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    const trimmed = content.trim();
    if (!trimmed || !onSend) return;
    onSend(trimmed);
    setContent('');
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="border-t border-gray-200 p-4 flex gap-2"
    >
      <input
        type="text"
        value={content}
        onChange={(e) => setContent(e.target.value)}
        placeholder={onSend ? 'Type a message...' : 'Connecting...'}
        disabled={!onSend}
        className="flex-1 px-4 py-2 border border-gray-300 rounded-full focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100"
      />
      <button
        type="submit"
        disabled={!onSend || !content.trim()}
        className="px-4 py-2 bg-blue-600 text-white rounded-full hover:bg-blue-700 disabled:opacity-50"
      >
        Send
      </button>
    </form>
  );
}
