package fragrant.doublestronghold.core.search;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import fragrant.b2j.generator.structure.BedrockStructureConfig;
import fragrant.b2j.generator.structure.BedrockStructureType;
import fragrant.b2j.generator.structure.overworld.Stronghold;
import fragrant.b2j.random.BedrockRandom;
import fragrant.b2j.util.Position;
import fragrant.doublestronghold.core.util.BiomeUtils;
import fragrant.doublestronghold.domain.PortalRoomInfo;
import fragrant.doublestronghold.domain.SearchParameters;
import fragrant.doublestronghold.domain.VillageStrongholdInfo;
import kaptainwutax.featureutils.structure.generator.StrongholdGenerator;
import kaptainwutax.featureutils.structure.generator.piece.StructurePiece;
import kaptainwutax.featureutils.structure.generator.piece.stronghold.PortalRoom;
import kaptainwutax.seedutils.mc.MCVersion;
import kaptainwutax.seedutils.util.BlockBox;

public class StrongholdSearcher {
    private static final int BEDROCK_VERSION = 1217;
    private static final MCVersion SH_VERSION = MCVersion.v1_16;
    private static final BedrockStructureConfig STRONGHOLD_CONFIG = BedrockStructureConfig.getForType(BedrockStructureType.STRONGHOLD, BEDROCK_VERSION);
    private final ConcurrentHashMap<Long, List<Position.Pos>> strongholdCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PortalRoomInfo> portalRoomCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> villageCache = new ConcurrentHashMap<>();
    private final AtomicBoolean isSearching;

    public StrongholdSearcher(AtomicBoolean isSearching) {
        this.isSearching = isSearching;
    }

    public void clearCaches() {
        strongholdCache.clear();
        portalRoomCache.clear();
        villageCache.clear();
    }

