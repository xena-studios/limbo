# Limbo — Developer Context

Fallback "limbo" server plugin for a proxied Minecraft network. When the main server is
down, players are routed here into an empty void world (unbreakable glass floor, creative +
flight) until they're sent back. **The server must never go down** — every code path fails
safe (never throws out of enable/reload, never blocks the main thread, never self-disables on
bad config). Priority order: **stability > performance > exploit-resistance > configurability >
code quality**.

## Conventions (IMPORTANT)
- **Conventional Commits** (`feat:`, `fix:`, `chore:`, `docs:`, `ci:`, `refactor:`, `test:`),
  scoped where useful. Commit in small, feature-grouped increments.
- **Do NOT** add a `Co-Authored-By: Claude` trailer to commits.

## Build / target
- **Paper API `1.18.2-R0.1-SNAPSHOT`**, **Java 21** bytecode. 1.18.2 is the oldest Paper with
  modern `ChunkGenerator` hooks + `BiomeProvider` (1.17) *and* bundled MiniMessage (1.18.2),
  so nothing extra is shaded. Java-21 bytecode requires a Java-21 runtime.
- Gradle Kotlin DSL + `com.gradleup.shadow`. Build: `./gradlew build` → shaded runnable jar in
  `build/libs/Limbo-<yyyy.MM.dd>+<sha>.jar`.
- Version is derived from git at build time; full SHA + timestamp injected into
  `src/main/resources/build-info.properties` and `plugin.yml` via `processResources` `expand`.

## Layout (`co.xenastudios.limbo`)
- `LimboPlugin` — fail-safe enable/disable, wiring, atomic settings swap.
- `config/` — `Settings` (immutable snapshot + pre-parsed MiniMessage), `ConfigLoader`
  (validate + migrate + version).
- `world/` — `VoidChunkGenerator`, `VoidBiomeProvider`, `WorldManager`.
- `player/` — `JoinListener`, `SpawnLocator`, `AutoJoinScheduler`.
- `protection/` — one small toggled listener per exploit vector.
- `command/` — `JoinCommand`, `LimboCommand`, `CooldownManager`.
- `task/` — `ActionBarTask` + task tracking.
- `proxy/` — `ProxyConnector` (BungeeCord `Connect` plugin message).

## Key design rules
- No `getConfig()` / MiniMessage parse on any per-tick / per-chunk / per-event hot path — read
  the immutable `Settings` snapshot, swapped atomically on `/limbo reload`.
- Async teleport (`teleportAsync`) after `getChunkAtAsync`; world create/reset must never block
  the tick or throw.
- Register listeners / schedule tasks ONLY for enabled features.
- World border bounds roaming; random spawns land inside it; ephemeral world = autosave off,
  spawn chunks not kept loaded, spread chunks not persisted.

## Status
See `PROGRESS.md` for the live build checklist / what's done vs. remaining.

Remote: `git@github.com:xena-studios/limbo.git`.
