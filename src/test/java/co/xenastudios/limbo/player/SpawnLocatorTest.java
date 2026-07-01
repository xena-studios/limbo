package co.xenastudios.limbo.player;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpawnLocatorTest {

    @Test
    void zeroRadiusAlwaysReturnsCenter() {
        Random rng = new Random(1234);
        for (int i = 0; i < 1000; i++) {
            assertEquals(0, SpawnLocator.randomCoordinate(rng, 0));
            assertEquals(0, SpawnLocator.randomCoordinate(rng, -50));
        }
    }

    @Test
    void coordinateStaysWithinInclusiveBounds() {
        Random rng = new Random(42);
        int radius = 4000;
        for (int i = 0; i < 100_000; i++) {
            int c = SpawnLocator.randomCoordinate(rng, radius);
            assertTrue(c >= -radius && c <= radius, "coordinate " + c + " out of bounds");
        }
    }

    @Test
    void coordinateCanReachBothExtremes() {
        Random rng = new Random(7);
        int radius = 3;
        boolean sawMin = false;
        boolean sawMax = false;
        for (int i = 0; i < 10_000; i++) {
            int c = SpawnLocator.randomCoordinate(rng, radius);
            if (c == -radius) sawMin = true;
            if (c == radius) sawMax = true;
        }
        assertTrue(sawMin, "never produced -radius");
        assertTrue(sawMax, "never produced +radius (inclusive upper bound)");
    }

    @Test
    void randomColumnStaysWithinBounds() {
        Random rng = new Random(99);
        int radius = 1000;
        for (int i = 0; i < 50_000; i++) {
            int[] xz = SpawnLocator.randomColumn(rng, radius);
            assertEquals(2, xz.length);
            assertTrue(xz[0] >= -radius && xz[0] <= radius);
            assertTrue(xz[1] >= -radius && xz[1] <= radius);
        }
    }
}
