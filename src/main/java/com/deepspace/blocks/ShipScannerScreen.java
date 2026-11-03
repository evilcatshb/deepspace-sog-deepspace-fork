package com.deepspace.blocks;

import com.deepspace.network.DSNetwork;
import com.deepspace.network.WarpRequestPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client-side screen for the Ship Scanner block.
 *
 * <h3>Layout (260 × 188 px)</h3>
 * <pre>
 *  ┌────────────────── SHIP SCANNER ──────────────────────┐  ← header (22 px)
 *  ├──────────────────────────────────────────────────────┤
 *  │ ● SHIP  "SS Prometheus" (Notch)          1,234m WARP │  ┐
 *  │ ● PLYR  "Notch"                            500m WARP │  │ scrollable list
 *  │ ● STRC  "mining_outpost"                 2,100m WARP │  │ (up to 8 rows)
 *  │  …                                                   │  ┘
 *  ├──────────────────────────────────────────────────────┤
 *  │ Range: 3,000m | deep_space          8 contacts       │  ← footer (14 px)
 *  └──────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h3>Interaction</h3>
 * <ul>
 *   <li>Mouse-wheel / click-drag: scrolls the entry list.</li>
 *   <li>WARP button: sends {@link WarpRequestPacket} (C→S), then closes the GUI.
 *       The server executes {@code /wd plan <dim> <x> <y> <z>} for the player.</li>
 * </ul>
 */
public class ShipScannerScreen extends AbstractContainerScreen<ShipScannerMenu> {

    // ── Layout constants ──────────────────────────────────────────────────────

    private static final int GUI_W        = 260;
    private static final int GUI_H        = 188;
    private static final int HEADER_H     = 22;
    private static final int FOOTER_H     = 14;

    /** Y of the first pixel below the header separator (list starts here). */
    private static final int LIST_TOP     = HEADER_H + 1;
    /** Y of the footer separator. */
    private static final int LIST_BOTTOM  = GUI_H - FOOTER_H - 1;
    /** Pixel height of each entry row. */
    private static final int ENTRY_H      = 19;
    /** Maximum rows visible without scrolling: floor((LIST_BOTTOM - LIST_TOP) / ENTRY_H). */
    private static final int VISIBLE_ROWS = (LIST_BOTTOM - LIST_TOP) / ENTRY_H; // = 8

    // Column X positions (GUI-relative, i.e. relative to leftPos)
    private static final int COL_DOT_X  = 5;    // 5×5 coloured dot
    private static final int COL_TYPE_X = 12;   // "SHIP" / "PLYR" / "STRC" badge
    private static final int COL_NAME_X = 42;   // name + owner substring
    private static final int COL_DIST_X = 202;  // right-edge of distance text
    private static final int COL_WARP_X = 206;  // left edge of WARP button
    private static final int WARP_W     = 48;
    private static final int WARP_H     = 12;

    // Scrollbar (right strip)
    private static final int SCROLL_X = GUI_W - 6;
    private static final int SCROLL_W = 4;

    // Name column widths
    private static final int NAME_W_WITH_OWNER = 82;  // chars budget when owner is shown
    private static final int NAME_W_NO_OWNER   = 152; // full width otherwise

    // ── Palette ───────────────────────────────────────────────────────────────

    private static final int C_BG         = 0xFF090912;  // near-black space
    private static final int C_PANEL      = 0xFF0F0F20;  // dark navy panel
    private static final int C_HEADER_BG  = 0xFF0B0B1C;
    private static final int C_BORDER     = 0xFF2B2B7A;  // dim blue border
    private static final int C_SEP        = 0xFF242460;  // separator line
    private static final int C_ENTRY_EVN  = 0xFF0C0C1E;  // even row
    private static final int C_ENTRY_ODD  = 0xFF101028;  // odd row
    private static final int C_ENTRY_HOV  = 0xFF16163A;  // hovered row
    private static final int C_DOT_SHIP   = 0xFF4488FF;  // blue  — ship
    private static final int C_DOT_PLYR   = 0xFF44CC77;  // green — player
    private static final int C_DOT_STRC   = 0xFFFFAA22;  // amber — structure
    private static final int C_TEXT       = 0xFFDDDDFF;  // primary text
    private static final int C_OWNER      = 0xFF7788AA;  // muted owner sub-text
    private static final int C_DIST       = 0xFF6699CC;  // distance value
    private static final int C_BADGE_SHIP = 0xFF4488FF;
    private static final int C_BADGE_PLYR = 0xFF44CC77;
    private static final int C_BADGE_STRC = 0xFFFFAA22;
    private static final int C_WARP_BG    = 0xFF0F2248;  // warp button resting
    private static final int C_WARP_HOV   = 0xFF1A3A72;  // warp button hovered
    private static final int C_WARP_TXT   = 0xFF88BBFF;  // warp button label
    private static final int C_WARP_BDR   = 0xFF2E55AA;  // warp button border
    private static final int C_FOOTER_BG  = 0xFF0B0B1C;
    private static final int C_FOOTER_TXT = 0xFF445577;  // dim footer info
    private static final int C_SCAN_WARN  = 0xFFFFCC44;  // "scanning…" warning
    private static final int C_EMPTY_MSG  = 0xFF3A3A66;
    private static final int C_SCROLL_TR  = 0xFF101030;  // scrollbar track
    private static final int C_SCROLL_TH  = 0xFF3A3A8A;  // scrollbar thumb

