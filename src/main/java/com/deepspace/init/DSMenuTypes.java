package com.deepspace.init;

import com.deepspace.DSConstants;
import com.deepspace.blocks.BreederReactorMenu;
import com.deepspace.blocks.ShipScannerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class DSMenuTypes {

    private DSMenuTypes() {}

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
        DeferredRegister.create(ForgeRegistries.MENU_TYPES, DSConstants.MODID);

    /** Menu for the Small Breeder Reactor GUI. */
    public static final RegistryObject<MenuType<BreederReactorMenu>> BREEDER_REACTOR_MENU =
        MENU_TYPES.register("breeder_reactor",
            () -> IForgeMenuType.create(BreederReactorMenu::networkConstructor));

    /** Menu for the Ship Scanner GUI (no item slots — carries scan results only). */
    public static final RegistryObject<MenuType<ShipScannerMenu>> SHIP_SCANNER_MENU =
        MENU_TYPES.register("ship_scanner",
            () -> IForgeMenuType.create(ShipScannerMenu::networkConstructor));
}
