import * as Y from 'yjs';
import { Awareness, applyAwarenessUpdate, encodeAwarenessUpdate } from 'y-protocols/awareness';

// Debounce delay for document updates in milliseconds.
const DEFAULT_UPDATE_DEBOUNCE_MS = 50;

/**
 * Merge multiple Yjs update arrays into a single update.
 * Returns null if the input array is empty.
 */
export const mergeDocumentUpdates = (updates) => {
  if (updates.length === 0) return null;
  return updates.length === 1 ? updates[0] : Y.mergeUpdates(updates);
};

/**
 * Create a Yjs document with debounced update handling and awareness sync.
 * @param callbacks Object with onDocumentUpdate and onAwarenessUpdate callbacks.
 * @param options Optional configuration: { updateDebounceMs }.
 * @returns Object with methods to interact with the Yjs document.
 */
export const createYjsDocument = (callbacks, { updateDebounceMs = DEFAULT_UPDATE_DEBOUNCE_MS } = {}) => {
  const doc = new Y.Doc();
  const text = doc.getText('code');
  const awareness = new Awareness(doc);
  let destroyed = false;
  let updateTimer = null;
  let pendingUpdates = [];

  const flushDocumentUpdates = () => {
    if (updateTimer !== null) {
      clearTimeout(updateTimer);
      updateTimer = null;
    }
    if (destroyed || pendingUpdates.length === 0) return;

    const updates = pendingUpdates;
    pendingUpdates = [];
    callbacks.onDocumentUpdate(mergeDocumentUpdates(updates));
  };

  const handleDocumentUpdate = (update, origin) => {
    if (origin === 'remote' || destroyed) return;
    pendingUpdates.push(update);
    if (updateTimer !== null) clearTimeout(updateTimer);
    updateTimer = setTimeout(flushDocumentUpdates, updateDebounceMs);
  };

  const handleAwarenessUpdate = ({ added, updated, removed }, origin) => {
    if (origin === 'remote' || destroyed) return;
    const clients = [...added, ...updated, ...removed];
    if (clients.length > 0) callbacks.onAwarenessUpdate(encodeAwarenessUpdate(awareness, clients));
  };

  doc.on('update', handleDocumentUpdate);
  awareness.on('update', handleAwarenessUpdate);

  return {
    doc,
    text,
    awareness,
    applyDocumentUpdate(update) {
      Y.applyUpdate(doc, update, 'remote');
    },
    applyAwarenessUpdate(update) {
      applyAwarenessUpdate(awareness, update, 'remote');
    },
    encodeDocumentState() {
      return Y.encodeStateAsUpdate(doc);
    },
    encodeLocalAwareness() {
      return encodeAwarenessUpdate(awareness, [doc.clientID]);
    },
    destroy() {
      if (destroyed) return;
      flushDocumentUpdates();
      destroyed = true;
      doc.off('update', handleDocumentUpdate);
      awareness.off('update', handleAwarenessUpdate);
      awareness.destroy();
      doc.destroy();
    },
  };
};
