// kubejs/client_scripts/deepspace_client.js
// Deep Space — Client-side tooltips
// Compatible with KubeJS 2001.6.5-build.16
//
// Fix: ItemEvents.tooltip is a REGISTRATION event only — it has no .item property.
// Static tooltips (same text for all matching items) use event.add(id, [...]).
// Dynamic tooltips (need to inspect the item) use event.addAdvanced(id, fn)
// where fn receives (stack, advanced, lines) — stack is the ItemStack.

ItemEvents.tooltip(function(event) {
    event.add('deepspace:exotic_ice', [
        Text.aqua('Yields ').append(Text.white('8,000 mB').bold()).append(Text.aqua(' water')),
        Text.gray('Process in an LV Fluid Heater (Circuit 2)'),
        Text.darkGray('Found in Zone 3 Exotic Ice Asteroids')
    ])
    event.add('deepspace:tar_shell', [
        Text.gold('Carbonaceous crust — oil vein below'),
        Text.gray('Drill to extract fluid (GT Oil Drilling Rig)'),
        Text.gray('Macerate for Carbon Dust')
    ])
    event.add('deepspace:alien_stone', [
        Text.green('Dense crystalline rock from the Outer System'),
        Text.gray('Indicates nearby Naquadah deposits'),
        Text.darkGray('Zone 4 exclusive')
    ])
    event.add('deepspace:breeder_reactor', [
        Text.yellow('ZPM/UV-tier dual-output power source'),
        Text.gray('Uranium Ingot = 16A @ ZPM, 8,388,608 FE/t (24h real time)'),
		Text.gray('Uranium 235 Ingot = 16A @ UV, 33,554,432 FE/t (24h real time)'),
        Text.darkGray('Right-click to check fuel level')
    ])

    // Fossil — static tooltip (no dynamic item inspection needed)
    event.add('deepspace:fossil', [
        Text.of('§7Ancient bio-mineral deposit. Sift for seeds & grass.')
    ])
})
