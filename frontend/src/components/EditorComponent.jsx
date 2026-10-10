import React, { useEffect, useRef, useState } from 'react';
import { EditorView } from '@codemirror/view';
import { basicSetup } from 'codemirror';
import { yCollab } from 'y-codemirror.next';
import { base64ToBytes, useWebSocketService } from '../services/WebSocketService';
import { createYjsDocument, mergeDocumentUpdates } from '../services/YjsService';

const queueDocumentUpdate = (outbox, documentId, update) => {
  const previousUpdate = outbox.get(documentId);
  outbox.set(documentId, previousUpdate ? mergeDocumentUpdates([previousUpdate, update]) : update);
};

const decodeDocumentBundle = (bundle) => {
  const updates = [];
  let offset = 0;

  while (offset < bundle.length) {
    if (offset + 4 > bundle.length) throw new Error('Incomplete document update length prefix');
    const length = new DataView(bundle.buffer, bundle.byteOffset + offset, 4).getUint32(0, true);
    offset += 4;
    if (length === 0 || offset + length > bundle.length) throw new Error('Invalid document update bundle');
    updates.push(bundle.subarray(offset, offset + length));
    offset += length;
  }

  return updates;
};

const createGuest = (clientId) => {
  const hue = (clientId * 137.508) % 360;
  const color = `hsl(${hue} 68% 46%)`;
  return {
    name: `Guest ${clientId.toString(16).slice(-4)}`,
    color,
    colorLight: `hsl(${hue} 68% 46% / 16%)`,
  };
};

const EditorComponent = ({ documentId }) => {
  const editorHostRef = useRef(null);
  const editorViewRef = useRef(null);
  const yjsRef = useRef(null);
  const pendingUpdatesRef = useRef(new Map());
  const [collaborators, setCollaborators] = useState([]);
  const { connected, clientSessionId, connectionError, sendBytes, sendMessage, subscribeToTopic } = useWebSocketService();

  useEffect(() => {
    const yjs = createYjsDocument({
      onDocumentUpdate: (update) => {
        const hasPendingUpdate = pendingUpdatesRef.current.has(documentId);
        if (hasPendingUpdate) queueDocumentUpdate(pendingUpdatesRef.current, documentId, update);
        if (!sendBytes('/document.update', documentId, update) && !hasPendingUpdate) {
          queueDocumentUpdate(pendingUpdatesRef.current, documentId, update);
        }
      },
      onAwarenessUpdate: (update) => {
        sendBytes('/document.awareness', documentId, update);
      },
    });
    yjsRef.current = yjs;

    const pendingUpdate = pendingUpdatesRef.current.get(documentId);
    if (pendingUpdate) yjs.applyDocumentUpdate(pendingUpdate);

    yjs.awareness.setLocalStateField('user', createGuest(yjs.doc.clientID));

    const view = new EditorView({
      doc: yjs.text.toString(),
      extensions: [basicSetup, yCollab(yjs.text, yjs.awareness)],
      parent: editorHostRef.current,
    });
    editorViewRef.current = view;

    return () => {
      yjs.awareness.setLocalState(null);
      view.destroy();
      yjs.destroy();
      editorViewRef.current = null;
      yjsRef.current = null;
      pendingUpdatesRef.current.clear();
      setCollaborators([]);
    };
  }, [documentId, sendBytes]);

  useEffect(() => {
    if (!connected || !yjsRef.current) return;

    const yjs = yjsRef.current;
    const updateSubscription = subscribeToTopic(`/topic/document/${documentId}`, (message) => {
      if (message.documentId !== documentId || typeof message.update !== 'string') return;
      try {
        yjs.applyDocumentUpdate(base64ToBytes(message.update));
      } catch (error) {
        console.error(`Invalid document update for ${documentId}`, error);
      }
    });
    const stateSubscription = subscribeToTopic('/user/queue/document/state', (message) => {
      if (message.documentId !== documentId || typeof message.update !== 'string') return;
      try {
        decodeDocumentBundle(base64ToBytes(message.update)).forEach((update) => yjs.applyDocumentUpdate(update));
        const fullState = yjs.encodeDocumentState();
        if (sendBytes('/document.update', documentId, fullState)) {
          pendingUpdatesRef.current.delete(documentId);
        } else {
          queueDocumentUpdate(pendingUpdatesRef.current, documentId, fullState);
        }
      } catch (error) {
        console.error(`Invalid document state for ${documentId}`, error);
      }
    });
    const awarenessSubscription = subscribeToTopic(`/topic/document/${documentId}/awareness`, (message) => {
      if (message.documentId !== documentId || typeof message.update !== 'string') return;
      try {
        yjs.applyAwarenessUpdate(base64ToBytes(message.update));
      } catch (error) {
        console.error(`Invalid awareness update for ${documentId}`, error);
      }
    });
    const handleAwarenessChange = () => {
      const activeCollaborators = [...yjs.awareness.getStates()].flatMap(([clientId, state]) => (
        state.user ? [{ clientId, ...state.user }] : []
      ));
      setCollaborators(activeCollaborators);
    };
    yjs.awareness.on('change', handleAwarenessChange);
    handleAwarenessChange();

    sendMessage('/document.load', { documentId });
    sendBytes('/document.awareness', documentId, yjs.encodeLocalAwareness());

    return () => {
      updateSubscription?.unsubscribe();
      stateSubscription?.unsubscribe();
      awarenessSubscription?.unsubscribe();
      yjs.awareness.off('change', handleAwarenessChange);
    };
  }, [connected, clientSessionId, documentId, sendBytes, sendMessage, subscribeToTopic]);

  return (
    <div className="editor-container">
      <div ref={editorHostRef} className="editor-textarea" />
      <div className="collaboration-footer">
        <span className="connection-status">
          <span className={`connection-indicator${connected ? ' is-connected' : ''}`} aria-hidden="true" />
          {connected ? 'Connected' : `Disconnected${connectionError ? `: ${connectionError}` : ''}`}
        </span>
        <div className="collaborators" aria-label="Active collaborators">
          {collaborators.map(({ clientId, name, color }) => (
            <span className="collaborator-chip" key={clientId}>
              <span className="collaborator-dot" style={{ backgroundColor: color }} aria-hidden="true" />
              {name}
            </span>
          ))}
          {collaborators.length === 0 && <span>No collaborators online</span>}
        </div>
      </div>
    </div>
  );
};

export default EditorComponent;
