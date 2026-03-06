# Copilot Context — CipherMesh

You are helping build a production-grade secure messaging platform called CipherMesh.

## Architecture Rules (VERY IMPORTANT)

- End-to-end encryption happens ONLY on the client side
- Server must NEVER decrypt messages
- Server acts only as a relay and prekey store
- Use Spring Boot 3 and Java 17
- Use WebSockets over WSS for real-time messaging
- Follow clean architecture and modular design
- Code must be production-quality, well-structured, and secure

## Project Modules

| Module       | Responsibility                              |
|--------------|---------------------------------------------|
| `auth`       | JWT authentication & token management       |
| `user`       | User registration & profile management      |
| `keys`       | Signal-style prekey bundle management       |
| `messaging`  | Encrypted message relay (no decryption)     |
| `websocket`  | WebSocket connection & session management   |
| `queue`      | Offline encrypted message queue             |
| `security`   | Security config, filters, utilities         |

## Security Requirements

- Never store plaintext messages
- Validate all inputs
- Use UUIDs for all identifiers
- Follow REST best practices
- Add proper exception handling
- Add logging but NEVER log sensitive data

## Coding Style

- Use Lombok where appropriate
- Use constructor injection (not field injection)
- Use DTO pattern for all request/response objects
- Follow SOLID principles
- Write clean, readable Java code

## When Generating Code

- Prefer simplicity but maintain scalability
- Add comments explaining security decisions
- Do not invent crypto logic on the server
- Keep business logic out of controllers (use service layer)
- Use `@Slf4j` for logging via Lombok

---

## Prompt History

### Prompt 1 — Project Skeleton

> Using Spring Boot 3 and Java 17, generate a clean modular package structure
> for a secure messaging relay server.
>
> Requirements:
> - Base package: com.ciphermesh
> - Modules: auth, user, keys, messaging, websocket, queue, security
> - Include main application class
> - Include basic configuration classes
> - Follow clean architecture
> - Do not implement business logic yet

### Prompt 2 — User Entity

> Create a production-ready User JPA entity for the CipherMesh relay server.
>
> Requirements:
> - UUID primary key
> - username (unique)
> - identityPublicKey (text)
> - createdAt timestamp
> - proper JPA annotations
> - Lombok usage
> - indexes where appropriate
> - no sensitive data stored
