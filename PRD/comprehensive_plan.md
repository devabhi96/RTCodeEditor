# Real-Time Collaborative Code Editor - Project Information

## 📋 Executive Summary
This document outlines the plan for building a real-time collaborative code editor that allows multiple users to simultaneously edit code with low-latency synchronization. The solution uses WebSockets for real-time communication, Java Spring Boot for the backend, React for the frontend, and Docker for containerization. Key innovations include CRDT-based conflict resolution (using Yjs) and secure code execution in isolated Docker containers.

## 🎯 Project Goals
1. Enable real-time collaborative code editing with sub-second latency
2. Provide a rich code editing experience with syntax highlighting and themes
3. Ensure secure execution of user code through containerization
4. Support multiple programming languages for code execution
5. Enable horizontal scaling for growing user bases
6. Provide a seamless developer and user experience

## 🛠️ Technology Stack & Justification

### Frontend: React 18 + CodeMirror 6
- **Why React**: Mature ecosystem, excellent performance with concurrent rendering, large talent pool
- **Why CodeMirror 6**: 
  - Modular architecture (~1MB bundle vs 2-4MB for Monaco)
  - Excellent collaborative editing support via `@codemirror/yjs`
  - Better performance for large files
  - Active development and good documentation
  - Can achieve VS Code-like experience with themes and plugins
- **Alternative Considered**: Monaco Editor (rejected due to bundle size and complexity for initial MVP)

### Backend: Java Spring Boot 3.x
- **Why Spring Boot**: 
  - Mature, well-documented technology stack
  - Built-in WebSocket support with STOMP over SockJS
  - Easy message routing with `@MessageMapping`
  - Spring Security integration for authentication
  - Scalable with external message brokers (Redis/RabbitMQ)
  - Familiar to Java developers in target audience
- **Key Dependencies**:
  - `spring-boot-starter-websocket`
  - `spring-boot-starter-security`
  - `spring-boot-starter-data-jpa` (for persistence)
  - `lombok` (to reduce boilerplate)

### Collaboration: Yjs Library (CRDTs)
- **Why Yjs (CRDTs over OT)**:
  - Battle-tested in production (Figma, Notion, etc.)
  - Excellent performance and memory efficiency
  - Simpler mental model than Operational Transformation
  - Eventually consistent with strong guarantees
  - Good React integration (`@codemirror/yjs`)
  - Active maintenance and community
  - Supports awareness (cursors, presence) natively
- **CRDT Benefits**:
  - Automatic conflict resolution without central server arbitration
  - Eventual consistency guarantees
  - Network partition tolerance
  - Peer-to-peer capable (though we use client-server for simplicity)

### Execution: Dockerized Service
- **Why Docker for Code Execution**:
  - Complete isolation from host system
  - Resource limitation capabilities (CPU, memory, time)
  - Consistent execution environment
  - Easy setup for multiple language runtimes
- **Security Measures**:
  - Run as non-root user (UID 1000)
  - Read-only root filesystem with tmpfs for /tmp
  - Drop all Linux capabilities except NET_RAW, SETGID, SETUID
  - No networking access
  - No new privileges
  - Seccomp and AppArmor profiles (where supported)
- **Resource Limits**:
  - CPU: 500ms time slice per execution
  - Memory: 256MB maximum
  - Execution timeout: 5 seconds
  - File system writes: Limited to /tmp directory

### DevOps & Infrastructure
- **Docker Compose**: Local development orchestration
- **Multi-stage Dockerfiles**: Production-optimized images
- **PostgreSQL**: Document persistence (optional for MVP)
- **NGINX**: Reverse proxy and static file serving (in production)

## 🏗️ System Architecture

```
┌─────────────────────────────────────┐    WebSocket (STOMP)    ┌──────────────────────────────────┐
│         React Frontend              │◄───────────────────────►│      Spring Boot Backend         │
│  (CodeMirror 6 Editor + Yjs)        │◄───── Yjs Updates ──────│  (WebSocket API + REST API)    │
│                                     │                         │                                  │
└─────────────────────────────────────┘                         └──────────────────────────────────┘
        │                                           │
        │                                           │
        ▼                                           ▼
┌─────────────────────────────────┐   ◄─────── API ─────►┌──────────────────────────────────┐
│       Docker Executor           │                      │     Execution Service            │
│  (Constrained Containers)       │                      │  (Orchestrator + Language Runners) │
└─────────────────────────────────┘                      └──────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────┐
│        PostgreSQL               │
│  (Document Persistence - Opt)   │
└─────────────────────────────────┘
```

