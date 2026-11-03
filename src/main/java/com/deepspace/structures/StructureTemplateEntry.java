package com.deepspace.structures;

/**
 * Metadata for a loaded structure NBT file.
 *
 * Named "StructureTemplateEntry" to avoid conflict with Minecraft's
 * {@link net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate}.
 */
public class StructureTemplateEntry {

    /** Filename of the structure NBT (e.g. "abandoned_station.nbt"). */
    public final String nbtFilename;

    /**
     * True when the structure was bundled at compile time in
     * {@code data/deepspace/structures/}.
     * False when loaded at runtime from
     * {@code config/deepspace/structures/}.
     */
    public final boolean compileTime;

    public StructureTemplateEntry(String nbtFilename, boolean compileTime) {
        this.nbtFilename = nbtFilename;
        this.compileTime = compileTime;
    }

    @Override
    public String toString() {
        return "StructureTemplateEntry{" + nbtFilename + ", compileTime=" + compileTime + '}';
    }
}
