import { useEffect, useRef, useCallback } from 'react';
import type { WSMessage } from '../types';

interface Callbacks {
  onMessageReceived?: (msg: WSMessage) => void;
  onMessageUpdated?: (msg: WSMessage) => void;
  onMessagesRead?: (msg: WSMessage) => void;
  onUserStatus?: (msg: WSMessage) => void;
}

export default function useWebSocket(
  token: string | null,
  callbacks: Callbacks,
) {
  const wsRef = useRef<WebSocket | null>(null);
  const callbacksRef = useRef(callbacks);
  callbacksRef.current = callbacks;
  const reconnectTimer = useRef<ReturnType<typeof setTimeout>>();
  const heartbeatTimer = useRef<ReturnType<typeof setInterval>>();

  const connect = useCallback(() => {
    if (!token) return;

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const ws = new WebSocket(
      `${protocol}//${window.location.host}/ws?token=${token}`,
    );
    wsRef.current = ws;

    ws.onopen = () => {
      heartbeatTimer.current = setInterval(() => {
        if (ws.readyState === WebSocket.OPEN) {
          ws.send(JSON.stringify({ type: 'heartbeat' }));
        }
      }, 30_000);
    };

    ws.onmessage = (event) => {
      const msg: WSMessage = JSON.parse(event.data);
      const cb = callbacksRef.current;
      switch (msg.type) {
        case 'message.received':
          cb.onMessageReceived?.(msg);
          break;
        case 'message.updated':
          cb.onMessageUpdated?.(msg);
          break;
        case 'messages.read':
          cb.onMessagesRead?.(msg);
          break;
        case 'user.status':
          cb.onUserStatus?.(msg);
          break;
      }
    };

    ws.onclose = () => {
      cleanup();
      reconnectTimer.current = setTimeout(connect, 3000);
    };

    ws.onerror = () => {
      ws.close();
    };
  }, [token]);

  function cleanup() {
    if (heartbeatTimer.current) clearInterval(heartbeatTimer.current);
    if (reconnectTimer.current) clearTimeout(reconnectTimer.current);
  }

  useEffect(() => {
    connect();
    return () => {
      cleanup();
      if (wsRef.current) {
        wsRef.current.onclose = null;
        wsRef.current.close();
        wsRef.current = null;
      }
    };
  }, [connect]);

  const send = useCallback((msg: WSMessage) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(msg));
    }
  }, []);

  return { send };
}
