package com.deepspace.init;

import com.deepspace.DSConstants;
import com.deepspace.items.AbnormalityScannerItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class DSItems {

    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, DSConstants.MODID);

    // ── Space Whale food ──────────────────────────────────────────────────────

    /** Raw Space Whale Meat: causes Food Poisoning (Hunger + Nausea) when eaten. */
    public static final RegistryObject<Item> SPACE_WHALE_MEAT = ITEMS.register("space_whale_meat",
        () -> new Item(new Item.Properties()
            .food(new FoodProperties.Builder()
                .nutrition(4)
                .saturationMod(0.3f)
                .meat()
                .effect(() -> new MobEffectInstance(MobEffects.HUNGER,   600, 1), 1.0f)
                .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 300, 0), 1.0f)
                .build())));

    /** Cooked Space Whale Steak: heals 8 hearts (16 HP). Smelt raw meat to obtain. */
    public static final RegistryObject<Item> SPACE_WHALE_STEAK = ITEMS.register("space_whale_steak",
        () -> new Item(new Item.Properties()
            .food(new FoodProperties.Builder()
                .nutrition(16)
                .saturationMod(1.0f)
                .meat()
                .build())));

    /** Space Whale Saddle: used to ride a tamed Space Whale. Creative/JEI only — no recipe. */
    public static final RegistryObject<Item> SPACE_WHALE_SADDLE = ITEMS.register("space_whale_saddle",
        () -> new Item(new Item.Properties().stacksTo(1)));

    // ── Tools / Scanners ─────────────────────────────────────────────────────

    /** Abnormality Scanner: handheld radar that locates nearest 3 NBT structures on right-click. */
    public static final RegistryObject<Item> ABNORMALITY_SCANNER = ITEMS.register("abnormality_scanner",
        () -> new AbnormalityScannerItem(new Item.Properties().stacksTo(1)));

    // ── Block items ───────────────────────────────────────────────────────────

    public static final RegistryObject<Item> EXOTIC_ICE_ITEM = ITEMS.register("exotic_ice",
        () -> new BlockItem(DSBlocks.EXOTIC_ICE.get(), new Item.Properties()));

    public static final RegistryObject<Item> TAR_SHELL_ITEM = ITEMS.register("tar_shell",
        () -> new BlockItem(DSBlocks.TAR_SHELL.get(), new Item.Properties()));

    public static final RegistryObject<Item> ALIEN_STONE_ITEM = ITEMS.register("alien_stone",
        () -> new BlockItem(DSBlocks.ALIEN_STONE.get(), new Item.Properties()));

    public static final RegistryObject<Item> FOSSIL_ITEM = ITEMS.register("fossil",
        () -> new BlockItem(DSBlocks.FOSSIL.get(), new Item.Properties()));

    public static final RegistryObject<Item> BREEDER_REACTOR_ITEM = ITEMS.register("breeder_reactor",
        () -> new BlockItem(DSBlocks.BREEDER_REACTOR.get(), new Item.Properties()));

    public static final RegistryObject<Item> SHIP_SCANNER_ITEM = ITEMS.register("ship_scanner",
        () -> new BlockItem(DSBlocks.SHIP_SCANNER.get(), new Item.Properties().stacksTo(64)));
}
