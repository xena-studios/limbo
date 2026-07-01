package co.xenastudios.xlimbo;

import co.xenastudios.xlimbo.command.CooldownManager;
import co.xenastudios.xlimbo.command.JoinCommand;
import co.xenastudios.xlimbo.command.XLimboCommand;
import co.xenastudios.xlimbo.config.ConfigLoader;
import co.xenastudios.xlimbo.config.Settings;
import co.xenastudios.xlimbo.player.AutoJoinScheduler;
import co.xenastudios.xlimbo.player.JoinListener;
import co.xenastudios.xlimbo.player.MessageListener;
import co.xenastudios.xlimbo.protection.BuildGuardListener;
import co.xenastudios.xlimbo.protection.EntityCleanupTask;
import co.xenastudios.xlimbo.protection.EntityProtectionListener;
import co.xenastudios.xlimbo.protection.FireProtectionListener;
import co.xenastudios.xlimbo.protection.FloorProtectionListener;
import co.xenastudios.xlimbo.protection.GravityBlockListener;
import co.xenastudios.xlimbo.protection.LiquidProtectionListener;
import co.xenastudios.xlimbo.protection.MiscProtectionListener;
import co.xenastudios.xlimbo.protection.PortalProtectionListener;
import co.xenastudios.xlimbo.protection.VoidProtectionListener;
import co.xenastudios.xlimbo.proxy.ProxyConnector;
import co.xenastudios.xlimbo.task.ActionBarTask;
import co.xenastudios.xlimbo.world.WorldManager;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * xLimbo — fallback void-sandbox plugin.
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
public final class XLimboPlugin extends JavaPlugin {

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

            getLogger().info("xLimbo enabled (" + getDescription().getVersion() + ").");
        } catch (Throwable t) {
            // Absolute backstop: even total failure must not crash the server.
            getLogger().log(Level.SEVERE, "xLimbo failed to fully enable; running in degraded mode.", t);
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
            World current = this.limboWorld;

            // world.name is effectively restart-only: creating a second world here would
            // block the tick and orphan players in the old (now-unprotected) world.
            if (current != null && !current.getName().equalsIgnoreCase(fresh.world().name())) {
                getLogger().warning("Config 'world.name' changed ('" + current.getName() + "' -> '"
                        + fresh.world().name() + "'); this requires a restart. Keeping the current world.");
            }

            this.settings = fresh; // atomic swap — in-flight reads see old or new, never partial

            // Re-apply settings to the already-loaded world; never create or reset on the
            // tick. (floor-y / floor-block changes still need a restart — the generator is
            // fixed at world creation.)
            if (current != null) {
                worldManager.applySettings(current, fresh);
            } else {
                this.limboWorld = worldManager.ensureWorld(fresh);
            }

            // Rebuild listeners + tasks so disabled features are torn down and
            // enabled ones pick up new values. Cancel pending auto-joins since
            // the delay/toggle may have changed.
            cancelTasks();
            unregisterListeners();
            autoJoinScheduler.cancelAll();
            registerFeatures();

            // Re-arm auto-join for players already waiting in limbo (cancelAll cleared
            // them); schedule() self-guards when auto-join is disabled.
            for (Player player : getServer().getOnlinePlayers()) {
                if (isLimboWorld(player.getWorld())) {
                    autoJoinScheduler.schedule(player);
                }
            }
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

            XLimboCommand limboCommand = new XLimboCommand(this);
            if (getCommand("xlimbo") != null) {
                getCommand("xlimbo").setExecutor(limboCommand);
                getCommand("xlimbo").setTabCompleter(limboCommand);
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
        register(() -> new JoinListener(this));
        register(() -> new MessageListener(this));

        if (p.floor()) {
            register(() -> new FloorProtectionListener(this));
        }
        // Void rescue and void/fall-damage cancellation share one listener; register it
        // if either is enabled (the move-rescue self-guards when rescue is disabled).
        if (p.voidRescue().enabled() || p.voidRescue().cancelVoidDamage()) {
            register(() -> new VoidProtectionListener(this));
        }
        if (p.portals()) {
            register(() -> new PortalProtectionListener(this));
        }
        if (p.liquids()) {
            register(() -> new LiquidProtectionListener(this));
        }
        if (p.gravityBlocks()) {
            register(() -> new GravityBlockListener(this));
        }
        if (p.fire()) {
            register(() -> new FireProtectionListener(this));
        }
        if (p.entities().blockEntityItems()) {
            register(() -> new EntityProtectionListener(this));
        }
        if (p.buildGuard().enabled()) {
            register(() -> new BuildGuardListener(this));
        }
        // Explosions / mob-spawns / redstone / item-drops share one listener,
        // each guarded by its own toggle inside.
        if (p.explosions() || p.mobSpawns() || p.redstone() || p.itemDrops()) {
            register(() -> new MiscProtectionListener(this));
        }

        // Tasks.
        if (s.actionBar().enabled()) {
            schedule(() -> new ActionBarTask(this).start());
        }
        if (p.entities().cleanupIntervalSeconds() > 0) {
            schedule(() -> new EntityCleanupTask(this).start());
        }
    }

    /**
     * Construct + register one listener, guarded so a single failing feature is
     * logged and skipped rather than aborting the whole (re)build.
     */
    private void register(Supplier<Listener> factory) {
        try {
            Listener listener = factory.get();
            getServer().getPluginManager().registerEvents(listener, this);
            registeredListeners.add(listener);
        } catch (Throwable t) {
            getLogger().log(Level.WARNING, "Failed to register a feature listener (skipped).", t);
        }
    }

    /** Construct + track one task, guarded the same way as {@link #register}. */
    private void schedule(Supplier<BukkitTask> factory) {
        try {
            BukkitTask task = factory.get();
            if (task != null) {
                tasks.add(task);
            }
        } catch (Throwable t) {
            getLogger().log(Level.WARNING, "Failed to schedule a feature task (skipped).", t);
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
