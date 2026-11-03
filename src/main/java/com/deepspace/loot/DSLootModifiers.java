package com.deepspace.loot;

import com.deepspace.DSConstants;
import com.mojang.serialization.Codec;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class DSLootModifiers {
    private DSLootModifiers() {}

    public static final DeferredRegister<Codec<? extends net.minecraftforge.common.loot.IGlobalLootModifier>> MODIFIERS =
        DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, DSConstants.MODID);

    public static final RegistryObject<Codec<TntLootModifier>> REMOVE_TNT =
        MODIFIERS.register("remove_tnt", () -> TntLootModifier.CODEC);
}
