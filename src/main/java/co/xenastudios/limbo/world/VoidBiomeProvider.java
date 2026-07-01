package co.xenastudios.limbo.world;

import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;

import java.util.List;

/**
 * Serves a single fixed biome for the whole world. Returning one constant biome
 * skips all per-column biome computation, keeping generation effectively O(1).
 */
public final class VoidBiomeProvider extends BiomeProvider {

    private final Biome biome;
    private final List<Biome> biomes;

    public VoidBiomeProvider(Biome biome) {
        this.biome = biome;
        this.biomes = List.of(biome);
    }

    @Override
    public Biome getBiome(WorldInfo worldInfo, int x, int y, int z) {
        return biome;
    }

    @Override
    public List<Biome> getBiomes(WorldInfo worldInfo) {
        return biomes;
    }
}
