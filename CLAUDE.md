# xLimbo — Developer Context

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
  `build/libs/xLimbo-<version>.jar`.
- **Semantic versioning, git-tag driven.** A commit tagged `vX.Y.Z` builds to a clean version
  (e.g. `1.2.0`); any commit after the latest tag builds to `<version>-nightly.<n>+<sha>`; with
  no tags it falls back to `0.0.0-nightly.<count>+<sha>`. Full SHA + timestamp are injected into
  `src/main/resources/build-info.properties` and the version into `plugin.yml` via
  `processResources` `expand`.

## Layout (`co.xenastudios.xlimbo`)
- `XLimboPlugin` — fail-safe enable/disable, wiring, atomic settings swap.
- `config/` — `Settings` (immutable snapshot + pre-parsed MiniMessage), `ConfigLoader`
  (validate + migrate + version).
- `world/` — `VoidChunkGenerator`, `VoidBiomeProvider`, `WorldManager`.
- `player/` — `JoinListener`, `SpawnLocator`, `AutoJoinScheduler`, `MessageListener`.
- `protection/` — one small toggled listener per exploit vector.
- `command/` — `JoinCommand`, `XLimboCommand`, `CooldownManager`.
- `task/` — `ActionBarTask` + task tracking.
- `proxy/` — `ProxyConnector` (BungeeCord `Connect` plugin message).

## Key design rules
- No `getConfig()` / MiniMessage parse on any per-tick / per-chunk / per-event hot path — read
  the immutable `Settings` snapshot, swapped atomically on `/xlimbo reload`.
- Async teleport (`teleportAsync`) after `getChunkAtAsync`; world create/reset must never block
  the tick or throw.
- Register listeners / schedule tasks ONLY for enabled features.
- World border bounds roaming; random spawns land inside it; ephemeral world = autosave off,
  spawn chunks not kept loaded, spread chunks not persisted.

## Status
Full plugin, unit tests, CI (`build.yml` + `nightly.yml` + `release.yml`), README, and config.
The rolling `nightly` pre-release carries a stable `xLimbo-nightly.jar` asset built from `main`;
semantic releases are cut by pushing a `vX.Y.Z` tag, which also publishes to Modrinth.

## Decisions log
- Floor is a single glass layer at a configurable Y (default 64); players spawn at floorY+1.
- Two release tracks: the `nightly` pre-release (stable asset name `xLimbo-nightly.jar`, rebuilt
  from `main` on schedule/push, deleted + recreated each run for a fresh timestamp) and tag-driven
  semantic releases (`vX.Y.Z`) that create a normal GitHub release and publish to Modrinth. The
  resolved version lives in the jar (plugin.yml / build-info.properties / `/xlimbo info`).
- Modrinth publishing is gated on a `MODRINTH_TOKEN` secret + `MODRINTH_PROJECT_ID` variable
  (optional `MODRINTH_GAME_VERSIONS`, `MODRINTH_LOADERS`); the release step skips cleanly if unset.
- `/join` base command is in `plugin.yml`; configured aliases are registered at enable via the
  server CommandMap (fail-safe). Alias changes therefore need a restart, not just `/xlimbo reload`.
- `/xlimbo reload` re-applies settings to the **already-loaded** world (`applySettings`) and never
  calls `createWorld`/`safeReset` on the tick. Consequently `world.name`, `world.floor-y` and
  `world.floor-block` are restart-only (the world + generator are fixed at creation); a live
  `world.name` change is logged and ignored rather than orphaning players in the old world.
- `.idea/` is untracked and gitignored.
- Supports the "default world **is** the limbo" setup: point the default world's
  generator at xLimbo in `bukkit.yml` and set `world.name` to it. The server can't
  create worlds during STARTUP, so `WorldManager.ensureWorld` catches that
  `IllegalStateException`, defers, and `LimboWorldLoadListener` finishes setup
  (`applySettings` + cache `limboWorld`) on `WorldLoadEvent`. The load listener is
  registered outside `registerFeatures()` so a reload never tears it down.

## Follow-ups (not required by the brief)
- No config-validation unit tests (would need MockBukkit); spawn math + cooldown are covered.
- Not yet smoke-tested on a live 1.18.2 / latest Paper server before production use.

Remote: `git@github.com:xena-studios/xlimbo.git` (push over gh HTTPS; SSH key isn't authorized).
