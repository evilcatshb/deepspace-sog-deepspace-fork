package com.deepspace.events;

import com.deepspace.DeepSpaceMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DeepSpaceMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class AttributeEvents {

    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeModificationEvent event) {

        // 🔥 FIX: allow villagers to safely use melee combat
        event.add(EntityType.VILLAGER, Attributes.ATTACK_DAMAGE);

        // optional (recommended for stability)
        event.add(EntityType.VILLAGER, Attributes.ATTACK_SPEED);
    }
}