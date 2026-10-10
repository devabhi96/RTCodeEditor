import assert from 'node:assert/strict';
import { Client } from 'file:///C:/Users/ASUS/OneDrive/Desktop/Spring%20boot%20projects/RTCodeEditor/RTCodeEditor/frontend/node_modules/@stomp/stompjs/esm6/index.js';
import SockJS from 'file:///C:/Users/ASUS/OneDrive/Desktop/Spring%20boot%20projects/RTCodeEditor/RTCodeEditor/frontend/node_modules/sockjs-client/dist/sockjs.js';

const firstClient = new Client({ webSocketFactory: () => new SockJS(process.env.BACKEND_URL || 'http://localhost:8081/ws'), reconnectDelay: 0 });
const secondClient = new Client({ webSocketFactory: () => new SockJS(process.env.BACKEND_URL || 'http://localhost:8081/ws'), reconnectDelay: 0 });
const timeout = (ms = 5000) => new Promise((_, reject) => setTimeout(() => reject(new Error('Timed out waiting for STOMP message')), ms));
const connected = (client) => Promise.race([new Promise((resolve, reject) => {
  client.onConnect = resolve;
  client.onStompError = (frame) => reject(new Error(frame.headers.message));
  client.activate();
}), timeout()]);

try {
  await Promise.all([connected(firstClient), connected(secondClient)]);
  const documentId = `smoke-${Date.now()}`;
  console.log('stage: sessions', firstClient.sessionId, secondClient.sessionId);
  const firstState = new Promise((resolve) => firstClient.subscribe(`/user/queue/document/${documentId}/state`, (message) => { console.log('first state', message.body); resolve(JSON.parse(message.body)); }));
  const secondState = new Promise((resolve) => secondClient.subscribe(`/user/queue/document/${documentId}/state`, (message) => resolve(JSON.parse(message.body))));
  const firstUpdates = [];
  const secondUpdates = [];
  firstClient.subscribe(`/topic/document/${documentId}`, (message) => firstUpdates.push(JSON.parse(message.body)));
  secondClient.subscribe(`/topic/document/${documentId}`, (message) => secondUpdates.push(JSON.parse(message.body)));
  const awareness = new Promise((resolve) => secondClient.subscribe(`/topic/document/${documentId}/awareness`, (message) => resolve(JSON.parse(message.body))));
  firstClient.publish({ destination: '/app/document.load', body: JSON.stringify({ documentId }) });
  secondClient.publish({ destination: '/app/document.load', body: JSON.stringify({ documentId }) });
  const [firstLoaded, secondLoaded] = await Promise.race([Promise.all([firstState, secondState]), timeout()]);
  assert.equal(firstLoaded.update, '');
  assert.equal(secondLoaded.update, '');
  const update = { documentId, update: Buffer.from([10, 20, 30]).toString('base64') };
  firstClient.publish({ destination: '/app/document.update', body: JSON.stringify(update) });
  const firstRelayed = new Promise((resolve) => setTimeout(() => resolve(firstUpdates.length), 200));
  await Promise.race([new Promise((resolve) => {
    const check = setInterval(() => {
      if (secondUpdates.length) { clearInterval(check); resolve(); }
    }, 10);
  }), timeout()]);
  assert.deepEqual(firstUpdates, [update]);
  assert.deepEqual(secondUpdates, [update]);
  const awarenessUpdate = { documentId, update: Buffer.from([40, 50]).toString('base64') };
  firstClient.publish({ destination: '/app/document.awareness', body: JSON.stringify(awarenessUpdate) });
  assert.deepEqual(await Promise.race([awareness, timeout()]), awarenessUpdate);
  console.log('Two-client state isolation, document relay, and awareness relay passed.');
} finally {
  await Promise.all([firstClient.deactivate(), secondClient.deactivate()]);
}
