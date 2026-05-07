# Memory Leak Arena

`Memory Leak Arena` is a desktop 1v1 real-time strategy game with an educational
systems-programming layer. Players control digital systems, capture `Memory` and
`CPU` nodes, deploy process-themed cards, protect their own `Core`, and destroy
the opponent's `Core`.

The project is built as a Kotlin multi-module application:

- `client` - LibGDX desktop client.
- `server` - Ktor WebSocket authoritative game server.
- `shared` - shared DTOs, events, ECS components, commands and game configs.

## Relevance

The project is relevant because many systems and architecture concepts are hard
to understand from lectures and source code alone. Memory, CPU, resource
contention, deadlocks, process roles and central failure points are abstract
until students can see actions, consequences and feedback.

The main defense formula is:

`Educational problem -> game mechanic -> IT meaning -> project demo`

## Relevance To Mechanics

| Concept | Game mechanic | Learning meaning |
| --- | --- | --- |
| `Memory` | Resource spent on cards | Memory becomes a limited working space, not just a number. |
| `CPU` | Resource needed for stronger and faster actions | Compute capacity limits system throughput. |
| `Resource Nodes` | Capturable Memory/CPU nodes | Infrastructure control creates technical advantage. |
| `Core` | Destroying Core ends the match | A central runtime/kernel failure terminates the system. |
| `Allocator` | Cheap node capturer | Allocation reserves working memory for active work. |
| `Garbage Collector` | Support and repair | Cleanup keeps the system stable over time. |
| `Thread Guard` | Defensive unit | Synchronization protects critical resources. |
| `Injector` | Aggressive attacker | Injection is powerful, direct and risky. |
| `Deadlock` | Area stun | Processes stop acting when execution is blocked. |
| `Overclock` | Temporary speed boost | Throughput can be raised, but only for a limited time. |

Additional selectable cards expand the same idea:

- `Cache Runner` - fast capture through cache-like locality.
- `Firewall` - defensive boundary around critical parts.
- `Coroutine Archer` - asynchronous long-range work.
- `Patch Healer` - operational repair and maintenance.

## Puzzle Lab

The project also includes a separate `Puzzle Lab` section. It is intentionally
different from PvP: there is no opponent, matchmaking, arena push or real-time
combat. Instead, the player receives a concrete debugging incident and must
repair the system while preserving the important invariant.

Levels unlock one by one, so the mode starts with beginner-friendly unit
metaphors and then moves toward real engineering constraints:

| Level | Unit basis | Puzzle mechanic | Learning meaning |
| --- | --- | --- |
| 1 | `Allocator` | Choose the correct unit-target pair for reserving workspace | Memory allocation is the first systems concept. |
| 2 | `Garbage Collector` / `Patch Healer` | Mark roots, detach a stale listener, then sweep unreachable objects | GC cannot free objects that are still reachable from roots. |
| 3 | `Firewall` / `Injector` / `Patch Healer` | Classify incoming requests as allow, block or trusted patch | Security is not "block everything"; it is correct filtering. |
| 4 | `Thread Guard` / `Deadlock` | Inspect a wait-for graph and break circular wait safely | Deadlock is a dependency cycle between processes and resources. |
| 5 | `Cache Runner` / `Coroutine Archer` / `Overclock` | Build a safe retried request pipeline in order | Speed, async work and cache need correctness rules. |

The defense framing is still the same:

`Educational problem -> operation -> system consequence -> IT meaning`

## Tech Stack

- Kotlin 2.2
- LibGDX / LWJGL3 for the desktop client
- Ktor WebSockets for networking
- PostgreSQL + Exposed for user data
- Gradle multi-module build

Use JDK 21. On this machine the Android Studio JBR works:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

## Run

Start PostgreSQL first. The default server config expects:

```text
jdbc:postgresql://localhost:5432/game
user: gameuser
password: password
```

One Docker-based option:

```bash
cd server
./run-postgresql.sh
cd ..
```

Then open three terminals from the project root:

```bash
./gradlew server:run
```

```bash
./gradlew client:run
```

```bash
./gradlew client:run
```

The server runs on `localhost:8080`. The client uses
`MEMORY_LEAK_SERVER_WS` when it is set; otherwise it connects to
`ws://localhost:8080`.

## Demo Scenario

1. Start PostgreSQL.
2. Start the server.
3. Start two desktop clients.
4. Register or log in with two accounts.
5. Optionally open `Deck / Cards` and save a 6-card deck.
6. Press `Find 1v1 Match` in both clients.
7. Select cards, click the arena to deploy them, capture `Memory`/`CPU` nodes.
8. Push toward the enemy `Core`.
9. Destroy the `Core` and show the victory/defeat screen.

For the non-PvP educational demo, open `Puzzle Lab` from the main menu and solve
one of the constrained system puzzles.

## Controls

- `WASD` / arrows - move camera.
- `Q` / `E` - zoom.
- Click a card - select it.
- Click arena - deploy selected card.
- Hover objects - see gameplay and IT explanation.

## Defense Responsibility Zones

- Relevance and concept: learning problem, mapping IT concepts to mechanics,
  and why this game loop was chosen.
- Client: LibGDX desktop UI, map rendering, card deck, HUD, hover info and
  controls.
- Server: Ktor WebSockets, matchmaking, rooms, authoritative economy, combat
  and victory logic.
- Shared: common API, events, commands, ECS components, unit configs and world
  configs.
- Demo: launch flow, recorded video, GitHub link and match walkthrough.

## Future Work

- Android client.
- Redis-backed matchmaking/session scaling.
- Ratings and match history.
- Replays.
- 2v2 mode.
- Larger tutorial with step-by-step learning tasks.
