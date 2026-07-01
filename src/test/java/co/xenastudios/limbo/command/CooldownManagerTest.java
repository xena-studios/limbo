package co.xenastudios.limbo.command;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CooldownManagerTest {

    private final UUID player = UUID.randomUUID();

    @Test
    void firstUseIsAllowed() {
        CooldownManager cm = new CooldownManager(() -> 0L);
        assertEquals(0L, cm.tryUse(player, 3));
    }

    @Test
    void secondUseWithinWindowIsBlockedWithRemaining() {
        AtomicLong now = new AtomicLong(0L);
        CooldownManager cm = new CooldownManager(now::get);

        assertEquals(0L, cm.tryUse(player, 3));   // consumes at t=0
        now.set(1000L);                            // 1s later
        long remaining = cm.tryUse(player, 3);
        assertEquals(2000L, remaining);            // 2s left
    }

    @Test
    void useAfterWindowIsAllowedAgain() {
        AtomicLong now = new AtomicLong(0L);
        CooldownManager cm = new CooldownManager(now::get);

        assertEquals(0L, cm.tryUse(player, 3));
        now.set(3000L);                            // exactly the window
        assertEquals(0L, cm.tryUse(player, 3));
    }

    @Test
    void zeroOrNegativeCooldownAlwaysAllows() {
        CooldownManager cm = new CooldownManager(() -> 0L);
        assertEquals(0L, cm.tryUse(player, 0));
        assertEquals(0L, cm.tryUse(player, -5));
    }

    @Test
    void clearResetsCooldown() {
        AtomicLong now = new AtomicLong(0L);
        CooldownManager cm = new CooldownManager(now::get);
        cm.tryUse(player, 10);
        now.set(1000L);
        assertTrue(cm.tryUse(player, 10) > 0);
        cm.clear(player);
        assertEquals(0L, cm.tryUse(player, 10));
    }

    @Test
    void remainingSecondsRoundsUp() {
        assertEquals(1, CooldownManager.toRemainingSeconds(1L));
        assertEquals(1, CooldownManager.toRemainingSeconds(1000L));
        assertEquals(2, CooldownManager.toRemainingSeconds(1001L));
        assertEquals(3, CooldownManager.toRemainingSeconds(2500L));
        assertEquals(0, CooldownManager.toRemainingSeconds(0L));
    }
}
