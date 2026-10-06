# Architecture

## System Overview
```
React Frontend (CodeMirror 6 + Yjs) 
      ←→ [WebSocket STOMP] ←→ 
Spring Boot Backend (Yjs Persistence Service + Execution API)
      ↓
[Docker Executor Service] ←→ [PostgreSQL]
```

## Core Components

### Collaboration Service (Backend)
- STOMP-over-WebSocket messaging with SockJS fallback
- Yjs backend provider managing document-specific Yjs instances
- Document room management (isolated collaboration spaces)
- Presence awareness tracking (cursors, selections, active users)
- Persistence layer with debounced writes to PostgreSQL
- JWT-based authentication for WebSocket connections
- Message broadcasting to all clients in a document room

### Secure Execution Service (Backend)
- Docker orchestrator managing container lifecycle
- Language-specific runners (Java, Python, JavaScript, etc.)
- Resource-constrained execution (CPU, memory, time)
- Security-hardened containers (non-root, read-only FS, dropped caps)
- Output streaming (stdout/stderr) back to clients via WebSocket
- Compilation error handling and runtime exception capture
- Language detection and version management

### Frontend (React)
- CodeMirror 6 editor with syntax highlighting/themes
- Remote cursors/presence via Yjs awareness
- Execution controls (run, language selector, output console)
- Connection status indicators with automatic reconnection