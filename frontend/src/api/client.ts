import type { FileUploadResponse, FileDownloadResponse, MessageSearchResult } from '../types';

const BASE_URL = '/api';

function getToken(): string | null {
  return localStorage.getItem('token');
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...((options.headers as Record<string, string>) || {}),
  };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const res = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers,
  });

  if (!res.ok) {
    if (res.status === 401 || res.status === 403) {
      localStorage.removeItem('token');
      localStorage.removeItem('userId');
      localStorage.removeItem('username');
      window.location.href = '/login';
      throw new Error('Session expired');
    }
    const text = await res.text();
    throw new Error(text || `HTTP ${res.status}`);
  }

  const contentType = res.headers.get('content-type');
  if (contentType?.includes('application/json')) {
    return res.json();
  }
  return res.text() as unknown as T;
}

export async function uploadFile(chatRoomId: number, file: File): Promise<FileUploadResponse> {
  const token = getToken();
  const formData = new FormData();
  formData.append('file', file);
  formData.append('chatRoomId', String(chatRoomId));

  const res = await fetch(`${BASE_URL}/files`, {
    method: 'POST',
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    body: formData,
  });

  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `Upload failed: HTTP ${res.status}`);
  }

  return res.json();
}

export async function getDownloadUrl(fileId: number): Promise<FileDownloadResponse> {
  return apiFetch(`/files/${fileId}/download-url`);
}

export async function getThumbnailUrl(fileId: number): Promise<FileDownloadResponse> {
  return apiFetch(`/files/${fileId}/thumbnail-url`);
}

export async function searchMessages(
  roomId: number,
  query: string,
  cursor?: string | null,
  size: number = 20,
): Promise<MessageSearchResult> {
  const params = new URLSearchParams({ q: query, size: String(size) });
  if (cursor) {
    params.set('cursor', cursor);
  }
  return apiFetch(`/chatrooms/${roomId}/messages/search?${params}`);
}

export async function searchAllMessages(
  query: string,
  cursor?: string | null,
  size: number = 20,
): Promise<MessageSearchResult> {
  const params = new URLSearchParams({ q: query, size: String(size) });
  if (cursor) {
    params.set('cursor', cursor);
  }
  return apiFetch(`/messages/search?${params}`);
}
