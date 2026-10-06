import React, { useEffect, useRef, useState } from 'react';
import { useWebSocketService } from '../services/WebSocketService';

const EditorComponent = ({ documentId }) => {
  const [content, setContent] = useState('');
  const editorRef = useRef(null);
  const { connected, sendMessage, subscribeToTopic } = useWebSocketService();

  useEffect(() => {
    // Subscribe to document updates when connected
    if (connected) {
      const subscription = subscribeToTopic(`/topic/document/${documentId}`, (data) => {
        // Update editor with received content (last-write-wins for MVP)
        if (editorRef.current && editorRef.current.value !== data.content) {
          editorRef.current.value = data.content;
          setContent(data.content);
        }
      });

      return () => {
        subscription.unsubscribe();
      };
    }
  }, [connected, documentId, subscribeToTopic]);

  const handleChange = (e) => {
    const newContent = e.target.value;
    setContent(newContent);

    // Send content to server via WebSocket (last-write-wins for MVP)
    if (connected) {
      sendMessage(`/app/document.update`, {
        documentId: documentId,
        content: newContent,
        timestamp: new Date().toISOString()
      });
    }
  };

  return (
    <div className="editor-container">
      <textarea
        ref={editorRef}
        value={content}
        onChange={handleChange}
        placeholder="Start typing here..."
        className="editor-textarea"
      />
      {!connected && <div className="connection-status">Disconnected</div>}
      {connected && <div className="connection-status">Connected</div>}
    </div>
  );
};

export default EditorComponent;