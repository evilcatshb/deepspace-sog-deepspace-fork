// kubejs/server_scripts/deepspace_events.js
// Deep Space -- Server-side KubeJS event handlers
// Compatible with KubeJS 2001.6.5-build.16

// -- Starter chest item injection -------------------------------------------
BlockEvents.rightClicked('minecraft:chest', event => {
    if (event.level.dimension.toString() !== 'deepspace:deep_space') return
    if (!event.player) return

    const be = event.level.getBlockEntity(event.block.pos)
    if (!be) return

    const tag = be.persistentData
    if (tag.getBoolean('ds_starter_injected')) return

    const item0 = be.getItem(0)
    if (!item0 || item0.id !== 'minecraft:iron_ingot') return

    const starters = [
        { slot: 0,  id: 'gtceu:uranium_ingot',        count: 14 },
        { slot: 1,  id: 'gtceu:steel_plate',           count: 32 },
        { slot: 2,  id: 'gtceu:copper_cable_1x',       count: 64 },
        { slot: 3,  id: 'gtceu:lv_circuit',            count: 8  },
        { slot: 4,  id: 'minecraft:glass',              count: 64 },
        { slot: 5,  id: 'ad_astra:oxygen_tank',         count: 2  },
        { slot: 6,  id: 'minecraft:cactus',             count: 16 },
        { slot: 7,  id: 'gtceu:lv_macerator',          count: 1  },
        { slot: 8,  id: 'gtceu:lv_chemical_reactor',   count: 1  },
        { slot: 9,  id: 'gtceu:lv_brewery',            count: 1  },
    ]

    starters.forEach(function(entry) {
        try {
            be.setItem(entry.slot, Item.of(entry.id, entry.count))
        } catch (e) {
            console.warn('[DeepSpace] Could not inject starter item: ' + entry.id)
        }
    })

    tag.putBoolean('ds_starter_injected', true)
    be.setChanged()
})

// -- Natural mob spawning is prevented by the dimension itself --------------
// deepspace:deep_space uses minecraft:the_void as its biome, which has empty
// spawner lists for every mob category. The vanilla NaturalSpawner queries
// those lists and finds nothing to spawn. DeepSpaceChunkGenerator also
// overrides spawnOriginalMobs() as a no-op. No KubeJS guard is needed here.
// Having one incorrectly blocks /summon and spawn-egg use as well.

console.log('[DeepSpace] Server events loaded (KubeJS 2001.6.5).')