    public boolean check(long worldSeed, SearchParameters params) {
        List<Position.Pos> staticStrongholds = strongholdCache.computeIfAbsent(worldSeed, seed -> getStaticStrongholds(seed, params.centerX, params.centerZ, params.searchRadius));

        if (staticStrongholds.isEmpty()) return false;
        for (Position.Pos staticPos : staticStrongholds) {
            Position.ChunkPos staticChunkPos = new Position.BlockPos(staticPos.getX(), staticPos.getZ()).toChunk();

            String staticPortalKey = worldSeed + ":" + staticChunkPos.x() + ":" + staticChunkPos.z();
            PortalRoomInfo staticPortal = portalRoomCache.computeIfAbsent(staticPortalKey,
                    key -> findPortalRoom(worldSeed, staticChunkPos.x(), staticChunkPos.z()));

            if (staticPortal == null) continue;
            int radius = 5;
            int maxChunkDist = (int)(params.maxStrongholdDistance / 16) + 1;
            if (maxChunkDist < radius) radius = maxChunkDist;
            for (int dx = -radius; dx <= radius && isSearching.get(); dx++) {
                for (int dz = -radius; dz <= radius && isSearching.get(); dz++) {
                    if (dx == 0 && dz == 0) continue;

                    Position.ChunkPos adjacentChunk = new Position.ChunkPos(
                            staticChunkPos.x() + dx,
                            staticChunkPos.z() + dz
                    );

                    int strongholdDistX = (adjacentChunk.x() << 4) - staticPos.getX();
                    int strongholdDistZ = (adjacentChunk.z() << 4) - staticPos.getZ();
                    double strongholdDistance = Math.sqrt(strongholdDistX * strongholdDistX + strongholdDistZ * strongholdDistZ);

                    if (strongholdDistance > params.maxStrongholdDistance) continue;
                    String villageKey = worldSeed + ":" + adjacentChunk.x() + ":" + adjacentChunk.z() + ":village";
                    Boolean hasVillage = villageCache.computeIfAbsent(villageKey,
                            key -> BiomeUtils.canHaveVillage(worldSeed, adjacentChunk));

                    if (!hasVillage) continue;
                    VillageStrongholdInfo villageInfo = haveStronghold(worldSeed, adjacentChunk);
                    if (villageInfo == null) continue;
                    String villagePortalKey = worldSeed + ":" + adjacentChunk.x() + ":" + adjacentChunk.z();
                    PortalRoomInfo villagePortal = portalRoomCache.computeIfAbsent(villagePortalKey,
                            key -> findPortalRoom(worldSeed, adjacentChunk.x(), adjacentChunk.z()));

                    if (villagePortal == null) continue;
                    double portalDistance = calculatePortalDistance(staticPortal, villagePortal);
                    if (portalDistance <= params.maxPortalDistance) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public PortalRoomInfo[] getPortalInfo(long worldSeed, SearchParameters params) {
        List<Position.Pos> staticStrongholds = strongholdCache.get(worldSeed);
        if (staticStrongholds == null || staticStrongholds.isEmpty()) {return null;}

        for (Position.Pos staticPos : staticStrongholds) {
            Position.ChunkPos staticChunkPos = new Position.BlockPos(staticPos.getX(), staticPos.getZ()).toChunk();
            String staticPortalKey = worldSeed + ":" + staticChunkPos.x() + ":" + staticChunkPos.z();
            PortalRoomInfo staticPortal = portalRoomCache.get(staticPortalKey);

            if (staticPortal == null) continue;
            int radius = 8;
            int maxChunkDist = (int)(params.maxStrongholdDistance / 16) + 1;
            if (maxChunkDist < radius) radius = maxChunkDist;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dz == 0) continue;

                    Position.ChunkPos adjacentChunk = new Position.ChunkPos(
                            staticChunkPos.x() + dx,
                            staticChunkPos.z() + dz
                    );

                    String villagePortalKey = worldSeed + ":" + adjacentChunk.x() + ":" + adjacentChunk.z();
                    PortalRoomInfo villagePortal = portalRoomCache.get(villagePortalKey);

                    if (villagePortal != null) {
                        double portalDistance = calculatePortalDistance(staticPortal, villagePortal);
                        if (portalDistance <= params.maxPortalDistance) {
                            return new PortalRoomInfo[] { staticPortal, villagePortal };
                        }
                    }
                }
            }
        }
        return null;
    }

    private List<Position.Pos> getStaticStrongholds(long worldSeed, int centerX, int centerZ, int searchRadius) {
        List<Position.Pos> strongholds = new ArrayList<>();
        if (STRONGHOLD_CONFIG == null) return strongholds;

        int regionSize = STRONGHOLD_CONFIG.getSpacing();
        int maxRegRadius = (int) Math.ceil((double) searchRadius / (regionSize * 16));
        int centerRegX = Math.floorDiv(centerX, regionSize * 16);
        int centerRegZ = Math.floorDiv(centerZ, regionSize * 16);
        int minRegX = centerRegX - maxRegRadius;
        int maxRegX = centerRegX + maxRegRadius;
        int minRegZ = centerRegZ - maxRegRadius;
        int maxRegZ = centerRegZ + maxRegRadius;
        int searchRadiusSq = searchRadius * searchRadius;

        for (int regX = minRegX; regX <= maxRegX; regX++) {
            for (int regZ = minRegZ; regZ <= maxRegZ; regZ++) {
                Position.Pos pos = Stronghold.getStaticStronghold(STRONGHOLD_CONFIG, worldSeed, regX, regZ);

                if (pos != null) {
                    int dx = pos.getX() - centerX;
                    int dz = pos.getZ() - centerZ;
                    int distSq = dx * dx + dz * dz;

                    if (distSq <= searchRadiusSq) {
                        strongholds.add(pos);
                    }
                }
            }
        }
        return strongholds;
    }

    private VillageStrongholdInfo haveStronghold(long worldSeed, Position.ChunkPos targetChunk) {
        BedrockRandom mt = new BedrockRandom(worldSeed);
        VillageStrongholdInfo result = new VillageStrongholdInfo();

        double angle = Math.PI * 2 * BedrockRandom.int2Float((int) mt.nextUnsignedInt());
        int dist = mt.nextInt(16) + 40;
        int count = 0;
        int biomeFailures = 0;
        while (count < 3) {
            int cx = (int) Math.floor(Math.cos(angle) * dist);
            int cz = (int) Math.floor(Math.sin(angle) * dist);

            boolean foundVillage = false;
            boolean isTargetVillage = false;
            for (int x = cx - 8; x < cx + 8; x++) {
                for (int z = cz - 8; z < cz + 8; z++) {
                    Position.ChunkPos chunkPos = new Position.ChunkPos(x, z);
                    if (Stronghold.isVillageChunk(worldSeed, chunkPos, BEDROCK_VERSION)) {
                        if (chunkPos.x() == targetChunk.x() && chunkPos.z() == targetChunk.z()) {
                            isTargetVillage = true;
                            result.index = count + 1;
                            result.biomeFailuresNeeded = biomeFailures;
                        }
                        foundVillage = true;
                        break;
                    }
                }
                if (foundVillage) break;
            }
            if (foundVillage) {
                count++;
                angle += 3 * Math.PI / 5;
                dist += 8;
                if (isTargetVillage) {
                    return result;
                }
            } else {
                biomeFailures++;
                angle += Math.PI / 4;
                dist += 4;
            }
        }
        return null;
    }

    private double calculatePortalDistance(PortalRoomInfo portal1, PortalRoomInfo portal2) {
        double p1CenterX = (portal1.portalMinX + portal1.portalMaxX) / 2.0;
        double p1CenterY = (portal1.portalMinY + portal1.portalMaxY) / 2.0;
        double p1CenterZ = (portal1.portalMinZ + portal1.portalMaxZ) / 2.0;
        double p2CenterX = (portal2.portalMinX + portal2.portalMaxX) / 2.0;
        double p2CenterY = (portal2.portalMinY + portal2.portalMaxY) / 2.0;
        double p2CenterZ = (portal2.portalMinZ + portal2.portalMaxZ) / 2.0;
        double dx = p2CenterX - p1CenterX;
        double dy = p2CenterY - p1CenterY;
        double dz = p2CenterZ - p1CenterZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private PortalRoomInfo findPortalRoom(long worldSeed, int chunkX, int chunkZ) {
        try {
            StrongholdGenerator generator = new StrongholdGenerator(SH_VERSION);
            generator.generate((int) worldSeed, chunkX, chunkZ);
            for (StructurePiece<?> piece : generator.pieceList) {
                if (piece instanceof PortalRoom portalRoom) {
                    BlockBox box = piece.getBoundingBox();
                    BlockBox portalBox = portalRoom.getEndFrameBB();
                    return new PortalRoomInfo(
                            box.minX, box.minY, box.minZ,
                            box.maxX, box.maxY, box.maxZ,
                            portalBox.minX, portalBox.minY, portalBox.minZ,
                            portalBox.maxX, portalBox.maxY, portalBox.maxZ,
                            piece.getFacing().toString()
                    );
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

}