package co.xenastudios.xlimbo.player;

import co.xenastudios.xlimbo.config.Settings;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Computes bounded, spread-out random spawn points and preloads their chunk so
 * the join teleport never blocks on generation.
 *
 * <p>The coordinate math is pure and static so it can be unit-tested without a
 * running server.
 */
public final class SpawnLocator {

    private SpawnLocator() {
    }

    /**
     * A uniformly random block coordinate in {@code [-radius, radius]}.
     * Returns 0 when {@code radius <= 0}.
     */
    public static int randomCoordinate(Random rng, int radius) {
        if (radius <= 0) {
            return 0;
        }
        // nextInt(bound) is [0, bound); +1 and shift to make it inclusive [-r, r].
        return rng.nextInt(radius * 2 + 1) - radius;
    }

    /**
     * Random block column (x, z) within the configured spawn radius, centred on 0.
     * Result: {@code [x, z]}.
     */
    public static int[] randomColumn(Random rng, int spawnRadius) {
        return new int[]{randomCoordinate(rng, spawnRadius), randomCoordinate(rng, spawnRadius)};
    }

    /** Landing location one block above the floor, centred on the chosen block. */
    public static Location toLanding(World world, int blockX, int blockZ, int floorY) {
        return new Location(world, blockX + 0.5, floorY + 1.0, blockZ + 0.5);
    }

    /**
     * Pick a random spawn and asynchronously preload its chunk. The returned
     * future completes on the main thread with a ready-to-teleport location.
     */
    public static CompletableFuture<Location> locateAsync(World world, Settings settings) {
        int[] xz = randomColumn(ThreadLocalRandom.current(), settings.world().spawnRadius());
        Location landing = toLanding(world, xz[0], xz[1], settings.world().floorY());
        int chunkX = xz[0] >> 4;
        int chunkZ = xz[1] >> 4;
        // Preload (generate if needed) the destination chunk off the main thread.
        return world.getChunkAtAsync(chunkX, chunkZ, true).thenApply(chunk -> landing);
    }
}
