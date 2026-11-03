package com.deepspace.scheduler;

import com.deepspace.DeepSpaceMod;
import net.minecraft.commands.CommandFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Persistent server-scoped scheduler for claimant-bound function execution.
 *
 * <p>Unlike vanilla /schedule, this manager persists the claimant UUID and
 * explicitly reconstructs an elevated claimant command source at execution
 * time. It never stores live player/source objects and never resolves a
 * claimant through a nearest-player selector.</p>
 */
public final class ScheduleContextManager extends SavedData {
    private static final String DATA_NAME = "deep_space_schedule_ctx";
    private static final String ENTRIES_KEY = "entries";

    private final List<ScheduleContextEntry> entries = new ArrayList<>();

    public static ScheduleContextManager getOrCreate(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld not loaded — cannot access ScheduleContextManager");
        }
        return overworld.getDataStorage().computeIfAbsent(
                ScheduleContextManager::load,
                ScheduleContextManager::new,
                DATA_NAME
        );
    }

    /** Adds a new event, replacing only this claimant's same-function event when requested. */
    public synchronized ScheduleContextEntry schedule(ServerPlayer claimant,
                                                       ResourceLocation functionId,
                                                       int delayTicks,
                                                       boolean replace) {
        if (delayTicks < 0) {
            throw new IllegalArgumentException("delayTicks must be non-negative");
        }
        MinecraftServer server = claimant.getServer();
        long now = server.getTickCount();
        long delay = delayTicks;
        if (delay > Long.MAX_VALUE - now) {
            throw new IllegalArgumentException("schedule delay overflows server tick range");
        }
        long dueTick = now + delay;

        if (replace) {
            entries.removeIf(e -> e.hasReplaceKey(claimant.getUUID(), functionId));
        }

        ScheduleContextEntry entry = new ScheduleContextEntry(
                UUID.randomUUID(),
                claimant.getUUID(),
                functionId,
                dueTick,
                replace,
                claimant.serverLevel().dimension().location(),
                claimant.getX(), claimant.getY(), claimant.getZ(),
                claimant.getYRot(), claimant.getXRot()
        );
        entries.add(entry);
        setDirty();
        return entry;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        // The event-bus registration is a lightweight listener. Always delegate
        // to the actual persistent SavedData instance; never process the
        // separate listener instance's in-memory list.
        ScheduleContextManager.getOrCreate(server).processDue(server);
    }

    private synchronized void processDue(MinecraftServer server) {
        long currentTick = server.getTickCount();
    
        List<ScheduleContextEntry> due = new ArrayList<>();
        synchronized (this) {
            for (ScheduleContextEntry entry : entries) {
                if (entry.dueTick() <= currentTick) due.add(entry);
            }
        }
        if (due.isEmpty()) return;
        for (ScheduleContextEntry entry : due) {
            boolean removed;
            synchronized (this) {
                removed = entries.remove(entry);
                if (removed) setDirty();
            }
            if (removed) executeDue(server, entry);
        }
    }

    private void executeDue(MinecraftServer server, ScheduleContextEntry entry) {
        ServerPlayer claimant = server.getPlayerList().getPlayer(entry.claimantUuid());
        if (claimant == null) {
            DeepSpaceMod.LOGGER.info("[ScheduleCtx] Cancelled event {} for offline claimant {}: {}",
                    entry.eventId(), entry.claimantUuid(), entry.functionId());
            return;
        }
        ServerFunctionManager functions = server.getFunctions();
        java.util.Optional<CommandFunction> resolved = functions.get(entry.functionId());
        if (resolved.isEmpty()) {
            DeepSpaceMod.LOGGER.error("[ScheduleCtx] Function {} no longer exists; cancelling event {} for {}",
                    entry.functionId(), entry.eventId(), entry.claimantUuid());
            return;
        }
        try {
            CommandSourceStack source = claimant.createCommandSourceStack().withPermission(2);
            functions.execute(resolved.get(), source);
            DeepSpaceMod.LOGGER.debug("[ScheduleCtx] Executed event {}: {} for claimant {}",
                    entry.eventId(), entry.functionId(), entry.claimantUuid());
        } catch (Throwable t) {
            DeepSpaceMod.LOGGER.error("[ScheduleCtx] Delayed function {} failed for event {} claimant {}",
                    entry.functionId(), entry.eventId(), entry.claimantUuid(), t);
        }
    }

    public synchronized int size() { return entries.size(); }

    @Override
    public synchronized CompoundTag save(CompoundTag compound) {
        ListTag list = new ListTag();
        for (ScheduleContextEntry entry : entries) list.add(entry.save());
        compound.put(ENTRIES_KEY, list);
        return compound;
    }

    private static ScheduleContextManager load(CompoundTag compound) {
        ScheduleContextManager manager = new ScheduleContextManager();
        ListTag list = compound.getList(ENTRIES_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            try {
                manager.entries.add(ScheduleContextEntry.load(list.getCompound(i)));
            } catch (Exception e) {
                DeepSpaceMod.LOGGER.warn("[ScheduleCtx] Skipping corrupt persisted event #{}: {}",
                        i, e.getMessage());
            }
        }
        DeepSpaceMod.LOGGER.info("[ScheduleCtx] Loaded {} pending events", manager.entries.size());
        return manager;
    }
}
