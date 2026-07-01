# Limbo — Build Progress

Live checklist so any session can resume. Update as work lands. See `CLAUDE.md` for context
and conventions, and the original build brief for full requirements.

## Done
- [x] Plan + target-version decision (Paper 1.18.2 API, Java 21).
- [x] Gradle build: shadow, Java-21 toolchain, git-derived version, `build-info.properties`.
- [x] `plugin.yml` (classic commands `join` / `limbo`, permissions).
- [x] Project docs: `CLAUDE.md`, `PROGRESS.md`, memory files.

## In progress / remaining
- [ ] `config.yml` (versioned, fully commented, all toggles/defaults).
- [ ] `config/Settings` immutable snapshot + `ConfigLoader` (validate + migrate, never throws).
- [ ] `world/VoidChunkGenerator` + `VoidBiomeProvider` (O(1) chunks, glass floor only).
- [ ] `world/WorldManager` (create/load, gamerules, border, autosave off, non-blocking reset).
- [ ] `player/SpawnLocator` (bounded random spawn) + unit tests.
- [ ] `player/JoinListener` (creative+flight, async teleport, messages, chat clear).
- [ ] `player/AutoJoinScheduler` (delayed Connect, cancel on quit).
- [ ] `proxy/ProxyConnector` (BungeeCord Connect plugin message).
- [ ] `command/JoinCommand` (aliases, permission, cooldown) + `CooldownManager` + tests.
- [ ] `command/LimboCommand` (reload/info).
- [ ] `task/ActionBarTask` (pre-parsed component loop).
- [ ] Protections: floor, void, portal, liquid, gravity, fire, entity, misc (explosions/drops/
      redstone/mob-spawn), build-guard — each toggled + registered only when enabled.
- [ ] `LimboPlugin` wiring: fail-safe enable/disable, atomic settings swap, task/listener mgmt.
- [ ] Suppress join/quit/death messages.
- [ ] Tests: config parse/validate, spawn math, cooldown.
- [ ] `.github/workflows/build.yml` + `release.yml`.
- [ ] `README.md` (features, install, full config reference, server-level tuning, proxy
      forwarding security note, "how it stays fast & stable").
- [ ] Verify: `./gradlew build` green; workflows valid; self-review vs. Definition of Done.

## Notes / decisions log
- Floor is a single glass layer at a configurable Y (default 64); players spawn at floorY+1.
- `/join` base command is in `plugin.yml`; configured aliases registered at runtime via the
  server CommandMap (fail-safe — base command still works if alias registration fails).
