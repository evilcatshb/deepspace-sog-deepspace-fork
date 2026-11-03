package com.deepspace.compat;

import com.deepspace.DeepSpaceMod;
import com.deepspace.crash.CrashBubbleData;
import com.deepspace.crash.DSWorldData;
import com.deepspace.crash.CrashSpawnUtil;
import com.deepspace.init.DSDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

/**
 * Ad Astra integration for Deep Space.
 *
 * ── Planet / Solar-System Registration ───────────────────────────────────────
 *   Ad Astra uses SimpleJsonResourceReloadListener for both planet data and sky
 *   renderers, scanning every resource pack for:
 *     data/<any>/planets/<name>.json          → planet data
 *     assets/<any>/planet_renderers/<name>.json → sky renderer
 *
 *   Our files:
 *     data/ad_astra/planets/deep_space.json          (planet data)
 *     assets/deepspace/planet_renderers/deep_space.json (sky renderer)
 *
 *   deep_space.json does NOT have an "orbit" field → isSpace() returns true,
 *   meaning Deep Space IS the space destination (like earth_orbit, not like moon).
 *   This is correct: Deep Space is a void full of asteroids, not a surface world.
 *
 *   The planet appears in Ad Astra's solar-system map under ad_astra:solar_system.
 *   It requires a Tier-2 rocket. The additional_launch_dimensions list includes all
 *   standard AA planets so the player can reach Deep Space from any of them.
 *
 * ── Rocket Landing Redirect ───────────────────────────────────────────────────
 *   When a player travels to deepspace:deep_space via rocket, Ad Astra fires a
 *   dimension change.  We hook PlayerChangedDimensionEvent to detect arrivals
 *   and teleport the player above their personal crashed ship so they can land
 *   the rocket nearby rather than at the default (0, 64, 0) origin.
 *
 *   The player is placed ABOVE the ship roof (Y + 20) so a descending rocket has
 *   clear air to land in.  If they entered without a rocket (e.g. /tp command)
 *   they are placed at the safe interior spawn inside the ship.
 *
 *   Rocket entity: Ad Astra's ITeleporter.placeEntity() keeps vehicle/passenger
 *   relationships intact, so redirecting the player also moves the rocket.
 */
public final class AdAstraCompat {

    private static final boolean AD_ASTRA_LOADED = ModList.get().isLoaded("ad_astra");

    /** Blocks above ship floor to spawn the rocket so it has room to descend. */
    private static final int ROCKET_ARRIVAL_Y_CLEARANCE = 20;

    public AdAstraCompat() {}

    // ── FMLCommonSetupEvent ────────────────────────────────────────────────────

    public static void register() {
        if (!AD_ASTRA_LOADED) {
            DeepSpaceMod.LOGGER.error(
                "[AdAstraCompat] Ad Astra NOT loaded -- Deep Space requires it.");
            return;
        }
        DeepSpaceMod.LOGGER.info(
            "[AdAstraCompat] Ad Astra present. " +
            "Planet: data/ad_astra/planets/deep_space.json | " +
            "Renderer: assets/deepspace/planet_renderers/deep_space.json");
    }

    public static boolean isDeepSpace(ResourceKey<Level> dim) {
        return DSDimensions.DEEP_SPACE.equals(dim);
    }

    // ── Rocket landing redirect ────────────────────────────────────────────────

    /**
     * Fires on the server after a player changes dimension.
     * Intercepts arrivals into deepspace:deep_space and redirects them to the
     * player's personal crash-site location rather than the world origin.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!AD_ASTRA_LOADED) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Only handle arrivals INTO Deep Space
        if (!DSDimensions.DEEP_SPACE.equals(event.getTo())) return;

        if (player.getServer() == null) return;
        ServerLevel deepSpace = player.getServer().getLevel(DSDimensions.DEEP_SPACE);
        if (deepSpace == null) return;

        DSWorldData wd = DSWorldData.getOrCreate(deepSpace);
        CrashBubbleData bubble = wd.getBubble(player.getUUID());

        if (bubble == null) {
            // No crash site exists for this player; leave the player where Ad Astra placed them.
            // but if it does, leave the player where AA placed them.
            DeepSpaceMod.LOGGER.warn(
                "[AdAstraCompat] {} arrived in Deep Space but has no crash bubble -- not redirecting",
                player.getName().getString());
            return;
        }

        // Defer by one tick so AA's own teleport finishes before we override the position
        player.getServer().execute(() -> redirectRocketArrival(player, deepSpace, bubble));
    }

    private static void redirectRocketArrival(ServerPlayer player,
                                               ServerLevel deepSpace,
                                               CrashBubbleData bubble) {
        // Guard: player may have moved again in that one tick
        if (!player.level().dimension().equals(DSDimensions.DEEP_SPACE)) return;

        int cx = bubble.shipCentreX();
        int cz = bubble.shipCentreZ();

        boolean inRocket = isRidingRocket(player);

        double arrivalX = cx + 0.5;
        double arrivalZ = cz + 0.5;
        double arrivalY;

        if (inRocket) {
            // Place rocket high above crash site – enough headroom to descend safely
            arrivalY = bubble.shipOriginY + ROCKET_ARRIVAL_Y_CLEARANCE;
        } else {
            // Foot-travel or command: put them inside the ship interior
            BlockPos safePos = CrashSpawnUtil.findVerifiedSafeSpawn(deepSpace, bubble);
            arrivalX = safePos.getX() + 0.5;
            arrivalY = safePos.getY();
            arrivalZ = safePos.getZ() + 0.5;
        }

        player.teleportTo(deepSpace, arrivalX, arrivalY, arrivalZ,
                          player.getYRot(), player.getXRot());


        DeepSpaceMod.LOGGER.info(
            "[AdAstraCompat] Redirected {} to crash-site [{}, {}, {}] (inRocket={})",
            player.getName().getString(),
            Math.round(arrivalX), Math.round(arrivalY), Math.round(arrivalZ),
            inRocket);
    }

    /**
     * Returns true if the player is currently riding an Ad Astra rocket entity.
     * Uses type name matching so it works without compiling against AA internals.
     */
    private static boolean isRidingRocket(ServerPlayer player) {
        if (!player.isPassenger()) return false;
        var vehicle = player.getVehicle();
        if (vehicle == null) return false;
        // Ad Astra rocket entity type RL: "ad_astra:rocket"
        var typeKey = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(vehicle.getType());
        if (typeKey == null) return false;
        return "ad_astra".equals(typeKey.getNamespace()) && typeKey.getPath().contains("rocket");
    }
}
