package com.deepspace.compat;

import com.deepspace.DeepSpaceMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fml.ModList;

/**
 * Wrapper for FTBChunks claim detection.
 *
 * Confirmed API from ftb-chunks-forge-2001.3.6 + ftb-library-forge-2001.2.10:
 *
 *   ChunkDimPos(ResourceKey<Level> dim, int x, int z)
 *   FTBChunksAPI.api().getManager().getChunk(ChunkDimPos) -> @Nullable ClaimedChunk
 *
 * Uses reflection so the mod compiles without ftb-library on the classpath.
 * Full claim checking activates at runtime when both ftbchunks + ftblibrary are present.
 */
public final class FTBChunksCompat {

    private FTBChunksCompat() {}

    private static final boolean FTBCHUNKS_LOADED = ModList.get().isLoaded("ftbchunks");

    // Reflection state
    private static volatile boolean reflectionReady  = false;
    private static volatile boolean reflectionFailed = false;

    private static java.lang.reflect.Method  mApi;
    private static java.lang.reflect.Method  mIsManagerLoaded;
    private static java.lang.reflect.Method  mGetManager;
    private static java.lang.reflect.Method  mGetChunk;
    private static java.lang.reflect.Constructor<?> ctorChunkDimPos;

    /**
     * Returns true if the chunk at (chunkX, chunkZ) in the given level is FTBChunks-claimed.
     * Returns false if FTBChunks is not loaded or claim check fails.
     */
    public static boolean isChunkClaimed(ServerLevel level, int chunkX, int chunkZ) {
        if (!FTBCHUNKS_LOADED) return false;
        if (reflectionFailed) return false;

        try {
            if (!reflectionReady) initReflection();
            if (reflectionFailed) return false;

            // FTBChunksAPI.api() -- static method
            Object api = mApi.invoke(null);

            // Check manager is loaded (can be false during world init)
            Boolean managerLoaded = (Boolean) mIsManagerLoaded.invoke(api);
            if (!managerLoaded) return false;

            // api.getManager()
            Object manager = mGetManager.invoke(api);
            if (manager == null) return false;

            // new ChunkDimPos(level.dimension(), chunkX, chunkZ)
            Object dimPos = ctorChunkDimPos.newInstance(level.dimension(), chunkX, chunkZ);

            // manager.getChunk(dimPos) -> nullable ClaimedChunk
            Object claimed = mGetChunk.invoke(manager, dimPos);
            return claimed != null;

        } catch (Exception e) {
            DeepSpaceMod.LOGGER.debug("[FTBChunksCompat] Claim check failed [{},{}]: {}",
                chunkX, chunkZ, e.getMessage());
            return false;
        }
    }

