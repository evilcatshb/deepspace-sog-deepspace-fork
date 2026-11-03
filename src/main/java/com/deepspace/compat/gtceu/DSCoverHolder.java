package com.deepspace.compat.gtceu;

import com.deepspace.DeepSpaceMod;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.blockentity.IGregtechBlockEntity;
import com.gregtechceu.gtceu.api.sync_system.SyncDataHolder;
import com.gregtechceu.gtceu.api.sync_system.managed.ISyncManaged;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.transfer.fluid.IFluidHandlerModifiable;
import com.gregtechceu.gtceu.utils.GTUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Lightweight {@link ICoverable} implementation for DeepSpace block entities.
 *
 * <p>Mirrors WarpDrive's {@code WarpDriveCoverHolder} but lives in the deepspace mod
 * so that deepspace does NOT need warpdrive as a compile-time dependency (BUG_14).
 *
 * <p>Provides the full GTCEu cover infrastructure (per-side storage, NBT persistence,
 * tick subscriptions, placement/removal) without requiring the block entity to be a
 * GTCEu MetaMachine.
 */
public class DSCoverHolder implements ICoverable {

    private final BlockEntity blockEntity;
    private final CoverBehavior[] covers = new CoverBehavior[6];
    private final List<TickableSubscription> tickSubscriptions = new ArrayList<>();
    private long lastTickLog = -1L;

    private final SyncDataHolder syncDataHolder = new SyncDataHolder(this);
    private IGregtechBlockEntity holderAdapter;

    public DSCoverHolder(BlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    // ── ICoverable — required by GT8 ─────────────────────────────────────────
    @Override
    public SyncDataHolder getSyncDataHolder() { return syncDataHolder; }

    @Override
    public ISyncManaged getParentSyncObject() { return null; }

    @Override
    public void markAsChanged() { markDirty(); }

    @Override
    public IGregtechBlockEntity getHolder() {
        if (holderAdapter == null) {
            holderAdapter = new IGregtechBlockEntity() {
                @Override public BlockEntity self() { return blockEntity; }
                @Override public long getOffsetTimer() { return DSCoverHolder.this.getOffsetTimer(); }
                @Override public SyncDataHolder getSyncDataHolder() { return syncDataHolder; }
                @Override public ISyncManaged getParentSyncObject() { return null; }
                @Override public void markAsChanged() { DSCoverHolder.this.markDirty(); }
                @Override public void scheduleRenderUpdate() { DSCoverHolder.this.scheduleRenderUpdate(); }
                @Override public TickableSubscription subscribeServerTick(Runnable runnable) {
                    return DSCoverHolder.this.subscribeServerTick(runnable);
                }
                @Override public void unsubscribe(TickableSubscription subscription) {
                    DSCoverHolder.this.unsubscribe(subscription);
                }
                @Override public net.minecraft.nbt.CompoundTag getPersistentData() {
                    return ((net.minecraftforge.common.extensions.IForgeBlockEntity) blockEntity).getPersistentData();
                }
                @Override public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
                    return blockEntity.getCapability(cap, side);
                }
            };
        }
        return holderAdapter;
    }


    // ── ICoverable — positional ───────────────────────────────────────────────

    @Override public Level getLevel()   { return blockEntity.getLevel(); }
    public BlockPos getPos()  { return blockEntity.getBlockPos(); }

    @Override
    public long getOffsetTimer() {
        Level level = getLevel();
        if (level != null && level.getServer() != null) return level.getServer().getTickCount();
        return level != null ? level.getGameTime() : 0;
    }

    public void markDirty()    { blockEntity.setChanged(); }
    public boolean isInValid() { return blockEntity.isRemoved(); }

