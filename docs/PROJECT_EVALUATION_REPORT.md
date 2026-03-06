# CipherMesh — Project Evaluation Report
### Complete Q&A Guide for Mentor/Evaluator Sessions

---

## TABLE OF CONTENTS

1. [Project Overview & Problem Statement](#1-project-overview--problem-statement)
2. [Architecture & Design Decisions](#2-architecture--design-decisions)
3. [Technology Stack Justification](#3-technology-stack-justification)
4. [Security Design](#4-security-design)
5. [Backend Deep-Dive](#5-backend-deep-dive)
6. [Frontend Deep-Dive](#6-frontend-deep-dive)
7. [Database Design](#7-database-design)
8. [API Design](#8-api-design)
9. [WebSocket & Real-Time Communication](#9-websocket--real-time-communication)
10. [Bugs Encountered & How They Were Fixed](#10-bugs-encountered--how-they-were-fixed)
11. [What Is Pending / Roadmap](#11-what-is-pending--roadmap)
12. [Scalability & Production Readiness](#12-scalability--production-readiness)
13. [Code Quality & Best Practices](#13-code-quality--best-practices)
14. [Quick-Fire Q&A Bank](#14-quick-fire-qa-bank)

---

## 1. Project Overview & Problem Statement

### Q: What is CipherMesh? Summarize it in 2 sentences.
**A:** CipherMesh is an anonymous, end-to-end encrypted group chat platform where the server acts as a pure relay — it routes messages without ever being able to read them. Users join or create themed public/private rooms without creating a traditional account; their identity is an ephemeral cryptographic keypair generated in-browser.

### Q: What problem does this solve?
**A:** Most chat platforms (Slack, Discord, WhatsApp Web) store user identity, message history, and metadata on their servers. CipherMesh solves the trust problem — a user does not need to trust the server operator because the server is architecturally incapable of reading message content. This is relevant for journalists, activists, whistleblowers, or anyone who values communication privacy.

### Q: Who is the target user?
**A:** Privacy-conscious users who want to join topic-based chat rooms (technology, gaming, music, etc.) without handing over a phone number, email, or real name. The flow is: open site → generate identity → browse rooms → chat.

### Q: Is this just a clone of an existing product?
**A:** The room discovery and browse experience is inspired by platforms like Discord, but the core architecture is different — Discord's servers store all message history; CipherMesh is designed so messages are encrypted before they leave the browser and the relay server never holds plaintext.

---

## 2. Architecture & Design Decisions

### Q: Draw the high-level architecture.
**A:**
```
Browser (Client)
   │  Web Crypto API — ECDH P-256 keygen
   │  SockJS + STOMP.js — WebSocket transport
   │  REST fetch() calls
   │
   ▼
Spring Boot Relay Server (Port 8080)
   ├── REST Layer: /api/auth/**, /api/v1/rooms/**
   ├── Security Layer: JwtAuthFilter (OncePerRequestFilter)
   ├── WebSocket Layer: STOMP broker (/ws endpoint, SockJS)
   ├── Business Logic: AuthService, RoomService
   └── Data Layer: Spring Data JPA → PostgreSQL
          └── Tables: users, rooms, room_tags, room_members

Frontend Static Server (Port 3000 — python3 -m http.server)
   └── 5 HTML pages served statically
```

### Q: Why is the server a "relay"? What does that mean architecturally?
**A:** A relay means the server routes/forwards messages between clients but does not store or decrypt them. The design intention is:
- Client encrypts with recipient's public key before `publish()` call
- Server calls `stompClient.subscribe` → `stompClient.publish` — passing the ciphertext blob
- Server logs never write message payloads
- `RelayServerApplication.java` logs on startup: *"acting as relay only, no message decryption"*

The `MessageRelayHandler.java` is currently a stub — the relay logic is the next implementation milestone.

### Q: Why separate the frontend from the backend instead of serving static files from Spring Boot?
**A:** 
1. **Separation of concerns** — frontend can be deployed to a CDN (Vercel, Cloudflare Pages) while backend scales independently on a server/container.
2. **Development speed** — `python3 -m http.server` hot-reloads HTML without restarting the JVM.
3. **Future-proofing** — if the frontend migrates to React/Next.js, the backend API stays unchanged.

### Q: Why use Spring Boot and not Node.js/Express for a chat server?
**A:** Three reasons:
1. **Type safety** — Java's static typing catches bugs at compile time; critical for security-sensitive code paths.
2. **Spring Security** — mature, battle-tested security framework with JWT filter chain, CSRF protection, CORS, and session management baked in.
3. **Spring Data JPA** — handles complex queries (paginated rooms, member counts, FETCH JOIN) with minimal boilerplate via JPQL annotations.

---

## 3. Technology Stack Justification

### Q: Why PostgreSQL and not MySQL or MongoDB?
**A:**
- **UUID primary keys** — PostgreSQL has native `uuid` column type; UUIDs prevent enumeration attacks (attacker cannot guess sequential IDs like 1, 2, 3...).
- **Relational integrity** — `room_members` is a join table; relational databases enforce foreign key constraints naturally.
- **JSONB if needed later** — PostgreSQL supports document-style storage for encrypted message payloads without migrating databases.

### Q: Why jjwt 0.12.6 specifically?
**A:** jjwt 0.12.x introduced a new non-deprecated API (`Jwts.builder().subject().signWith().compact()`). Older versions (0.9.x) had deprecated methods that flagged compiler warnings and were known to have weaker defaults. 0.12.6 is the latest stable release at time of development.

### Q: Why Tailwind CSS instead of a component library like Material UI or Bootstrap?
**A:** 
- No JavaScript runtime dependency — Tailwind is a CSS utility framework; it generates static CSS at build time (CDN version for prototyping).
- Full design control — the glassmorphism dark theme (mesh gradients, backdrop-blur, neon glow buttons) would require heavy overrides in Bootstrap.
- The dark theme config is defined in one place: `tailwind.config = { darkMode: "class", theme: { extend: { colors: { primary: "#7B2FBE", accent: "#00D4FF" } } } }`.

### Q: Why use STOMP over raw WebSocket?
**A:**
| Raw WebSocket | STOMP over WebSocket |
|---|---|
| Binary/text frames only | Structured headers + body (like HTTP) |
| No routing — all messages to all handlers | Destination-based routing (/topic, /queue, /user) |
| No pub/sub | Built-in pub/sub broker |
| No heartbeat protocol | Heartbeat negotiation built-in |

STOMP lets the server route a message to `/topic/room.{roomId}` (broadcast to room) or `/user/queue/messages` (direct to a specific user) without custom routing code.

### Q: What is SockJS and why is it needed?
**A:** SockJS is a WebSocket emulation library. In corporate environments, firewalls and proxies often block raw WebSocket upgrades. SockJS falls back to HTTP long-polling, Server-Sent Events, or iframe-based transport automatically. This makes the app work even in restrictive network environments.

---

## 4. Security Design

### Q: What are the top 5 security decisions in this project?

**A:**

**1. UUID Primary Keys**  
All entities use `@GeneratedValue(strategy = GenerationType.UUID)`. This prevents Insecure Direct Object Reference (IDOR) attacks — an attacker cannot enumerate users or rooms by incrementing an integer ID.

**2. Anonymous-First Authentication**  
No passwords stored for users. `AuthService.register()` accepts only `{username, identityPublicKey}`. The JWT is the proof of identity. Comment in code: *"Login uses username lookup only; identity key verification is intentionally deferred to a challenge-response scheme"* — flagged as a TODO for hardening.

**3. JWT Never Logged**  
In `JwtService.java`: *"Security: token content is never logged here"*. In `JwtAuthFilter.java`: *"The raw token string is NEVER logged"*. In `validateToken()` failure: *"log.warn("JWT validation failed: {}", ex.getClass().getSimpleName())"* — only the exception class name is logged, never the token.

**4. Stateless Session + CSRF Disabled**  
SecurityConfig: `SessionCreationPolicy.STATELESS` means no `JSESSIONID` cookie is created. Since authentication is Bearer token only (no cookies), CSRF attacks are not applicable and are explicitly disabled: `csrf(AbstractHttpConfigurer::disable)`.

**5. BCrypt for Room Passwords**  
Private rooms have a `passwordHash` field. `RoomServiceImpl` uses `BCryptPasswordEncoder.encode()` on creation and `passwordEncoder.matches()` on join — the raw password is never stored. `BCryptPasswordEncoder` uses adaptive work factor (default strength 10 = 2^10 iterations).

### Q: What is ECDH P-256 and why is it used?
**A:** ECDH (Elliptic Curve Diffie-Hellman) P-256 is a key agreement protocol. In CipherMesh:
1. When a user creates an identity, `window.crypto.subtle.generateKey({ name: 'ECDH', namedCurve: 'P-256' })` generates a keypair in the browser.
2. The **public key** is sent to the server as `identityPublicKey` (Base64-encoded) — safe to store.
3. The **private key** stays in `sessionStorage` — it **never leaves the device**.
4. When two users want to communicate, they can derive a shared secret using each other's public keys (Diffie-Hellman exchange), without either party's private key being transmitted.

### Q: What is the JWT token structure?
**A:** The token is HS256 signed with a secret of minimum 256 bits. Claims:
```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",  ← userId (UUID)
  "username": "SilentFox",
  "iat": 1709712000,
  "exp": 1709712900
}
```
- `sub` = userId (UUID, prevents IDOR)  
- Expiry = 900,000ms = **15 minutes** (configurable via `app.jwt.expiration-ms`)
- Secret is read from env var `JWT_SECRET` — never hardcoded in production

### Q: What are the public vs protected endpoints?
**A:**
| Endpoint | Auth Required | Reason |
|---|---|---|
| `POST /api/auth/register` | ❌ None | First visit |
| `POST /api/auth/login` | ❌ None | First visit |
| `GET /api/v1/rooms` | ❌ None | Landing page room listing |
| `GET /api/v1/rooms/count` | ❌ None | Landing page stats |
| `GET /api/v1/rooms/{id}` | ❌ None | Room detail preview |
| `POST /api/v1/rooms` | ✅ JWT | Must be registered to create |
| `POST /api/v1/rooms/{id}/join` | ✅ JWT | Must be registered to join |
| `DELETE /api/v1/rooms/{id}/leave` | ✅ JWT | Must be registered to leave |
| `/ws/**` | ❌ (handshake) | WebSocket upgrade |

### Q: How does the JWT filter work step by step?
**A:** `JwtAuthFilter extends OncePerRequestFilter`:
1. Read `Authorization` header — if absent or not starting with `Bearer `, skip (continue chain unauthenticated).
2. Call `jwtService.validateToken(token)` — parses and verifies signature + expiry; returns `false` instead of throwing.
3. If valid: extract `userId` (UUID) and `username` from claims.
4. Build `UsernamePasswordAuthenticationToken` with `ROLE_USER` authority.
5. Set on `SecurityContextHolder` — downstream code can call `.getAuthentication().getPrincipal()` to get the UUID.
6. If anything fails: `SecurityContextHolder.clearContext()` (fail-safe — never partially authenticate).

### Q: What security improvements are planned (TODOs)?
**A:**
1. **Challenge-response login** — server issues a nonce, client signs it with private key, server verifies with stored `identityPublicKey`. Currently login trusts username alone.
2. **Lock down CORS origins** — currently `allowedOriginPatterns("*")` for dev; must be restricted in production.
3. **Lock down WebSocket allowed origins** — same issue in `WebSocketConfig`.
4. **JWT refresh tokens** — `app.jwt.refresh-expiration-ms=604800000` is configured but the refresh endpoint is not implemented.
5. **Prekey bundle store** (Signal-style) — `KeyController.java` is a stub.

---

## 5. Backend Deep-Dive

### Q: Walk me through the Spring Boot module structure.
**A:**
```
com.ciphermesh/
├── RelayServerApplication.java       ← @SpringBootApplication entry point
├── auth/
│   ├── controller/AuthController.java  ← POST /api/auth/register, /login
│   ├── dto/                            ← RegisterRequest, LoginRequest, AuthResponse
│   ├── jwt/JwtService.java             ← Token generation + validation
│   ├── jwt/JwtAuthFilter.java          ← Per-request JWT filter
│   └── service/AuthService.java        ← register() and login() logic
├── common/
│   ├── exception/                      ← ConflictException, ResourceNotFoundException
│   ├── exception/GlobalExceptionHandler.java ← @RestControllerAdvice
│   └── response/ApiResponse.java       ← Standard response wrapper
├── keys/
│   └── controller/KeyController.java   ← STUB (Signal prekey store)
├── room/
│   ├── controller/RoomController.java  ← 6 endpoints
│   ├── dto/                            ← CreateRoomRequest, RoomResponse, JoinRoomRequest
│   ├── entity/Room.java                ← JPA entity
│   ├── repository/RoomRepository.java  ← JPQL queries
│   └── service/RoomServiceImpl.java    ← Business logic
├── security/
│   └── config/SecurityConfig.java      ← Filter chain, CORS, BCrypt bean
├── user/
│   ├── controller/UserController.java
│   ├── dto/, entity/, repository/, service/
│   └── entity/User.java                ← No passwords stored
└── websocket/
    ├── config/WebSocketConfig.java     ← STOMP + SockJS config
    └── handler/MessageRelayHandler.java ← STUB (relay logic pending)
```

### Q: What are the three annotations on RelayServerApplication and what do they do?
**A:**
- `@SpringBootApplication` — combines `@Configuration`, `@EnableAutoConfiguration`, `@ComponentScan`; bootstraps the entire Spring context.
- `@EnableJpaAuditing` — activates `@CreatedDate` / `@LastModifiedDate` auto-population in JPA entities (used in `User.createdAt`).
- `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)` — fixes `PageImpl` JSON serialization in Spring Boot 3. Without this, Spring Boot 3 throws a warning/error because `PageImpl` is not directly serializable as JSON; `VIA_DTO` converts it to `PageDTO` which is serializable.

### Q: How does the GlobalExceptionHandler work?
**A:** `@RestControllerAdvice` is a Spring annotation that intercepts exceptions thrown from any `@RestController`. Each `@ExceptionHandler` method maps an exception type to an HTTP response:
```
ResourceNotFoundException → 404 Not Found
ConflictException         → 409 Conflict
IllegalArgumentException  → 400 Bad Request
MethodArgumentNotValidException → 400 + field-level error messages
Exception (catchall)      → 500 Internal Server Error
```
All return `ApiResponse.error(message)` for consistent error shape. The catchall logs the full stack trace at ERROR level.

### Q: Explain the ApiResponse wrapper pattern.
**A:** Every endpoint returns `ResponseEntity<ApiResponse<T>>`. The wrapper has:
```json
{
  "success": true,
  "message": "Room created",
  "data": { ...actual payload... },
  "timestamp": "2026-03-06T10:00:00Z"
}
```
This gives frontend code a consistent shape to parse: always check `json.success`, always read from `json.data`. `@JsonInclude(NON_NULL)` means `message` is omitted when null (e.g., on simple GET responses).

**Exception:** `POST /api/auth/register` and `POST /api/auth/login` return a flat `AuthResponse` directly (not wrapped) because they were designed before the wrapper was added and the frontend already handles the flat shape.

### Q: What is the difference between @Transactional and @Transactional(readOnly=true)?
**A:**
- `@Transactional` — opens a read-write transaction. Hibernate tracks entity changes (dirty checking) and flushes at commit. Used for `create()`, `join()`, `leave()`.
- `@Transactional(readOnly=true)` — tells Hibernate "no writes expected"; it skips dirty checking (faster), and the underlying database connection can be routed to a read replica. Used for `listPublic()`, `getById()`, `countPublic()`.
- Class-level annotation in `AuthService`: `@Transactional(readOnly = true)` — methods that write override with `@Transactional`.

---

## 6. Frontend Deep-Dive

### Q: Walk through the user journey across all 5 pages.
**A:**

**Page 1: `index.html` (Landing)**  
- Calls `GET /api/v1/rooms?size=3` → shows 3 featured rooms
- Calls `GET /api/v1/rooms/count` → shows live room count in stats section
- Falls back to 3 hardcoded demo rooms if backend is unreachable
- CTA buttons navigate to `identity.html` or `rooms.html`

**Page 2: `identity.html` (Identity Generation)**  
- Calls `window.crypto.subtle.generateKey({ name: 'ECDH', namedCurve: 'P-256' })` — in-browser keypair
- Generates an anonymous username (e.g., "SilentFox#4821")
- Calls `POST /api/auth/register` with `{ username, identityPublicKey }`
- If 409 (username taken) → retries with `POST /api/auth/login`
- Stores `token`, `userId`, `username`, `identityPublicKey` in `sessionStorage`
- Redirects to `rooms.html` (or back to `chat.html?roomId=...` if came from a room invite)

**Page 3: `rooms.html` (Room Browser)**  
- Calls `GET /api/v1/rooms?page=0&size=9&category=...` with JWT header
- Category tabs: `all`, `technology`, `gaming`, `music`, `politics`, `science`, `random`
- Search box filters by name server-side
- `joinRoom(id, name)` calls `POST /api/v1/rooms/{id}/join` then redirects to `chat.html`
- Falls back to 10 hardcoded FALLBACK_ROOMS if API fails

**Page 4: `create-room.html` (Create Room)**  
- Form collects: name, description, category, max users, private toggle, password, tags
- Calls `POST /api/v1/rooms` with JWT header
- On success: reads `json.data.id` and redirects to `chat.html?roomId=...`

**Page 5: `chat.html` (Live Chat)**  
- Guards: if no `sessionStorage.token`, redirects to `identity.html`
- Connects via STOMP/SockJS to `http://localhost:8080/ws`
- Subscribes to `/topic/room.{roomId}` (room broadcast) and `/user/queue/messages` (DMs)
- Publishes JOIN event to `/app/room/{roomId}/join`
- Falls back to demo mode if STOMP connection fails

### Q: What is sessionStorage and why is it used instead of localStorage?
**A:** Both are browser key-value stores. The difference:
- `sessionStorage` — data is **cleared when the browser tab is closed**. Perfect for ephemeral anonymous identity — when you close the tab, your identity is gone.
- `localStorage` — data persists across browser restarts.

Using `sessionStorage` enforces the "no persistent accounts" design principle. Even if someone gets physical access to a device, closing the browser tab destroys the identity.

### Q: How does the frontend handle the backend being offline?
**A:** All API calls are wrapped in `try/catch`. On failure:
- `index.html` — renders 3 hardcoded featured rooms.
- `identity.html` — sets `token = 'offline-demo'` in sessionStorage, generates a local identity.
- `rooms.html` — renders from `FALLBACK_ROOMS` array (10 hardcoded rooms).
- `chat.html` — calls `loadDemoMode()`, loads DEMO_MESSAGES and DEMO_USERS, shows "Demo mode — messages are local only" system message.

This means the app is fully demonstrable without a running backend.

---

## 7. Database Design

### Q: What tables exist and what is their relationship?
**A:**
```
users
├── id (UUID PK)
├── username (VARCHAR 64, UNIQUE, INDEX)
├── identityPublicKey (TEXT)
└── createdAt (TIMESTAMP)

rooms
├── id (UUID PK)
├── name (VARCHAR 80, UNIQUE)
├── description (VARCHAR 300)
├── category (VARCHAR 40)
├── maxUsers (INT)
├── isPrivate (BOOLEAN)
├── passwordHash (VARCHAR, nullable)
├── creator_id (FK → users.id)
└── createdAt (TIMESTAMP)

room_tags
├── room_id (FK → rooms.id)
└── tag (VARCHAR 30)

room_members
├── room_id (FK → rooms.id)
└── user_id (FK → users.id)
```

Relationships:
- `rooms.creator_id` → `users.id` : Many-to-One (a room has one creator, a user can create many rooms)
- `room_members` : Many-to-Many join table (a room has many members, a user can be in many rooms)
- `room_tags` : `@ElementCollection` (tags are just strings, not a separate entity)

### Q: Why use UUID instead of auto-increment integers for PKs?
**A:** Three reasons:
1. **Security (IDOR prevention)** — UUID `550e8400-e29b-41d4-a716-446655440000` is impossible to guess; attacker cannot iterate `GET /api/v1/rooms/1`, `/rooms/2` etc.
2. **Distribution** — UUIDs can be generated on any node without a central sequence, useful for horizontal scaling or offline-first architectures.
3. **No information leakage** — sequential IDs leak business info (e.g., "user #10000 means they have about 10,000 users").

### Q: Why is `tags` an @ElementCollection with FetchType.EAGER and not a separate entity?
**A:** Tags are just strings — they have no identity, no relationships, no separate lifecycle. Making them a full `@Entity` (with their own table and PK) would be over-engineering. `@ElementCollection` maps them to a `room_tags` table with a composite key.

`FetchType.EAGER` is critical: if tags were `LAZY`, Jackson (JSON serializer) would try to access `room.tags` outside a Hibernate transaction → `LazyInitializationException`. Since tags are always needed in `RoomResponse`, EAGER loading is the correct choice here.

### Q: How is pagination implemented?
**A:** Spring Data `Pageable` + custom JPQL queries in `RoomRepository`:
```java
@Query(
  value = "SELECT r FROM Room r JOIN FETCH r.creator WHERE r.isPrivate = false",
  countQuery = "SELECT COUNT(r) FROM Room r WHERE r.isPrivate = false"
)
Page<Room> findByIsPrivateFalse(Pageable pageable);
```
The `countQuery` attribute is **critical** — without it, Hibernate tries to use the same FETCH JOIN query for counting, which causes `HHH90003004` warning and potentially incorrect results. The count query is a simple `COUNT(r)` without the JOIN FETCH.

The controller passes `PageRequest.of(page, size, Sort.by(DESC, "createdAt"))` — rooms sorted newest-first.

---

## 8. API Design

### Q: Why is the API versioned as /api/v1/?
**A:** API versioning allows breaking changes in a future `/api/v2/` without breaking existing clients. If the `RoomResponse` structure changes in v2, v1 clients continue working unchanged. It's a standard REST best practice for public-facing APIs.

### Q: How is the room list API called by the frontend?
**A:**
```
GET /api/v1/rooms?category=technology&search=crypto&page=0&size=9
Authorization: Bearer <JWT>

Response:
{
  "success": true,
  "data": {
    "content": [ { "id": "...", "name": "...", ... }, ... ],
    "totalElements": 42,
    "totalPages": 5,
    "number": 0,
    "size": 9
  }
}
```
Frontend reads `json.data.content` for the room array and `json.data.totalPages` for pagination controls.

### Q: How does room joining work end-to-end?
**A:**
1. Frontend calls `POST /api/v1/rooms/{id}/join` with `Authorization: Bearer <JWT>`
2. `JwtAuthFilter` validates token, extracts `userId` (UUID) from `sub` claim
3. `RoomController.joinRoom()` extracts userId from token, calls `roomService.join(roomId, userId, request)`
4. `RoomServiceImpl.join()`:
   - Fetches room with `findRoomWithMembers()` (initializes lazy members collection inside @Transactional)
   - Checks if user is already a member → return current state (idempotent)
   - Checks if room is full (`memberCount >= maxUsers`) → 409
   - If private: BCrypt checks provided password against `passwordHash` → 409 on mismatch
   - Adds user to `room.getMembers()`, saves, returns `RoomResponse`
5. Frontend navigates to `chat.html?roomId=...`

### Q: What HTTP status codes are used and why?
**A:**
| Code | When Used |
|---|---|
| 200 OK | Successful GET, join, leave |
| 201 Created | Room created (`POST /api/v1/rooms`) |
| 400 Bad Request | Validation failure (`@Valid` fails) |
| 404 Not Found | Room or user not found |
| 409 Conflict | Username taken, room name taken, wrong password, room full |
| 500 Internal Server Error | Unexpected exceptions |

---

## 9. WebSocket & Real-Time Communication

### Q: Describe the WebSocket architecture.
**A:**
```
Browser                         Spring Boot STOMP Broker
   │                                       │
   │──── SockJS handshake to /ws ─────────▶│
   │◀─── Upgrade to WebSocket ─────────────│
   │                                       │
   │──── STOMP CONNECT (Authorization: Bearer ...) ─▶│
   │                                       │
   │──── SUBSCRIBE /topic/room.{roomId} ───▶│ ← room broadcast
   │──── SUBSCRIBE /user/queue/messages ───▶│ ← personal messages
   │                                       │
   │──── PUBLISH /app/room/{id}/join ──────▶│ → MessageRelayHandler
   │──── PUBLISH /app/room/{id}/send ──────▶│ → MessageRelayHandler
   │                                       │
   │◀─── MESSAGE /topic/room.{roomId} ─────│ ← broadcast to all in room
```

### Q: What is the difference between /topic and /queue and /user in STOMP?
**A:**
- `/topic/room.{id}` — **broadcast** destination. A message published here is delivered to **all subscribers**. Used for room chat messages.
- `/queue/messages` — **point-to-point** destination. Used for direct messages to one user.
- `/user/queue/messages` — **user-specific** destination. Spring's `UserDestinationMessageHandler` prepends the connected user's session ID, so `/user/queue/messages` from one browser reaches only that specific connection.

### Q: Is the WebSocket fully implemented?
**A:** **No — partially.** The configuration is complete and the client can connect and subscribe. The `MessageRelayHandler.java` is currently a **stub**. What works:
- SockJS handshake and STOMP protocol establishment
- Subscription to topics
- Client-side publish calls reach the `/app` destination prefix
- STOMP broker config (in-memory broker, destinations, user prefix)

What is pending:
- `@MessageMapping("/room/{roomId}/send")` handler that reads, validates, and broadcasts the message to `/topic/room.{roomId}`
- User presence tracking (join/leave events)
- The in-memory broker would need to be replaced with RabbitMQ/Redis for multi-node deployment

### Q: What happens when WebSocket connection fails in the client?
**A:** The `onStompError` callback calls `loadDemoMode()`. In demo mode:
- A list of pre-baked `DEMO_MESSAGES` is rendered
- `DEMO_USERS` list is shown in the sidebar
- A system message: "Demo mode — messages are local only" is displayed
- User can still type and send — messages appear locally but are not transmitted
- The reconnect delay is set to 5000ms — STOMP client retries automatically

---

## 10. Bugs Encountered & How They Were Fixed

### Q: What were the hardest bugs you fixed?

**Bug 1: PageImpl JSON Serialization (Spring Boot 3)**  
*Symptom:* `GET /api/v1/rooms` returned a 500 error with Jackson serialization error for `PageImpl`.  
*Root cause:* Spring Boot 3 changed how `Page` objects serialize. `PageImpl` is not directly serializable.  
*Fix:* Added `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)` to `RelayServerApplication`. This instructs Spring to serialize Page via a DTO proxy (`PageDTO`) which Jackson can handle.

**Bug 2: LazyInitializationException on room.tags**  
*Symptom:* Fetching a room worked, but the `tags` field was always empty or caused an exception.  
*Root cause:* `@ElementCollection` defaults to `FetchType.LAZY`. When Jackson serialized the Room object, the Hibernate session was already closed → could not load lazy data.  
*Fix:* Changed `@ElementCollection(fetch = FetchType.EAGER)` on `Room.tags`. Since tags are always needed in the response, EAGER is appropriate.

**Bug 3: HHH90003004 — FETCH JOIN + COUNT Query Conflict**  
*Symptom:* Hibernate warning `HHH90003004: firstResult/maxResults specified with collection fetch` and potential wrong pagination results.  
*Root cause:* When using `JOIN FETCH r.creator` in a paginated query, Hibernate cannot apply SQL LIMIT at the DB level (because JOIN FETCH multiplies rows). It fetches all rows and paginates in memory, defeating the purpose of pagination.  
*Fix:* Added separate `countQuery` attribute to all `@Query` annotations in `RoomRepository`. The count query is a simple `SELECT COUNT(r)` without JOIN FETCH. Hibernate uses the count query for total count and the main query for data retrieval.

**Bug 4: UUID in JavaScript onclick handler**  
*Symptom:* Clicking "Join" on any room did nothing (no network request, no error shown).  
*Root cause:* The room card template was rendering `onclick="joinRoom(${room.id}, '${room.name}')"`. A UUID like `550e8400-e29b-41d4-a716-446655440000` contains hyphens, which JavaScript interprets as **subtraction operators**. The expression `550e8400 - e29b - ...` was evaluated as NaN and the wrong ID was passed.  
*Fix:* Changed to `onclick="joinRoom('${room.id}', '${room.name}')"` — wrapped the UUID in single quotes so JavaScript receives it as a string.

**Bug 5: Category Filter Not Working**  
*Symptom:* Clicking "Technology", "Gaming", etc. tabs showed the same rooms regardless.  
*Root cause 1:* The tab `data-cat` attribute values were `'Technology'` (capitalized), but rooms in the DB were seeded with lowercase categories (`'technology'`).  
*Root cause 2:* `setCategory()` was calling `renderRooms()` (which only re-renders already-fetched data client-side) instead of `loadRooms()` (which makes a new API call).  
*Fix:* Changed all `data-cat` values to lowercase; changed `setCategory()` to reset `page = 0` and call `loadRooms()`.

**Bug 6: ApiResponse Wrapper Not Unwrapped**  
*Symptom:* `create-room.html` submitted successfully but navigating to chat threw "Cannot read properties of undefined (reading 'id')".  
*Root cause:* The response from `POST /api/v1/rooms` is `{ "success": true, "data": { "id": "...", ... } }`. The frontend was reading `json.id` instead of `json.data.id`.  
*Fix:* Changed all response reads to `const room = json.data` and then `room.id`.

---

## 11. What Is Pending / Roadmap

### Q: What features are not implemented yet?
**A:**

| Feature | Status | Reason Not Done Yet |
|---|---|---|
| WebSocket message relay handler | 🔄 Stub | Core next milestone |
| Signal-style prekey bundles | 🔄 Stub (`KeyController`) | Complex cryptographic protocol |
| JWT refresh token endpoint | 🔄 Config only | `app.jwt.refresh-expiration-ms` set but no endpoint |
| Challenge-response login | 📋 TODO comment | Security hardening — current login trusts username only |
| Message history / offline queue | ❌ Not started | By design, relay servers shouldn't store messages |
| Docker / docker-compose | ❌ Not started | Local dev only right now |
| Unit + integration tests | ❌ Not started | `RelayServerApplicationTests.java` is the Spring placeholder |
| CORS lock-down | 📋 TODO comment | `allowedOriginPatterns("*")` needs env-specific origins |
| Production DB config | 📋 TODO | Env vars documented but not wired to deployment |

### Q: How would you implement the WebSocket message relay handler?
**A:**
```java
@Controller
@RequiredArgsConstructor
public class MessageRelayHandler {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    @MessageMapping("/room/{roomId}/send")
    public void relayMessage(@DestinationVariable String roomId,
                             @Payload ChatMessage message,
                             Principal principal) {
        // IMPORTANT: server does NOT decrypt message.content
        // It simply stamps the sender and forwards the ciphertext blob
        message.setSender(principal.getName());
        message.setTimestamp(Instant.now());
        message.setType(MessageType.MESSAGE);
        
        // Broadcast to all subscribers of this room
        messagingTemplate.convertAndSend("/topic/room." + roomId, message);
    }
}
```
Key security note: `message.content` is the encrypted ciphertext blob from the client. The server stamps `sender` and `timestamp` from server-side context (not trusting client-supplied values) but never inspects `content`.

---

## 12. Scalability & Production Readiness

### Q: Can this app scale horizontally (multiple server instances)?
**A:** Not currently, for two reasons:
1. **In-memory STOMP broker** — Spring's simple broker lives in one JVM. If two servers run, users on Server A cannot receive messages from users on Server B. Fix: Replace with RabbitMQ/Redis pub-sub broker.
2. **Stateless JWT** — ✅ Already designed correctly. JWT is self-contained, no session state on server. Any instance can validate any JWT.
3. **Database connection pool** — HikariCP configured (`maximum-pool-size=10, minimum-idle=2`). For high load, use a connection pooler like PgBouncer.

### Q: What would you change for a production deployment?
**A:**
1. **Environment variables for all secrets** — `DB_PASSWORD`, `JWT_SECRET` must come from env or secrets manager (AWS Secrets Manager, Vault).
2. **HTTPS/WSS** — TLS at the load balancer/reverse proxy (Nginx). The comment in `WebSocketConfig` says: *"WSS endpoint — TLS termination handled at load balancer/proxy level"*.
3. **CORS restricted** — `allowedOriginPatterns` set to specific frontend domain.
4. **`spring.jpa.hibernate.ddl-auto=validate`** — Never use `update` in production; use Flyway/Liquibase for schema migrations.
5. **Structured logging** — JSON log format for centralized log aggregation (ELK stack, CloudWatch).
6. **Health checks** — Actuator `health` and `info` endpoints exposed; can be connected to Kubernetes liveness/readiness probes.
7. **Rate limiting** — `/api/auth/register` needs rate limiting to prevent identity flooding.

### Q: What database indexes exist?
**A:** In `User.java`:
```java
@Index(name = "idx_user_username", columnList = "username", unique = true)
@Index(name = "idx_user_identity_key", columnList = "identityPublicKey")
```
`username` has a unique index (enforces uniqueness + fast lookup for login). `identityPublicKey` has an index for future challenge-response lookup. Rooms are queried by `isPrivate` and `category` — these would benefit from composite indexes in production.

---

## 13. Code Quality & Best Practices

### Q: What design patterns are used in the backend?
**A:**
| Pattern | Where |
|---|---|
| **Service Layer** | `RoomService` (interface) + `RoomServiceImpl` — business logic separated from HTTP layer |
| **Repository Pattern** | `RoomRepository extends JpaRepository` — data access abstracted |
| **Builder Pattern** | All entities and DTOs use Lombok `@Builder` — immutable construction |
| **Factory Method** | `ApiResponse.ok()` and `ApiResponse.error()` — static factory methods |
| **Filter Chain** | `JwtAuthFilter extends OncePerRequestFilter` — cross-cutting concern |
| **DTO Pattern** | `CreateRoomRequest`, `RoomResponse` — entities never exposed directly to API |
| **Fallback/Graceful Degradation** | Frontend demo mode when backend unreachable |

### Q: Why is RoomService an interface with RoomServiceImpl as the implementation?
**A:** Three reasons:
1. **Testability** — in unit tests, `RoomService` can be mocked with `@MockBean` without loading `RoomServiceImpl`.
2. **Loose coupling** — `RoomController` depends on the interface, not the implementation. If the impl changes, the controller is unaffected.
3. **Spring proxy support** — Spring creates a proxy around `@Transactional` beans. When a class implements an interface, Spring uses JDK dynamic proxy (lighter than CGLIB byte-buddy proxy).

### Q: Why Lombok? What Lombok annotations are used?
**A:** Lombok eliminates boilerplate Java code:
- `@Getter` / `@Setter` — generates getters/setters
- `@Builder` — generates builder pattern
- `@NoArgsConstructor` / `@AllArgsConstructor` — generates constructors
- `@RequiredArgsConstructor` — generates constructor for `final` fields (used with DI)
- `@Slf4j` — injects `private static final Logger log = LoggerFactory.getLogger(...)` 
- `@Value` — on `ApiResponse`: makes all fields `final`, no setters, class is effectively immutable
- `@ToString(exclude = {"identityPublicKey"})` on `User` — **security**: prevents private key from appearing in debug logs

### Q: What is spring.jpa.open-in-view=false and why is it set?
**A:** The "Open Session in View" anti-pattern keeps the JPA EntityManager (Hibernate session) open for the entire HTTP request — through the controller and even the view rendering layer. This allows lazy loading at any point but is dangerous because:
1. **N+1 queries** — lazy loads that happen unintentionally in the view layer cause additional SQL queries
2. **Holding DB connections** — the connection is held for the full request duration (including network I/O)
3. **Hides LazyInitializationException bugs** — they silently "work" in dev but fail if the session ever closes

Setting `open-in-view=false` forces all data access to happen within `@Transactional` service methods. It is the correct production setting.

---

## 14. Quick-Fire Q&A Bank

**Q: What port does the backend run on?** A: 8080

**Q: What port does the frontend run on?** A: 3000 (via `python3 -m http.server 3000`)

**Q: What database name, user, and password are configured?** A: database=`ciphermesh`, user=`ciphermesh`, password=`changeme` (dev defaults)

**Q: What is the JWT expiry?** A: 900,000ms = 15 minutes (configured as `app.jwt.expiration-ms`)

**Q: What algorithm does JWT use?** A: HS256 (HMAC-SHA256)

**Q: What is the minimum JWT secret length and why?** A: 32 characters (256 bits) — HS256 requires a 256-bit key; shorter keys are rejected at startup with `IllegalStateException`

**Q: Where is the JWT secret stored?** A: `application.properties` as `${JWT_SECRET:CHANGE_ME...}` — reads from env var `JWT_SECRET` with a dev fallback

**Q: What HTTP method does "leave room" use?** A: `DELETE /api/v1/rooms/{id}/leave`

**Q: Why is `@CreationTimestamp` used instead of `@Column(updatable=false)` + manual set?** A: `@CreationTimestamp` is Hibernate-managed — it sets the value automatically at INSERT time without any application code

**Q: What is Hikari?** A: HikariCP — the default Spring Boot connection pool. It maintains a pool of reusable DB connections to avoid the overhead of creating a new connection per request.

**Q: What happens if you call GET /api/v1/rooms without authentication?** A: Returns 200 with room list — this is an explicitly permitted endpoint in SecurityConfig

**Q: What happens if you call POST /api/v1/rooms without authentication?** A: Returns 403 Forbidden — caught by Spring Security before reaching the controller

**Q: What is `@JsonInclude(NON_NULL)` on ApiResponse?** A: Jackson omits fields with null values from the JSON output — `message` won't appear in the response JSON if it's null

**Q: How are the room tags stored in the DB?** A: In a separate `room_tags` table with `(room_id, tag)` — managed as an `@ElementCollection`

**Q: What is `ddl-auto=update`?** A: Hibernate auto-updates the DB schema on startup to match the entity definitions. Only safe in development — never use in production (use Flyway/Liquibase).

**Q: Why is `@ManyToOne(fetch = FetchType.LAZY)` on `Room.creator` if EAGER loading is needed?** A: Creator is loaded via `JOIN FETCH r.creator` in the JPQL `@Query` — so Hibernate eagerly fetches it for paginated queries. The LAZY annotation just means it won't be loaded in other non-query scenarios. The explicit JOIN FETCH overrides the FetchType for those specific queries.

**Q: What is the purpose of `findRoomWithMembers()` in `RoomServiceImpl`?** A: It loads the room and then calls `room.getMembers().size()` inside a `@Transactional` method — this forces Hibernate to initialize the lazy `members` collection while the session is still open. Doing `room.getMembers()` outside a transaction would throw `LazyInitializationException`.

**Q: What is `countMembersById()` in `RoomRepository` used for?** A: Instead of loading the entire members collection and calling `.size()`, this JPQL query `SELECT COUNT(m) FROM Room r JOIN r.members m WHERE r.id = :roomId` counts at the DB level — used in `toResponse()` for read-only listing to avoid loading all member objects.

**Q: How are private room passwords verified?** A: `BCryptPasswordEncoder.matches(providedPassword, storedHash)` — BCrypt handles the salt automatically (salt is stored in the hash string itself).

**Q: Why use `@Slf4j` instead of `System.out.println`?** A: SLF4J/Logback: (1) configurable log levels per package, (2) logs to files/external systems, (3) lazy evaluation (`log.debug("{}", obj)` only calls `obj.toString()` if DEBUG is enabled), (4) structured logging support.

---

*Report generated for CipherMesh project evaluation — March 2026*  
*GitHub: https://github.com/ayush23chaudhary/CipherMesh*
