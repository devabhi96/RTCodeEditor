import { Client } from '@stomp/stompjs';
import { SockJS } from 'sockjs-client';
import { useEffect, useState } from 'react';

let stompClient = null;

export const connectWebSocket = (onMessageReceived, onConnected, onDisconnected) => {
  const token = localStorage.getItem('token'); // Assuming JWT is stored in localStorage

  const ws = new SockJS('http://localhost:8080/ws'); // Backend URL
  stompClient = new Client({
    webSocketFactory: () => ws,
    debug: true,
    headers: {
      Authorization: `Bearer ${token}`
    },
    brokerURL: 'ws://localhost:8080/ws',
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
  });

  stompClient.onConnect = (frame) => {
    console.log('Connected: ' + frame);
    onConnected(frame);
  };

  stompClient.onDisconnect = (frame) => {
    console.log('Disconnected: ' + frame);
    onDisconnected(frame);
  };

  stompClient.activate();

  // Return a function to allow unsubscribing
  return () => {
    if (stompClient) {
      stompClient.deactivate();
    }
  };
};

export const subscribeToTopic = (topic, callback) => {
  if (stompClient && stompClient.connected) {
    return stompClient.subscribe(topic, (message) => {
      callback(JSON.parse(message.body));
    });
  }
  return null;
};

export const sendToApp = (destination, payload) => {
  if (stompClient && stompClient.connected) {
    stompClient.publish({
      destination: `/app${destination}`,
      body: JSON.stringify(payload)
    });
  }
};

export const disconnectWebSocket = () => {
  if (stompClient) {
    stompClient.deactivate();
  }
};