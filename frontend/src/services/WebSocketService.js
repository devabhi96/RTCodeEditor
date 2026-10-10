import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useCallback, useEffect, useRef, useState } from 'react';

const bytesToBase64 = (bytes) => {
  let binary = '';
  for (let offset = 0; offset < bytes.length; offset += 0x8000) {
    binary += String.fromCharCode(...bytes.subarray(offset, offset + 0x8000));
  }
  return btoa(binary);
};

const base64ToBytes = (base64) => Uint8Array.from(atob(base64), (character) => character.charCodeAt(0));

export { base64ToBytes };

export const useWebSocketService = () => {
  const [connected, setConnected] = useState(false);
  const [connectionError, setConnectionError] = useState('');
  const [clientSessionId, setClientSessionId] = useState('');
  const clientRef = useRef(null);

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS(`${import.meta.env.VITE_BACKEND_URL || 'http://localhost:8095'}/ws`),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });
    clientRef.current = client;
    client.onConnect = (frame) => {
      setConnected(true);
      setConnectionError('');
      setClientSessionId('connected');
    };
    client.onWebSocketClose = () => {
      setConnected(false);
      setClientSessionId('');
    };
    client.onStompError = (frame) => {
      setConnected(false);
      setClientSessionId('');
      setConnectionError(frame.headers.message || 'WebSocket connection error');
    };
    client.onDisconnect = () => {
      setConnected(false);
      setClientSessionId('');
    };
    void client.activate();

    return () => {
      clientRef.current = null;
      void client.deactivate();
    };
  }, []);

  const sendMessage = useCallback((destination, payload) => {
    const client = clientRef.current;
    if (!client?.connected) return false;

    client.publish({
      destination: destination.startsWith('/app/') ? destination : `/app${destination}`,
      body: JSON.stringify(payload),
    });
    return true;
  }, []);

  const subscribeToTopic = useCallback((topic, callback) => {
    const client = clientRef.current;
    if (!client?.connected) return null;

    return client.subscribe(topic, (message) => {
      try {
        callback(JSON.parse(message.body));
      } catch (error) {
        console.error(`Invalid message received on ${topic}`, error);
      }
    });
  }, []);

  const sendBytes = useCallback((destination, documentId, bytes) => {
    return sendMessage(destination, { documentId, update: bytesToBase64(bytes) });
  }, [sendMessage]);

  return { connected, clientSessionId, connectionError, sendMessage, sendBytes, subscribeToTopic };
};
