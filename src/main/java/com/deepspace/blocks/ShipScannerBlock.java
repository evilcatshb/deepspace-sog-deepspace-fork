package com.deepspace.blocks;

import com.deepspace.init.DSBlocks;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.item.IComponentItem;
import com.gregtechceu.gtceu.api.item.component.IItemComponent;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.common.item.behavior.CoverPlaceBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Ship Scanner block — ZPM-powered CC:Tweaked peripheral.
 *
 * BUG_14: Now also handles GTCEu cover placement/removal (right-click with cover item
 * or crowbar) without requiring warpdrive as a compile-time dependency. Cover interaction
 * is wired through DSCoverHolder (deepspace's own ICoverable implementation).
 */
public class ShipScannerBlock extends HorizontalDirectionalBlock implements EntityBlock {

    /** Hard-coded scan radius for the right-click GUI (blocks). */
    private static final int GUI_SCAN_RADIUS = 3000;

    public ShipScannerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player,
                                    BlockGetter level, BlockPos pos) {
        return DSWrenchHandler.destroyProgress(player, 60);
    }

    // ── BUG_14: cover interaction (right-click with cover item or crowbar) ────

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ShipScannerBlockEntity scanner)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);

        // Crowbar: remove cover on the targeted face
        if (isCrowbar(held)) {
            var holder = scanner.getCoverHolder();
            Direction side = ICoverable.rayTraceCoverableSide(holder, player);
            if (side != null && holder.hasCover(side)) {
                holder.removeCover(side, player);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        // Cover item: place on targeted face
        CoverDefinition coverDef = getCoverDefinition(held);
        if (coverDef != null) {
            var holder = scanner.getCoverHolder();
            Direction side = ICoverable.rayTraceCoverableSide(holder, player);
            if (side != null) {
                ServerPlayer sp = player instanceof ServerPlayer s ? s : null;
                boolean placed = holder.placeCoverOnSide(side, held, coverDef, sp);
                if (placed && !player.isCreative()) held.shrink(1);
                return placed ? InteractionResult.SUCCESS : InteractionResult.PASS;
            }
        }

        // ── No special item held — check power then open the Ship Scanner GUI ──
        if (player instanceof ServerPlayer sp && level instanceof ServerLevel sl) {

            // Require at least one LV tick of EU in the buffer.
            // hasSufficientPower() consumes the EU on success, same as every Lua call.
            // Power can be supplied by a directly-attached GTCEu cable/wire OR
            // by a GTMThings Wireless Energy Cover installed on any face.
            if (!scanner.hasSufficientPower()) {
                player.sendSystemMessage(
                    Component.literal("\u00a7cError: Ship Scanner is unpowered."));
                return InteractionResult.SUCCESS;
            }

            String dim = sl.dimension().location().toString();
            List<ShipScannerBlockEntity.GuiEntry> entries =
                scanner.collectGuiEntries(sl, GUI_SCAN_RADIUS);
            boolean scanning = scanner.isStructureScanInProgress();

            NetworkHooks.openScreen(sp,
                new SimpleMenuProvider(
                    (windowId, playerInv, p) ->
                        ShipScannerMenu.forServer(windowId, playerInv, entries, dim, scanning),
                    Component.literal("Ship Scanner")
                ),
                buf -> {
                    buf.writeUtf(dim);
                    buf.writeBoolean(scanning);
                    buf.writeVarInt(entries.size());
                    for (ShipScannerBlockEntity.GuiEntry e : entries) {
                        buf.writeByte(e.type());
                        buf.writeUtf(e.name(), 256);
                        buf.writeUtf(e.owner(), 64);
                        buf.writeUtf(e.faction(), 32);
                        buf.writeInt(e.x());
                        buf.writeInt(e.y());
                        buf.writeInt(e.z());
                        buf.writeInt(e.distance());
                    }
                }
            );
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    /**
     * BUG_14: Drop installed covers when the block is broken by a player.
     * Chunk-unload goes through setRemoved() which only clears tick subscriptions
     * and never drops items — only this path (real destruction) drops them.
     */
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ShipScannerBlockEntity scanner) {
                try {
                    var holder = scanner.getCoverHolder();
                    if (holder != null) holder.onBlockBroken();
                } catch (Throwable ignored) {}
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    // ── BlockEntity wiring ────────────────────────────────────────────────────

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShipScannerBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, DSBlocks.SHIP_SCANNER_BE.get(),
            ShipScannerBlockEntity::serverTick);
    }

    // ── Cover helpers (GTCEu only — no GTMThings import) ─────────────────────

    @Nullable
    private static CoverDefinition getCoverDefinition(ItemStack stack) {
        Item item = stack.getItem();
        if (!(item instanceof IComponentItem comp)) return null;
        for (IItemComponent component : comp.getComponents()) {
            if (component instanceof CoverPlaceBehavior cpb) return cpb.coverDefinition();
        }
        return null;
    }

    private static boolean isCrowbar(ItemStack stack) {
        try {
            return GTToolType.CROWBAR.matchTags.stream().anyMatch(stack::is);
        } catch (Throwable e) { return false; }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A>
    createTickerHelper(BlockEntityType<A> type, BlockEntityType<E> expected,
                       BlockEntityTicker<? super E> ticker) {
        return expected == type ? (BlockEntityTicker<A>) ticker : null;
    }
}
