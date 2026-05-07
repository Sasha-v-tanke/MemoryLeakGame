# Memory Leak Arena Shared Module

The `shared` module contains code used by both client and server. It keeps the
network protocol, game configuration and ECS-style components consistent across
the project.

## Responsibilities

- API request/response models.
- Server-to-client events.
- Game commands.
- Entity state snapshots.
- ECS components such as `Health`, `Core`, `ResourceNode`, `CombatStats`,
  `StatusEffects`, `Unit` and `Target`.
- World configuration.
- Unit/card registry.

## Important Packages

- `api` - base request/response interfaces and JSON settings.
- `api.auth` - login and registration DTOs.
- `api.matchmaking` - matchmaking DTOs.
- `api.game` - ready and play-card DTOs.
- `api.events` - `MatchFoundEvent`, `GameStartEvent`,
  `GameStateSnapshotEvent`, `SystemMessageEvent`, `GameOverEvent`.
- `engine.config` - world size, tick rate, starting resources and map objects.
- `engine.entities.components` - ECS components serialized in snapshots.
- `engine.entities.units` - card types, roles, stats and educational text.

## Unit/Card Model

Each card has:

- gameplay role;
- cost in `Memory` and `CPU`;
- HP, damage, speed and range;
- sprite path;
- gameplay description;
- IT metaphor description.

This is where the defense formula is encoded in data:

`Educational problem -> game mechanic -> IT meaning`

Examples:

- `Allocator` is a fast capturer and explains memory allocation.
- `Deadlock` is an area stun and explains blocked execution.
- `Overclock` is a temporary buff and explains throughput increase.
- `Firewall` is a defensive unit and explains protection boundaries.
- `Coroutine Archer` is a ranged attacker and explains asynchronous work.

## Defense Talking Points

- `shared` prevents client/server protocol drift.
- Polymorphic events and components make snapshots extensible.
- Unit configs combine balance numbers and educational text, so the learning
  layer is part of the game model rather than an external description.
