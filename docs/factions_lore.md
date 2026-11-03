

🌑 Scavengers  (default score 0) (visual identiy: piglins,resource pack to change the villager's skin to a piglin, wears space suits, and has worst-tier backpacks. uses copper tools. found on abandoned asteroids / space stations / wreck sites sometimes, earn reputation by selling random needed scrap items, e.g needs random tool, or 16 screws, or duct tape etc, buys food/water/oxygen tanks by deafult. quest line will start with trading, then to salvaging structures and recovering ship wreck cores, has a random demand system "12 screws, broken drill, oxygen tanks)

🧠 iMAX Group (default score 0)  (visual identity: none, hidden. high tech technology based faction, high tech endgame creative items as rewards when enough reputation has been earnt. should earn points every trade from their ftb quests shop. QUESTS: licencing, space dimension exploration / colonisation)

🧀 Lunarians (default score 0)  (visual identiy: ad astra Lunarians, friendly faction by default, points earnt by trade with ad astra lunarian race npcs)

🤖 Automata (default score 0)   (visual identity: Iron Golems, resource pack to change the villager's skin to a golem, earn reputation by supplying energy and trading circuits/components, lose reputation from killing automata npcs)

☠ Pirates (default score -25) (hostile faction by default, needs ftb quests done to get >0 points at which point the npcs will stop being hostile/aggressive to the player, location: nullsec, or if score <50 aggresive enough they might even spawn at the players base and attack them)

🛰️ Republic (default score: +10) (friendly faction by default, visual identiy: human npc (might need custom mod to add or datapack), red color, wears ad astra "Jet Suits" instead of space suits. armored/armed villagers or humans, that act as guards that will attack hostile pirates on sight. earn reputation by killing pirates, bounty contracts and maybe even escort missions)

🌱 Collective (default score: 0) (visual identity: villagers in green suits, or whatever color we can figure out how to make them always be consistantly with the most variety of professions. they are biological scientists / terraformers who love nature. earn reputation by planting crops, completing quests e.g grow farm goods on x dimension. encourages space exploration and colonisation in a eco-friendly manner. end game items that are purchasable include high tier solar panels etc)

Faction Relationship Matrix
Help Pirates +1 = Republic -1
Help Republic +1 = Pirates -1
Help Collective +1 = Automata -1
Help Automata +1 = Collective -1
Help Scavengers +1 = Pirates +1
Help Republic +1 = Lunarians +1
Help Pirates +1 = Lunarians -1
Help iMAX +1 = Republic +1

Kill Pirates -1 = Republic +1
Kill Republic -1 = Pirates +1
Kill Collective -1 = Automata +1
Kill Automata -1 = Collective +1
Kill Scavengers -1 = Pirates -1
Kill Republic -1 = Lunarians -1
Kill Pirates -1 = Lunarians +1
Kill iMAX -1 = Republic -1

Pirates hostile to: Republic
Pirates hostile to: Lunarians
Pirates hostile to: Collective
Pirates not hostile to: Scavengers
Republic hostile to: Pirates
Collective hostile to: Automata
Collective hostile to: Pirates
Automata not hostile to: Collective / flee/run away mechanic if possible instead
Automata hostile to: Pirates
Scavengers not hostile to: Pirates
 


Trade	Lunarians
Salvage	Scavengers
Combat	Republic/Pirates
Industry	Automata
Ecology	Collective
Meta Progression	iMAX

Reputation	Status
< -50 (Will spawn occasionally and attack you)
≤ -25	Hostile (on sight)
0	Neutral
25	Trusted
100	Allied
250	Elite Access

============= TEST COMMANDS ==============

each function assumes @s = player gaining/losing reputation so:
ftb quests or events should run:

/execute as @p run function factions:rep/add_scav
/execute as @p run function factions:rep/remove_pirate

Faction	Add Reputation	Remove Reputation
🌑 Scavengers	/function factions:rep/add_scav	/function factions:rep/remove_scav
🧠 iMAX Group	/function factions:rep/add_imax	/function factions:rep/remove_imax
🧀 Lunarians	/function factions:rep/add_luna	/function factions:rep/remove_luna
🤖 Automata	/function factions:rep/add_auto	/function factions:rep/remove_auto
☠ Pirates	/function factions:rep/add_pirate	/function factions:rep/remove_pirate
🛰️ Republic	/function factions:rep/add_republic	/function factions:rep/remove_republic
🌱 Collective	/function factions:rep/add_collective	/function factions:rep/remove_collective

1. check all the factions are loaded

/scoreboard objectives list

2. Give Yourself Reputation Points
/execute as @p run function factions:rep/add_scav
/execute as @p run function factions:rep/add_pirate
/execute as @p run function factions:rep/add_republic
/execute as @p run function factions:rep/add_luna

or bulk reputation points in these multiples: 8,16,32,64,128,256

/execute as @p run function factions:rep/add_luna_8
/execute as @p run function factions:rep/add_pirate_8

/execute as @p run function factions:rep/add_scav_32
/execute as @p run function factions:rep/add_auto_32

3. Check Your Current Scores
/scoreboard players get @p rep_scav
/scoreboard players get @p rep_pirate
/scoreboard players get @p rep_republic
/scoreboard players get @p rep_luna

INSULT:
/execute as @p run function factions:events/insult_call/insult_call_pirate
/execute as @p run function factions:events/insult_call/insult_call_auto
/execute as @p run function factions:events/insult_call/insult_call_republic
/execute as @p run function factions:events/insult_call/insult_call_scav
/execute as @p run function factions:events/insult_call/insult_call_luna
/execute as @p run function factions:events/insult_call/insult_call_collective

XXX: for faction reputation level up on trade - faction trader NPCs need to be villager-type entities (or wandering traders) with the faction tag set — e.g. a villager with Tags:["republic"]

> How do i summon a trader entity / villager that belongs to a faction with a command

List of entity factions tags:
scav
luna
auto
imax
pirate
republic
collective

List of entity factions tags/ui name mappings:
scav=Scavenger
luna=Lunarian
auto=Automata
imax=iMAX
pirate=Pirate
republic=Republic
collective=Collective


List of Talker tags:

scav_talker
scav_talker_traders
luna_talker
luna_talker_traders
auto_talker
auto_talker_traders
pirate_talker
pirate_talker_traders
republic_talker
republic_talker_traders
collective_talker
collective_talker_traders

# get entity data

/data get entity @e[type=!player,type=!mts:builder_existing,type=!mts:builder_rendering,sort=nearest,limit=1]

DEATHLESS / RESPAWN ENTITY 30-60s AFTER DEATH 

MAKE DEATHLESS NEAREST ENTITY WITH TAGS
/tag @e[type=!minecraft:item,tag=collective,sort=nearest,limit=1] add deathless

MAKE DEATHLESS ALL PIRATES WITHIN RADIUS OF PLAYERS
/tag @e[tag=collective,tag=!deathless,distance=..64] add deathless

MAKE ALL minecraft:pig in radius deathless
/tag @e[type=minecraft:pig,distance=..50] add deathless

REMOVE DEATHLESS NEAREST ENTITY WITH TAGS
/tag @e[tag=deathless,nbt={DeathTime:0s},sort=nearest,limit=1] remove deathless
/kill @e[type=ad_astra:lunarian,sort=nearest,limit=1]

GET TAGS OF ITEM IN HAND / GET ITEM NBT DATA

/data get entity @s SelectedItem

GET TAGS OF NEAREST ENTITY
/execute at @p run data get entity @e[type=!minecraft:player,distance=..10,sort=nearest,limit=1] Tags



RELOCATE/MOVE/TELEPORT ENTITY MOVE TO PLAYERS EXACT LOCATION/POSITON WITH ROTATION
/execute as @e[tag=trader,limit=1,sort=nearest] at @p run tp @s ~ ~ ~


CHANGE ENTITY FACING DIRECTION NORTH SOUTH EAST WEST
# South (default)
/data merge entity @e[tag=trader,limit=1,sort=nearest] {Rotation:[0f,0f]}
# West
/data merge entity @e[tag=trader,limit=1,sort=nearest] {Rotation:[90f,0f]}
# North
/data merge entity @e[tag=trader,limit=1,sort=nearest] {Rotation:[180f,0f]}
# East
/data merge entity @e[tag=trader,limit=1,sort=nearest] {Rotation:[-90f,0f]}


Remove/Disable NOAI/enable movement / navigation
DISABLE MOVEMENT: 
/data merge entity @e[tag=auto,limit=1,sort=nearest] {NoAI:1b}

ENABLE MOVEMENT:  
/data remove entity @e[tag=republic,limit=1,sort=nearest] NoAI

SET MAX HEALTH FOR MOB/ENTITY
/data merge entity @e[tag=luna,limit=1,sort=nearest] {Attributes:[{Name:"minecraft:generic.max_health",Base:40.0}],Health:40.0f}

ENABLE OXYGENLESS/SURVIVE IN SPACE:
/data modify entity @e[tag=luna,limit=1,sort=nearest] Tags append value "lives_without_oxygen"
/data modify entity @e[tag=luna,limit=1,sort=nearest] Tags append value "can_survive_extreme_cold"
/data modify entity @e[tag=luna,limit=1,sort=nearest] Tags append value "can_survive_extreme_heat"
/data modify entity @e[tag=luna,limit=1,sort=nearest] Tags append value "can_survive_in_space"

SUMMON/SPAWN A CAT THAT CAN LIVE IN SPACE
/summon minecraft:cat ~ ~ ~ {Tags:["lives_without_oxygen","can_survive_extreme_cold","can_survive_extreme_heat","can_survive_in_space","deathless"]}


DISABLE OXYGENLESS/TAKE DAMAGE IN SPACE
/tag @e[tag=luna,limit=1,sort=nearest] remove lives_without_oxygen
/tag @e[tag=luna,limit=1,sort=nearest] remove can_survive_extreme_cold
/tag @e[tag=luna,limit=1,sort=nearest] remove can_survive_extreme_heat
/tag @e[tag=luna,limit=1,sort=nearest] remove can_survive_in_space

HEAL ALL PIRATES IN RADIUS HEALTH TO FULL
/execute as @e[tag=pirate,distance=..16] run data merge entity @s {Health:24.0f}

REMOVE TRADER ITEM BY LAST ITEM IN TRADE GUI 
/data remove entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1]

CHANGE ENTITY GRAVITY ENABLED/DISABLED
CHECK:
/execute at @p run data get entity @e[tag=luna,limit=1,sort=nearest] NoGravity
ENABLE (walks):
/data merge entity @e[tag=luna,limit=1,sort=nearest] {NoGravity:0b}
DISABLE (floats):
/data merge entity @e[tag=luna,limit=1,sort=nearest] {NoGravity:1b}
Enable gravity for all pirate entities near player:
/execute as @e[tag=republic,distance=..16] at @s run data merge entity @s {NoGravity:0b}

HOSTILITY/VIOLENCE WHITELIST

/execute as @e[tag=pirate,limit=1,sort=nearest] run function factions:hostility/whitelist/add

Scavengers (zombified_piglin)
/execute as @p run function factions:spawn/scav
or
/execute as @p run function factions:spawn/scav_trader
then
/data modify entity @e[tag=scav,limit=1,sort=nearest] CustomName set value '{"text":"[Scavenger] Greg","color":"gray","bold":true,"italic":false}'
/data remove entity @e[tag=scav,limit=1,sort=nearest] NoAI


/data modify entity @e[tag=trader,limit=1,sort=nearest]  ForgeData.UniversalTrader.Offers set value {Recipes:[]}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:4b},sell:{id:"gtmutils:doge_coin",Count:1b},maxUses:999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:4b},maxUses:999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"kubejs:scavenger_scrap",Count:3b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"ad_astra:gas_tank",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BotariumData set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BotariumData.StoredFluids set value []
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BotariumData.StoredFluids append value {Fluid:"ad_astra:oxygen",Amount:1000L}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:2b},sell:{id:"minecraft:bread",Count:6b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:2b},sell:{id:"toughasnails:dirty_water_bottle",Count:6b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:5b},sell:{id:"gtceu:copper_single_wire",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:6b},sell:{id:"gtceu:fine_copper_wire",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:6b},sell:{id:"gtceu:tin_single_cable",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:5b},sell:{id:"gtceu:rubber_plate",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:20b},sell:{id:"gtceu:basic_electronic_circuit",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:12b},sell:{id:"gtceu:lv_machine_casing",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:18b},sell:{id:"gtceu:lv_electric_motor",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:18b},sell:{id:"gtceu:lv_electric_pump",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:18b},sell:{id:"gtceu:lv_conveyor_module",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:18b},sell:{id:"gtceu:transistor",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:12b},sell:{id:"gtceu:resistor",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:24b},sell:{id:"gtceu:vacuum_tube",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:3b},sell:{id:"gtceu:sticky_resin",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:4b},sell:{id:"gtceu:wood_plate",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:12b},sell:{id:"gtceu:steel_plate",Count:6b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:8b},sell:{id:"gtceu:wrought_iron_plate",Count:6b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:10b},sell:{id:"gtceu:iron_wrench",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:6b},sell:{id:"gtceu:iron_screwdriver",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:8b},sell:{id:"gtceu:iron_wire_cutter",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:5b},sell:{id:"gtceu:iron_file",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:7b},sell:{id:"gtceu:iron_hammer",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:6b},sell:{id:"gtceu:rubber_plunger",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:12b},sell:{id:"gtceu:terminal",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:14b},sell:{id:"gtceu:aluminium_normal_fluid_pipe",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:22b},sell:{id:"gtceu:cupronickel_normal_item_pipe",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:22b},sell:{id:"gtceu:cupronickel_normal_item_pipe",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:20b},sell:{id:"warpdrive:ship_hullfoam",Count:24b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:10b},sell:{id:"warpdrive:ship_hullfoam_slab",Count:24b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:12b},sell:{id:"warpdrive:ship_hullfoam_stairs",Count:24b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:32b},sell:{id:"warpdrive:ship_thruster_t1",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:64b},sell:{id:"warpdrive:ship_mining_laser",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:48b},sell:{id:"warpdrive:ship_salvage_laser",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:24b},sell:{id:"warpdrive:ship_laser_targeter",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:64b},sell:{id:"gtceu:rocket_fuel_bucket",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:64b},sell:{id:"deepspace:abnormality_scanner",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:6b},sell:{id:"gtceu:gray_dye_spray_can",Count:6b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:6b},sell:{id:"ae2:fluix_glass_cable",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:10b},sell:{id:"ae2:fluix_covered_dense_cable",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:18b},sell:{id:"ae2:terminal",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:45b},sell:{id:"ae2:item_storage_cell_16k",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:45b},sell:{id:"ae2:fluid_storage_cell_16k",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:4b},sell:{id:"ae2:fluix_dust",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:4b},sell:{id:"ae2:fluix_crystal",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:10b},sell:{id:"ae2:logic_processor",Count:2b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:12b},sell:{id:"ae2:engineering_processor",Count:2b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:14b},sell:{id:"ae2:calculation_processor",Count:2b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:16b},sell:{id:"modularrouters:modular_router",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:6b},sell:{id:"modularrouters:breaker_module",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:6b},sell:{id:"modularrouters:placer_module",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:10b},sell:{id:"modularrouters:vacuum_module",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:8b},sell:{id:"modularrouters:puller_module_1",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:4b},sell:{id:"modularrouters:speed_upgrade",Count:2b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:3b},sell:{id:"fluxnetworks:flux_dust",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:12b},sell:{id:"fluxnetworks:flux_plug",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:20b},sell:{id:"fluxnetworks:flux_controller",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:10b},sell:{id:"fluxnetworks:flux_configurator",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:5b},sell:{id:"farmersdelight:iron_knife",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:10b},sell:{id:"farmersdelight:cooking_pot",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:8b},sell:{id:"botanypots:terracotta_botany_pot",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:10b},sell:{id:"bonsaitrees3:bonsaipot",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:1b},sell:{id:"minecraft:dirt",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:6b},sell:{id:"minecraft:iron_axe",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:5b},sell:{id:"minecraft:shears",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:10b},sell:{id:"minecraft:hopper",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:14b},sell:{id:"computercraft:monitor_advanced",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:20b},sell:{id:"computercraft:computer_advanced",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:10b},sell:{id:"computercraft:pocket_computer_advanced",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:24b},sell:{id:"computercraft:turtle_advanced",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:8b},sell:{id:"computercraft:disk_drive",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:10b},sell:{id:"computercraft:wireless_modem_advanced",Count:2b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:12b},sell:{id:"advancedperipherals:chat_box",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:14b},sell:{id:"advancedperipherals:player_detector",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:18b},sell:{id:"advancedperipherals:me_bridge",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:16b},sell:{id:"advancedperipherals:inventory_manager",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:14b},sell:{id:"sophisticatedbackpacks:backpack",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:10b},sell:{id:"sophisticatedbackpacks:stack_upgrade_tier_1",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:8b},sell:{id:"sophisticatedbackpacks:pickup_upgrade",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:12b},sell:{id:"sophisticatedbackpacks:magnet_upgrade",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:12b},sell:{id:"sophisticatedbackpacks:smelting_upgrade",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:16b},sell:{id:"sophisticatedbackpacks:feeding_upgrade",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:16b},sell:{id:"drinkupgrade:drink_upgrade",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:12b},sell:{id:"sophisticatedbackpacks:compacting_upgrade",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:40b},sell:{id:"solarflux:sp_1",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:12b},sell:{id:"gtceu:hp_steam_solid_boiler",Count:1b},maxUses:9999999}


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"kubejs:scavenger_scrap",Count:10b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"gtceu:hv_lithium_battery",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag set value {Charge:1800000L}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:18b},sell:{id:"gtceu:hv_battery_buffer_16x",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:6b},sell:{id:"minecraft:iron_pickaxe",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:4b},sell:{id:"minecraft:iron_shovel",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:28b},sell:{id:"ad_astra:oxygen_loader",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:24b},sell:{id:"ad_astra:oxygen_distributor",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:2b},sell:{id:"minecraft:glass",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:3b},sell:{id:"minecraft:iron_bars",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:3b},sell:{id:"ad_astra:iron_plateblock",Count:32b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:2b},sell:{id:"ad_astra:iron_plating",Count:32b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:3b},sell:{id:"ad_astra:iron_plating_stairs",Count:32b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:1b},sell:{id:"ad_astra:iron_plating_slab",Count:32b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:1b},sell:{id:"ad_astra:iron_plating_slab",Count:32b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:2b},sell:{id:"minecraft:soul_torch",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:4b},sell:{id:"minecraft:soul_campfire",Count:2b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:20b},sell:{id:"trashcans:liquid_trash_can",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:16b},sell:{id:"trashcans:item_trash_can",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:2b},sell:{id:"enderio:fluid_tank",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:2b},sell:{id:"minecraft:chest",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:2b},sell:{id:"minecraft:barrel",Count:4b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:2b},sell:{id:"functionalstorage:oak_4",Count:1b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:28b},sell:{id:"functionalstorage:storage_controller",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:10b},sell:{id:"functionalstorage:linking_tool",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:12b},sell:{id:"minecraft:name_tag",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:4b},sell:{id:"minecraft:compass",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:6b},sell:{id:"minecraft:spyglass",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:20b},sell:{id:"torchmaster:feral_flare_lantern",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:12b},sell:{id:"elevatorid:elevator_gray",Count:4b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"kubejs:scavenger_scrap",Count:48b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"gtceu:prospector.hv",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag set value {Charge:1600000L,Mode:1}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:64b},sell:{id:"gtceu:uranium_ingot",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:4b},sell:{id:"gtceu:rubber_ring",Count:8b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:8b},sell:{id:"gtceu:small_steel_gear",Count:2b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:4b},sell:{id:"gtceu:small_iron_gear",Count:2b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:2b},sell:{id:"gtceu:iron_rod",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:4b},sell:{id:"gtceu:steel_screw",Count:64b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:2b},sell:{id:"gtceu:iron_screw",Count:64b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:3b},sell:{id:"gtceu:copper_rod",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:3b},sell:{id:"gtceu:tin_rod",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:6b},sell:{id:"gtceu:copper_rotor",Count:2b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:6b},sell:{id:"gtceu:steel_spring",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:4b},sell:{id:"gtceu:iron_spring",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:14b},sell:{id:"gtceu:lv_sensor",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:16b},sell:{id:"gtceu:lv_emitter",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:4b},sell:{id:"minecraft:map",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:6b},sell:{id:"minecraft:clock",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:4b},sell:{id:"minecraft:lead",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:4b},sell:{id:"minecraft:chain",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:4b},sell:{id:"minecraft:scaffolding",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:2b},sell:{id:"minecraft:tripwire_hook",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:6b},sell:{id:"minecraft:lightning_rod",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:24b},sell:{id:"exdeorum:porcelain_crucible",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:2b},sell:{id:"minecraft:string",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:1b},sell:{id:"minecraft:glass_bottle",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:3b},sell:{id:"minecraft:bucket",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:2b},sell:{id:"minecraft:leather",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:1b},sell:{id:"minecraft:paper",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:3b},sell:{id:"minecraft:book",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:6b},sell:{id:"minecraft:amethyst_shard",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:12b},sell:{id:"minecraft:echo_shard",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:4b},sell:{id:"minecraft:leather_horse_armor",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:8b},sell:{id:"minecraft:chainmail_boots",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:10b},sell:{id:"minecraft:music_disc_chirp",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:14b},sell:{id:"minecraft:jukebox",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:16b},sell:{id:"ad_astra:radio",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:12b},sell:{id:"ad_astra:oxygen_gear",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:20b},sell:{id:"minecraft:dragon_breath",Count:1b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:18b},sell:{id:"industrialforegoing:plant_gatherer",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:20b},sell:{id:"industrialforegoing:animal_rancher",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:30b},sell:{id:"industrialforegoing:mob_slaughter_factory",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:12b},sell:{id:"industrialforegoing:sewage_composter",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:6b},sell:{id:"exdeorum:compressed_dust",Count:64b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:2b},sell:{id:"minecraft:clay_ball",Count:24b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:2b},sell:{id:"ae2:certus_quartz_dust",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:scavenger_scrap",Count:4b},sell:{id:"ae2:ender_dust",Count:8b},maxUses:9999999}




== BUY START ==


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:bread",Count:8b},sell:{id:"kubejs:scavenger_scrap",Count:2b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:cooked_beef",Count:8b},sell:{id:"kubejs:scavenger_scrap",Count:3b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:cooked_chicken",Count:8b},sell:{id:"kubejs:scavenger_scrap",Count:3b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:cooked_porkchop",Count:8b},sell:{id:"kubejs:scavenger_scrap",Count:3b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:cooked_mutton",Count:8b},sell:{id:"kubejs:scavenger_scrap",Count:3b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:cooked_rabbit",Count:8b},sell:{id:"kubejs:scavenger_scrap",Count:3b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:baked_potato",Count:8b},sell:{id:"kubejs:scavenger_scrap",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:beetroot_soup",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:3b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:mushroom_stew",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:3b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:rabbit_stew",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:cake",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:3b},maxUses:9999999}


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:bone_broth",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:chicken_soup",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:6b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:beef_stew",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:6b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:pasta_with_meatballs",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:bacon_and_eggs",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:6b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:steak_and_potatoes",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:hamburger",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:6b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:hot_cocoa",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:apple_pie",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:6b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:cake_slice",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:roasted_mutton_chops",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:6b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:stuffed_potato",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:roast_chicken",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:stuffed_pumpkin",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"brewinandchewin:vegetable_omelet",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:vegetable_soup",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:6b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:vegetable_noodles",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:pasta_with_mutton_chop",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:6b},maxUses:9999999}


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:pasta_with_meatballs",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:6b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"brewinandchewin:cheesy_pasta",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:5b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"brewinandchewin:horror_lasagna",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:10b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"brewinandchewin:pizza",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:6b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"brewinandchewin:pizza_slice",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:2b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"brewinandchewin:quiche",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:6b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"brewinandchewin:quiche_slice",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:2b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:fish_stew",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:baked_cod_stew",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"brewinandchewin:fiery_fondue_pot",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"brewinandchewin:fiery_fondue",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:5b},maxUses:9999999}


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"ad_astra:cheese",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:3b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"brewinandchewin:flaxen_cheese_wheel",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:5b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"brewinandchewin:scarlet_cheese_wheel",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:6b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"brewinandchewin:cocoa_fudge",Count:4b},sell:{id:"kubejs:scavenger_scrap",Count:3b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:chocolate_pie",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:chocolate_pie_slice",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:chocolate_coin",Count:8b},sell:{id:"kubejs:scavenger_scrap",Count:2b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:cookie",Count:16b},sell:{id:"kubejs:scavenger_scrap",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:sweet_berry_cookie",Count:8b},sell:{id:"kubejs:scavenger_scrap",Count:2b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:honey_cookie",Count:8b},sell:{id:"kubejs:scavenger_scrap",Count:2b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"brewinandchewin:jerky",Count:4b},sell:{id:"kubejs:scavenger_scrap",Count:3b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"brewinandchewin:beer",Count:8b},sell:{id:"kubejs:scavenger_scrap",Count:2b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:apple_cider",Count:4b},sell:{id:"kubejs:scavenger_scrap",Count:3b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"brewinandchewin:mead",Count:4b},sell:{id:"kubejs:scavenger_scrap",Count:3b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"brewinandchewin:rice_wine",Count:4b},sell:{id:"kubejs:scavenger_scrap",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"brewinandchewin:vodka",Count:4b},sell:{id:"kubejs:scavenger_scrap",Count:5b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"brewinandchewin:bloody_mary",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:6b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:hot_cocoa",Count:4b},sell:{id:"kubejs:scavenger_scrap",Count:3b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"toughasnails:apple_juice",Count:8b},sell:{id:"kubejs:scavenger_scrap",Count:2b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"toughasnails:cactus_juice",Count:8b},sell:{id:"kubejs:scavenger_scrap",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"toughasnails:chorus_fruit_juice",Count:4b},sell:{id:"kubejs:scavenger_scrap",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"toughasnails:glow_berry_juice",Count:8b},sell:{id:"kubejs:scavenger_scrap",Count:3b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"toughasnails:melon_juice",Count:8b},sell:{id:"kubejs:scavenger_scrap",Count:2b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"toughasnails:pumpkin_juice",Count:8b},sell:{id:"kubejs:scavenger_scrap",Count:2b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"toughasnails:sweet_berry_juice",Count:8b},sell:{id:"kubejs:scavenger_scrap",Count:2b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:melon_juice",Count:8b},sell:{id:"kubejs:scavenger_scrap",Count:2b},maxUses:9999999}



/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"toughasnails:purified_water_bottle",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:1b},maxUses:9999999}



