// ============================================================
// Tough As Nails x GTCEu Modern - Water Bottle Recipes
// File location: kubejs/server_scripts/tan_gtceu_water_recipes.js
//
// Fix: .inputItems() has 5 overloads and Rhino cannot disambiguate any
// form of it (string, Item.of(), InputItem[], etc.).
// The correct KubeJS GTCEu API for item inputs is .itemInputs() which
// takes plain item strings — consistent with all other recipes in this pack.
// Similarly .itemOutputs() is used for item outputs.
// ============================================================

ServerEvents.recipes(function(event) {

  // Recipe 1: Purified Water Bottle
  // Inputs:  350 mB GTCEu Distilled Water + 1x glass bottle
  // Output:  1x toughasnails:purified_water_bottle
  // Machine: ULV Electrolyzer
  event.recipes.gtceu.electrolyzer('tan_gtceu:purified_water_bottle')
    .itemInputs('1x minecraft:glass_bottle')
    .inputFluids(Fluid.of('gtceu:distilled_water', 350))
    .itemOutputs('1x toughasnails:purified_water_bottle')
    .EUt(8)
    .duration(100)

  // Recipe 2: Dirty Water Bottle
  // Inputs:  350 mB Minecraft Water + 1x glass bottle
  // Output:  1x toughasnails:dirty_water_bottle
  // Machine: ULV Electrolyzer
  event.recipes.gtceu.electrolyzer('tan_gtceu:dirty_water_bottle')
    .itemInputs('1x minecraft:glass_bottle')
    .inputFluids(Fluid.of('minecraft:water', 350))
    .itemOutputs('1x toughasnails:dirty_water_bottle')
    .EUt(8)
    .duration(100)

})
