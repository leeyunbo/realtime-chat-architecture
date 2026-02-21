import { useEffect, useState, type FormEvent, type MutableRefObject } from 'react';
import { apiFetch } from '../api/client';
import type { ChatRoomResponse, FriendResponse, WSMessage } from '../types';

interface Props {
  onSelectRoom: (roomId: number) => void;
  loadRooms: () => void;
  userStatusRef: MutableRefObject<((msg: WSMessage) => void) | null>;
}

export default function FriendList({
  onSelectRoom,
  loadRooms,
  userStatusRef,
}: Props) {
  const [friends, setFriends] = useState<FriendResponse[]>([]);
  const [addInput, setAddInput] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    apiFetch<FriendResponse[]>('/friends').then(setFriends);
  }, []);

  // Subscribe to user status updates
  useEffect(() => {
    const prev = userStatusRef.current;
    userStatusRef.current = (msg: WSMessage) => {
      prev?.(msg);
      setFriends((f) =>
        f.map((friend) =>
          friend.id === msg.senderId
            ? { ...friend, online: msg.online ?? false }
            : friend,
        ),
      );
    };
    return () => {
      userStatusRef.current = prev;
    };
  }, [userStatusRef]);

  const handleAdd = async (e: FormEvent) => {
    e.preventDefault();
    const username = addInput.trim();
    if (!username) return;
    setError('');
    try {
      await apiFetch('/friends', {
        method: 'POST',
        body: JSON.stringify({ username }),
      });
      setAddInput('');
      const updated = await apiFetch<FriendResponse[]>('/friends');
      setFriends(updated);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to add friend');
    }
  };

  const handleClick = async (friendId: number) => {
    try {
      const room = await apiFetch<ChatRoomResponse>('/chatrooms', {
        method: 'POST',
        body: JSON.stringify({ userIds: [friendId] }),
      });
      loadRooms();
      onSelectRoom(room.id);
    } catch (err) {
      console.error('Failed to create/open chat room', err);
    }
  };

  // Sort: online first
  const sorted = [...friends].sort(
    (a, b) => Number(b.online) - Number(a.online),
  );

  return (
    <div>
      <form onSubmit={handleAdd} className="p-3 border-b border-gray-100">
        <div className="flex gap-2">
          <input
            type="text"
            value={addInput}
            onChange={(e) => setAddInput(e.target.value)}
            placeholder="Add friend by username"
            className="flex-1 px-3 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <button
            type="submit"
            className="px-3 py-1.5 text-sm bg-blue-600 text-white rounded-md hover:bg-blue-700"
          >
            Add
          </button>
        </div>
        {error && <p className="mt-1 text-xs text-red-500">{error}</p>}
      </form>
      {sorted.length === 0 && (
        <p className="p-4 text-sm text-gray-400">No friends yet</p>
      )}
      {sorted.map((friend) => (
        <button
          key={friend.id}
          onClick={() => handleClick(friend.id)}
          className="w-full text-left px-4 py-3 border-b border-gray-100 hover:bg-gray-50 flex items-center gap-2"
        >
          <span
            className={`w-2.5 h-2.5 rounded-full ${
              friend.online ? 'bg-green-500' : 'bg-gray-300'
            }`}
          />
          <span className="font-medium">{friend.username}</span>
        </button>
      ))}
    </div>
  );
}
