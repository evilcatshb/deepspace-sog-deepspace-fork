package com.deepspace.blocks;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the Small Breeder Reactor.
 *
 * Layout (176×166 GTCEu steel background):
 *
 *   y=6           Title "Small Breeder Reactor" (centred)
 *   y=20..37      Fuel bar  (18×18 single-frame, left of the fuel slot)
 *   y=35..52      Fuel slot (uranium ingot, 16×16 inside 18×18 frame at x=79)
 *   y=40          Fuel time label (right of bar, "335h 59m" style)
 *   y=72          "Inventory" label
 *   y=84..161     Player inventory slots (3 rows + hotbar)
 *                 — rendered on a vanilla-light-gray background so they are
 *                   visually distinct from the dark steel GUI area
 *
 * Fuel bar texture (progress_bar_boiler_fuel_steel.png, 18×36):
 *   Top    18px = empty state  (drawn always as background)
 *   Bottom 18px = filled state (drawn 0..18px from bottom based on fraction)
 *   We draw exactly 18×18 px — one frame — NOT stretched to 54.
 */
public class BreederReactorScreen extends AbstractContainerScreen<BreederReactorMenu> {

    private static final ResourceLocation BG_TEXTURE =
        new ResourceLocation("gtceu", "textures/gui/base/background_steel.png");
    private static final ResourceLocation FUEL_BAR_TEXTURE =
        new ResourceLocation("gtceu", "textures/gui/progress_bar/progress_bar_boiler_fuel_steel.png");
    private static final ResourceLocation SLOT_TEXTURE =
        new ResourceLocation("gtceu", "textures/gui/base/slot.png");

    private static final int GUI_W = 176;
    private static final int GUI_H = 166;

    // Fuel bar: exactly one frame of the texture (18×18 px)
    // Positioned just to the left of the fuel slot (slot is at 79,35 → bar at 58,35)
    private static final int BAR_X = 58;  // relative to leftPos
    private static final int BAR_Y = 35;  // same Y as the fuel slot
    private static final int BAR_W = 14;  // slightly inset from the 18px texture
    private static final int BAR_H = 14;  // one frame tall (inset from 18)

    private static final int TITLE_Y    = 6;
    private static final int INV_LABEL_Y = 72;   // just above inventory rows

    // Inventory area coordinates (GUI-relative)
    private static final int INV_BG_X = 7;
    private static final int INV_BG_Y = 83;
    private static final int INV_BG_W = 162;
    private static final int INV_BG_H = 76;  // 3 rows × 18 + 2 gaps = 58, plus padding = 76
    private static final int HOT_BG_Y = 141;
    private static final int HOT_BG_H = 18;

    // Vanilla inventory background color (light gray, same as chest GUI)
    private static final int INV_BG_COLOR   = 0xFFC6C6C6;
    // Slot inner fill (slightly darker than background, same as vanilla)
    private static final int SLOT_BG_COLOR  = 0xFF8B8B8B;

    private static final long TICKS_PER_INGOT = 1_728_000L;

    public BreederReactorScreen(BreederReactorMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth      = GUI_W;
        this.imageHeight     = GUI_H;
        this.titleLabelX     = 0; // overridden in renderLabels
        this.titleLabelY     = TITLE_Y;
        this.inventoryLabelY = INV_LABEL_Y;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        // 1. GTCEu steel background (176×166)
        g.blit(BG_TEXTURE, leftPos, topPos, 0, 0, GUI_W, GUI_H);

        // 2. Vanilla-style light gray inventory background
        //    This makes the inventory area consistent with chests / furnaces
        drawInventoryBackground(g);

        // 3. Slot frame for the uranium input
        g.blit(SLOT_TEXTURE,
            leftPos + BreederReactorMenu.FUEL_SLOT_X - 1,
            topPos  + BreederReactorMenu.FUEL_SLOT_Y - 1,
            0, 0, 18, 18);

        // 4. Fuel bar (single 18×18 frame, fills bottom-to-top)
        renderFuelBar(g);
    }

