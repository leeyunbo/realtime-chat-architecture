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
export type MessageType = 'CHAT' | 'SYSTEM' | 'FILE';

export interface MessageResponse {
  id: number;
  senderId: number;
  senderName: string;
  content: string;
  unreadCount: number;
  createdAt: string;
  fileId?: number;
  originalFilename?: string;
  contentType?: string;
  fileSize?: number;
}

// File
export interface FileUploadResponse {
  fileId: number;
  originalFilename: string;
  contentType: string;
  fileSize: number;
  thumbnailStatus: 'PENDING' | 'COMPLETED' | 'FAILED' | 'NONE';
}

export interface FileDownloadResponse {
  downloadUrl: string;
  originalFilename: string;
  contentType: string;
  fileSize: number;
}

// Search
export interface MessageSearchResult {
  messages: MessageResponse[];
  nextCursor: number | null;
  hasNext: boolean;
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
  fileId?: number;
  originalFilename?: string;
  contentType?: string;
  fileSize?: number;
}
