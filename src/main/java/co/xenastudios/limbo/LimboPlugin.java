package co.xenastudios.limbo;

import co.xenastudios.limbo.command.CooldownManager;
import co.xenastudios.limbo.command.JoinCommand;
import co.xenastudios.limbo.command.LimboCommand;
import co.xenastudios.limbo.config.ConfigLoader;
import co.xenastudios.limbo.config.Settings;
import co.xenastudios.limbo.player.AutoJoinScheduler;
import co.xenastudios.limbo.player.JoinListener;
import co.xenastudios.limbo.player.MessageListener;
import co.xenastudios.limbo.protection.BuildGuardListener;
import co.xenastudios.limbo.protection.EntityCleanupTask;
import co.xenastudios.limbo.protection.EntityProtectionListener;
import co.xenastudios.limbo.protection.FireProtectionListener;
import co.xenastudios.limbo.protection.FloorProtectionListener;
import co.xenastudios.limbo.protection.GravityBlockListener;
import co.xenastudios.limbo.protection.LiquidProtectionListener;
import co.xenastudios.limbo.protection.MiscProtectionListener;
import co.xenastudios.limbo.protection.PortalProtectionListener;
import co.xenastudios.limbo.protection.VoidProtectionListener;
import co.xenastudios.limbo.proxy.ProxyConnector;
import co.xenastudios.limbo.task.ActionBarTask;
import co.xenastudios.limbo.world.WorldManager;
import org.bukkit.World;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Limbo — fallback void-sandbox plugin.
 *
 * <p>Design contract: <b>never take the server down</b>. {@link #onEnable()} and
 * {@link #reload()} are wrapped so they can never throw out; a failing feature is
 * logged and skipped, degrading gracefully rather than disabling the plugin or
 * crashing the tick.
 *
 * <p>Configuration lives in an immutable {@link Settings} snapshot held in a
 * {@code volatile} field and swapped atomically on reload, so no hot path ever
 * reads {@code getConfig()} or re-parses MiniMessage.
 */
public final class LimboPlugin extends JavaPlugin {

    private volatile Settings settings;
    private volatile World limboWorld;

    private WorldManager worldManager;
    private ProxyConnector proxyConnector;
    private CooldownManager cooldownManager;
    private AutoJoinScheduler autoJoinScheduler;

    private final List<Listener> registeredListeners = new ArrayList<>();
    private final List<BukkitTask> tasks = new ArrayList<>();

    @Override
    public void onEnable() {
        try {
            this.settings = ConfigLoader.load(this);
            this.worldManager = new WorldManager(this);
            this.proxyConnector = new ProxyConnector(this);
            this.cooldownManager = new CooldownManager();
            this.autoJoinScheduler = new AutoJoinScheduler(this);

            this.proxyConnector.register();
            this.limboWorld = worldManager.ensureWorld(settings);

            registerCommands();
            registerFeatures();

            getLogger().info("Limbo enabled (" + getDescription().getVersion() + ").");
        } catch (Throwable t) {
            // Absolute backstop: even total failure must not crash the server.
            getLogger().log(Level.SEVERE, "Limbo failed to fully enable; running in degraded mode.", t);
        }
    }

    @Override
    public void onDisable() {
        try {
            cancelTasks();
            unregisterListeners();
            if (autoJoinScheduler != null) {
                autoJoinScheduler.cancelAll();
            }
            if (proxyConnector != null) {
                proxyConnector.unregister();
            }
        } catch (Throwable t) {
            getLogger().log(Level.WARNING, "Error during disable (ignored).", t);
        }
    }

    /**
     * Re-read + re-validate config and re-apply everything live. Never throws.
     *
     * @return true if the reload completed cleanly.
     */
    public boolean reload() {
        try {
            Settings fresh = ConfigLoader.load(this);
            this.settings = fresh; // atomic swap — in-flight reads see old or new, never partial

            // Re-apply world settings and refresh the cached world reference.
            World world = worldManager.ensureWorld(fresh);
            this.limboWorld = world;

            // Rebuild listeners + tasks so disabled features are torn down and
            // enabled ones pick up new values. Cancel pending auto-joins since
            // the delay/toggle may have changed.
            cancelTasks();
            unregisterListeners();
            autoJoinScheduler.cancelAll();
            registerFeatures();
            return true;
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "Reload failed; keeping previous state.", t);
            return false;
        }
    }

    // ---- feature wiring ----------------------------------------------------

    private void registerCommands() {
        try {
            JoinCommand joinCommand = new JoinCommand(this);
            if (getCommand("join") != null) {
                getCommand("join").setExecutor(joinCommand);
            }
            joinCommand.registerAliases(); // fail-safe extra aliases via CommandMap

            LimboCommand limboCommand = new LimboCommand(this);
            if (getCommand("limbo") != null) {
                getCommand("limbo").setExecutor(limboCommand);
                getCommand("limbo").setTabCompleter(limboCommand);
            }
        } catch (Throwable t) {
            getLogger().log(Level.WARNING, "Failed to register commands.", t);
        }
    }

    /** Register only the listeners and tasks whose features are enabled. */
    private void registerFeatures() {
        Settings s = this.settings;
        Settings.Protections p = s.protections();

        // Core: player state + join flow (always on) and message suppression.
        register(new JoinListener(this));
        register(new MessageListener(this));

        if (p.floor()) {
            register(new FloorProtectionListener(this));
        }
        if (p.voidRescue().enabled()) {
            register(new VoidProtectionListener(this));
        }
        if (p.portals()) {
            register(new PortalProtectionListener(this));
        }
        if (p.liquids()) {
            register(new LiquidProtectionListener(this));
        }
        if (p.gravityBlocks()) {
            register(new GravityBlockListener(this));
        }
        if (p.fire()) {
            register(new FireProtectionListener(this));
        }
        if (p.entities().blockEntityItems()) {
            register(new EntityProtectionListener(this));
        }
        if (p.buildGuard().enabled()) {
            register(new BuildGuardListener(this));
        }
        // Explosions / mob-spawns / redstone / item-drops share one listener,
        // each guarded by its own toggle inside.
        if (p.explosions() || p.mobSpawns() || p.redstone() || p.itemDrops()) {
            register(new MiscProtectionListener(this));
        }

        // Tasks.
        if (s.actionBar().enabled()) {
            schedule(new ActionBarTask(this).start());
        }
        if (p.entities().cleanupIntervalSeconds() > 0) {
            schedule(new EntityCleanupTask(this).start());
        }
    }

    private void register(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
        registeredListeners.add(listener);
    }

    private void schedule(BukkitTask task) {
        if (task != null) {
            tasks.add(task);
        }
    }

    private void unregisterListeners() {
        for (Listener l : registeredListeners) {
            HandlerList.unregisterAll(l);
        }
        registeredListeners.clear();
    }

    private void cancelTasks() {
        for (BukkitTask t : tasks) {
            try {
                t.cancel();
            } catch (Throwable ignored) {
                // Already cancelled or never started.
            }
        }
        tasks.clear();
    }

    // ---- accessors ---------------------------------------------------------

    /** Current immutable settings snapshot (safe to read from any thread). */
    public Settings settings() {
        return settings;
    }

    public World limboWorld() {
        return limboWorld;
    }

    /** True if {@code world} is the (currently cached) limbo world. */
    public boolean isLimboWorld(World world) {
        World limbo = this.limboWorld;
        return limbo != null && world != null && limbo.getUID().equals(world.getUID());
    }

    public ProxyConnector proxyConnector() {
        return proxyConnector;
    }

    public CooldownManager cooldownManager() {
        return cooldownManager;
    }

    public AutoJoinScheduler autoJoinScheduler() {
        return autoJoinScheduler;
    }

    public WorldManager worldManager() {
        return worldManager;
    }

    // Let the server use our generator/biome if the limbo world is in its world list.
    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        Settings s = this.settings;
        if (s != null && worldName.equalsIgnoreCase(s.world().name())) {
            return worldManager.createGenerator(s);
        }
        return super.getDefaultWorldGenerator(worldName, id);
    }

    @Override
    public BiomeProvider getDefaultBiomeProvider(String worldName, String id) {
        Settings s = this.settings;
        if (s != null && worldName.equalsIgnoreCase(s.world().name())) {
            return worldManager.createGenerator(s).getDefaultBiomeProvider(null);
        }
        return super.getDefaultBiomeProvider(worldName, id);
    }
}