    @Override
    public void notifyBlockUpdate() {
        Level level = getLevel();
        if (level != null) {
            BlockPos pos = getPos();
            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 1 | 2);
        }
    }

        public void scheduleRenderUpdate() {
        Level level = getLevel();
        if (level != null && level.isClientSide) {
            BlockPos pos = getPos();
            level.setBlocksDirty(pos, level.getBlockState(pos), level.getBlockState(pos));
        }
    }

    @Override
    public void scheduleNeighborShapeUpdate() {
        Level level = getLevel();
        if (level != null) {
            BlockPos pos = getPos();
            level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock());
        }
    }

    // ── Cover storage ─────────────────────────────────────────────────────────

    @Override
    public boolean canPlaceCoverOnSide(CoverDefinition definition, Direction side) {
        return getCoverAtSide(side) == null;
    }

    /** Skip canAttach() — our blocks accept any cover unconditionally. */
    @Override
    public boolean placeCoverOnSide(Direction side, ItemStack itemStack,
                                     CoverDefinition coverDefinition,
                                     @Nullable ServerPlayer player) {
        if (!canPlaceCoverOnSide(coverDefinition, side)) return false;
        if (getCoverAtSide(side) != null) removeCover(side, player);
        CoverBehavior cover = coverDefinition.createCoverBehavior(this, side);
        // LDLib is on the classpath (ldlib-forge-1.20.1-1.0.27.b.jar), so call
        // onAttached() directly: stores attachItem (crowbar drop) + fires LDLib
        // managed sync init (required for the cover overlay to render correctly).
        cover.onAttached(itemStack, player);
        cover.onLoad();
        setCoverAtSide(cover, side);
        notifyBlockUpdate();
        markDirty();
        scheduleNeighborShapeUpdate();
        DeepSpaceMod.LOGGER.info("[DeepSpace][DSCoverHolder] cover {} attached @ {} side={}",
            cover.getClass().getSimpleName(), getPos(), side);
        return true;
    }

    @Override public double getCoverPlateThickness() { return 1.0 / 16.0; }
    @Override public Direction getFrontFacing()       { return Direction.NORTH; }
    @Override public boolean shouldRenderBackSide()   { return true; }

    @Override
    public IItemHandlerModifiable getItemHandlerCap(@Nullable Direction side, boolean useCover) {
        return null;
    }

    @Override
    public IFluidHandlerModifiable getFluidHandlerCap(@Nullable Direction side, boolean useCover) {
        return null;
    }

    @Override
    public void setCoverAtSide(@Nullable CoverBehavior cover, Direction side) {
        covers[side.ordinal()] = cover;
    }

    @Nullable
    @Override
    public CoverBehavior getCoverAtSide(Direction side) {
        return covers[side.ordinal()];
    }

    // ── Tick subscriptions ────────────────────────────────────────────────────

    @Nullable
    @Override
    public TickableSubscription subscribeServerTick(Runnable runnable) {
        TickableSubscription sub = new TickableSubscription(runnable);
        tickSubscriptions.add(sub);
        return sub;
    }

    @Override
    public void unsubscribe(@Nullable TickableSubscription subscription) {
        if (subscription != null) subscription.unsubscribe();
    }

    /** Called from the host block entity's server tick. */
    public void serverTick() {
        if (tickSubscriptions.isEmpty()) return;
        Iterator<TickableSubscription> it = tickSubscriptions.iterator();
        while (it.hasNext()) {
            TickableSubscription sub = it.next();
            if (sub.isStillSubscribed()) {
                try { sub.run(); }
                catch (Throwable t) {
                    DeepSpaceMod.LOGGER.error("[DeepSpace][DSCoverHolder] cover tick crashed @ {}", getPos(), t);
                    it.remove();
                }
            } else {
                it.remove();
            }
        }
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    public void saveToTag(CompoundTag tag) {
        CompoundTag coversTag = new CompoundTag();
        for (Direction dir : GTUtil.DIRECTIONS) {
            CoverBehavior cover = getCoverAtSide(dir);
            if (cover != null) {
                CompoundTag ct = new CompoundTag();
                ResourceLocation id = GTRegistries.COVERS.getKey(cover.coverDefinition);
                if (id != null) {
                    ct.putString("id", id.toString());
                    ct.put("item", cover.getAttachItem().serializeNBT());
                }
                coversTag.put(dir.getName(), ct);
            }
        }
        tag.put("DSCovers", coversTag);
    }

    public void loadFromTag(CompoundTag tag) {
        if (!tag.contains("DSCovers", Tag.TAG_COMPOUND)) return;
        CompoundTag coversTag = tag.getCompound("DSCovers");
        for (Direction dir : GTUtil.DIRECTIONS) {
            CompoundTag ct = coversTag.contains(dir.getName(), Tag.TAG_COMPOUND)
                    ? coversTag.getCompound(dir.getName()) : null;
            boolean present = ct != null && ct.contains("id");
            CoverBehavior existing = getCoverAtSide(dir);
            if (!present) {
                if (existing != null) { try { existing.onUnload(); } catch (Throwable ignored) {} setCoverAtSide(null, dir); }
                continue;
            }
            ResourceLocation id = new ResourceLocation(ct.getString("id"));
            ItemStack attachItem = ItemStack.of(ct.getCompound("item"));
            if (existing != null && existing.coverDefinition != null
                    && id.equals(GTRegistries.COVERS.getKey(existing.coverDefinition))) {
                refreshAttachItem(existing, attachItem);
                continue;
            }
            if (existing != null) { try { existing.onUnload(); } catch (Throwable ignored) {} }
            CoverDefinition def = GTRegistries.COVERS.get(id);
            if (def == null) { setCoverAtSide(null, dir); continue; }
            CoverBehavior cover = def.createCoverBehavior(this, dir);
            setCoverAtSide(cover, dir);
            refreshAttachItem(cover, attachItem);
        }
    }

    private static void refreshAttachItem(CoverBehavior cover, ItemStack attachItem) {
        try {
            java.lang.reflect.Field f = CoverBehavior.class.getDeclaredField("attachItem");
            f.setAccessible(true);
            ItemStack copy = attachItem.copy(); copy.setCount(1);
            f.set(cover, copy);
        } catch (ReflectiveOperationException ignored) {}
    }

    public void onLoad() {
        for (CoverBehavior cover : getCovers()) cover.onLoad();
    }

    public void onRemoved() {
        tickSubscriptions.clear();
    }

    public void onBlockBroken() {
        dropAllCovers();
        tickSubscriptions.clear();
    }
}