/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtceu:salt_dust",Count:32b},sell:{id:"kubejs:scavenger_scrap",Count:2b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtceu:rock_salt_dust",Count:32b},sell:{id:"kubejs:scavenger_scrap",Count:2b},maxUses:9999999}


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:sugar",Count:32b},sell:{id:"kubejs:scavenger_scrap",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtceu:sugar_gem",Count:16b},sell:{id:"kubejs:scavenger_scrap",Count:2b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtceu:wheat_dust",Count:16b},sell:{id:"kubejs:scavenger_scrap",Count:2b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:milk_bucket",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:3b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtceu:gelatin_dust",Count:16b},sell:{id:"kubejs:scavenger_scrap",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtceu:sodium_bicarbonate_dust",Count:16b},sell:{id:"kubejs:scavenger_scrap",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:rice",Count:16b},sell:{id:"kubejs:scavenger_scrap",Count:1b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:egg",Count:12b},sell:{id:"kubejs:scavenger_scrap",Count:1b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:honey_bottle",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:3b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:raw_pasta",Count:48b},sell:{id:"kubejs:scavenger_scrap",Count:4b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:pie_crust",Count:4b},sell:{id:"kubejs:scavenger_scrap",Count:2b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:tomato_sauce",Count:64b},sell:{id:"kubejs:scavenger_scrap",Count:3b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:milk_bottle",Count:6b},sell:{id:"kubejs:scavenger_scrap",Count:4b},maxUses:9999999}


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:tomato_crate",Count:8b},sell:{id:"kubejs:scavenger_scrap",Count:12b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:carrot_crate",Count:8b},sell:{id:"kubejs:scavenger_scrap",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:potato_crate",Count:8b},sell:{id:"kubejs:scavenger_scrap",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:beetroot_crate",Count:8b},sell:{id:"kubejs:scavenger_scrap",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:cabbage_crate",Count:8b},sell:{id:"kubejs:scavenger_scrap",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:onion_crate",Count:8b},sell:{id:"kubejs:scavenger_scrap",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:hay_block",Count:8b},sell:{id:"kubejs:scavenger_scrap",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:rice_bale",Count:8b},sell:{id:"kubejs:scavenger_scrap",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:rice_bag",Count:8b},sell:{id:"kubejs:scavenger_scrap",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:smoked_ham",Count:16b},sell:{id:"kubejs:scavenger_scrap",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:mutton_chops",Count:16b},sell:{id:"kubejs:scavenger_scrap",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:chicken_cuts",Count:16b},sell:{id:"kubejs:scavenger_scrap",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:bacon",Count:16b},sell:{id:"kubejs:scavenger_scrap",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:cod_slice",Count:16b},sell:{id:"kubejs:scavenger_scrap",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:salmon_slice",Count:16b},sell:{id:"kubejs:scavenger_scrap",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:beef",Count:32b},sell:{id:"kubejs:scavenger_scrap",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:porkchop",Count:32b},sell:{id:"kubejs:scavenger_scrap",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:mutton",Count:32b},sell:{id:"kubejs:scavenger_scrap",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:rabbit",Count:32b},sell:{id:"kubejs:scavenger_scrap",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:cod",Count:32b},sell:{id:"kubejs:scavenger_scrap",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:salmon",Count:32b},sell:{id:"kubejs:scavenger_scrap",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"ad_astra:space_helmet",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:3b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"ad_astra:space_suit",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:5b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"ad_astra:space_pants",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"ad_astra:space_boots",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:3b},maxUses:9999999}


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"kubejs:scavenger_scrap",Count:2b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"ad_astra:gas_tank",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy.tag.BotariumData set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy.tag.BotariumData.StoredFluids set value []
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy.tag.BotariumData.StoredFluids append value {Fluid:"ad_astra:oxygen",Amount:1000L}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"kubejs:scavenger_scrap",Count:4b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"ad_astra:large_gas_tank",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy.tag.BotariumData set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy.tag.BotariumData.StoredFluids set value []
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy.tag.BotariumData.StoredFluids append value {Fluid:"ad_astra:oxygen",Amount:3000L}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"gtceu:hv_lithium_battery",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy.tag set value {Charge:1800000L}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"kubejs:scavenger_scrap",Count:5b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtceu:rocket_fuel_bucket",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:2b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtceu:uranium_ingot",Count:1b},sell:{id:"kubejs:scavenger_scrap",Count:16b},maxUses:9999999}




