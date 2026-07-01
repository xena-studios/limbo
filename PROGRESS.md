# Limbo — Build Progress

Live checklist so any session can resume. See `CLAUDE.md` for context and conventions, and the
original build brief for full requirements.

## Done — initial build complete
- [x] Plan + target-version decision (Paper 1.18.2 API, Java 21). Verified 1.18.2 pulls Adventure
      4.11.0 → modern MiniMessage API present without shading.
- [x] Gradle build: shadow, Java-21 toolchain, git-derived version, `build-info.properties`.
- [x] `plugin.yml` (classic commands `join` / `limbo`, permissions) + GPLv3 `LICENSE`.
- [x] `config.yml` (versioned, fully commented, all toggles/defaults).
- [x] `config/Settings` immutable snapshot + `ConfigLoader` (validate + migrate, never throws).
- [x] `world/VoidChunkGenerator` + `VoidBiomeProvider` (O(1) chunks, glass floor only).
- [x] `world/WorldManager` (create/load, gamerules, border, autosave off, non-blocking safe reset).
- [x] `player/SpawnLocator` (bounded random spawn) + unit tests.
- [x] `player/JoinListener` (creative+flight, async teleport, messages, chat clear).
- [x] `player/AutoJoinScheduler` (delayed Connect, cancel on quit, persists across reload).
- [x] `player/MessageListener` (suppress quit/death; join handled in JoinListener).
- [x] `proxy/ProxyConnector` (BungeeCord Connect plugin message).
- [x] `command/JoinCommand` (runtime aliases, permission, cooldown) + `CooldownManager` + tests.
- [x] `command/LimboCommand` (reload/info) + `util/BuildInfo`.
- [x] `task/ActionBarTask` (pre-parsed component loop) + `protection/EntityCleanupTask`.
- [x] Protections: floor, void, portal, liquid, gravity, fire, entity, misc (explosions/drops/
      redstone/mob-spawn), build-guard — each toggled + registered only when enabled.
- [x] `LimboPlugin` wiring: fail-safe enable/disable, atomic settings swap, task/listener mgmt.
- [x] Tests: spawn math + cooldown. `./gradlew build` green.
- [x] `.github/workflows/build.yml` + `release.yml` (YAML validated).
- [x] `README.md` (features, install, full config reference, server tuning, proxy security note,
      "how it stays fast & stable").

## Shipped
- All commits pushed to `main`; CI `Build` + `Release (latest)` workflows verified green.
- Rolling `latest` release is a normal (non-prerelease) release with stable asset
  `Limbo-latest.jar`; it is the repo's headline "Latest release".
- `.idea/` is untracked/gitignored (IDE files kept out of the repo).

## Possible follow-ups (not required by the brief)
- No config-validation unit tests (would need MockBukkit); spawn math + cooldown are covered.
- Alias changes to `/join` currently need a restart (registered via CommandMap at enable, not on
  reload) — documented limitation.
- Consider verifying on a live 1.18.2 and a latest Paper server before production use.

## Notes / decisions log
- Floor is a single glass layer at a configurable Y (default 64); players spawn at floorY+1.
- Rolling release uses a stable asset name `Limbo-latest.jar`; the commit-derived version lives in
  the jar (plugin.yml / build-info.properties / `/limbo info`) to avoid asset accumulation.
- `main-server` default is `main`; proxy forwarding security is a documented server-side concern.
