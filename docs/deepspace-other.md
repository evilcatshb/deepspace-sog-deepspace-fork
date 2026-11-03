deepspace-other.md
Deep Space Dimension — Supplemental Implementation Requirements

This document contains ALL remaining rules, clarifications, edge-cases, assumptions, integration notes, balancing guarantees, and implementation constraints not fully covered in:

deepspace-design.md

deepspace-technical.md

It exists to ensure a programming AI can implement the project with 100% behavioral accuracy and without interpretation gaps.

1. Implementation Philosophy

The Deep Space mod is NOT a typical dimension mod.

It is a progression framework combining:

GregTech CEu industrial scaling

Ad Astra life-support survival

AE2 logistics automation

Multiplayer economic isolation → convergence

Every system must reinforce:

Survival → Automation → Expansion → Interconnection

2. Absolute Gameplay Rules (Non-Negotiable)
2.1 Player Never Hard-Locks

The system must guarantee:

Player always has a solvable path to oxygen.

Starter power lasts long enough to establish renewable loop.

At least one ice resource exists inside every crash bubble.

Failure of this rule = implementation bug.

2.2 Early Game Isolation

Players must NOT:

Spawn near another crash site.

Compete for early LV resources.

Spawn inside explored territory.

Isolation distance minimum:

120 chunks between crash centers
2.3 Distance = Progression Law

ALL scaling derives from:

distance_from_crash_site

NOT world origin.

Never compute difficulty from global coordinates.

3. Personal Crash Bubble — Behavioral Guarantees
Bubble Responsibilities

Inside bubble generator MUST ensure:

Resource Category	Guarantee
Ice	≥2 asteroids
Iron	≥1 vein equivalent
Copper	≥1 vein
Tin	≥1 vein
Organic material	≥1 asteroid
Stone	abundant
Safe build space	present
Bubble Protection

Bubble chunks must be tagged:

deepspace:bubble_owned=true
ownerUUID=<uuid>

These chunks:

cannot regenerate

cannot be reassigned

ignore future worldgen passes

4. Crash Ship Initialization Details
4.1 Machine Energy State

Upon generation:

Machine	Energy State
AE2 network	powered
Oxygen distributor	active
Oxygen generator	running
GT machines	unpowered
Reactor	enabled
4.2 Oxygen Bootstrap Values

Ship tanks start with:

Oxygen: 25%
Water: 10%

Enough for ~45 minutes real time without player action.

4.3 AE2 Starter Storage

Must contain:

Item	Qty
Uranium ingot	14
Steel plates	32
Copper cable	64
Circuits LV	8
Glass	64
Oxygen tanks	2

Purpose: prevent early crafting deadlocks.

5. Oxygen Ecology Rules
Plants vs Oxygen

Plants must obey:

oxygen_produced_by_loop
>
oxygen_consumed_by:
   player + plants + distributor loss

Plants themselves do NOT directly produce oxygen blocks.

Only chemical processing produces usable oxygen.

Closed System Principle

After bootstrap:

No external water input required.

If testing reveals water loss → recipe imbalance bug.

6. Bonsai/Botany Pot Assumptions

Implementation MUST normalize output rates.

If modpack changes growth speeds:

System recalculates using multiplier:

growthMultiplier = actualRate / expectedRate

Chemical outputs scale dynamically.

This prevents modpack incompatibility.

7. Water Freezing System

Trigger when:

fluid = water

dimension = deep_space

atmosphere = vacuum

Conversion delay:

5 ticks

Prevents instant griefing during placement.

8. Asteroid Vertical Design

Asteroid band intentionally compressed:

Y range: 0–120

Reasons:

prevents clutter in build space

allows megaships above field

improves visibility

Above Y160 = guaranteed empty space.

9. Multiplayer Scaling Behavior
Multiple Players Online

Each player has:

independent tier origin

Chunk tier resolution:

if inside bubble → personalSeed rules
else → nearest crash site distance

Nearest crash site lookup must be cached spatially.

10. Resource Depletion Prevention

Problem addressed:

First player mines everything.

Solution layers:

Personal bubble resources.

Infinite outward expansion.

Distance-based rarity increase.

High-tier ores spawn ONLY far away.

Result:
resources effectively infinite.

11. Warp Progression Integration

Warp drive exists but starts broken.

Repair requires materials from:

MV+

HV+

off-bubble exploration

Warp enables:

visiting other players

trading economy

large logistics networks

Warp is social unlock.

12. Fluid & Gas Asteroid Rules

Fluid asteroids MUST:

share chunk alignment with GT fluid vein logic

expose pumpable fluid blocks

regenerate only via normal world expansion

Never infinite sources.

13. Exotic Ice Design Intent

Purpose:

midgame logistics breakthrough

replaces early water scarcity

rewards exploration

Must be rare but reliable in ice zones.

14. Performance Constraints

Target server:

100 players

Generation rules:

async structure placement

no recursive chunk loads

no neighbor chunk forcing during gen

Maximum generation budget:

≤5 ms per chunk average
15. Save Data Requirements

Persistent data files:

/data/deepspace/
   bubbles.dat
   crash_sites.dat
   reserved_regions.dat

Each entry includes:

UUID

center coords

seed modifier

generation version

16. Versioning & Future Updates

All generated objects must include:

generationVersion

Allows migration scripts later.

17. Failure Recovery Rules

If crash generation fails:

Rollback reservation.

Retry new coordinate.

Never spawn player before structure success.

18. Compatibility Requirements

Must NOT break:

AE2 chunk loading

GregTech multiblocks

Ad Astra oxygen mechanics

FTBChunks claiming

All hooks must use public APIs only.

19. Testing Scenarios (Required)

Implementation considered complete only if:

Scenario A

New player joins populated server → isolated spawn works.

Scenario B

100 players generated → no overlaps.

Scenario C

8 plants sustain oxygen indefinitely.

Scenario D

Water freezes correctly in vacuum.

Scenario E

Distance tiers generate correct ores.

Scenario F

Claimed chunks never overwritten.

20. Balancing Targets (Reference Metrics)
Phase	Duration
Survival bootstrap	1–2 hrs
Stable oxygen loop	3–5 hrs
MV expansion	10 hrs
Warp repair	40–60 hrs
Interplayer economy	80+ hrs
21. Non-Goals (Do NOT Implement)

Planet terrain

Natural atmosphere zones

Infinite water blocks

Random player respawns

Shared starter base

22. Final Implementation Definition

The project is complete when:

Every player owns a personal crash asteroid.

Oxygen is engineerable but never trivial early.

Water is initially scarce but later industrialized.

Exploration distance equals technological advancement.

Multiplayer economy emerges naturally.

END OF SUPPLEMENTAL SPECIFICATION