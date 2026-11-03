// kubejs/client_scripts/battery_hud.js
// Deep Space — GTCEu Battery HUD Overlay
//
// WHY PREVIOUS APPROACHES FAILED:
//
// Attempt 1: stack.nbt.contains() / stack.nbt.getLong()
//   FAILED — stack.nbt returns a MapJS (plain JS object). Those methods don't exist.
//
// Attempt 2: stack.nbt['Charge'] as a number
//   FAILED — MapJS.of(CompoundTag) stores raw Tag objects, not converted values.
//   nbt['Charge'] is a net.minecraft.nbt.LongTag Java object, not a JS number.
//   Arithmetic on it silently produces 0 or NaN.
//
// CORRECT APPROACH (confirmed from ElectricStats.getStoredPredicate() bytecode):
//   GTCapabilityHelper.getElectricItem(itemStack) -> IElectricItem capability
//   electricItem.getCharge()    -> Java long (Rhino auto-coerces to number for maths)
//   electricItem.getMaxCharge() -> Java long
//   Returns null if the item doesn't implement the capability — safe to null-check.
//
// POSITIONING FIX:
//   Using a single text object "Battery(s): XX%" eliminates all x-offset guesswork.
//   The percentage colour applies to the whole string (acceptable trade-off vs
//   two-object approach which kept breaking due to font-width estimation errors).

var GTCapabilityHelper = Java.loadClass('com.gregtechceu.gtceu.api.capability.GTCapabilityHelper')
var CompoundTag        = Java.loadClass('net.minecraft.nbt.CompoundTag')
var ListTag            = Java.loadClass('net.minecraft.nbt.ListTag')
var StringTag          = Java.loadClass('net.minecraft.nbt.StringTag')

// ─── Battery whitelist ────────────────────────────────────────────────────
// Items that implement the IElectricItem capability in GTCEu 7.5.2.
// GTCapabilityHelper.getElectricItem() returns null for non-battery items,
// so this whitelist is a fast early-exit to avoid the capability call overhead
// on every inventory slot every tick.

var GTCEU_BATTERY_IDS = {
    'gtceu:lv_cadmium_battery':         true,
    'gtceu:lv_lithium_battery':         true,
    'gtceu:lv_sodium_battery':          true,
    'gtceu:mv_cadmium_battery':         true,
    'gtceu:mv_lithium_battery':         true,
    'gtceu:mv_sodium_battery':          true,
    'gtceu:hv_cadmium_battery':         true,
    'gtceu:hv_lithium_battery':         true,
    'gtceu:hv_sodium_battery':          true,
    'gtceu:battery.re.lv.cadmium':      true,
    'gtceu:battery.re.lv.lithium':      true,
    'gtceu:battery.re.lv.sodium':       true,
    'gtceu:battery.re.mv.cadmium':      true,
    'gtceu:battery.re.mv.lithium':      true,
    'gtceu:battery.re.mv.sodium':       true,
    'gtceu:battery.re.hv.cadmium':      true,
    'gtceu:battery.re.hv.lithium':      true,
    'gtceu:battery.re.hv.sodium':       true,
    'gtceu:battery.su.lv.mercury':      true,
    'gtceu:battery.su.lv.sulfuricacid': true,
    'gtceu:battery.su.mv.mercury':      true,
    'gtceu:battery.su.mv.sulfuricacid': true,
    'gtceu:battery.su.hv.mercury':      true,
    'gtceu:battery.su.hv.sulfuricacid': true,
    'gtceu:ev_vanadium_battery':        true,
    'gtceu:iv_vanadium_battery':        true,
    'gtceu:luv_vanadium_battery':       true,
    'gtceu:ev_lapotronic_battery':      true,
    'gtceu:iv_lapotronic_battery':      true,
    'gtceu:luv_lapotronic_battery':     true,
    'gtceu:zpm_lapotronic_battery':     true,
    'gtceu:uv_lapotronic_battery':      true,
    'gtceu:energy.lapotronicorb':       true,
    'gtceu:energy.lapotronicorb2':      true,
    'gtceu:zpm_naquadria_battery':      true,
    'gtceu:uv_naquadria_battery':       true,
    'gtceu:uhv_ultimate_battery':       true,
    'gtceu:max_battery':                true,
    'gtceu:lv_battery_hull':            true,
    'gtceu:mv_battery_hull':            true,
    'gtceu:hv_battery_hull':            true,
    'gtceu:ev_battery_hull':            true,
    'gtceu:iv_battery_hull':            true,
    'gtceu:luv_battery_hull':           true,
    'gtceu:zpm_battery_hull':           true,
    'gtceu:uv_battery_hull':            true,
    'gtceu:empty_tier_i_battery':       true,
    'gtceu:empty_tier_ii_battery':      true,
    'gtceu:empty_tier_iii_battery':     true
}

// ─── Helpers ──────────────────────────────────────────────────────────────

function chargeColour(pct) {
    if (pct < 25)  return '#FF5555'  // red
    if (pct <= 75) return '#FFFF55'  // yellow
    return '#55FF55'                 // green
}

function buildPainterNbt(pct) {
    // Single text object — no x-offset calculation needed.
    var obj = new CompoundTag()
    obj.putString('type',   'text')
    obj.putString('draw',   'ingame')
    obj.putString('x',      '4')
    obj.putString('y',      '4')
    obj.put(      'text',   StringTag.valueOf('Battery(s): ' + pct + '%'))
    obj.putByte(  'shadow', 1)
    obj.putString('color',  chargeColour(pct))

    var entry = new CompoundTag()
    entry.put('ds_battery_hud', obj)

    var bulk = new ListTag()
    bulk.add(entry)

    var root = new CompoundTag()
    root.put('bulk', bulk)
    return root
}

// ─── Client tick ──────────────────────────────────────────────────────────

ClientEvents.tick(function(event) {
    var player = Client.player
    if (!player) return

    var totalCharge = 0
    var totalMax    = 0
    var items       = player.inventory.items

    for (var i = 0; i < items.length; i++) {
        var stack = items[i]
        if (!stack || stack.isEmpty()) continue
        if (!GTCEU_BATTERY_IDS[stack.id]) continue

        // Use the GTCEu capability API — the only reliable way to read EU.
        // stack.nbt returns raw Tag objects (not numbers), so direct NBT access fails.
        var ei = GTCapabilityHelper.getElectricItem(stack)
        if (!ei) continue

        var maxCharge = ei.getMaxCharge()
        if (!maxCharge || maxCharge <= 0) continue

        // Java longs from getCharge()/getMaxCharge() are auto-coerced to
        // JS doubles by Rhino when used in += arithmetic. Safe for % display.
        totalCharge += ei.getCharge()
        totalMax    += maxCharge
    }

    var pct = totalMax > 0 ? Math.round(totalCharge / totalMax * 100) : 0
    if (pct < 0)   pct = 0
    if (pct > 100) pct = 100

    Painter.paint(buildPainterNbt(pct))
})
