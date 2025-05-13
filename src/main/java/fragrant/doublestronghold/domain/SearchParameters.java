package fragrant.doublestronghold.domain;

public class SearchParameters {
    public final int centerX;
    public final int centerZ;
    public final int searchRadius;
    public final double maxStrongholdDistance;
    public final double maxPortalDistance;

    public SearchParameters(int centerX, int centerZ, int searchRadius,
                            double maxStrongholdDistance, double maxPortalDistance) {
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.searchRadius = searchRadius;
        this.maxStrongholdDistance = maxStrongholdDistance;
        this.maxPortalDistance = maxPortalDistance;
    }
}