/kill @e[type=minecraft:zombified_piglin,limit=1]
/kill @e[tag=scav,limit=1,sort=nearest]
t
iMAX

/summon minecraft:villager ~ ~1 ~ {Tags:["imax","trader"]}
(IMAX DONT SPAWN/THEY ARE AN INVISIBLE GROUP)

Lunarians (Lunarians)
/execute as @p run function factions:spawn/luna
or
/execute as @p run function factions:spawn/luna_trader  
then
/data modify entity @e[tag=luna,limit=1,sort=nearest] CustomName set value '{"text":"[Lunarian] YAK WOO","color":"yellow","bold":true,"italic":false}'
/data remove entity @e[tag=luna,limit=1,sort=nearest] NoAI

/data modify entity @e[tag=trader,limit=1,sort=nearest]  ForgeData.UniversalTrader.Offers set value {Recipes:[]}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:1b},sell:{id:"gtmutils:doge_coin",Count:2b},maxUses:999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:2b},sell:{id:"kubejs:lunarian_coin",Count:1b},maxUses:999999}


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:1b},sell:{id:"ad_astra:cheese",Count:4b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:2b},sell:{id:"brewinandchewin:flaxen_cheese_wheel",Count:1b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:2b},sell:{id:"brewinandchewin:scarlet_cheese_wheel",Count:1b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:1b},sell:{id:"toughasnails:purified_water_bottle",Count:1b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:6b},sell:{id:"ad_astra:oxygen_distributor",Count:1b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:6b},sell:{id:"ad_astra:oxygen_loader",Count:1b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:5b},sell:{id:"ad_astra:oxygen_sensor",Count:1b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:7b},sell:{id:"ad_astra:airlock",Count:1b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:7b},sell:{id:"warpdrive:ship_airlock_t1",Count:1b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:5b},sell:{id:"warpdrive:dehumidifier",Count:1b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:8b},sell:{id:"ad_astra:iron_plateblock",Count:48b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:8b},sell:{id:"ad_astra:iron_plating",Count:48b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:6b},sell:{id:"ad_astra:iron_plating_stairs",Count:32b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:6b},sell:{id:"ad_astra:iron_plating_slab",Count:32b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:3b},sell:{id:"minecraft:glass",Count:64b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:12b},sell:{id:"gtceu:mv_machine_hull",Count:1b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:6b},sell:{id:"gtceu:mv_electric_motor",Count:1b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:6b},sell:{id:"gtceu:mv_electric_pump",Count:1b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:5b},sell:{id:"gtceu:mv_conveyor_module",Count:1b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:10b},sell:{id:"gtceu:good_integrated_circuit",Count:1b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:8b},sell:{id:"gtceu:mv_voltage_coil",Count:1b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:4b},sell:{id:"gtceu:fine_copper_wire",Count:16b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:10b},sell:{id:"solarflux:sp_1",Count:16b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:14b},sell:{id:"gtceu:mv_battery_buffer_16x",Count:1b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:16b},sell:{id:"gtceu:iron_rotor",Count:1b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:18b},sell:{id:"gtceu:mv_super_chest",Count:1b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:20b},sell:{id:"gtceu:mv_super_tank",Count:1b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:1b},sell:{id:"gtceu:distilled_water_bucket",Count:1b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:24b},sell:{id:"gtceu:infinite_water_cover",Count:1b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:1b},sell:{id:"exdeorum:compressed_dust",Count:64b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:1b},sell:{id:"ad_astra:moon_sand",Count:64b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:1b},sell:{id:"ad_astra:moon_stone",Count:64b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:1b},sell:{id:"gtceu:rocket_fuel_bucket",Count:1b},maxUses:999999}


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"kubejs:lunarian_coin",Count:8b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"enderio:fluid_tank",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.RedstoneControl set value 0
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.IoConfig set value {0:0b,1:0b,2:0b,3:0b,4:0b,5:0b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.Fluid set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.Fluid.Tanks set value []
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.Fluid.Size set value 1
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.Fluid.Tanks append value {Index:0,FluidName:"gtceu:rocket_fuel",Capacity:16000,Amount:16000}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.Items set value {Size:4,Items:[]}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"kubejs:lunarian_coin",Count:8b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"enderio:fluid_tank",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.RedstoneControl set value 0
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.IoConfig set value {0:0b,1:0b,2:0b,3:0b,4:0b,5:0b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.Fluid set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.Fluid.Tanks set value []
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.Fluid.Size set value 1
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.Fluid.Tanks append value {Index:0,FluidName:"gtceu:distilled_water",Capacity:16000,Amount:16000}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.Items set value {Size:4,Items:[]}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:1b},sell:{id:"minecraft:glass_bottle",Count:24b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:lunarian_coin",Count:4b},sell:{id:"gtceu:uranium_ingot",Count:1b},maxUses:999999}


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"kubejs:lunarian_coin",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"ad_astra:gas_tank",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BotariumData set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BotariumData.StoredFluids set value []
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BotariumData.StoredFluids append value {Fluid:"ad_astra:oxygen",Amount:1000L}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"kubejs:lunarian_coin",Count:2b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"ad_astra:large_gas_tank",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BotariumData set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BotariumData.StoredFluids set value []
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BotariumData.StoredFluids append value {Fluid:"ad_astra:oxygen",Amount:3000L}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"kubejs:lunarian_coin",Count:4b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"gtceu:mv_lithium_battery",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.Charge set value 420000L
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"kubejs:lunarian_coin",Count:8b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"gtceu:hv_lithium_battery",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.Charge set value 1800000L
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999

lunarian buy:



/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"ad_astra:gas_tank",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"kubejs:lunarian_coin",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy.tag.BotariumData set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy.tag.BotariumData.StoredFluids set value []
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy.tag.BotariumData.StoredFluids append value {Fluid:"ad_astra:oxygen",Amount:0L}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"ad_astra:large_gas_tank",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"kubejs:lunarian_coin",Count:2b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy.tag.BotariumData set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy.tag.BotariumData.StoredFluids set value []
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy.tag.BotariumData.StoredFluids append value {Fluid:"ad_astra:oxygen",Amount:0L}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"ad_astra:gas_tank",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"kubejs:lunarian_coin",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy.tag.BotariumData set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy.tag.BotariumData.StoredFluids set value []
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy.tag.BotariumData.StoredFluids append value {Fluid:"ad_astra:oxygen",Amount:1000L}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"ad_astra:large_gas_tank",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"kubejs:lunarian_coin",Count:2b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy.tag.BotariumData set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy.tag.BotariumData.StoredFluids set value []
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy.tag.BotariumData.StoredFluids append value {Fluid:"ad_astra:oxygen",Amount:3000L}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"gtceu:mv_lithium_battery",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"kubejs:lunarian_coin",Count:2b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy.tag set value {Charge:0L}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"gtceu:hv_lithium_battery",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"kubejs:lunarian_coin",Count:4b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy.tag set value {Charge:0L}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999



/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"gtceu:mv_lithium_battery",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"kubejs:lunarian_coin",Count:2b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy.tag set value {Charge:420000L}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"gtceu:hv_lithium_battery",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"kubejs:lunarian_coin",Count:4b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy.tag set value {Charge:1800000L}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999



/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:water_bucket",Count:1b},sell:{id:"kubejs:lunarian_coin",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"toughasnails:dirty_water_bottle",Count:1b},sell:{id:"kubejs:lunarian_coin",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"toughasnails:purified_water_bottle",Count:1b},sell:{id:"kubejs:lunarian_coin",Count:1b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:glass_bottle",Count:24b},sell:{id:"kubejs:lunarian_coin",Count:1b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:rotten_flesh",Count:32b},sell:{id:"kubejs:lunarian_coin",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:bone",Count:32b},sell:{id:"kubejs:lunarian_coin",Count:1b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:leather",Count:16b},sell:{id:"kubejs:lunarian_coin",Count:2b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:bread",Count:16b},sell:{id:"kubejs:lunarian_coin",Count:2b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"ad_astra:cheese",Count:16b},sell:{id:"kubejs:lunarian_coin",Count:2b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"ad_astra:moon_sand",Count:64b},sell:{id:"kubejs:lunarian_coin",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"ad_astra:moon_stone",Count:64b},sell:{id:"kubejs:lunarian_coin",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"deepspace:alien_stone",Count:64b},sell:{id:"kubejs:lunarian_coin",Count:1b},maxUses:9999999}