    // Faction badge colours (shown after ship name for TYPE_SHIP entries)
    private static final int C_FACTION_SCAV       = 0xFFFF8800;  // orange
    private static final int C_FACTION_LUNA        = 0xFFCCCCDD;  // silver
    private static final int C_FACTION_AUTO        = 0xFF00DDDD;  // cyan
    private static final int C_FACTION_IMAX        = 0xFF44BB44;  // green
    private static final int C_FACTION_PIRATE      = 0xFFDD3333;  // red
    private static final int C_FACTION_REPUBLIC    = 0xFF4488FF;  // blue
    private static final int C_FACTION_COLLECTIVE  = 0xFFAA44CC;  // purple
    private static final int C_FACTION_NEUTRAL     = 0xFF556677;  // grey

    // ── State ─────────────────────────────────────────────────────────────────

    private int scrollOffset = 0;

    // ── Constructor ───────────────────────────────────────────────────────────

    public ShipScannerScreen(ShipScannerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = GUI_W;
        this.imageHeight = GUI_H;
        // Suppress vanilla title / inventory-label rendering
        this.titleLabelX = -9999;
        this.inventoryLabelY = -9999;
    }

    @Override
    protected void init() {
        super.init();
        // No Minecraft Button widgets — clicks are handled manually in mouseClicked()
        // so that button positions can be computed dynamically from the scroll offset.
    }

    // ── Render pipeline ───────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }

    /**
     * Draws all background elements (panel, header, entry list, scrollbar, footer).
     * Coordinates are in screen-space (leftPos / topPos are already added).
     */
    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        final int lx = leftPos;
        final int ty = topPos;

        // ── Outer panel ───────────────────────────────────────────────────────
        g.fill(lx, ty, lx + GUI_W, ty + GUI_H, C_BG);
        g.fill(lx + 1, ty + 1, lx + GUI_W - 1, ty + GUI_H - 1, C_PANEL);
        drawRect(g, lx, ty, GUI_W, GUI_H, C_BORDER);

        // ── Header ────────────────────────────────────────────────────────────
        g.fill(lx + 1, ty + 1, lx + GUI_W - 1, ty + HEADER_H, C_HEADER_BG);
        // Decorative radar blips to the left of the title
        g.fill(lx + 7,  ty + 9,  lx + 9,  ty + 11, 0xFF1A2255);
        g.fill(lx + 10, ty + 8,  lx + 13, ty + 12, 0xFF2233AA);
        g.fill(lx + 14, ty + 7,  lx + 18, ty + 13, C_DOT_SHIP);
        // Header separator
        g.fill(lx + 1, ty + HEADER_H, lx + GUI_W - 1, ty + HEADER_H + 1, C_SEP);

        // ── Footer ────────────────────────────────────────────────────────────
        int footerTop = ty + LIST_BOTTOM + 1;
        g.fill(lx + 1, footerTop, lx + GUI_W - 1, ty + GUI_H - 1, C_FOOTER_BG);
        g.fill(lx + 1, ty + LIST_BOTTOM, lx + GUI_W - 1, ty + LIST_BOTTOM + 1, C_SEP);

        // ── Entry list ────────────────────────────────────────────────────────
        var entries    = menu.getEntries();
        int entryCount = entries.size();

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int idx = i + scrollOffset;
            int ey  = ty + LIST_TOP + i * ENTRY_H;

            if (idx >= entryCount) {
                // Empty padding row
                g.fill(lx + 2, ey, lx + SCROLL_X - 1, ey + ENTRY_H - 1, C_ENTRY_EVN);
                continue;
            }

            ShipScannerMenu.ScanEntry e = entries.get(idx);
            boolean hoverRow  = isOverEntry(mx, my, ey);
            boolean hoverWarp = isOverWarpBtn(mx, my, ey);

            // Row background
            int rowBg = hoverRow ? C_ENTRY_HOV : (i % 2 == 0 ? C_ENTRY_EVN : C_ENTRY_ODD);
            g.fill(lx + 2, ey, lx + SCROLL_X - 1, ey + ENTRY_H - 1, rowBg);

