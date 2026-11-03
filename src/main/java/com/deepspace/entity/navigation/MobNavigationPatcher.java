package com.deepspace.entity.navigation;

import com.deepspace.DeepSpaceMod;
import com.deepspace.entity.SpaceWhaleEntity;
import com.deepspace.init.DSDimensions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.util.Set;

/**
 * Forge event handler that installs {@link DeepSpaceNavigation} on the four
 * ground-mob types listed below whenever they enter (or are loaded in) the
 * {@code deepspace:deep_space} dimension.
 *
 * <h3>Affected entity types</h3>
 * <ul>
 *   <li>{@code minecraft:zombified_piglin}</li>
 *   <li>{@code minecraft:villager}</li>
 *   <li>{@code minecraft:iron_golem}</li>
 *   <li>{@code minecraft:pillager}</li>
 * </ul>
 *
 * All other entities – including {@link SpaceWhaleEntity} – are untouched and
 * keep their original navigation.
 *
 * <h3>Why reflection?</h3>
 * {@code Mob.navigation} is {@code protected}, making it inaccessible from an
 * external event handler without an access transformer.  Searching for the
 * field by its declared type ({@link PathNavigation}) is resilient to SRG/
 * Parchment mapping changes and avoids coupling to a specific obfuscated name.
 *
 * <h3>One-tick deferral</h3>
 * The patch is applied one server tick after the entity joins.  This ensures
 * the mob's own constructor and Forge entity-join hooks have finished before
 * we overwrite the navigation reference.
 */
public final class MobNavigationPatcher {

    // ── Affected types ────────────────────────────────────────────────────────

    private static final Set<ResourceLocation> PATCHED_TYPES = Set.of(
        new ResourceLocation("minecraft", "zombified_piglin"),
        new ResourceLocation("minecraft", "villager"),
        new ResourceLocation("minecraft", "iron_golem"),
        new ResourceLocation("minecraft", "pillager")
    );

    // ── Reflection handle ─────────────────────────────────────────────────────

    /**
     * Lazily initialised handle to {@code Mob.navigation}.
     * Searched by declared type so it works under both SRG and Parchment.
     */
    private static final Field NAV_FIELD = resolveNavigationField();

    private static Field resolveNavigationField() {
        // Walk the Mob class hierarchy looking for the PathNavigation field.
        // It is declared directly in Mob, but searching both Mob and its parent
        // (LivingEntity) is a harmless safety net.
        for (Class<?> cls : new Class<?>[] { Mob.class, Mob.class.getSuperclass() }) {
            for (Field f : cls.getDeclaredFields()) {
                if (PathNavigation.class.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        DeepSpaceMod.LOGGER.info(
                            "[MobNavPatcher] Resolved navigation field: {}.{}",
                            cls.getSimpleName(), f.getName());
                        return f;
                    } catch (Exception e) {
                        DeepSpaceMod.LOGGER.error(
                            "[MobNavPatcher] Found navigation field but could not make it accessible: {}",
                            e.getMessage());
                    }
                }
            }
        }
        DeepSpaceMod.LOGGER.error(
            "[MobNavPatcher] Could not locate Mob.navigation field. " +
            "Deep Space pathfinding will not be applied.");
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────

    public MobNavigationPatcher() {}

    // ── Event listener ────────────────────────────────────────────────────────

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {

        // ── Guard: only act in Deep Space, server-side ─────────────────────
        if (!DSDimensions.DEEP_SPACE.equals(event.getLevel().dimension())) return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        // ── Guard: must be a ground Mob ────────────────────────────────────
        if (!(event.getEntity() instanceof Mob mob)) return;

        // ── Guard: never patch the Space Whale ────────────────────────────
        if (mob instanceof SpaceWhaleEntity) return;

        // ── Guard: only the four listed vanilla types ──────────────────────
        ResourceLocation typeKey = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        if (typeKey == null || !PATCHED_TYPES.contains(typeKey)) return;

        // ── Guard: reflection handle must be available ─────────────────────
        if (NAV_FIELD == null) return;

        // ── Defer one tick ─────────────────────────────────────────────────
        // Server.execute() schedules the task at the start of the next
        // server tick, after all entity-join hooks have completed.
        serverLevel.getServer().execute(() -> {
            if (!mob.isAlive()) return;
            try {
                DeepSpaceNavigation nav = new DeepSpaceNavigation(mob, serverLevel);
                NAV_FIELD.set(mob, nav);

                DeepSpaceMod.LOGGER.info(
                    "[MobNavPatcher] Installed DeepSpaceNavigation on {} at {}",
                    typeKey, mob.blockPosition());

            } catch (IllegalAccessException e) {
                DeepSpaceMod.LOGGER.error(
                    "[MobNavPatcher] Access denied setting navigation on {}: {}",
                    typeKey, e.getMessage());
            } catch (Exception e) {
                DeepSpaceMod.LOGGER.error(
                    "[MobNavPatcher] Unexpected error patching {}: {}",
                    typeKey, e.getMessage());
            }
        });
    }
}
