# Real-Time Collaborative Code Editor

## Project Overview
This project aims to build a real-time collaborative code editor that enables multiple users to simultaneously edit code with low-latency synchronization, similar to tools like VS Code Live Share or Google Docs for code.

## Vision
To provide developers with a seamless, secure, and efficient platform for real-time code collaboration, featuring:
- Real-time synchronization with conflict resolution
- Secure code execution in isolated environments
- Rich code editing experience with syntax highlighting
- Cross-platform accessibility through web technologies

## Key Features
1. **Real-Time Collaboration**: Multiple users can edit the same code file simultaneously with changes propagating instantly
2. **Conflict Resolution**: Uses CRDTs (Yjs library) for automatic, conflict-free merging of concurrent edits
3. **Secure Code Execution**: User code runs in isolated Docker containers with strict security constraints
4. **Rich Editing Experience**: Syntax highlighting, themes, and intelligent code assistance
5. **Cross-Language Support**: Execute code in multiple programming languages (Java, Python, JavaScript, etc.)
6. **Presence Awareness**: See where others are editing and their cursors/selections
7. **Persistence**: Optional document saving to PostgreSQL for recovery and history

## Technology Stack
- **Frontend**: React 18 + CodeMirror 6 with Yjs integration
- **Backend**: Java Spring Boot 3.x with WebSocket (STOMP over SockJS)
- **Collaboration**: Yjs library for CRDT-based conflict resolution
- **Execution**: Dockerized service with security hardening and resource limits
- **DevOps**: Docker Compose for orchestration, multi-stage builds
- **Storage**: Optional PostgreSQL for document persistence

## Target Users
- Development teams practicing pair or mob programming
- Educational institutions for collaborative coding exercises
- Open-source contributors working remotely
- Software architects conducting design sessions
- Anyone needing real-time code collaboration

## Non-Goals (for MVP)
- Advanced IDE features (debugging, refactoring tools)
- Built-in chat or video conferencing
- Complex project management features
- Mobile-native applications (web-responsive only)

## Success Criteria
- Sub-200ms latency for code change propagation
- Support for 50+ concurrent collaborators per document
- Secure execution preventing host system compromise
- Intuitive user experience requiring minimal training
- Reliable operation with automatic recovery from network issues

## Related Documentation
- [Comprehensive Plan](./comprehensive_plan.md) - Detailed implementation strategy
- Architecture decisions, requirements, and API specifications to be added as the project progresses