/tag @e[tag=luna,nbt={DeathTime:0s},sort=nearest,limit=1] remove deathless
/kill @e[type=ad_astra:lunarian,limit=1]

Automata (iron golems)

/execute as @p run function factions:spawn/auto
or
/execute as @p run function factions:spawn/auto_trader
then
/data modify entity @e[tag=auto,limit=1,sort=nearest] CustomName set value '{"text":"[Automata] Ship Robotron","color":"gray","bold":true,"italic":false}'
/data remove entity @e[tag=auto,limit=1,sort=nearest] NoAI

/data modify entity @e[tag=auto,limit=1,sort=nearest] CustomName set value '{"text":"[Automata] NODE-Δ7","color":"gray","bold":true,"italic":false}'

/data modify entity @e[tag=trader,limit=1,sort=nearest]  ForgeData.UniversalTrader.Offers set value {Recipes:[]}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:automata_cheque",Count:1b},sell:{id:"gtmutils:doge_coin",Count:4b},maxUses:999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:4b},sell:{id:"kubejs:automata_cheque",Count:1b},maxUses:999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:automata_cheque",Count:1b},sell:{id:"minecraft:bread",Count:12b},maxUses:9999999}


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"kubejs:automata_cheque",Count:1b},sell:{id:"toughasnails:purified_water_bottle",Count:12b},maxUses:9999999}


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"kubejs:automata_cheque",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"ad_astra:gas_tank",Count:2b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BotariumData set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BotariumData.StoredFluids set value []
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BotariumData.StoredFluids append value {Fluid:"ad_astra:oxygen",Amount:1000L}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:redstone",Count:64b},sell:{id:"kubejs:automata_cheque",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:iron_ingot",Count:32b},sell:{id:"kubejs:automata_cheque",Count:1b},maxUses:9999999}