## 🧩 Core Components

### 1. Collaboration Service (Backend)
**Responsibilities**: Manage real-time synchronization, document state, and user presence
**Key Features**:
- STOMP-over-WebSocket messaging with SockJS fallback
- Yjs backend provider managing document-specific Yjs instances
- Document room management (isolated collaboration spaces)
- Presence awareness tracking (cursors, selections, active users)
- Persistence layer with debounced writes to PostgreSQL
- JWT-based authentication for WebSocket connections
- Message broadcasting to all clients in a document room

**Key Classes**:
- `CollaborationController`: Handles `/app/doc.update` and presence messages
- `YjsPersistenceService`: Manages Yjs document lifecycle and persistence
- `DocumentService`: Handles document metadata and access control
- `WebSocketConfig`: STOMP configuration with security and CORS
- `SecurityConfig`: JWT authentication configuration

### 2. Secure Execution Service (Backend)
**Responsibilities**: Safely execute user code in isolated environments
**Key Features**:
- Docker orchestrator managing container lifecycle
- Language-specific runners (Java, Python, JavaScript, etc.)
- Resource-constrained execution (CPU, memory, time)
- Security-hardened containers (non-root, read-only FS, dropped caps)
- Output streaming (stdout/stderr) back to clients via WebSocket
- Compilation error handling and runtime exception capture
- Language detection and version management

**Key Classes**:
- `ExecutionService`: Public API for code execution requests
- `DockerOrchestrator`: Low-level Docker container management
- `LanguageRunner` interface and implementations per language
- `ExecutionRequest`/`ExecutionResponse`: DTOs for execution API

### 3. Frontend (React)
**Responsibilities**: Provide user interface for code editing and collaboration
**Key Features**:
- CodeMirror 6 editor with syntax highlighting and themes
- Real-time remote cursors and presence indicators (via Yjs awareness)
- Execution controls (language selector, run button, output console)
- Connection status indicators with automatic reconnection
- Responsive layout working on desktop and tablet
- Accessible design with keyboard navigation
- Loading states and error handling

**Key Components**:
- `EditorComponent`: Wrapper around CodeMirror 6 with Yjs integration
- `CollaborationPanel`: Shows connected users and presence indicators
- `ExecutionConsole`: Displays code execution output and controls
- `WebSocketService`: Manages STOMP connection and messaging
- `YjsService`: Integrates Yjs with CodeMirror 6 editor
- `App.js`: Main application component with routing and state

## 📅 Implementation Roadmap

### Phase 1: Foundation (Weeks 1-2)
**Goal**: Establish basic real-time text synchronization
- [ ] Set up Spring Boot project with WebSocket and Security
- [ ] Initialize React project with Vite and CodeMirror 6
- [ ] Implement basic STOMP connection between client and server
- [ ] Create simple text synchronization (last-write-wins MVP)
- [ ] Set up development environment with hot reloading
- [ ] Create Dockerfiles for backend and frontend
- [ ] Implement basic docker-compose.yml for local development
- [ ] Test: Two users can see each other's text changes in real-time

### Phase 2: CRDT Collaboration (Weeks 3-4)
**Goal**: Implement robust conflict-free collaboration
- [ ] Integrate Yjs into frontend CodeMirror 6 editor
- [ ] Create Yjs backend provider in Spring Boot
- [ ] Implement document rooms (separate Yjs instances per doc ID)
- [ ] Persist Yjs document state to PostgreSQL (debounced writes)
- [ ] Load initial document state from database on client connect
- [ ] Implement user presence awareness (cursors, selections)
- [ ] Add visual indicators for remote cursors and user avatars
- [ ] Test: Collaborative editing with conflict resolution works correctly

