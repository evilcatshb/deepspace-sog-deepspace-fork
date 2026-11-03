package com.deepspace.crash;

import com.deepspace.DeepSpaceMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Verifies ME Drive cells loaded from the structure NBT and logs the result.
 *
 * AE2 15.4.x NBT format — confirmed from AEBaseInvBlockEntity bytecode:
 *   tag["inv"] = TAG_Compound {
 *     "item0": ItemStack_NBT,   (physical cell in slot 0)
 *     "item1": ItemStack_NBT,   (physical cell in slot 1)
 *     ...
 *   }
 *   Each ItemStack: { "id": string, "Count": byte, "tag": compound (cell data) }
 *
 * The crashed_ship.nbt already embeds this format. Structure placement loads it.
 *
 * Fallback: if for any reason the structure did not populate the cells (e.g. the
 * block entity was re-initialized after placement), re-inject bare empty cells
 * using the correct CompoundTag / "item0"/"item1" format so the drive is at
 * least usable.
 */
public final class MeDriveInitialiser {

    private MeDriveInitialiser() {}

    private static final int SCAN_RANGE = 20;

    public static void injectCells(ServerLevel level, BlockPos structureOrigin) {
        for (int dx = -1; dx <= SCAN_RANGE; dx++) {
            for (int dy = 0; dy <= 12; dy++) {
                for (int dz = -1; dz <= SCAN_RANGE; dz++) {
                    BlockPos pos = structureOrigin.offset(dx, dy, dz);
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be == null) continue;

                    String beType = net.minecraftforge.registries.ForgeRegistries.BLOCK_ENTITY_TYPES
                        .getKey(be.getType()) != null
                        ? net.minecraftforge.registries.ForgeRegistries.BLOCK_ENTITY_TYPES
                            .getKey(be.getType()).toString()
                        : "";

                    if (!beType.equals("ae2:drive")) continue;

                    CompoundTag beTag = be.saveWithoutMetadata();

                    // AE2 stores "inv" as TAG_Compound (type 10) with "item0","item1" slot keys.
                    // Confirmed from AEBaseInvBlockEntity: m_128469_ = getCompound("inv")
                    // and string concat 'item\x01' = "item" + slot_index.
                    if (beTag.contains("inv", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                        CompoundTag inv = beTag.getCompound("inv");
                        if (inv.contains("item0") || inv.contains("item1")) {
                            DeepSpaceMod.LOGGER.info(
                                "[MeDriveInitialiser] Drive at {} already has cells from structure NBT.",
                                pos);
                            return;
                        }
                    }

                    // Fallback: structure did not load cells — inject using correct AE2 format.
                    // This happens if the block entity was reset after structure placement.
                    DeepSpaceMod.LOGGER.warn(
                        "[MeDriveInitialiser] Drive at {} missing cells — injecting fallback.", pos);

                    CompoundTag inv = new CompoundTag();

                    CompoundTag cell0 = new CompoundTag();
                    cell0.putString("id", "ae2:item_storage_cell_4k");
                    cell0.putByte("Count", (byte) 1);
                    inv.put("item0", cell0);

                    CompoundTag cell1 = new CompoundTag();
                    cell1.putString("id", "ae2:fluid_storage_cell_1k");
                    cell1.putByte("Count", (byte) 1);
                    inv.put("item1", cell1);

                    beTag.put("inv", inv);
                    be.load(beTag);
                    be.setChanged();
                    level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), 3);

                    DeepSpaceMod.LOGGER.info(
                        "[MeDriveInitialiser] Injected 2 fallback cells into Drive at {}.", pos);
                    return;
                }
            }
        }
        DeepSpaceMod.LOGGER.warn("[MeDriveInitialiser] No ae2:drive found near {}.", structureOrigin);
    }
}
