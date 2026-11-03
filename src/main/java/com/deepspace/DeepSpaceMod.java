package com.deepspace;

import com.deepspace.commands.ExpandWandCommand;
import com.deepspace.network.DSNetwork;
import com.deepspace.commands.SpaceWhaleCommand;
import com.deepspace.commands.StructureCommand;
import com.deepspace.entity.FactionGoalInjector;
import com.deepspace.entity.MigrationManager;
import com.deepspace.entity.WhaleNaturalSpawnSystem;
import com.deepspace.entity.navigation.MobNavigationPatcher;
import com.deepspace.init.DSEntities;
import com.deepspace.loot.DSLootModifiers;
import net.minecraftforge.event.RegisterCommandsEvent;
import com.deepspace.compat.kubejs.LargeBreederReactorEnergyTracker;

import com.deepspace.blocks.BreederReactorScreen;
import com.deepspace.blocks.ShipScannerScreen;
import com.deepspace.commands.ExportShipCommand;
import com.deepspace.commands.ImportShipCommand;
import com.deepspace.commands.ScheduleContextCommand;
import com.deepspace.scheduler.ScheduleContextManager;
import com.deepspace.compat.AdAstraCompat;
import com.deepspace.compat.SpaceSuitBatterySystem;
import com.deepspace.compat.WarpDriveCompat;
import com.deepspace.compat.cctweaked.DSCCTweakedCompat;
import com.deepspace.compat.enderio.EnderIOConduitSanitizer;
import com.deepspace.crash.CrashedShipPlacer;
import com.deepspace.deathless.DeathlessDeathHandler;
import com.deepspace.deathless.DeathlessRespawnScheduler;
import com.deepspace.deathless.TraderDeathlessEnforcer;
import com.deepspace.dimension.DSCodecs;
import com.deepspace.events.CommandLoginGuard;
import com.deepspace.events.OverworldBlocker;
import com.deepspace.events.SpawnEggHandler;
import com.deepspace.events.VacuumFluidHandler;
import com.deepspace.init.DSBlocks;
import com.deepspace.init.DSItems;
import com.deepspace.init.DSMenuTypes;
import com.deepspace.registry.AsteroidRegistry;
import com.deepspace.registry.PlanetRegistry;
import com.deepspace.structures.DeepSpaceStructureRegistry;
import com.deepspace.structures.StructureRadiusMapImpl;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import com.deepspace.worldgen.ore.ForgeOreResolver;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(DeepSpaceMod.MODID)
public class DeepSpaceMod {

    public static final String MODID  = "deepspace";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static boolean CC_TWEAKED_LOADED = false;
    public static boolean WARPDRIVE_LOADED  = false;
    public static boolean ENDERIO_LOADED    = false;