    /**
     * Returns true if the chunk at (chunkX, chunkZ) is claimed by someone OTHER
     * than one of the players in allowedOwners.
     *
     * @param allowedOwners UUIDs of players whose claims are acceptable (target player + team).
     *                      If null or empty, any claim counts as "other".
     */
    public static boolean isChunkClaimedByOther(
            ServerLevel level, int chunkX, int chunkZ,
            java.util.Set<java.util.UUID> allowedOwners,
            java.util.UUID claimantUUID) {

        if (!FTBCHUNKS_LOADED) return false;
        if (reflectionFailed) return false;

        try {
            if (!reflectionReady) initReflection();
            if (reflectionFailed) return false;

            Object api = mApi.invoke(null);
            Boolean managerLoaded = (Boolean) mIsManagerLoaded.invoke(api);
            if (!managerLoaded) return false;

            Object manager = mGetManager.invoke(api);
            if (manager == null) return false;

            Object dimPos = ctorChunkDimPos.newInstance(level.dimension(), chunkX, chunkZ);
            Object claimed = mGetChunk.invoke(manager, dimPos);
            if (claimed == null) return false; // unclaimed

            // If we have no allowed owners set, any claim counts as "other"
            if (allowedOwners == null || allowedOwners.isEmpty()) return true;

            // Inspect the FTB Chunks team that owns this claim.
            // Existing behavior allows the target player / their FTB Teams members.
            // FTB Chunks 2001.3.6 additionally exposes ChunkTeamData.isAlly(UUID),
            // so an allied claim can be authorized directly without enumerating allies.
            try {
                java.lang.reflect.Method getTeamData = claimed.getClass().getMethod("getTeamData");
                Object teamData = getTeamData.invoke(claimed);
                if (teamData != null) {
                    java.util.UUID ownerUUID = null;
                    try {
                        java.lang.reflect.Method getTeam = teamData.getClass().getMethod("getTeam");
                        Object team = getTeam.invoke(teamData);
                        if (team != null) {
                            java.lang.reflect.Method getId = team.getClass().getMethod("getId");
                            Object id = getId.invoke(team);
                            if (id instanceof java.util.UUID uid) ownerUUID = uid;
                        }
                    } catch (Exception ignored) {
                        // Owner identity lookup is best-effort; alliance check is independent.
                    }

                    if (ownerUUID != null && allowedOwners.contains(ownerUUID)) {
                        return false;
                    }

                    if (claimantUUID != null) {
                        java.lang.reflect.Method isAlly = teamData.getClass().getMethod(
                                "isAlly", java.util.UUID.class);
                        Object ally = isAlly.invoke(teamData, claimantUUID);
                        if (Boolean.TRUE.equals(ally)) {
                            DeepSpaceMod.LOGGER.debug(
                                    "[FTBChunksCompat] Allowing claimed chunk [{},{}]: claimant {} is an FTB Chunks ally of claim owner {}.",
                                    chunkX, chunkZ, claimantUUID, ownerUUID);
                            return false;
                        }
                    }
                }
            } catch (Exception e) {
                DeepSpaceMod.LOGGER.debug(
                        "[FTBChunksCompat] Could not evaluate FTB Chunks claim ownership/alliance [{},{}]: {}",
                        chunkX, chunkZ, e.getMessage());
            }
            return true;

        } catch (Exception e) {
            DeepSpaceMod.LOGGER.debug("[FTBChunksCompat] Claim-by-other check failed [{},{}]: {}",
                    chunkX, chunkZ, e.getMessage());
            return false;
        }
    }

    private static void initReflection() {
        try {
            // FTBChunksAPI
            Class<?> apiClass     = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI");
            Class<?> apiInterface = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI$API");
            Class<?> managerClass = Class.forName("dev.ftb.mods.ftbchunks.api.ClaimedChunkManager");
            // ChunkDimPos from ftb-library-forge-2001.2.10
            // Constructor: (ResourceKey<Level>, int, int) -- confirmed from bytecode
            Class<?> dimPosClass  = Class.forName("dev.ftb.mods.ftblibrary.math.ChunkDimPos");

            mApi             = apiClass.getMethod("api");
            Object apiInst   = mApi.invoke(null);
            mIsManagerLoaded = apiInst.getClass().getMethod("isManagerLoaded");
            mGetManager      = apiInst.getClass().getMethod("getManager");
            mGetChunk        = managerClass.getMethod("getChunk", dimPosClass);
            ctorChunkDimPos  = dimPosClass.getConstructor(
                net.minecraft.resources.ResourceKey.class, int.class, int.class);

            reflectionReady = true;
            DeepSpaceMod.LOGGER.info("[FTBChunksCompat] Claim checking active " +
                "(ftb-library-2001.2.10 + ftb-chunks-2001.3.6).");
        } catch (Exception e) {
            reflectionFailed = true;
            DeepSpaceMod.LOGGER.warn("[FTBChunksCompat] Could not init claim checking: {}. " +
                "Generation will proceed without FTBChunks claim safety.", e.getMessage());
        }
    }
}