/tag @e[type=minecraft:iron_golem,limit=1,sort=nearest] remove deathless

/kill @e[type=minecraft:iron_golem,limit=1,sort=nearest]

/data remove entity @e[tag=auto,limit=1,sort=nearest] NoAI

Pirates (pillagers) 

/execute as @p run function factions:spawn/pirate
or
/execute as @p run function factions:spawn/pirate_trader
then
/data modify entity @e[type=minecraft:pillager,tag=pirate,limit=1,sort=nearest] CustomName set value '{"text":"[Pirate] Shiphand","color":"red","bold":true,"italic":false}'
/data modify entity @e[type=minecraft:pillager,tag=pirate,limit=1,sort=nearest] CustomName set value '{"text":"[Pirate] Pilot","color":"red","bold":true,"italic":false}'
/data modify entity @e[type=minecraft:pillager,tag=pirate,limit=1,sort=nearest] CustomName set value '{"text":"[Pirate] Co-Pilot","color":"red","bold":true,"italic":false}'
/data modify entity @e[type=minecraft:pillager,tag=pirate,limit=1,sort=nearest] CustomName set value '{"text":"[Pirate] Janitor","color":"red","bold":true,"italic":false}'

/data modify entity @e[type=minecraft:pillager,tag=trader,limit=1,sort=nearest] CustomName set value '{"text":"[Pirate] Captain Rook","color":"red","bold":true,"italic":false}'


/data modify entity @e[tag=trader,limit=1,sort=nearest]  ForgeData.UniversalTrader.Offers set value {Recipes:[]}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:1b},sell:{id:"gtmutils:doge_coin",Count:1b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:1b},sell:{id:"gtmutils:ancient_gold_coin",Count:1b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:1b},sell:{id:"minecraft:bread",Count:8b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:1b},sell:{id:"minecraft:water_bucket",Count:1b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:1b},sell:{id:"minecraft:leather",Count:8b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:1b},sell:{id:"minecraft:glass_bottle",Count:16b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:2b},sell:{id:"minecraft:compass",Count:1b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:2b},sell:{id:"minecraft:map",Count:1b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:1b},sell:{id:"farmersdelight:rope",Count:4b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:3b},sell:{id:"minecraft:iron_sword",Count:1b},maxUses:999999}


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"gtmutils:ancient_gold_coin",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"ad_astra:gas_tank",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BotariumData set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BotariumData.StoredFluids set value []
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BotariumData.StoredFluids append value {Fluid:"ad_astra:oxygen",Amount:1000L}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:3b},sell:{id:"ad_astra:oxygen_distributor",Count:1b},maxUses:999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:4b},sell:{id:"ad_astra:oxygen_loader",Count:1b},maxUses:999999}


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:5b},sell:{id:"warpdrive:ship_airlock_t1",Count:1b},maxUses:999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:4b},sell:{id:"warpdrive:ship_hullfoam",Count:32b},maxUses:999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:6b},sell:{id:"gtceu:uranium_ingot",Count:1b},maxUses:999999}


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"gtmutils:ancient_gold_coin",Count:6b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"enderio:fluid_tank",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.RedstoneControl set value 0
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.IoConfig set value {0:0b,1:0b,2:0b,3:0b,4:0b,5:0b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.Fluid set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.Fluid.Tanks set value []
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.Fluid.Size set value 1
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.Fluid.Tanks append value {Index:0,FluidName:"gtceu:rocket_fuel",Capacity:16000,Amount:16000}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.Items set value {Size:4,Items:[]}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"gtmutils:ancient_gold_coin",Count:3b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"gtceu:hv_lithium_battery",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.Charge set value 1800000L
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:2b},sell:{id:"gtceu:wood_plate",Count:24b},maxUses:999999}


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:8b},sell:{id:"warpdrive:ship_hull_t1",Count:48b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:4b},sell:{id:"warpdrive:ship_hull_stairs_t1",Count:24b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:4b},sell:{id:"warpdrive:ship_hull_slab_t1",Count:24b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:16b},sell:{id:"warpdrive:ship_core",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:32b},sell:{id:"warpdrive:ship_nav_core",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:64b},sell:{id:"deepspace:ship_scanner",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:16b},sell:{id:"warpdrive:ship_thruster_t1",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:32b},sell:{id:"deepspace:breeder_reactor",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:8b},sell:{id:"gtceu:crystal_superconductor_hex_wire",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:4b},sell:{id:"enderio:vibrant_capacitor_bank",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:1b},sell:{id:"farmersdelight:iron_knife",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:1b},sell:{id:"minecraft:firework_rocket",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:4b},sell:{id:"gtceu:industrial_tnt",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:1b},sell:{id:"minecraft:gunpowder",Count:48b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:1b},sell:{id:"minecraft:lead",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:1b},sell:{id:"minecraft:chain",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:1b},sell:{id:"minecraft:coal",Count:48b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:1b},sell:{id:"minecraft:flint_and_steel",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:1b},sell:{id:"minecraft:bucket",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:2b},sell:{id:"gtceu:rubber_plunger",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:ancient_gold_coin",Count:2b},sell:{id:"minecraft:spyglass",Count:1b},maxUses:9999999}

SELL:


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"warpdrive:ship_hull_t1",Count:48b},sell:{id:"gtmutils:ancient_gold_coin",Count:3b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"warpdrive:ship_hull_stairs_t1",Count:24b},sell:{id:"gtmutils:ancient_gold_coin",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"warpdrive:ship_hull_slab_t1",Count:24b},sell:{id:"gtmutils:ancient_gold_coin",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"warpdrive:ship_core",Count:1b},sell:{id:"gtmutils:ancient_gold_coin",Count:6b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"warpdrive:ship_nav_core",Count:1b},sell:{id:"gtmutils:ancient_gold_coin",Count:12b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"warpdrive:ship_thruster_t1",Count:4b},sell:{id:"gtmutils:ancient_gold_coin",Count:6b},maxUses:9999999}



