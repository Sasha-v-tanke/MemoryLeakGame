# Memory Leak Arena

`Memory Leak Arena` is a desktop 1v1 real-time strategy game with an educational systems-programming layer. Players control digital systems, allocate memory, reclaim dead objects, scale factories, protect their own Core, and destroy the opponent's Core.

The project is built as a Kotlin multi-module application:

- `client` - LibGDX desktop client.
- `server` - Ktor WebSocket authoritative game server.
- `shared` - shared DTOs, events, ECS components, commands and game configs.

## Core Learning Model

The game explains programming concepts through match consequences:

`Programming concept -> visible game behavior -> player decision -> system consequence`

## Current Mechanics

| Concept | Game behavior | Learning meaning |
| --- | --- | --- |
| Memory allocation | `Allocator` works at a Memory Source, creates usable Memory, then exits | Memory capacity is not automatically usable; it must be allocated. |
| Garbage collection | `Garbage Collector` sweeps dead allied units and restores their held Memory | GC reclaims dead/unreachable allocations; it does not heal living objects. |
| Memory leak | Dead units remain dimmed on the map until GC removes them | Dead objects still occupy memory if not collected. |
| CPU throughput | CPU grows over time and from CPU nodes | CPU limits how many operations the system can run. |
| Factory scaling | Building extra factories increases production parallelism and queue capacity | More build pipelines improve throughput but cost resources. |
| Core failure | Destroying Core ends the match | Central runtime/kernel failure terminates the system. |
| Deadlock | Area stun | Processes stop making progress when circular waiting blocks execution. |
| Overclock | Temporary speed boost | Throughput can be raised temporarily at compute cost. |

## Units

### Allocator

Creates Memory from Memory Sources or binds CPU nodes. It costs CPU but no Memory. After successful work it disappears.

- Strong: early resource setup.
- Weak: no combat value.
- IT meaning: allocation is a short-lived operation that reserves usable workspace.

### Garbage Collector

Finds dead allied units, frees their allocated Memory, removes them, then exits. It costs CPU but no Memory.

- Strong: recovers Memory after losses.
- Weak: cannot fight or heal living units.
- IT meaning: GC frees dead/unreachable allocations, not active objects.

### Patch Healer

Repairs living allied processes.

- Strong: keeps live pushes alive.
- Weak: does not reclaim Memory.
- IT meaning: patches stabilize running services; they are not garbage collection.

### Thread Guard

Defends a local area.

- Strong: holds nodes and approaches.
- Weak: slow and poor at chasing.
- IT meaning: synchronization protects critical sections but reduces throughput.

### Firewall

Heavy defensive boundary.

- Strong: static defense.
- Weak: expensive and slow.
- IT meaning: boundaries filter hostile traffic before critical parts.

### Injector

Aggressive attacker that prioritizes factories and Core.

- Strong: structure pressure.
- Weak: fragile.
- IT meaning: injection is powerful and risky because it changes execution directly.

### Coroutine Archer

Long-range attacker.

- Strong: safe damage.
- Weak: fragile.
- IT meaning: asynchronous work can continue without blocking the whole system.

### Deadlock

Area stun.

- Strong: interrupts enemy execution.
- Weak: no damage.
- IT meaning: deadlock is progress failure caused by circular waiting.

### Overclock

Temporary boost.

- Strong: accelerates active allied processes.
- Weak: situational.
- IT meaning: throughput boosts are temporary and resource-dependent.

## Run

Start PostgreSQL first:

```bash
cd server
./run-postgresql.sh
cd ..
# Run server:
Bash
./gradlew server:run
# Run two clients:
./gradlew client:run
./gradlew client:run
```

## Controls

WASD / arrows - move camera.
Q / E - zoom.
Click card - select card.
Click arena - set rally/work target.
Build Factory buttons - scale production near your Core.
Forfeit - leave match with automatic defeat after confirmation.
Hover objects - see gameplay role and IT explanation.

## Demo Scenario

Start server and two clients.
Login with two accounts.
Find a 1v1 match.
Use Allocator on Memory Sources to create usable Memory.
Use Allocator or Cache Runner to control CPU nodes.
Build extra factories to increase production throughput.
Use combat units to pressure factories and Core.
Let dead units remain as memory leaks.
Use Garbage Collector to reclaim dead allied units.
End the match by destroying Core or forfeiting.
Show post-match statistics.

---

## Что сделано по поведению юнитов

- `Allocator`: не стоит Memory, стоит CPU. Идёт к ближайшей ноде. На Memory Source создаёт пачку Memory и исчезает. На CPU Node связывает/захватывает ноду и исчезает.
- `Garbage Collector`: не стоит Memory, стоит много CPU. Не лечит. Ищет только мёртвые свои юниты, идёт к ним, делает sweep, возвращает их `allocatedMemory`, удаляет объект, затем сам исчезает.
- `Patch Healer`: лечит только живых союзников. Не освобождает Memory.
- `Thread Guard`: удерживает область и атакует вражеские юниты поблизости.
- `Firewall`: более тяжёлая оборонительная версия Thread Guard.
- `Injector`: приоритетно атакует Factory/Core, затем юнитов.
- `Coroutine Archer`: дальняя атака, хрупкий асинхронный “процесс”.
- `Deadlock`: временно станит процессы в области.
- `Overclock`: временно ускоряет союзные процессы в области.
- Мёртвые юниты остаются на карте как “утечки” до GC.
- Фабрики можно строить около Core, они увеличивают очереди и скорость производства.
