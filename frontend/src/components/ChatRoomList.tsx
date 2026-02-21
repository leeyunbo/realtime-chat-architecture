import { useAuth } from '../contexts/AuthContext';
import type { ChatRoomResponse } from '../types';

interface Props {
  rooms: ChatRoomResponse[];
  selectedRoomId: number | null;
  onSelectRoom: (roomId: number) => void;
}

export default function ChatRoomList({
  rooms,
  selectedRoomId,
  onSelectRoom,
}: Props) {
  const { username } = useAuth();

  function getDisplayName(room: ChatRoomResponse) {
    if (room.type === 'DIRECT') {
      return room.members.find((m) => m !== username) ?? 'Unknown';
    }
    return room.members.join(', ');
  }

  return (
    <div>
      {rooms.length === 0 && (
        <p className="p-4 text-sm text-gray-400">No chat rooms yet</p>
      )}
      {rooms.map((room) => (
        <button
          key={room.id}
          onClick={() => onSelectRoom(room.id)}
          className={`w-full text-left px-4 py-3 border-b border-gray-100 hover:bg-gray-50 flex items-center justify-between ${
            selectedRoomId === room.id ? 'bg-blue-50' : ''
          }`}
        >
          <span className="truncate font-medium">{getDisplayName(room)}</span>
          {room.unreadCount > 0 && (
            <span className="ml-2 bg-red-500 text-white text-xs rounded-full px-2 py-0.5 min-w-[20px] text-center">
              {room.unreadCount}
            </span>
          )}
        </button>
      ))}
    </div>
  );
}