### Phase 3: Secure Execution (Weeks 5-6)
**Goal**: Add secure code execution capabilities
- [ ] Design execution API (execute code → return output)
- [ ] Create Dockerfile for executor service (multi-language support)
- [ ] Implement resource limits via Docker run flags
- [ ] Add security hardening (non-root user, read-only FS, dropped capabilities)
- [ ] Create execution service endpoint in Spring Boot
- [ ] Stream execution output back to client via WebSocket
- [ ] Handle timeouts, errors, and language-specific compilation
- [ ] Test: Users can run code securely and see output in real-time

### Phase 4: Polish & Deploy (Weeks 7-8)
**Goal**: Enhance features and prepare for production
- [ ] Support multiple programming languages in editor (syntax highlighting)
- [ ] Implement JWT authentication for WebSocket and REST endpoints
- [ ] Optimize performance (debouncing, batching updates)
- [ ] Add file tree sidebar for multi-file projects (optional)
- [ ] Implement undo/redo history synchronization
- [ ] Add comprehensive logging and monitoring
- [ ] Create production Docker Compose with NGINX reverse proxy
- [ ] Write comprehensive documentation and setup guides
- [ ] Test: End-to-end scenarios with multiple users executing code

## 📁 Key Files to Create

### Backend (Spring Boot)
```
src/main/java/com/collabeditor/
├── CollabEditorApplication.java
├── config/
│   ├── WebSocketConfig.java
│   └── SecurityConfig.java
├── controller/
│   └── CollaborationController.java
├── service/
│   ├── YjsPersistenceService.java
│   ├── ExecutionService.java
│   └── DocumentService.java
├── docker/
│   └── DockerOrchestrator.java
├── model/
│   ├── Document.java
│   ├── ExecutionRequest.java
│   └── ExecutionResponse.java
└── repository/
    └── DocumentRepository.java
```

### Frontend (React)
```
src/
├── main.jsx
├── App.jsx
├── components/
│   ├── EditorComponent.jsx
│   ├── CollaborationPanel.jsx
│   ├── ExecutionConsole.jsx
│   └── FileTree.jsx (optional)
├── services/
│   ├── WebSocketService.js
│   └── YjsService.js
├── utils/
│   └── debounce.js
└── styles/
    └── main.css
```

### DevOps & Infrastructure
```
- Dockerfile.backend
- Dockerfile.frontend
- Dockerfile.executor
- docker-compose.yml
- nginx/
  └── nginx.conf (for production)
- .github/workflows/ (CI/CD - optional)
- docs/
  └── SETUP.md
```

## ✅ Verification Strategy

### Unit Testing
- Test Yjs convergence with various concurrent edit scenarios
- Test Docker resource limits and security constraints
- Test WebSocket message handling and broadcasting
- Test execution service with various languages and error cases
- Test JWT authentication and authorization

### Integration Testing
- Test end-to-end collaboration: two users editing same document
- Test secure execution: run code in container and verify correct output
- Test conflict resolution: simultaneous edits from multiple users converge
- Test persistence: document state saved and loaded correctly
- Test automatic reconnection after network interruption

### End-to-End Testing
- Simulate multiple users in different browser tabs editing same file
- Verify all clients converge to same document state after edits
- Verify code execution works correctly and securely
- Test performance with increasing number of concurrent users (load testing)
- Verify UI remains responsive under load

### Security Testing
- Attempt to break out of Docker container (should fail)
- Test resource exhaustion attacks (should be limited by Docker constraints)
- Verify malicious code cannot harm host system or access unauthorized resources
- Test WebSocket connection validation and authentication
- Check for XSS vulnerabilities in code display
- Verify no sensitive information leakage through error messages

## 📊 Success Metrics & KPIs

### Performance Targets
- **Latency**: <200ms for code change propagation between clients
- **Startup Time**: <3s for initial editor load
- **Execution Time**: <5s for code execution (including container startup)
- **Concurrent Users**: Support 50+ simultaneous collaborators per document
- **Memory Usage**: <500MB per client browser instance
- **Server Usage**: <200MB RAM + <20% CPU per 50 concurrent users