    public DeepSpaceMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        DSBlocks.BLOCKS.register(modBus);
        DSBlocks.BLOCK_ENTITIES.register(modBus);
        DSItems.ITEMS.register(modBus);
        DSMenuTypes.MENU_TYPES.register(modBus);
        DSEntities.ENTITY_TYPES.register(modBus);
        DSLootModifiers.MODIFIERS.register(modBus);
        DSCodecs.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::clientSetup);
        modBus.addListener(this::addCreativeTabItems);
        // Client renderer registration handled by DSClientEvents (Dist.CLIENT class)

        IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        // Core game-event handlers
        forgeBus.register(new OverworldBlocker());          // prevent overworld/nether access
        forgeBus.register(new DeathlessDeathHandler());     // tag "deathless": capture NBT; faction traders retain loot and respawn
        forgeBus.register(new DeathlessRespawnScheduler());
        forgeBus.register(new TraderDeathlessEnforcer()); // tag "deathless": respawn 30-60s later; faction traders exactly 60s
        forgeBus.register(new VacuumFluidHandler());        // water -> ice in vacuum
        forgeBus.register(new SpawnEggHandler());           // allow spawn eggs in Deep Space
        forgeBus.register(new CrashedShipPlacer());         // pipe visual sync flush

        // Ad Astra: rocket landing redirect to personal crash site
        forgeBus.register(new AdAstraCompat());

        // Space suit battery life-support: PlayerTickEvent EU drain handler
        forgeBus.register(new SpaceSuitBatterySystem());

        // Space Whale migration (real-clock 12am/12pm trigger)
        forgeBus.register(MigrationManager.INSTANCE);

        // Space Whale natural pod spawning (>10k blocks from origin)
        forgeBus.register(new WhaleNaturalSpawnSystem());

        // Deep Space mob pathfinding: replaces navigation on the 4 specified
        // ground-mob types so they can walk, climb ladders, and hover-float
        // within oxygen zones rather than spinning endlessly.
        forgeBus.register(new MobNavigationPatcher());

        // Faction vs faction combat: injects NearestAttackableTargetGoal and
        // MeleeAttackGoal fallback into faction-tagged mobs in Deep Space.
        // Fixes: pillager targeting lunarians (and other cross-faction combat),
        // and pillager melee attack when holding non-bow weapons (nano_saber).
        forgeBus.register(new FactionGoalInjector());

                // generators etc
                forgeBus.register(new LargeBreederReactorEnergyTracker());

        // EnderIO conduit crash guard: chunk-load scan in deep_space dim that
        // detects broken conduit bundles ("no node matching original type")
        // and removes them before they crash the server during ticking.
        // Self-checks ENDERIO_LOADED at runtime so it's a no-op if the user
        // doesn't have EnderIO installed.
        forgeBus.register(new EnderIOConduitSanitizer());

        // Commands
        forgeBus.register(new ExportShipCommand());         // /exportship
        forgeBus.register(new ImportShipCommand());         // /importship
        forgeBus.register(new ScheduleContextCommand()); // /schedule_ctx
        forgeBus.register(new ScheduleContextManager()); // persistent claimant-bound scheduler
        forgeBus.register(new SpaceWhaleCommand());         // /spacewhale spawn|migrate|migrate progress
        forgeBus.register(new ExpandWandCommand());         // /expandwand
        forgeBus.register(new CommandLoginGuard());          // block all cmds for offline players until /login

        // Server tick handler for mob validation in StructureRadiusMap
        forgeBus.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            DSNetwork.register();
            AsteroidRegistry.load();
            PlanetRegistry.init();
            AdAstraCompat.register();
            CC_TWEAKED_LOADED = net.minecraftforge.fml.ModList.get().isLoaded("computercraft");
            WARPDRIVE_LOADED  = net.minecraftforge.fml.ModList.get().isLoaded("warpdrive");
            ENDERIO_LOADED    = net.minecraftforge.fml.ModList.get().isLoaded("enderio");
            if (CC_TWEAKED_LOADED) DSCCTweakedCompat.init();
            SpaceSuitBatterySystem.register();
            // WarpDrive: gate plant/sapling/tree growth behind CO₂ presence in oxygen zones.
            if (WARPDRIVE_LOADED) WarpDriveCompat.register();
            // Pre-load runtime structures from config/deepspace/structures/
            DeepSpaceStructureRegistry.getInstance().loadRuntimeStructures();
            LOGGER.info("[DeepSpace] Ready -- {} asteroids, {} zones, {} planet types, {} structures in pool.",
                AsteroidRegistry.asteroidCount(), AsteroidRegistry.zoneCount(), PlanetRegistry.count(),
                DeepSpaceStructureRegistry.getInstance().getPool().size());
        });
    }

    private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SEARCH ||
            event.getTabKey().location().toString().equals("deepspace:main")) {
            event.accept(DSItems.BREEDER_REACTOR_ITEM.get());
            event.accept(DSItems.EXOTIC_ICE_ITEM.get());
            event.accept(DSItems.TAR_SHELL_ITEM.get());
            event.accept(DSItems.ALIEN_STONE_ITEM.get());
            event.accept(DSItems.FOSSIL_ITEM.get());
            event.accept(DSItems.SHIP_SCANNER_ITEM.get());
            event.accept(DSItems.SPACE_WHALE_MEAT.get());
            event.accept(DSItems.SPACE_WHALE_STEAK.get());
            event.accept(DSItems.SPACE_WHALE_SADDLE.get());
            event.accept(DSItems.ABNORMALITY_SCANNER.get());
        }
    }

    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(DSMenuTypes.BREEDER_REACTOR_MENU.get(), BreederReactorScreen::new);
            MenuScreens.register(DSMenuTypes.SHIP_SCANNER_MENU.get(), ShipScannerScreen::new);
            LOGGER.debug("[DeepSpace] Registered BreederReactorScreen and ShipScannerScreen.");
        });
    }


    /**
     * Server tick handler: drives StructureRadiusMapImpl mob validation for the
     * Deep Space dimension (checks every 100 ticks whether mobs in structures are dead).
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        try {
            ServerLevel deepSpaceLevel = event.getServer().getLevel(DSConstants.DEEP_SPACE_KEY);
            if (deepSpaceLevel != null) {
                StructureRadiusMapImpl.INSTANCE.tickMobValidation(deepSpaceLevel);
            }
        } catch (Exception e) {
            // Non-fatal — dimension may not be loaded
        }
    }

    /**
     * Build the host-matched #forge:ores lookup. Tags are only bound at this
     * point (not at FMLCommonSetupEvent), and this fires again on /reload.
     */
    @SubscribeEvent
    public void onTagsUpdated(TagsUpdatedEvent event) {
        ForgeOreResolver.build(event.getRegistryAccess());
    }

    /**
     * Register the structure registry as a data-pack reload listener.
     * This handles /reload and initial resource manager load.
     */
    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(DeepSpaceStructureRegistry.getInstance());
        LOGGER.info("[DeepSpace] Registered DeepSpaceStructureRegistry as reload listener.");
    }
                
        /**
         * Register all Deep Space commands.
         * Runs once on server startup / when commands are registered.
         */
        @SubscribeEvent
        public void onRegisterCommands(RegisterCommandsEvent event) {
                StructureCommand.register(event.getDispatcher());
        }
}