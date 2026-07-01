package co.xenastudios.xlimbo.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads, validates and migrates {@code config.yml} into an immutable
 * {@link Settings} snapshot.
 *
 * <p>Contract: {@link #load(Plugin)} <b>never throws</b>. Every value is read
 * defensively; a bad value logs a warning and falls back to its default. A
 * catastrophic failure falls back to a fully-default snapshot so the plugin can
 * always come up — stability first.
 */
public final class ConfigLoader {

    /** Current config schema version. Bump when adding/renaming keys. */
    public static final int CONFIG_VERSION = 1;

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final FileConfiguration cfg;
    private final Logger log;

    private ConfigLoader(FileConfiguration cfg, Logger log) {
        this.cfg = cfg;
        this.log = log;
    }

    /**
     * Reload the config from disk, migrate it forward, and build a snapshot.
     * Always returns a usable {@link Settings} — never throws.
     */
    public static Settings load(Plugin plugin) {
        Logger log = plugin.getLogger();
        try {
            plugin.saveDefaultConfig();
            plugin.reloadConfig();
            FileConfiguration cfg = plugin.getConfig();

            // Forward-merge: seed defaults from the bundled resource so missing
            // keys read as their defaults, and rewrite the file if out of date
            // without wiping the user's existing values.
            YamlConfiguration bundled = loadBundledDefaults(plugin);
            if (bundled != null) {
                cfg.setDefaults(bundled);
                cfg.options().copyDefaults(true);
                migrate(plugin, cfg, bundled, log);
            }

            return new ConfigLoader(cfg, log).build();
        } catch (Throwable t) {
            log.log(Level.SEVERE, "Failed to load config; falling back to built-in defaults.", t);
            return defaults();
        }
    }

    private static YamlConfiguration loadBundledDefaults(Plugin plugin) {
        try (InputStream in = plugin.getResource("config.yml")) {
            if (in == null) return null;
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Could not read bundled config defaults.", e);
            return null;
        }
    }

    private static void migrate(Plugin plugin, FileConfiguration cfg, YamlConfiguration bundled, Logger log) {
        try {
            int current = cfg.getInt("config-version", 0);
            int target = bundled.getInt("config-version", CONFIG_VERSION);
            if (current < target) {
                cfg.set("config-version", target);
                plugin.saveConfig();
                log.info("Migrated config.yml from version " + current + " to " + target + ".");
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Config migration step failed (continuing with in-memory values).", e);
        }
    }

    private Settings build() {
        String mainServer = str("main-server", "main");

        Settings.World world = new Settings.World(
                str("world.name", "xlimbo"),
                material("world.floor-block", Material.GLASS),
                clampInt("world.floor-y", 64, -64, 300),
                bool("world.auto-save", false),
                bool("world.reset-on-startup", false),
                clampDouble("world.border.size", 10000, 16, 59_999_968),
                Math.max(0, intVal("world.border.spawn-radius", 4000)),
                clampViewDistance("world.view-distance", 4),
                clampViewDistance("world.simulation-distance", 4)
        );
        world = clampSpawnToBorder(world);

        Settings.Player player = new Settings.Player(
                bool("player.set-creative", true),
                bool("player.enable-flight", true)
        );

        Settings.Join join = new Settings.Join(
                bool("join.suppress-join-message", true),
                miniOrNull("join.custom-join-message"),
                bool("join.clear-chat-on-join", false)
        );

        Settings.Messages messages = new Settings.Messages(
                bool("quit.suppress-quit-message", true),
                bool("death.suppress-death-message", true)
        );

        Settings.AutoJoin autoJoin = new Settings.AutoJoin(
                bool("auto-join.enabled", false),
                Math.max(1, intVal("auto-join.delay-seconds", 30)),
                miniOrNull("auto-join.message")
        );

        Settings.JoinCommand joinCommand = new Settings.JoinCommand(
                bool("join-command.enabled", true),
                stringList("join-command.aliases"),
                str("join-command.permission", "xlimbo.join"),
                Math.max(0, intVal("join-command.cooldown-seconds", 3)),
                mini("join-command.message-sending",
                        "<green>Connecting you to <aqua><server></aqua>...</green>",
                        Placeholder.unparsed("server", mainServer)),
                str("join-command.message-cooldown", "<red>Please wait <yellow><seconds></yellow>s.</red>")
        );

        Settings.ActionBar actionBar = new Settings.ActionBar(
                bool("action-bar.enabled", true),
                Math.max(1, intVal("action-bar.interval-ticks", 40)),
                mini("action-bar.message", "<gray>Use <yellow>/join</yellow> to join the server!</gray>")
        );

        Settings.Protections protections = new Settings.Protections(
                bool("protections.floor", true),
                new Settings.Protections.Void(
                        bool("protections.void.enabled", true),
                        intVal("protections.void.threshold-y", 0),
                        bool("protections.void.cancel-void-damage", true)
                ),
                bool("protections.portals", true),
                bool("protections.liquids", true),
                bool("protections.gravity-blocks", true),
                bool("protections.fire", true),
                bool("protections.explosions", true),
                bool("protections.mob-spawns", true),
                bool("protections.redstone", true),
                bool("protections.item-drops", true),
                new Settings.Protections.Entities(
                        bool("protections.entities.block-entity-items", true),
                        Math.max(0, intVal("protections.entities.cleanup-interval-seconds", 300))
                ),
                new Settings.Protections.BuildGuard(
                        bool("protections.build-guard.enabled", false),
                        Math.max(0, intVal("protections.build-guard.max-places-per-second", 30))
                )
        );

        return new Settings(world, player, join, messages,
                mainServer, autoJoin, joinCommand, actionBar, protections);
    }

    /** Ensure random spawns land comfortably inside the border wall. */
    private Settings.World clampSpawnToBorder(Settings.World w) {
        int borderRadius = (int) Math.floor(w.borderSize() / 2.0);
        int maxSpawn = Math.max(0, borderRadius - 128); // leave a buffer inside the wall
        if (w.spawnRadius() > maxSpawn) {
            warn("world.border.spawn-radius", w.spawnRadius(), maxSpawn,
                    "must stay inside the border with a buffer");
            return new Settings.World(w.name(), w.floorBlock(), w.floorY(), w.autoSave(),
                    w.resetOnStartup(), w.borderSize(), maxSpawn, w.viewDistance(), w.simulationDistance());
        }
        return w;
    }

    // ---- validated readers -------------------------------------------------

    private boolean bool(String path, boolean def) {
        return cfg.getBoolean(path, def);
    }

    private int intVal(String path, int def) {
        return cfg.getInt(path, def);
    }

    /**
     * View/simulation distance: a value {@code < 2} means "leave the server default"
     * (see config.yml, {@code -1}). Any other value is clamped to Paper's accepted
     * {@code [2, 32]} range so a misconfiguration can't blow up the chunk footprint.
     */
    private int clampViewDistance(String path, int def) {
        int v = intVal(path, def);
        if (v < 2) {
            return v;
        }
        if (v > 32) {
            warn(path, v, 32, "out of range [2, 32]");
            return 32;
        }
        return v;
    }

    private int clampInt(String path, int def, int min, int max) {
        int v = cfg.getInt(path, def);
        if (v < min || v > max) {
            int clamped = Math.max(min, Math.min(max, v));
            warn(path, v, clamped, "out of range [" + min + ", " + max + "]");
            return clamped;
        }
        return v;
    }

    private double clampDouble(String path, double def, double min, double max) {
        double v = cfg.getDouble(path, def);
        if (v < min || v > max) {
            double clamped = Math.max(min, Math.min(max, v));
            warn(path, v, clamped, "out of range [" + min + ", " + max + "]");
            return clamped;
        }
        return v;
    }

    private String str(String path, String def) {
        String v = cfg.getString(path, def);
        return v == null ? def : v;
    }

    private List<String> stringList(String path) {
        List<String> raw = cfg.getStringList(path);
        List<String> out = new ArrayList<>(raw.size());
        for (String s : raw) {
            if (s != null && !s.isBlank()) out.add(s.trim().toLowerCase(Locale.ROOT));
        }
        return List.copyOf(out);
    }

    private Material material(String path, Material def) {
        String raw = cfg.getString(path);
        if (raw == null || raw.isBlank()) return def;
        Material m = Material.matchMaterial(raw.trim());
        if (m == null || !m.isBlock()) {
            warn(path, raw, def.name(), "not a valid block material");
            return def;
        }
        if (!m.isSolid()) {
            // The floor must be something players stand on; AIR / flowers / torches
            // would silently remove the safety floor.
            warn(path, raw, def.name(), "not a solid block (players would fall through)");
            return def;
        }
        return m;
    }

    /** Parse a required MiniMessage string; empty falls back to {@code def}. */
    private Component mini(String path, String def, net.kyori.adventure.text.minimessage.tag.resolver.TagResolver... resolvers) {
        String raw = cfg.getString(path, def);
        if (raw == null || raw.isBlank()) raw = def;
        try {
            return MM.deserialize(raw, resolvers);
        } catch (Exception e) {
            warn(path, raw, "(default)", "invalid MiniMessage");
            try {
                return MM.deserialize(def, resolvers);
            } catch (Exception ignored) {
                return Component.empty();
            }
        }
    }

    /** Parse an optional MiniMessage string; blank/absent yields {@code null}. */
    private Component miniOrNull(String path) {
        String raw = cfg.getString(path, "");
        if (raw == null || raw.isBlank()) return null;
        try {
            return MM.deserialize(raw);
        } catch (Exception e) {
            warn(path, raw, "(none)", "invalid MiniMessage");
            return null;
        }
    }

    private void warn(String path, Object bad, Object used, String why) {
        log.warning("Config '" + path + "' = '" + bad + "' is invalid (" + why
                + "); using '" + used + "' instead.");
    }

    // ---- last-resort defaults ---------------------------------------------

    /**
     * A fully-default snapshot used only if config loading blows up entirely.
     * Mirrors the shipped {@code config.yml} defaults.
     */
    public static Settings defaults() {
        Settings.World world = new Settings.World(
                "xlimbo", Material.GLASS, 64, false, false, 10000, 4000, 4, 4);
        Settings.Player player = new Settings.Player(true, true);
        Settings.Join join = new Settings.Join(true, null, false);
        Settings.Messages messages = new Settings.Messages(true, true);
        Settings.AutoJoin autoJoin = new Settings.AutoJoin(false, 30, null);
        Settings.JoinCommand joinCommand = new Settings.JoinCommand(
                true, List.of("server", "lobby"), "xlimbo.join", 3,
                MM.deserialize("<green>Connecting you to <aqua>main</aqua>...</green>"),
                "<red>Please wait <yellow><seconds></yellow>s.</red>");
        Settings.ActionBar actionBar = new Settings.ActionBar(true, 40,
                MM.deserialize("<gray>Use <yellow>/join</yellow> to join the server!</gray>"));
        Settings.Protections protections = new Settings.Protections(
                true,
                new Settings.Protections.Void(true, 0, true),
                true, true, true, true, true, true, true, true,
                new Settings.Protections.Entities(true, 300),
                new Settings.Protections.BuildGuard(false, 30));
        return new Settings(world, player, join, messages,
                "main", autoJoin, joinCommand, actionBar, protections);
    }
}
