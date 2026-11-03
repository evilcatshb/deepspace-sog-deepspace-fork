package com.deepspace.blocks;

import com.deepspace.init.DSMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Container menu for the Ship Scanner GUI.
 *
 * <p>Contains no item slots.  Its sole purpose is to carry the snapshot of
 * nearby scan results from the server (where the block entity lives) to the
 * client (where the screen renders them).
 *
 * <h3>Data flow</h3>
 * <ol>
 *   <li>Server calls {@link #forServer} with a {@code List<ShipScannerBlockEntity.GuiEntry>}
 *       and the current dimension string.</li>
 *   <li>The raw entries are serialised into the {@code FriendlyByteBuf} that
 *       {@code NetworkHooks.openScreen} sends to the client.</li>
 *   <li>The client reconstructs the menu via {@link #networkConstructor}.</li>
 *   <li>{@link ShipScannerScreen} reads {@link #getEntries()} to render the list.</li>
 * </ol>
 */
public class ShipScannerMenu extends AbstractContainerMenu {

    // ── Entry type constants ──────────────────────────────────────────────────

    public static final byte TYPE_SHIP      = 0;
    public static final byte TYPE_PLAYER    = 1;
    public static final byte TYPE_STRUCTURE = 2;

    // ── Scan entry (client-visible record) ────────────────────────────────────

    /**
     * A single entry in the scan list.
     *
     * @param type      One of {@link #TYPE_SHIP}, {@link #TYPE_PLAYER},
     *                  or {@link #TYPE_STRUCTURE}.
     * @param name      Display name (ship name / player name / NBT filename).
     * @param owner     Ship owner display string — non-empty only for TYPE_SHIP.
     *                  Uses {@code ShipCoreBlockEntity#getOwnerDisplayName(MinecraftServer)},
     *                  so it shows the FTBTeams team name when applicable.
     * @param x y z     World-space coordinates of the target.
     * @param distance  Euclidean distance from the scanner block, in blocks.
     */
    public record ScanEntry(byte type, String name, String owner, String faction,
                            int x, int y, int z, int distance) {}

    // ── Fields ────────────────────────────────────────────────────────────────

    private final List<ScanEntry> entries;
    private final String          dimension;
    private final boolean         structuresScanning;

    // ── Private primary constructor ───────────────────────────────────────────

    private ShipScannerMenu(int windowId, Inventory playerInv,
                            List<ScanEntry> entries,
                            String dimension,
                            boolean structuresScanning) {
        super(DSMenuTypes.SHIP_SCANNER_MENU.get(), windowId);
        this.entries            = entries;
        this.dimension          = dimension;
        this.structuresScanning = structuresScanning;
    }

    // ── Server-side factory ───────────────────────────────────────────────────

    /**
     * Called on the server thread when the player right-clicks the block.
     * Converts the raw {@code GuiEntry} list (server-internal type) into
     * {@code ScanEntry} records (shared client/server type).
     */
    public static ShipScannerMenu forServer(int windowId, Inventory playerInv,
                                            List<ShipScannerBlockEntity.GuiEntry> raw,
                                            String dimension, boolean structuresScanning) {
        List<ScanEntry> list = new ArrayList<>(raw.size());
        for (ShipScannerBlockEntity.GuiEntry e : raw) {
            list.add(new ScanEntry(
                e.type(), e.name(), e.owner(), e.faction(),
                e.x(), e.y(), e.z(), e.distance()
            ));
        }
        return new ShipScannerMenu(windowId, playerInv, list, dimension, structuresScanning);
    }

    // ── Network constructor (client-side) ─────────────────────────────────────

    /**
     * Registered with {@code IForgeMenuType.create}; called on the client
     * when the server sends the open-screen packet.
     */
    public static ShipScannerMenu networkConstructor(int windowId, Inventory playerInv,
                                                      FriendlyByteBuf buf) {
        String  dim      = buf.readUtf(256);
        boolean scanning = buf.readBoolean();
        int     count    = buf.readVarInt();

        List<ScanEntry> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(new ScanEntry(
                buf.readByte(),       // type
                buf.readUtf(256),     // name
                buf.readUtf(64),      // owner
                buf.readUtf(32),      // faction
                buf.readInt(),        // x
                buf.readInt(),        // y
                buf.readInt(),        // z
                buf.readInt()         // distance
            ));
        }
        return new ShipScannerMenu(windowId, playerInv, list, dim, scanning);
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Scan results sorted by distance ascending. */
    public List<ScanEntry> getEntries()           { return entries; }

    /** Dimension resource-location string, e.g. {@code "deepspace:deep_space"}. */
    public String          getDimension()         { return dimension; }

    /**
     * True if the async structure scan was still in-flight when the GUI was opened.
     * The screen uses this to show a "Scanning…" notice in the footer.
     */
    public boolean         isStructuresScanning() { return structuresScanning; }

    // ── Container contract ────────────────────────────────────────────────────

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) { return true; }
}
