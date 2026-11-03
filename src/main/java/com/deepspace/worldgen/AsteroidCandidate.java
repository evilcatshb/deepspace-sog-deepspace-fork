package com.deepspace.worldgen;

import com.deepspace.registry.AsteroidDefinition;
import com.deepspace.worldgen.ore.HostKey;

/**
 * A resolved asteroid instance for one chunk-generation pass.
 * Created transiently inside {@link AsteroidSectorGrid} — never persisted.
 * All fields final; safe to share read-only across worldgen threads.
 */
public final class AsteroidCandidate {

    public final int centerX, centerY, centerZ;
    public final int rx, ry, rz;
    public final AsteroidShapeType shape;
    public final AsteroidShapeType.Params shapeParams;
    public final AsteroidDefinition def;
    public final HostKey host;
    public final long shapeSeed;
    public final long oreSeed;

    /** True if this asteroid is bridged to a neighbour (walkable neck). */
    public final boolean connected;
    /** Neighbour centre (X/Y/Z) + its radii — used to clip the platform to
     *  both asteroids' real curvature (BUG_12). */
    public final int linkX, linkZ, linkCenterY, linkRx, linkRy, linkRz;
    /** Shared platform top (BUG_6) and bottom (BUG_13) altitudes. */
    public final int walkTopY, walkBotY;
    /** Walkway platform radius = avg of the two asteroids' radii (BUG_0). */
    public final int linkR;
    /** Symmetric per-pair seed so both endpoints erode the bridge identically. */
    public final long linkSeed;
    /** Ramp endpoints (each asteroid's surface Y) + thickness = avg asteroid
     *  height, so the bridge inclines and is as tall as the bodies it joins
     *  (BUG_12/15/16). */
    public final int walkAtopY, walkBtopY, walkHeight;
    /** Neighbour ore identity so the bridge can carry a mix of both
     *  asteroids' ores (BUG_13). */
    public final AsteroidDefinition linkDef;
    public final HostKey linkHost;
    public final long linkOreSeed;
    /** Guaranteed-spawn asteroids keep their JSON ores instead of #forge:ores (BUG_8). */
    public final boolean forceDefOres;

    public AsteroidCandidate(int centerX, int centerY, int centerZ,
                             int rx, int ry, int rz,
                             AsteroidShapeType shape, AsteroidShapeType.Params shapeParams,
                             AsteroidDefinition def, HostKey host,
                             long shapeSeed, long oreSeed,
                             boolean connected, int linkX, int linkZ, int walkTopY,
                             int linkR, int walkBotY, int linkCenterY,
                             int linkRx, int linkRy, int linkRz, long linkSeed,
                             int walkAtopY, int walkBtopY, int walkHeight,
                             AsteroidDefinition linkDef, HostKey linkHost,
                             long linkOreSeed,
                             boolean forceDefOres) {
        this.centerX = centerX; this.centerY = centerY; this.centerZ = centerZ;
        this.rx = rx; this.ry = ry; this.rz = rz;
        this.shape = shape; this.shapeParams = shapeParams;
        this.def = def; this.host = host;
        this.shapeSeed = shapeSeed; this.oreSeed = oreSeed;
        this.connected = connected;
        this.linkX = linkX; this.linkZ = linkZ; this.walkTopY = walkTopY;
        this.linkR = linkR; this.walkBotY = walkBotY;
        this.linkCenterY = linkCenterY;
        this.linkRx = linkRx; this.linkRy = linkRy; this.linkRz = linkRz;
        this.linkSeed = linkSeed;
        this.walkAtopY = walkAtopY;
        this.walkBtopY = walkBtopY;
        this.walkHeight = walkHeight;
        this.linkDef = linkDef;
        this.linkHost = linkHost;
        this.linkOreSeed = linkOreSeed;
        this.forceDefOres = forceDefOres;
    }

    /** Half-extent padding for surface warp + connection neck. */
    public int pad() { return 4; }

    public int minX() { return centerX - rx - pad(); }
    public int maxX() { return centerX + rx + pad(); }
    public int minZ() { return centerZ - rz - pad(); }
    public int maxZ() { return centerZ + rz + pad(); }

    /** Does this asteroid's bounding box reach into chunk (cx, cz)? */
    public boolean reachesChunk(int cx, int cz) {
        int chunkMinX = cx << 4, chunkMinZ = cz << 4;
        int chunkMaxX = chunkMinX + 15, chunkMaxZ = chunkMinZ + 15;
        int aMinX = minX(), aMaxX = maxX(), aMinZ = minZ(), aMaxZ = maxZ();
        if (connected) { // include the neck reach toward the link point + platform radius
            aMinX = Math.min(aMinX, linkX) - linkR;
            aMaxX = Math.max(aMaxX, linkX) + linkR;
            aMinZ = Math.min(aMinZ, linkZ) - linkR;
            aMaxZ = Math.max(aMaxZ, linkZ) + linkR;
        }
        return aMaxX >= chunkMinX && aMinX <= chunkMaxX
            && aMaxZ >= chunkMinZ && aMinZ <= chunkMaxZ;
    }
}