            int textY = ey + (ENTRY_H - 8) / 2;   // vertically centre 8px-tall glyphs

            // ── Coloured type dot (5×5) ───────────────────────────────────────
            int dotColor = typeDotColor(e.type());
            int dotY = ey + (ENTRY_H - 5) / 2;
            g.fill(lx + COL_DOT_X, dotY, lx + COL_DOT_X + 5, dotY + 5, dotColor);

            // ── Type badge ────────────────────────────────────────────────────
            String badge     = typeBadge(e.type());
            int    badgeColor = typeDotColor(e.type());
            g.drawString(font, badge, lx + COL_TYPE_X, textY, badgeColor, false);

            // ── Name ─────────────────────────────────────────────────────────
            boolean hasOwner = e.type() == ShipScannerMenu.TYPE_SHIP && !e.owner().isEmpty();
            int nameMaxW = hasOwner ? NAME_W_WITH_OWNER : NAME_W_NO_OWNER;
            String nameStr = truncate(e.name(), nameMaxW);
            g.drawString(font, nameStr, lx + COL_NAME_X, textY, C_TEXT, false);

            // ── Owner + faction (ships only) ────────────────────────────────────
            if (hasOwner) {
                int afterName = lx + COL_NAME_X + font.width(nameStr) + 2;
                String ownerStr = truncate("(" + e.owner() + ")", COL_DIST_X - COL_NAME_X - NAME_W_WITH_OWNER - 4);
                g.drawString(font, ownerStr, afterName, textY, C_OWNER, false);
            }
            // Faction badge: shown on the second line for ships and structures
            if (e.type() == ShipScannerMenu.TYPE_SHIP || e.type() == ShipScannerMenu.TYPE_STRUCTURE) {
                String fac = e.faction();
                if (fac != null && !fac.isEmpty() && !fac.equals("neutral")) {
                    int facY = textY + 8;
                    String facStr = "[" + fac.toUpperCase() + "]";
                    g.drawString(font, facStr, lx + COL_NAME_X, facY, factionColor(fac), false);
                }
            }

            // ── Distance ─────────────────────────────────────────────────────
            String distStr = formatDist(e.distance());
            g.drawString(font, distStr, lx + COL_DIST_X - font.width(distStr), textY, C_DIST, false);

