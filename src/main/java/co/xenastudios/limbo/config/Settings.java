package co.xenastudios.limbo.config;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.List;

/**
 * Immutable snapshot of all plugin configuration. Loaded once by
 * {@link ConfigLoader} and swapped atomically on {@code /limbo reload}.
 *
 * <p>MiniMessage strings that never change are pre-parsed into {@link Component}s
 * here so no hot path ever re-parses them. Only messages carrying a per-use
 * dynamic placeholder (e.g. cooldown seconds remaining) keep a raw template and
 * are parsed on their (rare) invocation.
 */
public record Settings(
        World world,
        Player player,
        Join join,
        Messages messages,
        String mainServer,
        AutoJoin autoJoin,
        JoinCommand joinCommand,
        ActionBar actionBar,
        Protections protections
) {

    public record World(
            String name,
            Material floorBlock,
            int floorY,
            boolean autoSave,
            boolean resetOnStartup,
            double borderSize,
            int spawnRadius,
            int viewDistance,
            int simulationDistance
    ) {}

    public record Player(
            boolean setCreative,
            boolean enableFlight
    ) {}

    /** {@code customJoinMessage} is null when unset. */
    public record Join(
            boolean suppressJoinMessage,
            Component customJoinMessage,
            boolean clearChatOnJoin
    ) {}

    public record Messages(
            boolean suppressQuitMessage,
            boolean suppressDeathMessage
    ) {}

    /** {@code message} is null when unset. */
    public record AutoJoin(
            boolean enabled,
            int delaySeconds,
            Component message
    ) {}

    /**
     * {@code messageSending} is pre-parsed (server name resolved at load).
     * {@code messageCooldownTemplate} keeps the raw string because it carries a
     * per-use {@code <seconds>} placeholder; it is parsed only when a cooldown
     * actually blocks a command (a rare path).
     */
    public record JoinCommand(
            boolean enabled,
            List<String> aliases,
            String permission,
            int cooldownSeconds,
            Component messageSending,
            String messageCooldownTemplate
    ) {}

    public record ActionBar(
            boolean enabled,
            int intervalTicks,
            Component message
    ) {}

    public record Protections(
            boolean floor,
            Void voidRescue,
            boolean portals,
            boolean liquids,
            boolean gravityBlocks,
            boolean fire,
            boolean explosions,
            boolean mobSpawns,
            boolean redstone,
            boolean itemDrops,
            Entities entities,
            BuildGuard buildGuard
    ) {

        public record Void(
                boolean enabled,
                int thresholdY,
                boolean cancelVoidDamage
        ) {}

        public record Entities(
                boolean blockEntityItems,
                int cleanupIntervalSeconds
        ) {}

        public record BuildGuard(
                boolean enabled,
                int maxPlacesPerSecond
        ) {}
    }
}
