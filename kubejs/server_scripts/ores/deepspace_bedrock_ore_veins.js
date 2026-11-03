// DeepSpace bedrock ore veins for the deepspace:deep_space dimension.
//
// These are registered via the GTCEuServerEvents.bedrockOreVeins KubeJS event,
// which fires inside BedrockOreLoader.apply() and is the officially supported
// way to add bedrock ore veins in GTCEu Modern 7.5.x.
//
// IDs use the "deepspace:" namespace so GTCEuCompat.findOreDefinition() can look
// them up with: new ResourceLocation(DSConstants.MODID, "deepspace_" + material)
//
// REQUIRED CONFIG: config/gtceu/gtceu-common.toml must have:
//   [machines]
//     doBedrockOres = true
//
// All veins:  weight=80 (rare: 40-70), size=16, yield=60, depleted_yield=0
// Materials weights are identical to the original JSON files.

GTCEuServerEvents.bedrockOreVeins(event => {

    // ── aluminium ────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_aluminium', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Aluminium, 35)
            .material(GTMaterials.Bauxite, 30)
            .material(GTMaterials.Ilmenite, 25)
            .dimensions('deepspace:deep_space')
    })

    // ── apatite ──────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_apatite', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Apatite, 35)
            .material(GTMaterials.TricalciumPhosphate, 30)
            .dimensions('deepspace:deep_space')
    })

    // ── bastnasite ───────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_bastnasite', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Bastnasite, 35)
            .material(GTMaterials.Monazite, 30)
            .material(GTMaterials.Neodymium, 25)
            .dimensions('deepspace:deep_space')
    })

    // ── bauxite ──────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_bauxite', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Bauxite, 35)
            .material(GTMaterials.Aluminium, 25)
            .dimensions('deepspace:deep_space')
    })

    // ── beryllium ────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_beryllium', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Beryllium, 35)
            .material(GTMaterials.Emerald, 30)
            .material(GTMaterials.Thorium, 25)
            .dimensions('deepspace:deep_space')
    })

    // ── calcite ──────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_calcite', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Calcite, 35)
            .dimensions('deepspace:deep_space')
    })

    // ── cassiterite ──────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_cassiterite', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Cassiterite, 35)
            .dimensions('deepspace:deep_space')
    })

    // ── chalcopyrite ─────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_chalcopyrite', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Chalcopyrite, 35)
            .material(GTMaterials.Copper, 30)
            .material(GTMaterials.Pyrite, 25)
            .dimensions('deepspace:deep_space')
    })

    // ── chromite ─────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_chromite', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Chromite, 35)
            .material(GTMaterials.VanadiumMagnetite, 30)
            .dimensions('deepspace:deep_space')
    })

    // ── coal ─────────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_coal', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Coal, 35)
            .material(GTMaterials.Graphite, 30)
            .dimensions('deepspace:deep_space')
    })

    // ── cobaltite ────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_cobaltite', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Cobaltite, 35)
            .material(GTMaterials.Pyrite, 25)
            .dimensions('deepspace:deep_space')
    })

    // ── cooperite ────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_cooperite', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Cooperite, 35)
            .material(GTMaterials.Palladium, 30)
            .material(GTMaterials.Platinum, 25)
            .dimensions('deepspace:deep_space')
    })

    // ── copper ───────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_copper', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Copper, 35)
            .material(GTMaterials.Chalcopyrite, 30)
            .material(GTMaterials.Bornite, 25)
            .material(GTMaterials.Chalcocite, 20)
            .dimensions('deepspace:deep_space')
    })

    // ── diamond ──────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_diamond', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Diamond, 35)
            .material(GTMaterials.Graphite, 30)
            .dimensions('deepspace:deep_space')
    })

    // ── electrotine ──────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_electrotine', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Electrotine, 35)
            .material(GTMaterials.Redstone, 30)
            .material(GTMaterials.Ruby, 25)
            .dimensions('deepspace:deep_space')
    })

    // ── emerald ──────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_emerald', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Emerald, 35)
            .material(GTMaterials.Beryllium, 30)
            .dimensions('deepspace:deep_space')
    })

    // ── garnet_red ───────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_garnet_red', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.GarnetRed, 35)
            .material(GTMaterials.GarnetYellow, 30)
            .material(GTMaterials.Almandine, 25)
            .material(GTMaterials.Grossular, 20)
            .material(GTMaterials.Pyrope, 15)
            .material(GTMaterials.Spessartine, 10)
            .dimensions('deepspace:deep_space')
    })

    // ── gold ─────────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_gold', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Gold, 35)
            .material(GTMaterials.Magnetite, 30)
            .material(GTMaterials.Hematite, 25)
            .dimensions('deepspace:deep_space')
    })

    // ── iridium ──────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_iridium', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Iridium, 35)
            .material(GTMaterials.Osmium, 30)
            .material(GTMaterials.Platinum, 25)
            .dimensions('deepspace:deep_space')
    })

    // ── iron ─────────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_iron', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Iron, 35)
            .material(GTMaterials.Cobaltite, 30)
            .material(GTMaterials.Pyrite, 25)
            .material(GTMaterials.Magnetite, 20)
            .dimensions('deepspace:deep_space')
    })

    // ── kyanite ──────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_kyanite', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Kyanite, 35)
            .material(GTMaterials.Lazurite, 30)
            .material(GTMaterials.Sodalite, 25)
            .dimensions('deepspace:deep_space')
    })

    // ── lead ─────────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_lead', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Lead, 35)
            .material(GTMaterials.Galena, 30)
            .dimensions('deepspace:deep_space')
    })

    // ── lithium  (weight 70) ─────────────────────────────────────────────────
    event.add('deepspace:deepspace_lithium', vein => {
        vein.weight(70).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Lithium, 35)
            .material(GTMaterials.Spodumene, 30)
            .material(GTMaterials.Lepidolite, 20)
            .dimensions('deepspace:deep_space')
    })

    // ── manganese ────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_manganese', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Manganese, 35)
            .material(GTMaterials.Tantalite, 30)
            .material(GTMaterials.Chromite, 25)
            .dimensions('deepspace:deep_space')
    })

    // ── mica ─────────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_mica', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Mica, 35)
            .material(GTMaterials.Bentonite, 30)
            .material(GTMaterials.Zeolite, 25)
            .dimensions('deepspace:deep_space')
    })

    // ── molybdenum ───────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_molybdenum', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Molybdenum, 35)
            .material(GTMaterials.Molybdenite, 30)
            .material(GTMaterials.Powellite, 25)
            .material(GTMaterials.Wulfenite, 20)
            .dimensions('deepspace:deep_space')
    })

    // ── monazite ─────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_monazite', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Monazite, 35)
            .material(GTMaterials.Neodymium, 30)
            .material(GTMaterials.Bastnasite, 25)
            .material(GTMaterials.Thorium, 20)
            .dimensions('deepspace:deep_space')
    })

    // ── naquadah ─────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_naquadah', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Naquadah, 35)
            .dimensions('deepspace:deep_space')
    })

    // ── neodymium  (weight 60) ───────────────────────────────────────────────
    event.add('deepspace:deepspace_neodymium', vein => {
        vein.weight(60).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Neodymium, 35)
            .material(GTMaterials.Monazite, 25)
            .material(GTMaterials.Bastnasite, 20)
            .dimensions('deepspace:deep_space')
    })

    // ── nickel ───────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_nickel', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Nickel, 35)
            .material(GTMaterials.Pentlandite, 30)
            .material(GTMaterials.Garnierite, 25)
            .dimensions('deepspace:deep_space')
    })

    // ── olivine ──────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_olivine', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Olivine, 35)
            .material(GTMaterials.Calcite, 30)
            .material(GTMaterials.Magnesite, 25)
            .dimensions('deepspace:deep_space')
    })

    // ── osmium  (weight 40) ──────────────────────────────────────────────────
    event.add('deepspace:deepspace_osmium', vein => {
        vein.weight(40).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Osmium, 35)
            .material(GTMaterials.Iridium, 25)
            .dimensions('deepspace:deep_space')
    })

    // ── pitchblende ──────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_pitchblende', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Pitchblende, 35)
            .material(GTMaterials.Uraninite, 30)
            .dimensions('deepspace:deep_space')
    })

    // ── platinum ─────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_platinum', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Platinum, 35)
            .material(GTMaterials.Cooperite, 30)
            .material(GTMaterials.Palladium, 25)
            .dimensions('deepspace:deep_space')
    })

    // ── pyrochlore  (weight 60) ──────────────────────────────────────────────
    event.add('deepspace:deepspace_pyrochlore', vein => {
        vein.weight(60).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Pyrochlore, 35)
            .material(GTMaterials.Tantalite, 25)
            .dimensions('deepspace:deep_space')
    })

    // ── realgar  (weight 70) ─────────────────────────────────────────────────
    event.add('deepspace:deepspace_realgar', vein => {
        vein.weight(70).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Realgar, 35)
            .material(GTMaterials.Pyrite, 25)
            .material(GTMaterials.Sulfur, 20)
            .dimensions('deepspace:deep_space')
    })

    // ── redstone ─────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_redstone', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Redstone, 35)
            .material(GTMaterials.Ruby, 30)
            .material(GTMaterials.Cinnabar, 25)
            .dimensions('deepspace:deep_space')
    })

    // ── salt ─────────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_salt', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Salt, 35)
            .material(GTMaterials.RockSalt, 30)
            .material(GTMaterials.Lepidolite, 25)
            .material(GTMaterials.Spodumene, 20)
            .dimensions('deepspace:deep_space')
    })

    // ── scheelite ────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_scheelite', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Scheelite, 35)
            .material(GTMaterials.Tungstate, 30)
            .dimensions('deepspace:deep_space')
    })

    // ── silver ───────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_silver', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Silver, 35)
            .material(GTMaterials.Galena, 30)
            .dimensions('deepspace:deep_space')
    })

    // ── sphalerite ───────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_sphalerite', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Sphalerite, 35)
            .material(GTMaterials.Tetrahedrite, 30)
            .dimensions('deepspace:deep_space')
    })

    // ── spodumene ────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_spodumene', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Spodumene, 35)
            .material(GTMaterials.Lepidolite, 30)
            .material(GTMaterials.Pollucite, 25)
            .material(GTMaterials.Zeolite, 20)
            .dimensions('deepspace:deep_space')
    })

    // ── stibnite ─────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_stibnite', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Stibnite, 35)
            .dimensions('deepspace:deep_space')
    })

    // ── sulfur ───────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_sulfur', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Sulfur, 35)
            .material(GTMaterials.Pyrite, 30)
            .material(GTMaterials.Saltpeter, 25)
            .dimensions('deepspace:deep_space')
    })

    // ── thorium ──────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_thorium', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Thorium, 35)
            .material(GTMaterials.Uranium238, 30)
            .material(GTMaterials.Uraninite, 25)
            .dimensions('deepspace:deep_space')
    })

    // ── tin ──────────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_tin', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Tin, 35)
            .material(GTMaterials.Cassiterite, 30)
            .dimensions('deepspace:deep_space')
    })

    // ── topaz ────────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_topaz', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Topaz, 35)
            .material(GTMaterials.BlueTopaz, 30)
            .material(GTMaterials.Opal, 25)
            .material(GTMaterials.Amethyst, 20)
            .material(GTMaterials.GreenSapphire, 15)
            .material(GTMaterials.Sapphire, 10)
            .dimensions('deepspace:deep_space')
    })

    // ── tungsten ─────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_tungsten', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Tungsten, 35)
            .material(GTMaterials.Scheelite, 30)
            .material(GTMaterials.Tungstate, 25)
            .dimensions('deepspace:deep_space')
    })

    // ── uraninite ────────────────────────────────────────────────────────────
    event.add('deepspace:deepspace_uraninite', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Uraninite, 35)
            .material(GTMaterials.Pitchblende, 30)
            .dimensions('deepspace:deep_space')
    })

    // ── vanadium_magnetite ───────────────────────────────────────────────────
    event.add('deepspace:deepspace_vanadium_magnetite', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.VanadiumMagnetite, 35)
            .material(GTMaterials.Chromite, 30)
            .material(GTMaterials.Magnetite, 25)
            .dimensions('deepspace:deep_space')
    })

    // ── ilmenite ─────────────────────────────────────────────────────────────
    // Primary vein for stony_asteroid (ores[0] = gtceu:stone_ilmenite_ore).
    // Ilmenite is a titanium-iron oxide; paired with bauxite and hematite.
    event.add('deepspace:deepspace_ilmenite', vein => {
        vein.weight(80).size(16).yield(60, 60).depletedYield(0)
            .material(GTMaterials.Ilmenite, 35)
            .material(GTMaterials.Bauxite, 30)
            .material(GTMaterials.Hematite, 25)
            .material(GTMaterials.Magnetite, 20)
            .dimensions('deepspace:deep_space')
    })

})