            // ── WARP button ───────────────────────────────────────────────────
            int bx = lx + COL_WARP_X;
            int by = ey + (ENTRY_H - WARP_H) / 2;
            g.fill(bx, by, bx + WARP_W, by + WARP_H, hoverWarp ? C_WARP_HOV : C_WARP_BG);
            drawRect(g, bx, by, WARP_W, WARP_H, C_WARP_BDR);
            String warpLabel = "\u25BA WARP";
            g.drawString(font, warpLabel,
                bx + (WARP_W - font.width(warpLabel)) / 2,
                by + (WARP_H - 8) / 2,
                C_WARP_TXT, false);
        }

        // ── Scrollbar ─────────────────────────────────────────────────────────
        if (entryCount > VISIBLE_ROWS) {
            int trackH  = LIST_BOTTOM - LIST_TOP;
            int thumbH  = Math.max(14, trackH * VISIBLE_ROWS / entryCount);
            int maxScr  = entryCount - VISIBLE_ROWS;
            int thumbOff = (trackH - thumbH) * scrollOffset / maxScr;

            g.fill(lx + SCROLL_X, ty + LIST_TOP,
                   lx + SCROLL_X + SCROLL_W, ty + LIST_BOTTOM, C_SCROLL_TR);
            g.fill(lx + SCROLL_X, ty + LIST_TOP + thumbOff,
                   lx + SCROLL_X + SCROLL_W, ty + LIST_TOP + thumbOff + thumbH, C_SCROLL_TH);
        }

        // ── Empty-state message ───────────────────────────────────────────────
        if (entryCount == 0) {
            String msg = "No contacts within range";
            g.drawString(font, msg,
                lx + (GUI_W - font.width(msg)) / 2,
                ty + LIST_TOP + (LIST_BOTTOM - LIST_TOP) / 2 - 4,
                C_EMPTY_MSG, false);
        }
    }

    /**
     * Draws text labels that are rendered in GUI-space (0,0 = top-left of GUI),
     * after the pose-stack translation applied by {@code AbstractContainerScreen}.
     */
    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        // Title
        String title = "SHIP SCANNER";
        g.drawString(font, title, (imageWidth - font.width(title)) / 2, 7, 0xFF7799EE, false);

        // Footer left: scan range + short dimension name
        String rangeInfo = "Range: 3,000m  |  " + shortDim(menu.getDimension());
        g.drawString(font, rangeInfo, 4, imageHeight - FOOTER_H + 3, C_FOOTER_TXT, false);

        // Footer right: contact count OR "Scanning structures…" warning
        if (menu.isStructuresScanning()) {
            String warn = "Scanning structures\u2026";
            g.drawString(font, warn, imageWidth - font.width(warn) - 4,
                imageHeight - FOOTER_H + 3, C_SCAN_WARN, false);
        } else {
            int n      = menu.getEntries().size();
            String cnt = n + " contact" + (n == 1 ? "" : "s");
            g.drawString(font, cnt, imageWidth - font.width(cnt) - 4,
                imageHeight - FOOTER_H + 3, C_FOOTER_TXT, false);
        }
    }

    // ── Input handling ────────────────────────────────────────────────────────

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int maxScroll = Math.max(0, menu.getEntries().size() - VISIBLE_ROWS);
        scrollOffset  = (int) Math.max(0, Math.min(maxScroll, scrollOffset - delta));
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            var entries = menu.getEntries();
            for (int i = 0; i < VISIBLE_ROWS; i++) {
                int idx = i + scrollOffset;
                if (idx >= entries.size()) break;

                int ey = topPos + LIST_TOP + i * ENTRY_H;
                if (isOverWarpBtn((int) mx, (int) my, ey)) {
                    ShipScannerMenu.ScanEntry e = entries.get(idx);
                    // Send warp request then close the GUI
                    DSNetwork.CHANNEL.sendToServer(
                        new WarpRequestPacket(menu.getDimension(), e.x(), e.y(), e.z()));
                    this.onClose();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    // ── Hit-testing helpers ───────────────────────────────────────────────────

    private boolean isOverEntry(int mx, int my, int ey) {
        return mx >= leftPos + 2 && mx < leftPos + SCROLL_X - 1
            && my >= ey && my < ey + ENTRY_H - 1;
    }

    private boolean isOverWarpBtn(int mx, int my, int ey) {
        int bx = leftPos + COL_WARP_X;
        int by = ey + (ENTRY_H - WARP_H) / 2;
        return mx >= bx && mx < bx + WARP_W
            && my >= by && my < by + WARP_H;
    }

    // ── Formatting helpers ────────────────────────────────────────────────────

    private int typeDotColor(byte type) {
        return switch (type) {
            case ShipScannerMenu.TYPE_SHIP   -> C_DOT_SHIP;
            case ShipScannerMenu.TYPE_PLAYER -> C_DOT_PLYR;
            default                          -> C_DOT_STRC;
        };
    }

    private static String typeBadge(byte type) {
        return switch (type) {
            case ShipScannerMenu.TYPE_SHIP   -> "SHIP";
            case ShipScannerMenu.TYPE_PLAYER -> "PLYR";
            default                          -> "STRC";
        };
    }

    /**
     * Truncates {@code s} so that {@code font.width(result)} ≤ {@code maxWidth},
     * appending ".." if truncation occurred.
     */
    private String truncate(String s, int maxWidth) {
        if (font.width(s) <= maxWidth) return s;
        while (s.length() > 1 && font.width(s + "..") > maxWidth) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "..";
    }

    /** Formats a block distance as {@code "1,234m"} or {@code "12k"} for large values. */
    private static String formatDist(int dist) {
        if (dist >= 10_000) return String.format("%dk", dist / 1000);
        return String.format("%,dm", dist);
    }

    /** Strips the namespace prefix from a dimension ID for compact display. */
    private static String shortDim(String dim) {
        int colon = dim.indexOf(':');
        return colon >= 0 ? dim.substring(colon + 1) : dim;
    }

    /**
     * Draws a single-pixel border rectangle (unfilled).
     *
     * @param x      top-left X in screen space
     * @param y      top-left Y in screen space
     * @param w      total width including border pixels
     * @param h      total height including border pixels
     * @param color  ARGB colour
     */
    private static void drawRect(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x,         y,         x + w, y + 1,     color); // top
        g.fill(x,         y + h - 1, x + w, y + h,     color); // bottom
        g.fill(x,         y + 1,     x + 1, y + h - 1, color); // left
        g.fill(x + w - 1, y + 1,     x + w, y + h - 1, color); // right
    }

    /** Returns the display colour for a faction name, used in the faction badge. */
    private static int factionColor(String faction) {
        return switch (faction == null ? "" : faction) {
            case "scav"       -> C_FACTION_SCAV;
            case "luna"       -> C_FACTION_LUNA;
            case "auto"       -> C_FACTION_AUTO;
            case "imax"       -> C_FACTION_IMAX;
            case "pirate"     -> C_FACTION_PIRATE;
            case "republic"   -> C_FACTION_REPUBLIC;
            case "collective" -> C_FACTION_COLLECTIVE;
            default           -> C_FACTION_NEUTRAL;
        };
    }


}