    /**
     * Draws a light gray rectangle for the player inventory area, matching
     * the appearance of a vanilla chest (C6C6C6 background, slots visible).
     *
     * GuiGraphics.fill() takes ARGB.  We draw:
     *   - outer light gray panel (3 rows + gap)
     *   - outer light gray panel (hotbar)
     */
    private void drawInventoryBackground(GuiGraphics g) {
        int ax = leftPos + INV_BG_X;
        int ay = topPos  + INV_BG_Y;

        // Main 3-row inventory panel
        g.fill(ax, ay, ax + INV_BG_W, ay + INV_BG_H, INV_BG_COLOR);

        // Hotbar panel (separated by 4px gap from the main rows)
        int hx = leftPos + INV_BG_X;
        int hy = topPos  + HOT_BG_Y;
        g.fill(hx, hy, hx + INV_BG_W, hy + HOT_BG_H, INV_BG_COLOR);

        // Draw darker slot-sized cells so each slot is clearly delineated
        // Inventory rows (slots at y=84,102,120 each 18px)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = leftPos + 8 + col * 18;
                int sy = topPos  + 84 + row * 18;
                g.fill(sx, sy, sx + 16, sy + 16, SLOT_BG_COLOR);
            }
        }
        // Hotbar row (slots at y=142)
        for (int col = 0; col < 9; col++) {
            int sx = leftPos + 8 + col * 18;
            int sy = topPos  + 142;
            g.fill(sx, sy, sx + 16, sy + 16, SLOT_BG_COLOR);
        }
    }

    /**
     * Fuel bar: draws one 18×18 frame of the boiler_fuel_steel texture.
     * The texture is 18×36 — top half (v=0..18) = empty, bottom half (v=18..36) = filled.
     * We draw the empty background first, then overdraw the filled portion
     * scaled by the fraction from the BOTTOM up (like a fuel gauge).
     */
    private void renderFuelBar(GuiGraphics g) {
        int bx = leftPos + BAR_X;
        int by = topPos  + BAR_Y;

        // Empty background (top half of texture, 18×18 → drawn as BAR_W×BAR_H)
        g.blit(FUEL_BAR_TEXTURE,
            bx, by,
            0, 0,           // u, v = top-left of texture
            BAR_W, BAR_H,   // dest size (slightly inset)
            18, 36);        // full texture dimensions

        long fuel = menu.getFuelRemainingTicks();
        float frac = fuel <= 0 ? 0f : (float) Math.min(1.0, (double) fuel / TICKS_PER_INGOT);
        int fillPx = Math.round(BAR_H * frac);

        if (fillPx > 0) {
            // Filled portion comes from the BOTTOM half (v=18..36).
            // Draw `fillPx` pixels from the bottom of the bar.
            int dstY  = by + BAR_H - fillPx;
            // Source v: proportionally into the bottom half
            int srcV  = 18 + BAR_H - fillPx;

            g.blit(FUEL_BAR_TEXTURE,
                bx, dstY,
                0, srcV,
                BAR_W, fillPx,
                18, 36);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        // Title — centred
        int tx = (imageWidth - font.width(title)) / 2;
        g.drawString(font, title, tx, TITLE_Y, 0x404040, false);

        // "Inventory" label above the inventory rows
        g.drawString(font, playerInventoryTitle, 8, INV_LABEL_Y, 0x404040, false);

        // Fuel time — centred below the machine area, above the "Inventory" label
        // Slot bottom = FUEL_SLOT_Y(35) + 18 = 53, INV_LABEL_Y = 72
        // y=60 gives a clean gap below the slot and above the inventory section
        Component fuelLine = buildFuelLine();
        int fuelLineX = (imageWidth - font.width(fuelLine)) / 2; // centre horizontally
        g.drawString(font, fuelLine, fuelLineX, 60, 0x404040, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mx, int my) {
        super.renderTooltip(g, mx, my);
        int bx = leftPos + BAR_X;
        int by = topPos  + BAR_Y;
        if (mx >= bx && mx < bx + BAR_W && my >= by && my < by + BAR_H) {
            long fuel = menu.getFuelRemainingTicks();
            long effective = menu.getEffectiveFuelRemainingTicks();
            long h = effective / 20 / 3600, m = (effective / 20 / 60) % 60;
            g.renderTooltip(font,
                Component.literal(String.format("Fuel: %,d ticks (%dh %dm) @ %d%%",
                    fuel, h, m, menu.getOutputPercent())),
                mx, my);
        }
    }

    private Component buildFuelLine() {
        long fuel = menu.getFuelRemainingTicks();
        if (fuel <= 0) return Component.literal("Empty");
        int pct = menu.getOutputPercent();
        long effectiveTicks = menu.getEffectiveFuelRemainingTicks();
        long h = effectiveTicks / 20 / 3600, m = (effectiveTicks / 20 / 60) % 60;
        String timeStr = h > 0 ? (h + "h " + m + "m") : (m + "m");
        if (pct < 100) {
            return Component.literal(timeStr + " (" + pct + "%)");
        }
        return Component.literal(timeStr);
    }
}
