package fragrant.doublestronghold.core.util;

import fragrant.b2j.biome.BiomeCache;
import fragrant.b2j.biome.BiomeVerifier;
import fragrant.b2j.generator.structure.BedrockStructure;
import fragrant.b2j.generator.structure.BedrockStructureType;
import fragrant.b2j.util.Position;
import nl.kallestruik.noisesampler.minecraft.Dimension;
import nl.jellejurre.biomesampler.minecraft.Biome;

/**
 * Utility class for biome and structure related operations
 */
public class BiomeUtils {
    // Constants
    private static final int STRUCTURE_SPACING = 1217;

    /**
     * Checks if a chunk can have a village structure
     *
     * @param worldSeed The world seed
     * @param chunkPos The chunk position
     * @return True if the chunk can have a village
     */
    public static boolean canHaveVillage(long worldSeed, Position.ChunkPos chunkPos) {
        Position.Pos villagePos = BedrockStructure.isBedrockStructureChunk(
                BedrockStructureType.VILLAGE, STRUCTURE_SPACING, worldSeed, chunkPos.x(), chunkPos.z()
        );

        if (villagePos == null) {
            return false;
        }

        int blockX = chunkPos.x() << 4;
        int blockZ = chunkPos.z() << 4;
        BiomeCache biomeCache = new BiomeCache(worldSeed);
        Biome biome = biomeCache.getBiomeAt(blockX, 64, blockZ, Dimension.OVERWORLD);

        return BiomeVerifier.VILLAGE_BIOMES.contains(biome);
    }
}