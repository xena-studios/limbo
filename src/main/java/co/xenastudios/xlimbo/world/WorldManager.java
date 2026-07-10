package co.xenastudios.xlimbo.world;

import co.xenastudios.xlimbo.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Owns the limbo world lifecycle: optional safe reset, creation via
 * {@link WorldCreator} with our generator, and one-time application of gamerules,
 * world border and per-world footprint settings.
 *
 * <p>Every operation is guarded — nothing here throws out of enable. World
 * creation runs once at startup (before players are routed here), never per-tick.
 */
public final class WorldManager {

    /** Fixed biome for the whole world (cheap; calm daytime lighting). */
    private static final Biome BIOME = Biome.PLAINS;

    private final Plugin plugin;
    private final Logger log;

    public WorldManager(Plugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    /** Build the generator for the current settings (used here and by the plugin). */
    public ChunkGenerator createGenerator(Settings settings) {
        return new VoidChunkGenerator(settings.world().floorBlock(), settings.world().floorY(), BIOME);
    }

    /**
     * Ensure the limbo world exists and is configured. Returns the world, or
     * {@code null} on failure (caller degrades gracefully).
     */
    public World ensureWorld(Settings settings) {
        String name = settings.world().name();
        try {
            if (settings.world().resetOnStartup()) {
                safeReset(name);
            }

            World world = Bukkit.getWorld(name);
            if (world == null) {
                try {
                    world = new WorldCreator(name)
                            .generator(createGenerator(settings))
                            .biomeProvider(new VoidBiomeProvider(BIOME))
                            .environment(World.Environment.NORMAL)
                            .type(WorldType.NORMAL)
                            .generateStructures(false)
                            .createWorld();
                } catch (IllegalStateException notYet) {
                    // The server refuses world creation during STARTUP. This happens when
                    // our configured world IS the server's own default world (its generator
                    // is pointed at xLimbo in bukkit.yml): the server hasn't loaded it yet
                    // and will do so shortly using getDefaultWorldGenerator. Finish setup on
                    // WorldLoadEvent instead (see LimboWorldLoadListener) — this is expected,
                    // not an error.
                    log.info("xLimbo world '" + name + "' will be loaded by the server; "
                            + "deferring limbo setup until it loads.");
                    return null;
                }
            }
            if (world == null) {
                log.severe("xLimbo world '" + name + "' could not be created.");
                return null;
            }
            applySettings(world, settings);
            return world;
        } catch (Throwable t) {
            log.log(Level.SEVERE, "Failed to ensure limbo world '" + name + "'.", t);
            return Bukkit.getWorld(name); // may be non-null if created before the failure
        }
    }

    /** Apply gamerules, border, footprint and quiet-world settings once. */
    public void applySettings(World world, Settings settings) {
        try {
            world.setAutoSave(settings.world().autoSave());
            world.setKeepSpawnInMemory(false);
            world.setDifficulty(Difficulty.PEACEFUL);
            world.setTime(6000L);       // noon
            world.setStorm(false);
            world.setThundering(false);
            world.setWeatherDuration(Integer.MAX_VALUE);

            // World border bounds all roaming; spawns are clamped inside it.
            world.getWorldBorder().setCenter(0.5, 0.5);
            world.getWorldBorder().setSize(settings.world().borderSize());
            world.getWorldBorder().setWarningDistance(0);
            world.getWorldBorder().setDamageAmount(0);

            applyGameRules(world);
            applyFootprint(world, settings);
        } catch (Throwable t) {
            log.log(Level.WARNING, "Failed applying some world settings (continuing).", t);
        }
    }

    private void applyGameRules(World world) {
        setRule(world, GameRule.DO_DAYLIGHT_CYCLE, false);
        setRule(world, GameRule.DO_WEATHER_CYCLE, false);
        setRule(world, GameRule.DO_MOB_SPAWNING, false);
        setRule(world, GameRule.DO_FIRE_TICK, false);
        setRule(world, GameRule.RANDOM_TICK_SPEED, 0);
        setRule(world, GameRule.MOB_GRIEFING, false);
        setRule(world, GameRule.DO_MOB_LOOT, false);
        setRule(world, GameRule.DO_TILE_DROPS, false);
        setRule(world, GameRule.FALL_DAMAGE, false);
        setRule(world, GameRule.FIRE_DAMAGE, false);
        setRule(world, GameRule.DROWNING_DAMAGE, false);
        setRule(world, GameRule.DO_INSOMNIA, false);
        setRule(world, GameRule.DO_TRADER_SPAWNING, false);
        setRule(world, GameRule.DO_PATROL_SPAWNING, false);
        setRule(world, GameRule.DISABLE_RAIDS, true);
        setRule(world, GameRule.ANNOUNCE_ADVANCEMENTS, false);
        setRule(world, GameRule.SPAWN_RADIUS, 0);
        setRule(world, GameRule.KEEP_INVENTORY, true);
        setRule(world, GameRule.SHOW_DEATH_MESSAGES, false);
    }

    private void applyFootprint(World world, Settings settings) {
        int view = settings.world().viewDistance();
        int sim = settings.world().simulationDistance();
        if (view >= 2) {
            try {
                world.setViewDistance(view);
            } catch (Throwable t) {
                log.log(Level.FINE, "setViewDistance unsupported; leaving server default.", t);
            }
        }
        if (sim >= 2) {
            try {
                world.setSimulationDistance(sim);
            } catch (Throwable t) {
                log.log(Level.FINE, "setSimulationDistance unsupported; leaving server default.", t);
            }
        }
    }

    private <T> void setRule(World world, GameRule<T> rule, T value) {
        try {
            world.setGameRule(rule, value);
        } catch (Throwable t) {
            log.log(Level.FINE, "Could not set gamerule " + rule.getName(), t);
        }
    }

    /**
     * Safely delete the limbo world folder before (re)creation. Uses an absolute
     * path resolved against the server's world container, refuses to touch a
     * loaded world or anything outside that container, and never throws.
     */
    private void safeReset(String name) {
        try {
            if (Bukkit.getWorld(name) != null) {
                log.warning("reset-on-startup: world '" + name + "' is already loaded; skipping reset.");
                return;
            }
            // Reject anything that could escape the world container.
            String safe = name.toLowerCase(Locale.ROOT);
            if (name.contains("..") || name.contains("/") || name.contains("\\") || !safe.equals(name)) {
                log.warning("reset-on-startup: unsafe world name '" + name + "'; skipping reset.");
                return;
            }
            File container = Bukkit.getWorldContainer();
            Path base = container.getCanonicalFile().toPath();
            Path target = new File(container, name).getCanonicalFile().toPath();
            if (!target.startsWith(base) || target.equals(base)) {
                log.warning("reset-on-startup: refusing to delete path outside world container.");
                return;
            }
            if (!Files.exists(target)) {
                return;
            }
            deleteRecursively(target);
            log.info("reset-on-startup: cleared limbo world folder '" + name + "'.");
        } catch (Throwable t) {
            log.log(Level.WARNING, "reset-on-startup failed (continuing without reset).", t);
        }
    }

    private void deleteRecursively(Path root) throws Exception {
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (Exception e) {
                    log.log(Level.FINE, "Could not delete " + p, e);
                }
            });
        }
    }
}