/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:iron_ingot",Count:64b},sell:{id:"gtmutils:ancient_gold_coin",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:copper_ingot",Count:64b},sell:{id:"gtmutils:ancient_gold_coin",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:gold_ingot",Count:16b},sell:{id:"gtmutils:ancient_gold_coin",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:gunpowder",Count:64b},sell:{id:"gtmutils:ancient_gold_coin",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtceu:sulfur_dust",Count:64b},sell:{id:"gtmutils:ancient_gold_coin",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtceu:gelled_toluene",Count:64b},sell:{id:"gtmutils:ancient_gold_coin",Count:2b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtceu:industrial_tnt",Count:64b},sell:{id:"gtmutils:ancient_gold_coin",Count:3b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:chain",Count:16b},sell:{id:"gtmutils:ancient_gold_coin",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:iron_nugget",Count:64b},sell:{id:"gtmutils:ancient_gold_coin",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:bread",Count:16b},sell:{id:"gtmutils:ancient_gold_coin",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:steak_and_potatoes",Count:4b},sell:{id:"gtmutils:ancient_gold_coin",Count:2b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:bacon_sandwich",Count:4b},sell:{id:"gtmutils:ancient_gold_coin",Count:2b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:bacon_and_eggs",Count:4b},sell:{id:"gtmutils:ancient_gold_coin",Count:2b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:cookie",Count:24b},sell:{id:"gtmutils:ancient_gold_coin",Count:2b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:glass_bottle",Count:16b},sell:{id:"gtmutils:ancient_gold_coin",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"toughasnails:purified_water_bottle",Count:1b},sell:{id:"gtmutils:ancient_gold_coin",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:rice_bag",Count:8b},sell:{id:"gtmutils:ancient_gold_coin",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:carrot_crate",Count:8b},sell:{id:"gtmutils:ancient_gold_coin",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:potato_crate",Count:8b},sell:{id:"gtmutils:ancient_gold_coin",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:beetroot_crate",Count:8b},sell:{id:"gtmutils:ancient_gold_coin",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:cabbage_crate",Count:8b},sell:{id:"gtmutils:ancient_gold_coin",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:tomato_crate",Count:8b},sell:{id:"gtmutils:ancient_gold_coin",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"farmersdelight:onion_crate",Count:8b},sell:{id:"gtmutils:ancient_gold_coin",Count:1b},maxUses:9999999}


/data merge entity @e[type=minecraft:pillager,tag=pirate,limit=1,sort=nearest]  {HandItems:[{id:"gtmutils:ancient_gold_coin",Count:1b,tag:{}},{}],HandDropChances:[2.0f,0.0f]}

/data remove entity @e[tag=pirate,limit=1,sort=nearest] NoAI



/kill @e[type=minecraft:pillager,limit=1]

Republic  (zombified_piglin+jet_suit)

/execute as @p run function factions:spawn/republic
or
/execute as @p run function factions:spawn/republic_trader
then
/data modify entity @e[type=minecraft:zombified_piglin,tag=republic,limit=1,sort=nearest] CustomName set value '{"text":"[Republic] Supervisor","color":"aqua","bold":true,"italic":false}'
/data remove entity @e[tag=republic,limit=1,sort=nearest] NoAI

/data merge entity @e[type=minecraft:zombified_piglin,tag=republic,limit=1,sort=nearest]  {HandItems:[{id:"gtceu:mv_iron_drill",Count:1b,tag:{}},{}],HandDropChances:[2.0f,0.0f]}
/data merge entity @e[tag=republic,limit=1] {CanPickUpLoot:0b,Brain:{memories:{}}}

/data merge entity @e[type=minecraft:zombified_piglin,tag=republic,limit=1,sort=nearest]  {HandItems:[{id:"gtmutils:doge_coin",Count:1b,tag:{}},{}],HandDropChances:[2.0f,0.0f]}

/data modify entity @e[type=minecraft:zombified_piglin,tag=republic,limit=1,sort=nearest] CustomName set value '{"text":"[Republic] Guard","color":"aqua","bold":true,"italic":false}'

/data modify entity @e[type=minecraft:zombified_piglin,tag=republic,limit=1,sort=nearest] HandItems[0] set value {id:"gtceu:nano_saber",Count:1b}


/data modify entity @e[tag=trader,limit=1,sort=nearest]  ForgeData.UniversalTrader.Offers set value {Recipes:[]}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"gtmutils:doge_coin",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"ad_astra:gas_tank",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BotariumData set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BotariumData.StoredFluids set value []
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BotariumData.StoredFluids append value {Fluid:"ad_astra:oxygen",Amount:1000L}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"gtmutils:doge_coin",Count:2b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"ad_astra:large_gas_tank",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BotariumData set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BotariumData.StoredFluids set value []
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BotariumData.StoredFluids append value {Fluid:"ad_astra:oxygen",Amount:3000L}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"gtmutils:doge_coin",Count:12b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"enderio:fluid_tank",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.RedstoneControl set value 0
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.IoConfig set value {0:0b,1:0b,2:0b,3:0b,4:0b,5:0b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.Fluid set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.Fluid.Tanks set value []
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.Fluid.Size set value 1
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.Fluid.Tanks append value {Index:0,FluidName:"gtceu:rocket_fuel",Capacity:16000,Amount:16000}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.Items set value {Size:4,Items:[]}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"gtmutils:doge_coin",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"enderio:fluid_tank",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.RedstoneControl set value 0
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.IoConfig set value {0:0b,1:0b,2:0b,3:0b,4:0b,5:0b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.Fluid set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.Fluid.Tanks set value []
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.Fluid.Size set value 1
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.Fluid.Tanks append value {Index:0,FluidName:"gtceu:distilled_water",Capacity:16000,Amount:16000}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.Items set value {Size:4,Items:[]}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999



/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:1b},sell:{id:"toughasnails:purified_water_bottle",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:1b},sell:{id:"minecraft:bread",Count:12b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:4b},sell:{id:"farmersdelight:rice_bag",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:4b},sell:{id:"minecraft:cooked_beef",Count:16b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:1b},sell:{id:"minecraft:iron_ingot",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:1b},sell:{id:"minecraft:copper_ingot",Count:32b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:1b},sell:{id:"minecraft:glass",Count:32b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:1b},sell:{id:"minecraft:redstone",Count:16b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:1b},sell:{id:"gtceu:steel_ingot",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:1b},sell:{id:"warpdrive:ship_hull_t1",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:1b},sell:{id:"warpdrive:ship_glass_t1",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:1b},sell:{id:"warpdrive:ship_airlock_t1",Count:1b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"gtmutils:doge_coin",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"gtceu:hv_lithium_battery",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.Charge set value 1800000L
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:24b},sell:{id:"deepspace:abnormality_scanner",Count:1b},maxUses:9999999}


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"gtmutils:doge_coin",Count:48b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"gtceu:prospector.hv",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag set value {Charge:1600000L,Mode:1}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:6b},sell:{id:"gtceu:terminal",Count:1b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:1b},sell:{id:"minecraft:iron_pickaxe",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:1b},sell:{id:"minecraft:iron_axe",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:1b},sell:{id:"minecraft:iron_shovel",Count:1b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:2b},sell:{id:"gtceu:iron_wrench",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:2b},sell:{id:"gtceu:iron_hammer",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:2b},sell:{id:"gtceu:iron_crowbar",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:2b},sell:{id:"gtceu:iron_file",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:2b},sell:{id:"gtceu:iron_screwdriver",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:2b},sell:{id:"gtceu:iron_scythe",Count:1b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:1b},sell:{id:"farmersdelight:iron_knife",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:2b},sell:{id:"gtceu:iron_mortar",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:2b},sell:{id:"gtceu:iron_wire_cutter",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:2b},sell:{id:"gtceu:iron_mining_hammer",Count:1b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:1b},sell:{id:"toughasnails:empty_iron_canteen",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:1b},sell:{id:"minecraft:bucket",Count:1b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:2b},sell:{id:"gtceu:rubber_plunger",Count:1b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:4b},sell:{id:"gtceu:hv_iron_drill",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:4b},sell:{id:"gtceu:lv_iron_chainsaw",Count:1b},maxUses:9999999}


/kill @e[type=minecraft:zombified_piglin,limit=1]

Collective Cat

/data merge entity @e[type=minecraft:cat,limit=1,sort=nearest] {PersistenceRequired:1b}
/data merge entity @e[type=minecraft:cat,limit=1,sort=nearest] {CustomName:'{"text":"Willow\\u0027s Cat","color":"green","italic":false}',CustomNameVisible:1b}


Collective (villager) 

/execute as @p run function factions:spawn/collective
or
/execute as @p run function factions:spawn/collective_trader
then
/data modify entity @e[type=minecraft:villager,tag=collective,limit=1,sort=nearest] CustomName set value '{"text":"[Collective] Stranded Traveller","color":"green","bold":true,"italic":false}'

/data modify entity @e[type=minecraft:villager,tag=collective,limit=1,sort=nearest] CustomName set value '{"text":"[Collective] Moss Oracle","color":"green","bold":true,"italic":false}'

/data remove entity @e[tag=collective,limit=1,sort=nearest] NoAI

/data modify entity @e[type=minecraft:villager,tag=collective,limit=1,sort=nearest] CustomName set value '{"text":"[Collective] Willow Drift","color":"green","bold":true,"italic":false}'

/data modify entity @e[type=minecraft:villager,tag=collective,limit=1] HandItems[0] set value {id:"minecraft:nautilus_shell",Count:1b}

/data modify entity @e[tag=trader,limit=1,sort=nearest]  ForgeData.UniversalTrader.Offers set value {Recipes:[]}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:1b},sell:{id:"minecraft:nautilus_shell",Count:4b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:4b},sell:{id:"gtmutils:doge_coin",Count:1b},maxUses:9999999}


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"minecraft:nautilus_shell",Count:4b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"ad_astra:gas_tank",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BotariumData set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BotariumData.StoredFluids set value []
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BotariumData.StoredFluids append value {Fluid:"ad_astra:oxygen",Amount:1000L}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"minecraft:nautilus_shell",Count:16b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"ad_astra:large_gas_tank",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BotariumData set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BotariumData.StoredFluids set value []
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BotariumData.StoredFluids append value {Fluid:"ad_astra:oxygen",Amount:3000L}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"minecraft:nautilus_shell",Count:5b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"gtceu:hv_lithium_battery",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag set value {Charge:1800000L}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"minecraft:nautilus_shell",Count:32b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"enderio:fluid_tank",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.RedstoneControl set value 0
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.IoConfig set value {0:0b,1:0b,2:0b,3:0b,4:0b,5:0b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.Fluid set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.Fluid.Tanks set value []
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.Fluid.Size set value 1
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.Fluid.Tanks append value {Index:0,FluidName:"gtceu:distilled_water",Capacity:16000,Amount:16000}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.Items set value {Size:4,Items:[]}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"minecraft:nautilus_shell",Count:40b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"enderio:fluid_tank",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.RedstoneControl set value 0
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.IoConfig set value {0:0b,1:0b,2:0b,3:0b,4:0b,5:0b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.Fluid set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.Fluid.Tanks set value []
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.Fluid.Size set value 1
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BlockEntityTag.Fluid.Tanks append value {Index:0,FluidName:"gtceu:rocket_fuel",Capacity:16000,Amount:16000}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.Items set value {Size:4,Items:[]}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:1b},sell:{id:"toughasnails:purified_water_bottle",Count:1b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:2b},sell:{id:"minecraft:glass_bottle",Count:16b},maxUses:9999999}

