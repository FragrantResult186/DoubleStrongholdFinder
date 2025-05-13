package fragrant.doublestronghold.domain;

public class PortalRoomInfo {
    public final int roomMinX, roomMinY, roomMinZ;
    public final int roomMaxX, roomMaxY, roomMaxZ;
    public final int portalMinX, portalMinY, portalMinZ;
    public final int portalMaxX, portalMaxY, portalMaxZ;
    public final String facing;

    public PortalRoomInfo(
            int roomMinX, int roomMinY, int roomMinZ,
            int roomMaxX, int roomMaxY, int roomMaxZ,
            int portalMinX, int portalMinY, int portalMinZ,
            int portalMaxX, int portalMaxY, int portalMaxZ,
            String facing) {
        this.roomMinX = roomMinX;
        this.roomMinY = roomMinY;
        this.roomMinZ = roomMinZ;
        this.roomMaxX = roomMaxX;
        this.roomMaxY = roomMaxY;
        this.roomMaxZ = roomMaxZ;
        this.portalMinX = portalMinX;
        this.portalMinY = portalMinY;
        this.portalMinZ = portalMinZ;
        this.portalMaxX = portalMaxX;
        this.portalMaxY = portalMaxY;
        this.portalMaxZ = portalMaxZ;
        this.facing = facing;
    }
}