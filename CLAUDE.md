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
Initial build is complete and pushed to `main`: full plugin, unit tests, CI (`build.yml` +
`release.yml`), README, and config. CI is green; the rolling `latest` release is a normal
(non-prerelease) release with a stable `Limbo-latest.jar` asset.

## Decisions log
- Floor is a single glass layer at a configurable Y (default 64); players spawn at floorY+1.
- Rolling release uses a stable asset name `Limbo-latest.jar`; the commit-derived version lives in
  the jar (plugin.yml / build-info.properties / `/limbo info`). The release workflow deletes +
  recreates the release each run so its published timestamp reflects the newest build.
- `/join` base command is in `plugin.yml`; configured aliases are registered at enable via the
  server CommandMap (fail-safe). Alias changes therefore need a restart, not just `/limbo reload`.
- `.idea/` is untracked and gitignored.

## Follow-ups (not required by the brief)
- No config-validation unit tests (would need MockBukkit); spawn math + cooldown are covered.
- Not yet smoke-tested on a live 1.18.2 / latest Paper server before production use.

Remote: `git@github.com:xena-studios/limbo.git` (push over gh HTTPS; SSH key isn't authorized).
