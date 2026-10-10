import assert from 'node:assert/strict';
import test from 'node:test';
import { createYjsDocument } from './YjsService.js';

const createPeer = ({ onDocumentUpdate = () => {}, onAwarenessUpdate = () => {}, updateDebounceMs = 1000 } = {}) => (
  createYjsDocument({ onDocumentUpdate, onAwarenessUpdate }, { updateDebounceMs })
);

test('independent concurrent edits converge after exchanging Yjs state', () => {
  const left = createPeer();
  const right = createPeer();

  try {
    left.text.insert(0, 'left');
    right.text.insert(0, 'right');

    const leftState = left.encodeDocumentState();
    const rightState = right.encodeDocumentState();
    left.applyDocumentUpdate(rightState);
    right.applyDocumentUpdate(leftState);

    assert.equal(left.text.toString(), right.text.toString());
    assert.equal(left.text.toString().length, 'left'.length + 'right'.length);
  } finally {
    left.destroy();
    right.destroy();
  }
});

test('separate document instances remain isolated without exchanged updates', () => {
  const firstRoom = createPeer();
  const secondRoom = createPeer();

  try {
    firstRoom.text.insert(0, 'private to room one');
    assert.equal(firstRoom.text.toString(), 'private to room one');
    assert.equal(secondRoom.text.toString(), '');
  } finally {
    firstRoom.destroy();
    secondRoom.destroy();
  }
});

test('debounces and merges local document updates, then flushes on destroy', async () => {
  const sentUpdates = [];
  const sender = createPeer({
    onDocumentUpdate: (update) => sentUpdates.push(update),
    updateDebounceMs: 20,
  });
  const receiver = createPeer();

  sender.text.insert(0, 'hello');
  sender.text.insert(5, ' world');
  assert.equal(sentUpdates.length, 0);

  await new Promise((resolve) => setTimeout(resolve, 60));
  assert.equal(sentUpdates.length, 1);

  receiver.applyDocumentUpdate(sentUpdates[0]);
  assert.equal(receiver.text.toString(), 'hello world');

  sender.text.insert(11, '!');
  sender.destroy();
  assert.equal(sentUpdates.length, 2);
  receiver.applyDocumentUpdate(sentUpdates[1]);
  assert.equal(receiver.text.toString(), 'hello world!');
  receiver.destroy();
});

test('relays awareness state without modifying document updates', () => {
  const awarenessUpdates = [];
  const publisher = createPeer({ onAwarenessUpdate: (update) => awarenessUpdates.push(update) });
  const subscriber = createPeer();

  try {
    publisher.awareness.setLocalStateField('user', { name: 'Guest A', color: '#123456' });
    assert.equal(awarenessUpdates.length, 1);

    subscriber.applyAwarenessUpdate(awarenessUpdates[0]);
    const remoteUser = subscriber.awareness.getStates().get(publisher.doc.clientID)?.user;
    assert.deepEqual(remoteUser, { name: 'Guest A', color: '#123456' });
    assert.equal(subscriber.text.toString(), '');
  } finally {
    publisher.destroy();
    subscriber.destroy();
  }
});
