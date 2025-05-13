package fragrant.doublestronghold.core.search;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import fragrant.doublestronghold.domain.PortalRoomInfo;
import fragrant.doublestronghold.domain.SearchParameters;

public class SearchEngine {
    private final AtomicBoolean isSearching;
    private final AtomicLong currentStartSeed;
    private final AtomicLong seedsProcessed;
    private final AtomicInteger resultsFound;
    private final BlockingQueue<String> resultQueue;
    private final StrongholdSearcher searcher;
    private final AtomicLong nextSeed = new AtomicLong(0);
    private static final int BATCH_SIZE = 2000;
    private static final int UPDATE_INTERVAL = 100;

    public SearchEngine(AtomicBoolean isSearching,
                        AtomicLong currentStartSeed,
                        AtomicLong seedsProcessed,
                        AtomicInteger resultsFound,
                        BlockingQueue<String> resultQueue,
                        StrongholdSearcher searcher) {
        this.isSearching = isSearching;
        this.currentStartSeed = currentStartSeed;
        this.seedsProcessed = seedsProcessed;
        this.resultsFound = resultsFound;
        this.resultQueue = resultQueue;
        this.searcher = searcher;
    }

    public void search(long startSeed, long endSeed, SearchParameters params) {
        try {
            long processed = 0;
            for (long currentSeed = startSeed; currentSeed <= endSeed && isSearching.get(); currentSeed += BATCH_SIZE) {
                long batchEnd = Math.min(currentSeed + BATCH_SIZE - 1, endSeed);
                for (long seed = currentSeed; seed <= batchEnd && isSearching.get(); seed++) {
                    boolean found = searcher.check(seed, params);
                    if (found) {
                        PortalRoomInfo[] portals = searcher.getPortalInfo(seed, params);
                        if (portals != null && portals.length == 2) {
                            save(seed, portals[0], portals[1]);
                            resultsFound.incrementAndGet();
                        }
                    }
                    processed++;
                    if (processed % UPDATE_INTERVAL == 0) {
                        seedsProcessed.addAndGet(UPDATE_INTERVAL);
                        currentStartSeed.set(seed + 1);
                    }
                }
            }
            long remaining = processed % UPDATE_INTERVAL;
            if (remaining > 0) {
                seedsProcessed.addAndGet(remaining);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void searchSequential(long startSeed, long endSeed, SearchParameters params, int threadCount) {
        nextSeed.compareAndSet(0, startSeed);
        try {
            while (isSearching.get()) {
                long currentSeed = nextSeed.getAndAdd(BATCH_SIZE);
                if (currentSeed > endSeed) break;

                long batchEnd = Math.min(currentSeed + BATCH_SIZE - 1, endSeed);
                long processed = 0;

                for (long seed = currentSeed; seed <= batchEnd && isSearching.get(); seed++) {
                    boolean found = searcher.check(seed, params);
                    if (found) {
                        PortalRoomInfo[] portals = searcher.getPortalInfo(seed, params);
                        if (portals != null && portals.length == 2) {
                            save(seed, portals[0], portals[1]);
                            resultsFound.incrementAndGet();
                        }
                    }
                    processed++;
                    if (processed % UPDATE_INTERVAL == 0) {
                        seedsProcessed.addAndGet(UPDATE_INTERVAL);
                        currentStartSeed.set(seed + 1);
                    }
                }

                long remaining = processed % UPDATE_INTERVAL;
                if (remaining > 0) {
                    seedsProcessed.addAndGet(remaining);
                    currentStartSeed.set(batchEnd + 1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void save(long seed, PortalRoomInfo portal1, PortalRoomInfo portal2) {
        int midpointX = (portal1.portalMinX + portal1.portalMaxX + portal2.portalMinX + portal2.portalMaxX) / 4;
        int midpointZ = (portal1.portalMinZ + portal1.portalMaxZ + portal2.portalMinZ + portal2.portalMaxZ) / 4;
        String output = String.format("%d /tp %d ~ %d", seed, midpointX, midpointZ);
        resultQueue.offer(output);
    }

}