# CipherMesh — Secure Anonymous Chat Platform

> **End-to-end encrypted, relay-only, anonymous messaging platform.**
> The server is a pure relay — it never decrypts messages. All E2E encryption is client-side.

---

## 🏗️ Architecture

```
ciphermesh/
├── backend/
│   └── relay-server/          # Spring Boot 3.5 relay + prekey store
└── frontend/                  # Vanilla HTML/CSS/JS (Tailwind, STOMP/SockJS)
```

---

## ⚡ Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.5 · Java 17 · Spring Security · Spring WebSocket (STOMP) |
| Auth | JWT (HS256, jjwt 0.12.6) · Anonymous-first (no passwords) |
| Database | PostgreSQL 14 · Spring Data JPA · Hibernate |
| Frontend | Vanilla HTML · Tailwind CSS (CDN) · SockJS · STOMP.js |
| Design | Dark glassmorphism · Space Grotesk font · Material Symbols |

---

## 🚀 Running Locally

### Prerequisites
- Java 17+
- PostgreSQL 14 running on `localhost:5432`

### 1 — Database setup
```sql
CREATE DATABASE ciphermesh;
CREATE USER ciphermesh WITH PASSWORD 'changeme';
GRANT ALL PRIVILEGES ON DATABASE ciphermesh TO ciphermesh;
```

### 2 — Start the backend
```bash
cd backend/relay-server
./mvnw spring-boot:run
# Server starts on http://localhost:8080
```

### 3 — Start the frontend
```bash
cd frontend
python3 -m http.server 3000
# Open http://localhost:3000
```

---

## 🌐 Page Flow

```
index.html       → Landing page (live room count, featured rooms)
identity.html    → Anonymous identity generation (ECDH keypair + JWT)
rooms.html       → Browse & search public rooms
create-room.html → Create a new room
chat.html        → Live chat (STOMP/SockJS WebSocket)
```

---

## 🔌 API Reference

### Auth
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | /api/auth/register | None | Register anonymous identity |
| POST | /api/auth/login | None | Login (anonymous, no password) |

### Rooms
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | /api/v1/rooms | None | List public rooms |
| GET | /api/v1/rooms/count | None | Count of public rooms |
| GET | /api/v1/rooms/{id} | None | Get room by ID |
| POST | /api/v1/rooms | JWT | Create a room |
| POST | /api/v1/rooms/{id}/join | JWT | Join a room |
| DELETE | /api/v1/rooms/{id}/leave | JWT | Leave a room |

---

## 🔒 Security Notes

- The server never reads message content — relay only.
- JWT is stateless (HS256), stored in sessionStorage (ephemeral).
- E2E encryption (ECDH P-256) generated in-browser via Web Crypto API.
- No persistent user accounts — identity is anonymous and session-scoped.
- Room passwords are BCrypt-hashed server-side.

---

## 🗺️ Roadmap

- [x] User module (register, JWT auth)
- [x] Room module (CRUD, join/leave, categories, tags, private rooms)
- [x] Frontend — 5 pages fully wired to backend API
- [ ] WebSocket relay handler (STOMP message routing)
- [ ] Signal-style prekey bundle store
- [ ] Offline message queue
- [ ] Docker / docker-compose
- [ ] Unit and integration tests

---

## 👤 Author

**Ayush Chaudhary** — [@ayush23chaudhary](https://github.com/ayush23chaudhary)
