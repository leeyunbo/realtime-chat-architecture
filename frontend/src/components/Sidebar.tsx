import { useState, type MutableRefObject } from 'react';
import ChatRoomList from './ChatRoomList';
import FriendList from './FriendList';
import GlobalSearch from './GlobalSearch';
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
  const [showGlobalSearch, setShowGlobalSearch] = useState(false);

  const handleSelectFromFriend = (roomId: number) => {
    onSelectRoom(roomId);
    setTab('chat');
  };

  const handleSearchNavigate = (roomId: number) => {
    onSelectRoom(roomId);
    setShowGlobalSearch(false);
    setTab('chat');
  };

  return (
    <div className="w-80 border-r border-gray-200 bg-white flex flex-col">
      <div className="p-4 border-b border-gray-200 flex items-center justify-between">
        <span className="font-bold text-lg">Chat</span>
        <div className="flex items-center gap-2">
          <button
            onClick={() => setShowGlobalSearch((v) => !v)}
            className={`p-1.5 rounded-lg ${
              showGlobalSearch
                ? 'bg-blue-100 text-blue-600'
                : 'text-gray-500 hover:bg-gray-100'
            }`}
            title="전체 검색"
          >
            <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </button>
          <span className="text-sm text-gray-500">{username}</span>
          <button
            onClick={onLogout}
            className="text-sm text-red-500 hover:text-red-700"
          >
            Logout
          </button>
        </div>
      </div>

      {showGlobalSearch ? (
        <GlobalSearch
          onClose={() => setShowGlobalSearch(false)}
          onNavigate={handleSearchNavigate}
        />
      ) : (
        <>
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
        </>
      )}
    </div>
  );
}
