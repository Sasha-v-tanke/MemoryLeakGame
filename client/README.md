# Memory Leak Arena Client

The client is a LibGDX desktop application. It renders the arena, handles login,
matchmaking, card selection, HUD, hover explanations and player commands.

## Responsibilities

- Login and registration screens.
- Main menu.
- Deck/card selection.
- Matchmaking screen.
- Arena rendering from server snapshots.
- HUD with `Memory` and `CPU`.
- Card deployment by selecting a card and clicking the arena.
- Hover information with both gameplay role and IT metaphor.
- Victory/defeat screen.

## Run

From the project root:

```bash
./gradlew client:run
```

For a full 1v1 demo, run two client windows:

```bash
./gradlew client:run
./gradlew client:run
```

The client connects to `ws://localhost:8080` by default. To use another server:

```bash
MEMORY_LEAK_SERVER_WS=ws://192.168.1.10:8080 ./gradlew client:run
```

## Controls

- `WASD` / arrows - move camera.
- `Q` / `E` - zoom.
- Click a card - select it.
- Click the arena - deploy the selected card.
- Hover objects - show role, stats and IT explanation.

## Deck Rules

- Deck size: 6 cards.
- Maximum copies of one card: 2.
- If no saved deck exists, the default deck is used.
- The selected deck is saved locally per player profile and used in the next
  match.

Available cards:

- `Allocator`
- `Garbage Collector`
- `Thread Guard`
- `Injector`
- `Deadlock`
- `Overclock`
- `Cache Runner`
- `Firewall`
- `Coroutine Archer`
- `Patch Healer`

## Defense Talking Points

- The client does not own match logic; it sends commands and renders server
  snapshots.
- Hover descriptions are part of the educational layer: each object explains
  both what it does in game and what IT concept it represents.
- Deck selection shows architecture trade-offs: fast capture, defense, attack,
  support and control.