...


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:2b},sell:{id:"minecraft:wheat",Count:32b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:2b},sell:{id:"minecraft:potato",Count:32b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:2b},sell:{id:"minecraft:carrot",Count:32b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:2b},sell:{id:"minecraft:beetroot",Count:32b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:2b},sell:{id:"minecraft:melon_slice",Count:16b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:2b},sell:{id:"minecraft:sweet_berries",Count:16b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:2b},sell:{id:"minecraft:apple",Count:8b},maxUses:9999999}

...

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:2b},sell:{id:"farmersdelight:rice",Count:32b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:2b},sell:{id:"farmersdelight:cabbage",Count:16b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:2b},sell:{id:"farmersdelight:tomato",Count:16b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:2b},sell:{id:"farmersdelight:onion",Count:16b},maxUses:9999999}



/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:4b},sell:{id:"farmersdelight:rice_bag",Count:8b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:8b},sell:{id:"farmersdelight:carrot_crate",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:8b},sell:{id:"farmersdelight:potato_crate",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:8b},sell:{id:"farmersdelight:cabbage_crate",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:8b},sell:{id:"farmersdelight:tomato_crate",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:8b},sell:{id:"farmersdelight:onion_crate",Count:1b},maxUses:9999999}

...
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:1b},sell:{id:"minecraft:wheat_seeds",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:1b},sell:{id:"minecraft:pumpkin_seeds",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:1b},sell:{id:"minecraft:melon_seeds",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:1b},sell:{id:"minecraft:beetroot_seeds",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:1b},sell:{id:"exdeorum:grass_seeds",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:1b},sell:{id:"farmersdelight:cabbage_seeds",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:1b},sell:{id:"farmersdelight:tomato_seeds",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:1b},sell:{id:"minecraft:brown_mushroom",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:1b},sell:{id:"minecraft:red_mushroom",Count:8b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:2b},sell:{id:"gtceu:rubber_sapling",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:2b},sell:{id:"minecraft:cherry_sapling",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:2b},sell:{id:"minecraft:dark_oak_sapling",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:2b},sell:{id:"minecraft:acacia_sapling",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:2b},sell:{id:"minecraft:jungle_sapling",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:2b},sell:{id:"minecraft:birch_sapling",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:2b},sell:{id:"minecraft:spruce_sapling",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:2b},sell:{id:"minecraft:oak_sapling",Count:1b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:2b},sell:{id:"minecraft:cactus",Count:8b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:2b},sell:{id:"minecraft:sugar_cane",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:2b},sell:{id:"minecraft:bamboo",Count:32b},maxUses:9999999}

...
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:1b},sell:{id:"minecraft:kelp",Count:32b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:1b},sell:{id:"minecraft:sea_pickle",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:2b},sell:{id:"farmersdelight:rice_panicle",Count:32b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:2b},sell:{id:"minecraft:bone_meal",Count:64b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:4b},sell:{id:"gtceu:sticky_resin",Count:64b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:1b},sell:{id:"minecraft:dirt",Count:32b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:4b},sell:{id:"farmersdelight:rich_soil_farmland",Count:16b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:6b},sell:{id:"exdeorum:iron_watering_can",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:1b},sell:{id:"minecraft:bucket",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:4b},sell:{id:"gtceu:iron_scythe",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:2b},sell:{id:"minecraft:shears",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:3b},sell:{id:"minecraft:iron_hoe",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:4b},sell:{id:"minecraft:iron_axe",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:5b},sell:{id:"gtceu:iron_mortar",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:3b},sell:{id:"farmersdelight:iron_knife",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:8b},sell:{id:"botanypots:lime_concrete_botany_pot",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:8b},sell:{id:"bonsaitrees3:bonsaipot",Count:1b},maxUses:9999999}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:nautilus_shell",Count:4b},sell:{id:"minecraft:hopper",Count:1b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtceu:uranium_ingot",Count:1b},sell:{id:"minecraft:nautilus_shell",Count:16b},maxUses:9999999}


/kill @e[type=minecraft:villager,limit=1]

================= HOW TO USE CUSTOM FORCED TRADER SHOPS FOR NON-VILLAGERS

Step 0 - Summon the scavenger trader already with a faction
e.g

/execute as @p run function factions:spawn/scav_trader

Step 2 — Add some trades to it:

/data modify entity @e[tag=trader,limit=1,sort=nearest]  ForgeData.UniversalTrader.Offers set value {Recipes:[]}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:diamond",Count:1b},sell:{id:"minecraft:netherite_scrap",Count:1b},maxUses:100}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:emerald",Count:1b},sell:{id:"minecraft:bread",Count:6b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:emerald",Count:1b},sell:{id:"toughasnails:dirty_water_bottle",Count:6b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:emerald",Count:1b},sell:{id:"toughasnails:purified_water_bottle",Count:6b},maxUses:9999999}


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].buy set value {id:"minecraft:emerald",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell set value {id:"ad_astra:gas_tank",Count:1b}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BotariumData set value {}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BotariumData.StoredFluids set value []
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].sell.tag.BotariumData.StoredFluids append value {Fluid:"ad_astra:oxygen",Amount:1000L}
/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes[-1].maxUses set value 9999999


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:emerald",Count:1b},sell:{id:"minecraft:carrot",Count:12b},maxUses:9999999}


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtceu:pcb_coolant_bucket",Count:1b},sell:{id:"minecraft:emerald",Count:8b},maxUses:9999999}

/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtceu:iron_gear",Count:4b},sell:{id:"minecraft:emerald",Count:8b},maxUses:9999999}


/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtceu:fine_copper_wire",Count:16b},sell:{id:"minecraft:emerald",Count:8b},maxUses:9999999}





/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:1b},sell:{id:"gtmutils:ancient_gold_coin",Count:1b},maxUses:9999999}



/data modify entity @e[tag=trader,limit=1,sort=nearest] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:emerald",Count:4b},sell:{id:"minecraft:iron_ingot",Count:4b},maxUses:9999999}


Step 4 — Right-click it. The vanilla trade GUI opens.

/kill @e[tag=trader,limit=1,sort=nearest]

================== HOW TO ADD AE2 CARD WITH ITEM UFOR TRADE

(reset offers)
/data modify entity @e[type=minecraft:zombified_piglin,tag=trader,limit=1] ForgeData.UniversalTrader.Offers set value {Recipes:[]}

/data modify entity @e[type=minecraft:zombified_piglin,tag=trader,limit=1] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"gtmutils:doge_coin",Count:64b},sell:{id:"ae2:fluid_storage_cell_1k",Count:1b,tag:{ic:1024000L}},maxUses:9999999}

/data modify entity @e[type=minecraft:zombified_piglin,tag=trader,limit=1] ForgeData.UniversalTrader.Offers.Recipes[0].sell.tag.keys set value [{"#c":"ae2:f",id:"gtceu:rocket_fuel"}]

/data modify entity @e[type=minecraft:zombified_piglin,tag=trader,limit=1] ForgeData.UniversalTrader.Offers.Recipes[0].sell.tag.amts set value [L;1024000L]


...

/data modify entity @e[type=minecraft:zombified_piglin,tag=trader,limit=1] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"minecraft:bread",Count:1b},sell:{id:"gtmutils:doge_coin",Count:1b},maxUses:9999999,rewardExp:0b}

/data modify entity @e[type=minecraft:zombified_piglin,tag=trader,limit=1] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"ad_astra:cheese",Count:1b},sell:{id:"gtmutils:doge_coin",Count:16b},maxUses:9999999,rewardExp:0b}

/data modify entity @e[type=minecraft:zombified_piglin,tag=trader,limit=1] ForgeData.UniversalTrader.Offers.Recipes append value {buy:{id:"toughasnails:purified_water_bottle",Count:1b},sell:{id:"gtmutils:doge_coin",Count:1b},maxUses:9999999,rewardExp:0b}


==================
Features:

Killing NPC of faction negatively modifies the scoreboard faction relationship matrix
Trading NPC of faction positively modifies the scoreboard faction relationship matrix
Trading with npcs with the trader tag if it also has e.g auto luna scav etc will increase faction relationship matrix
FTB Quests advancements for faction hate, and faction alliance levels
FTB Quests advancements for faction entity killcounts
> killing lunarians on the moon affects lunar faction reputation matrix
> trading lunarians on the moon affects lunar faction reputation matrix

==================

> make a quest that requires X faction reputation or hate:

126 advancement JSONs in data/factions/advancements/reputation/, named <faction>_pos<n>.json and <faction>_neg<n>.json. The tier names baked into each title are:

 in FTB Quests, add an Advancement task and point it at e.g. factions:reputation/scav_pos32 for the first Scavengers goodwill tier, or factions:reputation/scav_neg128 for Hostile standing.
 
32: Neutral+
64: Acquainted
128: Friendly
256: Trusted
512: Allied
1024: Honored
2048: Revered
4096: Exalted
8096: Legendary

Negative:

-32: Suspicious
-64: Unfriendly
-128: Hostile
-256: Hated
-512: Despised
-1024: Vilified
-2048: Condemned
-4096: Nemesis
-8096: Arch-Enemy

================================================
Kill counts

