// kubejs/startup_scripts/deepspace_startup.js
// Deep Space — Startup registrations
// KubeJS 2001.6.5-build.16
//
// The creative tab is registered here; items are added by Java-side
// BuildCreativeModeTabContentsEvent in DeepSpaceMod.java so they reliably
// appear in the creative inventory search regardless of KubeJS load order.

StartupEvents.registry('creative_mode_tab', event => {
    event.create('deepspace:main')
        .displayName('Deep Space')
        .icon(() => 'deepspace:breeder_reactor')
})

global.DEEPSPACE_STARTER_ITEMS = [
    ['gtceu:uranium_ingot',        14],
    ['gtceu:steel_plate',          32],
    ['gtceu:copper_cable_1x',      64],
    ['gtceu:lv_circuit',            8],
    ['minecraft:glass',            64],
    ['ad_astra:oxygen_tank',        2],
    ['minecraft:cactus',           16],
]

console.log('[DeepSpace] Startup loaded (KubeJS 2001.6.5).')
