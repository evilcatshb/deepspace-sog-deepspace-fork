package com.deepspace.init;

import com.deepspace.DSConstants;
import com.deepspace.blocks.AlienStoneBlock;
import com.deepspace.blocks.BreederReactorBlock;
import com.deepspace.blocks.BreederReactorBlockEntity;
import com.deepspace.blocks.ExoticIceBlock;
import com.deepspace.blocks.ShipScannerBlock;
import com.deepspace.blocks.ShipScannerBlockEntity;
import com.deepspace.blocks.TarShellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class DSBlocks {

    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, DSConstants.MODID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DSConstants.MODID);

    /** Exotic Ice: mid-game water breakthrough. 1 block -> 16,000 mB water via LV Heater. */
    public static final RegistryObject<Block> EXOTIC_ICE = BLOCKS.register("exotic_ice",
        () -> new ExoticIceBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.ICE)
            .strength(0.5f)
            .sound(SoundType.GLASS)
            .noOcclusion()
            .isViewBlocking((s, r, p) -> false)));

    /** Tar Shell -- dark carbonaceous crust of oil-bearing asteroids. */
    public static final RegistryObject<Block> TAR_SHELL = BLOCKS.register("tar_shell",
        () -> new TarShellBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .strength(2.0f, 6.0f)
            .sound(SoundType.BASALT)));

    /** Alien Stone -- dense crystalline outer shell of Zone 4 naquadah asteroids. */
    public static final RegistryObject<Block> ALIEN_STONE = BLOCKS.register("alien_stone",
        () -> new AlienStoneBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GREEN)
            .strength(3.0f, 8.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()));


    /**
     * Fossil -- ancient bio-mineral deposit found in dirt and ice asteroids.
     * White bone-like appearance with a spiral pattern on each face.
     * Sift in a GT Sifter to obtain biological starter resources.
     */
    public static final RegistryObject<Block> FOSSIL = BLOCKS.register("fossil",
        () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.SAND)
            .strength(1.5f, 3.0f)
            .sound(SoundType.BONE_BLOCK)
            .requiresCorrectToolForDrops()));

    /** Small Breeder Reactor -- ZPM tier, starter power. Dual FE + GT EU output. */
    public static final RegistryObject<Block> BREEDER_REACTOR = BLOCKS.register("breeder_reactor",
        () -> new BreederReactorBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_YELLOW)
            .strength(5.0f, 30.0f)
            .sound(SoundType.METAL)
            .lightLevel(s -> s.getValue(BreederReactorBlock.ACTIVE) ? 7 : 0)));

    public static final RegistryObject<BlockEntityType<BreederReactorBlockEntity>> BREEDER_REACTOR_BE =
        BLOCK_ENTITIES.register("breeder_reactor",
            () -> BlockEntityType.Builder
                .of(BreederReactorBlockEntity::new, BREEDER_REACTOR.get())
                .build(null));

    // ── Ship Scanner ─────────────────────────────────────────────────────────

    /**
     * Ship Scanner -- ZPM-powered CC:Tweaked peripheral.
     * Detects randomly placed NBT structures in deepspace:deep_space.
     * Default scan range: 2500 blocks, max: 5000 blocks.
     * No crafting recipe -- registered manually via KubeJS.
     */
    public static final RegistryObject<Block> SHIP_SCANNER = BLOCKS.register("ship_scanner",
        () -> new ShipScannerBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(3.0f, 15.0f)
            .sound(SoundType.METAL)));

    public static final RegistryObject<BlockEntityType<ShipScannerBlockEntity>> SHIP_SCANNER_BE =
        BLOCK_ENTITIES.register("ship_scanner",
            () -> BlockEntityType.Builder
                .of(ShipScannerBlockEntity::new, SHIP_SCANNER.get())
                .build(null));
}
