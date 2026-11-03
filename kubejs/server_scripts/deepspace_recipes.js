// kubejs/server_scripts/deepspace_recipes.js
// Deep Space — Custom GT recipe additions
// Compatible with KubeJS 2001.6.5-build.16

ServerEvents.recipes(event => {

    // ── Remove conflicting vanilla GT recipes ────────────────────────────
    try { event.remove({ id: 'gtceu:biomass_from_cactus' }) } catch(e) {}

    if (Platform.isLoaded('expatternprovider')) {

        event.remove({ id: 'expatternprovider:cobblestone_cell' })
        event.remove({ id: 'expatternprovider:water_cell' })

    }
    // ── STAGE 1: LV Brewery — Cactus + Water → Biomass ──────────────────
    event.recipes.gtceu.brewery('deepspace:cactus_to_biomass')
        .itemInputs('1x minecraft:cactus')
        .inputFluids(Fluid.of('minecraft:water', 20))
        .outputFluids(Fluid.of('gtceu:biomass', 20))
        .duration(160)
        .EUt(8)

    // ── LV Fluid Heater — Biomass → Oxygen ───────────────────────────────
    event.recipes.gtceu.fluid_heater('deepspace:biomass_to_oxygen')
        .inputFluids(Fluid.of('gtceu:biomass', 4))
        .outputFluids(Fluid.of('gtceu:oxygen', 1000))
        .duration(80)
        .EUt(8)

    // ── LV Fluid Heater — Ice → Water ────────────────────────────────────
    event.recipes.gtceu.fluid_heater('deepspace:ice_to_water')
        .itemInputs('1x minecraft:ice')
        .outputFluids(Fluid.of('minecraft:water', 1000))
        .duration(40)
        .EUt(8)

    // ── LV Fluid Heater — Exotic Ice → Water (1,024,000 mB) ──────────────
    event.recipes.gtceu.fluid_heater('deepspace:exotic_ice_to_water')
        .itemInputs('1x deepspace:exotic_ice')
        .outputFluids(Fluid.of('minecraft:water', 8000))
        .duration(40)
        .EUt(8)

    // ── LV Macerator — Alien Stone → Carbon Dust ─────────────────────────
    event.recipes.gtceu.macerator('deepspace:alien_stone_macerate')
        .itemInputs('1x deepspace:alien_stone')
        .itemOutputs('1x gtceu:dark_ash_dust')
        .duration(120)
        .EUt(8)

    // ── LV Macerator — Tar Shell → Carbon Dust ───────────────────────────
    event.recipes.gtceu.macerator('deepspace:tar_shell_macerate')
        .itemInputs('1x deepspace:tar_shell')
        .itemOutputs('2x gtceu:carbon_dust')
        .duration(80)
        .EUt(8)

    // ── Small Breeder Reactor — ZPM Assembler recipe ─────────────────────
    // All items confirmed from GTCEu 7.5.2 lang file bytecode analysis.
    // EUt: 131072 = ZPM voltage (GTValues.V[GTValues.ZPM]).
    // Items:
    //   gtceu:zpm_machine_hull          — "ZPM Machine Hull"
    //   gtceu:zpm_electric_pump         — "ZPM Electric Pump"
    //   gtceu:uranium_ingot             — uranium fuel core
    //   gtceu:zpm_naquadria_battery     — "Medium Naquadria Battery" (ZPM-tier)
    //   gtceu:crystal_processor_computer — "Crystal Processor Supercomputer" (ZPM-tier circuit)
    //   gtceu:zpm_field_generator       — "ZPM Field Generator" (magnetic containment)
    // Fluid:
    //   gtceu:tin (1440 mB)             — standard GT solder fluid (molten tin)
    event.recipes.gtceu.assembler('deepspace:breeder_reactor_assemble')
        .itemInputs(
            '4x gtceu:zpm_machine_hull',
            '4x gtceu:zpm_electric_pump',
            '2x gtceu:uranium_ingot',
            '2x gtceu:zpm_naquadria_battery',
            '4x gtceu:crystal_processor_computer',
            '1x gtceu:zpm_field_generator'
        )
        .inputFluids(Fluid.of('gtceu:tin', 1440))
        .itemOutputs('1x deepspace:breeder_reactor')
        .duration(400)
        .EUt(131072)


    // ── SIFTER: Fossil → Biological starter resources ────────────────────
    // Fossil blocks found in dirt & ice asteroids can be sifted for
    // seeds, grass and rubber saplings.
    // chancedOutput(item, chance/10000, boost_per_tier)  →  25% = 2500
	event.recipes.gtceu.sifter('deepspace:fossil_sift')
		.itemInputs('1x deepspace:fossil')
		.chancedOutput('gtceu:rubber_sapling', 2500, 100)
		.chancedOutput('minecraft:oak_sapling', 2500, 100)
		.chancedOutput('exdeorum:grass_seeds', 2500, 100)
		.chancedOutput('minecraft:grass', 2500, 100)
		.duration(200)
		.EUt(8)
	// ── SIFTER: Sand → Organic surface resources ─────────────────────────
	event.recipes.gtceu.sifter('deepspace:sand_sift')
		.itemInputs('1x minecraft:sand')
		.chancedOutput('minecraft:bone_meal', 2500, 100)
		.chancedOutput('minecraft:sugar_cane', 2500, 100)
		.chancedOutput('minecraft:cactus', 2500, 100)
		.chancedOutput('minecraft:rotten_flesh', 2500, 100)
		.duration(160)
		.EUt(8)

	// ── SIFTER: Ice → Aquatic resources ──────────────────────────────────
	event.recipes.gtceu.sifter('deepspace:ice_sift')
		.itemInputs('1x minecraft:ice')
		.chancedOutput('minecraft:kelp', 2500, 100)
		.chancedOutput('minecraft:lily_pad', 2500, 100)
		.chancedOutput('minecraft:ink_sac', 2500, 100)
		.duration(160)
		.EUt(8)
		
    console.log('[DeepSpace] Custom recipes loaded (KubeJS 2001.6.5).')
})
