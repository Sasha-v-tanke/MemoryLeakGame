# Memory Leak Arena Server

The server is an authoritative Ktor WebSocket backend. It owns matchmaking,
game rooms, resource income, card deployment, unit movement, combat, support
logic, node capture and victory detection.

## Responsibilities

- Auth over `/auth`.
- Listen socket over `/listen` for async events.
- Matchmaking over `/matchmaking`.
- Game commands over `/game`.
- PostgreSQL-backed user storage.
- Authoritative game state snapshots.

Clients send commands; the server calculates the real match state.

## Requirements

- JDK 21.
- PostgreSQL.
- Gradle wrapper from the project root.

Default database config:

```text
MEMORY_LEAK_DB_URL=jdbc:postgresql://localhost:5432/game
MEMORY_LEAK_DB_USER=gameuser
MEMORY_LEAK_DB_PASSWORD=password
```

## Start PostgreSQL

From `server/`:

```bash
./run-postgresql.sh
```

The script starts a Docker container named `memory-leak-postgres` with the
default database/user/password expected by the server.

## Run Server

From the project root:

```bash
./gradlew server:run
```

The server listens on port `8080`.

Verbose game logs are disabled by default. To enable them:

```bash
MEMORY_LEAK_DEBUG=true ./gradlew server:run
```

## Game Loop

After both players are ready, the server:

1. Builds the world from `WorldConfig`.
2. Sends initial state snapshots.
3. Applies card commands from clients.
4. Updates resource income every second.
5. Captures `Memory`/`CPU` nodes when capturer units stand nearby.
6. Selects targets based on unit role.
7. Moves units, applies combat and support logic.
8. Ends the game when a `Core` reaches zero HP.

## Defense Talking Points

- The server is authoritative: clients do not calculate resources, damage or
  victory.
- `Memory` and `CPU` are not just UI counters; they are enforced by server-side
  card costs.
- `Deadlock` and `Overclock` are server-side status effects, so their behavior
  is synchronized for both players.