/scoreboard players get @s kills_luna
/scoreboard players add @p kills_luna 1

Skirmish 8
Raider 16
Hunter 32
Slayer 64
Executioner 128
Warlord 256
Destroyer 512
Annihilator 1024

====================================================

data stick research format

/give @p gtceu:data_stick{assembly_line_research:{research_id:"1x_gtceu_iv_electric_piston",research_type:"gtceu:assembly_line"}} 1

======================================================
9 skill objectives (`skill_mining`, `skill_digging`, etc.)

/scoreboard players get @s skill_mining
/scoreboard players get @s skill_crafting
/scoreboard players get @s skill_construction
/scoreboard players get @s skill_combat
/scoreboard players get @s skill_woodcutting
/scoreboard players get @s skill_digging
/scoreboard players get @s skill_farming
/scoreboard players get @s skill_smithing
/scoreboard players get @s skill_fishing
/scoreboard players get @s skill_crafting

| Skill | Trigger mechanism |
|---|---|
| Mining PASS DONE| `item_durability_changed` — pickaxe loses durability |
| Digging PASS DONE| `item_durability_changed` — shovel loses durability |
| Woodcutting PASS DONE | `item_durability_changed` — axe loses durability |
| Farming PASS DONE| `item_durability_changed` — hoe loses durability |
| Smithing PASS DONE| Tick-based stat comparison of `interact_with_furnace` counter |
| Combat PASS DONE | `player_killed_entity` — any entity kill |
| Fishing TO_TEST DONE| `fishing_rod_hooked` — reel-in catch |
| Crafting TODO PASS | `recipe_crafted` — any crafting table recipe |
| Construction FAIL| `placed_block` — any block placed |


Tier names: Novice → Apprentice → Journeyman → Adept → Expert → Master → Grandmaster → Legend → Mythic → Ancient → Transcendent → Divine → Omnipotent.

=======================================================
ENTITY HOSTILITY (SEE docs/ENTITY_HOSTILITY_COMMAND_REFERENCE.md


========================================================
WARP DRIVE CREW 

Functional Tags
ship_nav_mod
ship_warp_mod

/tag @e[tag=scav] add ship_nav_mod
/tag @e[tag=scav] add ship_warp_mod

/tag @e[tag=luna] add ship_nav_mod
/tag @e[tag=luna] add ship_warp_mod

/tag @e[tag=auto] add ship_nav_mod
/tag @e[tag=auto] add ship_warp_mod

/tag @e[tag=imax] add ship_nav_mod
/tag @e[tag=imax] add ship_warp_mod

/tag @e[tag=pirate] add ship_nav_mod
/tag @e[tag=pirate] add ship_warp_mod

/tag @e[tag=republic] add ship_nav_mod
/tag @e[tag=republic] add ship_warp_mod

/tag @e[tag=collective] add ship_nav_mod
/tag @e[tag=collective] add ship_warp_mod

=

/execute as @p run function factions:spawn/scav_shiphand
/execute as @p run function factions:spawn/luna_shiphand
/execute as @p run function factions:spawn/auto_shiphand
/execute as @p run function factions:spawn/pirate_shiphand
/execute as @p run function factions:spawn/republic_shiphand
/execute as @p run function factions:spawn/collective_shiphand

========================================================
SPAWN FIRST CONTACT FACTION SHIP/TRADER | First Contact Ship Spawn

Spawns a faction's first-contact trading ship near the player with a dramatic 3-line chat sequence (5s delays).

**Factions:** scav (DONE), luna, auto, pirate, republic, collective

**FTB Quests Setup:**
- Reward type: **Command**
- Run with Elevated Permission: **True**
- Command:
```
/execute as @p run function factions:spawn/spawn_first_contact_ship_scav
```

**What happens:**
1. `⚠️ Unknown signal detected...` (red, bold — immediate)
2. `⚠️ Scanning...` (red, bold — 5s later)
3. `⚠️ Vessel exiting warp...` (red, bold — 10s later)
4. Ship spawns via `/importship shop_first_contact_<faction>.nbt @p`
e.g
shop_first_contact_luna.nbt

========================================================
Faction Amnesty - reset faction reputation | Reset Reputation 

Sets a single faction's reputation to 0. Bypasses the faction matrix — only the target faction is affected.

**Factions:** scav, luna, auto, imax, pirate, republic, collective

**FTB Quests Setup:**
- Reward type: **Command**
- Run with Elevated Permission: **True**
- Command:
```
/execute as @p run function factions:rep/reset/reset_pirate
```

========================================================
“Betrayal Questline” / Defect Questline | Defect / Betrayal

Player betrays a faction. Sets that faction's rep to **-128** and gives **+128** to all opposing/rival factions (per the hostility matrix).

**Opposing faction map:**

| Defect From | Opposing Factions Getting +128 |
|-------------|-------------------------------|
| scav | *(none — neutral faction)* |
| luna | pirate |
| auto | pirate, collective |
| imax | *(none — neutral faction)* |
| pirate | republic, luna, collective, auto |
| republic | pirate |
| collective | auto, pirate |

**FTB Quests Setup:**
- Reward type: **Command**
- Run with Elevated Permission: **True**
- Command:
```
/execute as @p run function factions:defect/defect_pirate
```


========================================================
"Declare Alliance” / "Disband Alliance" quests

## 4. Alliance (Form / Disband)

Per the `faction_alliance_spec.md`, each faction has two functions:

### Form Alliance
- **Prerequisite:** `rep_<faction> > 0` (check via FTB Quests visibility condition)
- **Visibility:** Player does NOT have tag `allied_<tag>`
- Applies a one-time **+32 reputation bonus** (idempotent — no stacking)
- Adds the alliance tag

**Alliance tags per faction:**

| Faction | Tag |
|---------|-----|
| scav | `allied_scav` |
| luna | `allied_lunarian` |
| auto | `allied_automata` |
| imax | `allied_imax` |
| pirate | `allied_pirates` |
| republic | `allied_republic` |
| collective | `allied_collective` |

**FTB Quests — Form Alliance Quest:**
- Visibility: `!has_tag(allied_pirates)` (for pirate example)
- Requirement: `rep_pirate > 0`
- Reward Command (Elevated):
```
execute as @p run function factions:alliance/form_pirate_alliance
```

### Disband Alliance
- **Visibility:** Player HAS the alliance tag
- Removes the **+32 bonus** and clears the alliance tag

**FTB Quests — Disband Alliance Quest:**
- Visibility: `has_tag(allied_pirates)`
- No requirements
- Reward Command (Elevated):
```
execute as @p run function factions:alliance/disband_pirate_alliance
```


========================================================
COMPLEMENT/PRAISE CALL +1 if below 32

Gives **+1 reputation** to a faction, capped at **32**. Has a **15-minute cooldown** per faction per player.

**Three possible outcomes per call:**

| Condition | Result |
|-----------|--------|
| Cooldown active (praised within 15min) | Throttle message, no rep change |
| Rep already ≥ 32 | Cap-reached message, no rep change |
| Rep < 32 and no cooldown | +1 rep applied, 15min cooldown starts, accepted message |

**FTB Quests Setup:**
- Reward type: **Command**
- Run with Elevated Permission: **True**
- Command:
```
execute as @p run function factions:praise/praise_pirate
```

**Cooldown system:**
- Uses per-player scoreboards `praise_cd_<faction>` (value = seconds remaining)
- Decremented once per second via `praise/_tick_cooldowns` (called every 20 ticks from `tick.mcfunction`)
- 900 seconds = 15 minutes

=======================================================

```
# First Contact (6 factions)
factions:spawn/spawn_first_contact_ship_scav
factions:spawn/spawn_first_contact_ship_luna
factions:spawn/spawn_first_contact_ship_auto
factions:spawn/spawn_first_contact_ship_pirate
factions:spawn/spawn_first_contact_ship_republic
factions:spawn/spawn_first_contact_ship_collective

# Reset Rep (7 factions)
factions:rep/reset/reset_scav
factions:rep/reset/reset_luna
factions:rep/reset/reset_auto
factions:rep/reset/reset_imax
factions:rep/reset/reset_pirate
factions:rep/reset/reset_republic
factions:rep/reset/reset_collective

# Defect (7 factions)
factions:defect/defect_scav
factions:defect/defect_luna
factions:defect/defect_auto
factions:defect/defect_imax
factions:defect/defect_pirate
factions:defect/defect_republic
factions:defect/defect_collective

# Alliance — Form (7 factions)
factions:alliance/form_scav_alliance
factions:alliance/form_luna_alliance
factions:alliance/form_auto_alliance
factions:alliance/form_imax_alliance
factions:alliance/form_pirate_alliance
factions:alliance/form_republic_alliance
factions:alliance/form_collective_alliance

# Alliance — Disband (7 factions)
factions:alliance/disband_scav_alliance
factions:alliance/disband_luna_alliance
factions:alliance/disband_auto_alliance
factions:alliance/disband_imax_alliance
factions:alliance/disband_pirate_alliance
factions:alliance/disband_republic_alliance
factions:alliance/disband_collective_alliance

# Praise (7 factions)
factions:praise/praise_scav
factions:praise/praise_luna
factions:praise/praise_auto
factions:praise/praise_imax
factions:praise/praise_pirate
factions:praise/praise_republic
factions:praise/praise_collective
```

All functions are designed to be called from FTB Quests with `Run with Elevated Permission: True` using the pattern `execute as @p run function factions:<path>`.

/execute as @p run function factions:spawn/spawn_first_contact_ship_scav

/execute as @p run function factions:praise/praise_scav


/execute as @p run function factions:alliance/disband_scav_alliance
/execute as @p run function factions:spawn/spawn_first_contact_ship_luna


(execute as @p at @s run importship shop_first_contact_luna.nbt @p)