### Quality Targets
- **Code Quality**: >80% test coverage, no critical security vulnerabilities
- **User Experience**: >4.0/5.0 satisfaction in usability testing
- **Reliability**: 99.9% uptime for collaboration service
- **Correctness**: 100% convergence in collaborative editing scenarios

### Adoption Targets
- **Time to First Edit**: <10 seconds from page load to collaborative editing
- **Feature Discovery**: Users can execute code without documentation
- **Error Recovery**: Automatic recovery from transient network issues

## ❓ Open Questions for User

### Technical Decisions
1. **Initial Language Support**: Which languages should the execution service support initially?
   - Suggested: Java, Python, JavaScript (covers major use cases)
   - Additional options: C++, Go, Ruby, PHP

2. **Authentication Level**: What level of authentication is required for the MVP?
   - Option A: None (for local development/testing only)
   - Option B: Basic JWT-based authentication (recommended for sharing)
   - Option C: Full OAuth2 integration (Google/GitHub login)

3. **Persistence Requirements**: Is document persistence required for the MVP?
   - Option A: In-memory only (data lost on server restart)
   - Option B: PostgreSQL persistence (recommended for any sharing)
   - Option C: Hybrid approach (recent documents in DB, cache for performance)

4. **Advanced Features**: Which advanced features should be included in V1?
   - Option A: Core collaboration + execution only (lean MVP)
   - Option B: Plus syntax highlighting for 5+ languages
   - Option C: Plus file tree sidebar for multi-file projects
   - Option D: Plus undo/redo history and conflict resolution visualization

### Deployment & Operations
5. **Deployment Target**: Where should this be deployed initially?
   - Option A: Local development only (Docker Compose)
   - Option B: Single server deployment (Docker on VPS/cloud VM)
   - Option C: Kubernetes production deployment from start

6. **Monitoring & Analytics**: What level of observability is required?
   - Option A: Basic logging and health checks
   - Option B: Plus Prometheus metrics and Grafana dashboards
   - Option C: Plus distributed tracing and error tracking

## 📎 Appendices

### Appendix A: Research References
- **CRDTs vs OT**: https://systeminternals.dev/system-design/collaborative-editing/
- **Yjs Documentation**: https://docs.yjs.dev/
- **Spring Boot WebSocket Best Practices**: Derived from Scribble-clone project analysis
- **CodeMirror 6 Collaboration**: https://codemirror.net/docs/ref/#collab
- **Docker Security Best Practices**: CIS Docker Benchmark

### Appendix B: Alternative Technologies Considered
| Category | Option Evaluated | Decision | Reason |
|----------|------------------|----------|--------|
| Frontend Editor | Monaco Editor | Rejected | Larger bundle (2-4MB), more complex |
| Frontend Editor | Ace Editor | Rejected | Limited collaboration ecosystem |
| Backend WebSocket | Socket.io | Rejected | Less standard, harder to scale with Spring Boot |
| Collaboration | ShareJS (OT) | Rejected | More complex conflict resolution |
| Database | MongoDB | Rejected | Overkill for document storage, SQL sufficient |
| Message Broker | Redis Pub/Sub | Selected | Good balance of performance and simplicity |
| Message Broker | RabbitMQ | Alternative | More robust but heavier weight |

### Appendix C: Glossary
- **CRDT**: Conflict-free Replicated Data Type - data structure that automatically resolves conflicts
- **Yjs**: JavaScript library implementing CRDTs for collaborative applications
- **STOMP**: Simple Text Oriented Messaging Protocol - framework over WebSocket
- **SockJS**: WebSocket polyfill providing fallback options for older browsers
- **MVP**: Minimum Viable Product - initial version with core features
- **Debouncing**: Technique to limit rate of function calls (used for persistence writes)

## 📝 Next Steps
1. Review this plan with stakeholders and technical leads
2. Provide answers to the open questions above
3. Approve the technology stack and implementation approach
4. Begin Phase 1: Foundation development
5. Schedule regular check-ins to review progress and adjust plan as needed

---
*This document is a living artifact and will be updated as the project progresses and new information becomes available.*
*Last updated: 2026-10-06*