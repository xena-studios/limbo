package co.xenastudios.xlimbo.command;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Per-player command cooldown tracker. Rate-limits actions (notably {@code /join})
 * to stop proxy/command spam.
 *
 * <p>The time source is injectable so the logic is unit-testable without sleeping.
 */
public final class CooldownManager {

    private final Map<UUID, Long> lastUseMillis = new ConcurrentHashMap<>();
    private final LongSupplier clock;

    public CooldownManager() {
        this(System::currentTimeMillis);
    }

    public CooldownManager(LongSupplier clock) {
        this.clock = clock;
    }

    /**
     * Remaining cooldown in milliseconds, or {@code 0} if the action is allowed.
     * When allowed, the player's timestamp is recorded (i.e. this both checks and
     * consumes). A non-positive {@code cooldownSeconds} always allows.
     */
    public long tryUse(UUID player, int cooldownSeconds) {
        if (cooldownSeconds <= 0) {
            return 0L;
        }
        long now = clock.getAsLong();
        long cooldownMs = cooldownSeconds * 1000L;
        Long last = lastUseMillis.get(player);
        if (last != null) {
            long elapsed = now - last;
            if (elapsed < cooldownMs) {
                return cooldownMs - elapsed;
            }
        }
        lastUseMillis.put(player, now);
        return 0L;
    }

    /** Remaining cooldown in whole seconds (rounded up), for user-facing text. */
    public static int toRemainingSeconds(long remainingMillis) {
        return (int) Math.ceil(remainingMillis / 1000.0);
    }

    public void clear(UUID player) {
        lastUseMillis.remove(player);
    }

    public void clearAll() {
        lastUseMillis.clear();
    }
}
