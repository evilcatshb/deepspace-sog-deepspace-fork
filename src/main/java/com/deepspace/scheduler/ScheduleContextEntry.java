package com.deepspace.scheduler;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * One persistent claimant-bound delayed function invocation.
 *
 * <p>The UUID/function pair is the logical replacement key. Position, rotation,
 * and dimension are retained only as the original scheduling context for
 * diagnostics; execution always reconstructs the claimant from the UUID.</p>
 */
public final class ScheduleContextEntry {
    private final UUID eventId;
    private final UUID claimantUuid;
    private final ResourceLocation functionId;
    private final long dueTick;
    private final boolean replace;
    private final ResourceLocation scheduledDimension;
    private final double scheduledX;
    private final double scheduledY;
    private final double scheduledZ;
    private final float scheduledYaw;
    private final float scheduledPitch;

    public ScheduleContextEntry(UUID eventId, UUID claimantUuid, ResourceLocation functionId,
                                long dueTick, boolean replace,
                                ResourceLocation scheduledDimension,
                                double scheduledX, double scheduledY, double scheduledZ,
                                float scheduledYaw, float scheduledPitch) {
        this.eventId = eventId;
        this.claimantUuid = claimantUuid;
        this.functionId = functionId;
        this.dueTick = dueTick;
        this.replace = replace;
        this.scheduledDimension = scheduledDimension;
        this.scheduledX = scheduledX;
        this.scheduledY = scheduledY;
        this.scheduledZ = scheduledZ;
        this.scheduledYaw = scheduledYaw;
        this.scheduledPitch = scheduledPitch;
    }

    public UUID eventId() { return eventId; }
    public UUID claimantUuid() { return claimantUuid; }
    public ResourceLocation functionId() { return functionId; }
    public long dueTick() { return dueTick; }
    public boolean replace() { return replace; }
    public ResourceLocation scheduledDimension() { return scheduledDimension; }
    public double scheduledX() { return scheduledX; }
    public double scheduledY() { return scheduledY; }
    public double scheduledZ() { return scheduledZ; }
    public float scheduledYaw() { return scheduledYaw; }
    public float scheduledPitch() { return scheduledPitch; }

    /** Logical replacement identity required by the PAXI/raid design. */
    public boolean hasReplaceKey(UUID claimant, ResourceLocation function) {
        return claimantUuid.equals(claimant) && functionId.equals(function);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("event_id", eventId);
        tag.putUUID("claimant", claimantUuid);
        tag.putString("function", functionId.toString());
        tag.putLong("due_tick", dueTick);
        tag.putBoolean("replace", replace);
        tag.putString("scheduled_dimension", scheduledDimension.toString());
        tag.putDouble("scheduled_x", scheduledX);
        tag.putDouble("scheduled_y", scheduledY);
        tag.putDouble("scheduled_z", scheduledZ);
        tag.putFloat("scheduled_yaw", scheduledYaw);
        tag.putFloat("scheduled_pitch", scheduledPitch);
        return tag;
    }

    /**
     * Reads one persisted record. Invalid records throw and are handled by the
     * manager so one corrupt event cannot prevent the server from starting.
     */
    public static ScheduleContextEntry load(CompoundTag tag) {
        UUID eventId = tag.getUUID("event_id");
        UUID claimant = tag.getUUID("claimant");
        ResourceLocation function = new ResourceLocation(tag.getString("function"));
        ResourceLocation dimension = new ResourceLocation(tag.getString("scheduled_dimension"));
        return new ScheduleContextEntry(
                eventId,
                claimant,
                function,
                tag.getLong("due_tick"),
                tag.getBoolean("replace"),
                dimension,
                tag.getDouble("scheduled_x"),
                tag.getDouble("scheduled_y"),
                tag.getDouble("scheduled_z"),
                tag.getFloat("scheduled_yaw"),
                tag.getFloat("scheduled_pitch")
        );
    }
}
