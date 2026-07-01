package co.xenastudios.xlimbo.world;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

/**
 * Ultra-cheap void generator: every chunk is empty except a single shared floor
 * layer. All vanilla generation phases (noise, caves, decorations, mobs,
 * structures) are disabled via the {@code shouldGenerate*} hooks, so producing a
 * chunk is effectively O(256) block sets and nothing else.
 */
public final class VoidChunkGenerator extends ChunkGenerator {

    private final Material floorBlock;
    private final int floorY;
    private final BiomeProvider biomeProvider;

    public VoidChunkGenerator(Material floorBlock, int floorY, Biome biome) {
        this.floorBlock = floorBlock;
        this.floorY = floorY;
        this.biomeProvider = new VoidBiomeProvider(biome);
    }

    /**
     * Place the single floor layer. This override is our custom generation and
     * always runs; the {@code shouldGenerate*} flags below only gate the vanilla
     * passes, which we keep off.
     */
    @Override
    public void generateSurface(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {
        int minY = chunkData.getMinHeight();
        int maxY = chunkData.getMaxHeight() - 1;
        int y = Math.max(minY, Math.min(maxY, floorY));
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                chunkData.setBlock(x, y, z, floorBlock);
            }
        }
    }

    @Override
    public BiomeProvider getDefaultBiomeProvider(WorldInfo worldInfo) {
        return biomeProvider;
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return new Location(world, 0.5, floorY + 1.0, 0.5);
    }

    // ---- disable every vanilla generation phase ----------------------------

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }
}
