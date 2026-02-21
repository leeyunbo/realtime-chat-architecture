// Auth
export interface LoginResponse {
  token: string;
  userId: number;
  username: string;
}

// Chat Room
export type ChatRoomType = 'DIRECT' | 'GROUP';

export interface ChatRoomResponse {
  id: number;
  type: ChatRoomType;
  members: string[];
  unreadCount: number;
  createdAt: string;
}

// Message
export interface MessageResponse {
  id: number;
  senderId: number;
  senderName: string;
  content: string;
  unreadCount: number;
  createdAt: string;
}

// Friend
export interface FriendResponse {
  id: number;
  username: string;
  online: boolean;
}

// WebSocket
export type WSMessageType =
  | 'message.send'
  | 'message.read'
  | 'message.received'
  | 'message.updated'
  | 'heartbeat'
  | 'user.status'
  | 'messages.read';

export interface WSMessage {
  type: WSMessageType;
  chatRoomId?: number;
  senderId?: number;
  senderName?: string;
  content?: string;
  messageId?: number;
  unreadCount?: number;
  online?: boolean;
}
