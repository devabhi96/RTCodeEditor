import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useCallback, useEffect, useState } from 'react';

let stompClient = null;

export const connectWebSocket = (onConnected, onDisconnected) => {
  const client = new Client({
    webSocketFactory: () => new SockJS(`${import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080'}/ws`),
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
  });
  stompClient = client;
  client.onConnect = onConnected;
  client.onWebSocketClose = onDisconnected;
  client.onStompError = onDisconnected;
  client.onDisconnect = onDisconnected;
  client.activate();

  return () => {
    if (stompClient === client) stompClient = null;
    void client.deactivate();
  };
};

export const subscribeToTopic = (topic, callback) => {
  if (stompClient?.connected) {
    return stompClient.subscribe(topic, (message) => {
      callback(JSON.parse(message.body));
    });
  }
  return null;
};

export const sendToApp = (destination, payload) => {
  if (stompClient?.connected) {
    stompClient.publish({
      destination: destination.startsWith('/app/') ? destination : `/app${destination}`,
      body: JSON.stringify(payload)
    });
  }
};

export const useWebSocketService = () => {
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    const disconnect = connectWebSocket(
      () => setConnected(true),
      () => setConnected(false)
    );
    return () => {
      disconnect();
      setConnected(false);
    };
  }, []);

  const sendMessage = useCallback((destination, payload) => {
    sendToApp(destination, payload);
  }, []);

  const subscribe = useCallback((topic, callback) => subscribeToTopic(topic, callback), []);

  return { connected, sendMessage, subscribeToTopic: subscribe };
};
