import { useState, type MutableRefObject } from 'react';
import ChatRoomList from './ChatRoomList';
import FriendList from './FriendList';
import type { ChatRoomResponse, WSMessage } from '../types';
import { useAuth } from '../contexts/AuthContext';

type Tab = 'friends' | 'chat';

interface Props {
  rooms: ChatRoomResponse[];
  selectedRoomId: number | null;
  onSelectRoom: (roomId: number) => void;
  onLogout: () => void;
  loadRooms: () => void;
  userStatusRef: MutableRefObject<((msg: WSMessage) => void) | null>;
}

export default function Sidebar({
  rooms,
  selectedRoomId,
  onSelectRoom,
  onLogout,
  loadRooms,
  userStatusRef,
}: Props) {
  const { username } = useAuth();
  const [tab, setTab] = useState<Tab>('chat');

  const handleSelectFromFriend = (roomId: number) => {
    onSelectRoom(roomId);
    setTab('chat');
  };

  return (
    <div className="w-80 border-r border-gray-200 bg-white flex flex-col">
      <div className="p-4 border-b border-gray-200 flex items-center justify-between">
        <span className="font-bold text-lg">Chat</span>
        <div className="flex items-center gap-2">
          <span className="text-sm text-gray-500">{username}</span>
          <button
            onClick={onLogout}
            className="text-sm text-red-500 hover:text-red-700"
          >
            Logout
          </button>
        </div>
      </div>
      <div className="flex border-b border-gray-200">
        <button
          onClick={() => setTab('friends')}
          className={`flex-1 py-2 text-sm font-medium ${
            tab === 'friends'
              ? 'text-blue-600 border-b-2 border-blue-600'
              : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          Friends
        </button>
        <button
          onClick={() => setTab('chat')}
          className={`flex-1 py-2 text-sm font-medium ${
            tab === 'chat'
              ? 'text-blue-600 border-b-2 border-blue-600'
              : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          Chat
        </button>
      </div>
      <div className="flex-1 overflow-y-auto">
        {tab === 'chat' ? (
          <ChatRoomList
            rooms={rooms}
            selectedRoomId={selectedRoomId}
            onSelectRoom={onSelectRoom}
          />
        ) : (
          <FriendList
            onSelectRoom={handleSelectFromFriend}
            loadRooms={loadRooms}
            userStatusRef={userStatusRef}
          />
        )}
      </div>
    </div>
  );
}
