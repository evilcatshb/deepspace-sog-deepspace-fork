package com.deepspace.init;

import com.deepspace.DSConstants;
import com.deepspace.entity.SpaceWhaleEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Entity type registration for Deep Space mobs.
 *
 * @Mod.EventBusSubscriber(bus=MOD) means all @SubscribeEvent methods here
 * run on the mod event bus automatically — no manual forgeBus.register() needed.
 * The DeferredRegister is still wired in DeepSpaceMod constructor via
 *   DSEntities.ENTITY_TYPES.register(modBus)
 */
@Mod.EventBusSubscriber(modid = DSConstants.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DSEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DSConstants.MODID);

    /** Space Whale — giant peaceful/hostile flying mob of deep space. */
    public static final RegistryObject<EntityType<SpaceWhaleEntity>> SPACE_WHALE =
        ENTITY_TYPES.register("space_whale", () ->
            EntityType.Builder.<SpaceWhaleEntity>of(SpaceWhaleEntity::new, MobCategory.CREATURE)
                // BUG_1: These are base hitbox dims used by EntityType for network tracking range.
                // The actual per-tick AABB is computed directionally in SpaceWhaleEntity#makeBoundingBox()
                // so the full head-to-tail length (30 blocks) rotates with the whale correctly.
                .sized(12.0f, 6.0f)
                .clientTrackingRange(10)     // 160-block render distance (10 chunks)
                .updateInterval(3)
                .build("space_whale"));

    /**
     * Fires on the MOD bus after entities are registered.
     * Must be static and the class must be annotated @Mod.EventBusSubscriber(bus=MOD).
     * GeckoLib itself has no separate attribute registration hook — we simply call
     * createAttributes().build() here exactly as vanilla does.
     */
    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(SPACE_WHALE.get(), SpaceWhaleEntity.createAttributes().build());
    }
}
