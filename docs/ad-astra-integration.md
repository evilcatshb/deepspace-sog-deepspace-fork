# ad-astra-integration.md

## Deep Space Dimension — Ad Astra Integration Specification

This document defines how the **Deep Space dimension** integrates with **Ad Astra** mechanics so that all survival systems behave identically to official space environments.

The goal is:

> Deep Space must behave as a native Ad Astra space dimension.

---

# 1. Dimension Registration

## Dimension ID

```
deepspace:deep_space
```

## Required Dimension Tags

The dimension MUST be tagged as space:

```json
{
  "type": "minecraft:dimension",
  "effects": "ad_astra:space",
  "has_skylight": false,
  "ultrawarm": false,
  "natural": false
}
```

### Required Tag Injection

Add to tag:

```
ad_astra:space_dimensions
```

Result:

* Oxygen required
* Vacuum physics enabled
* Ad Astra equipment functional

---

# 2. Atmosphere Configuration

Deep Space atmosphere properties:

| Property        | Value          |
| --------------- | -------------- |
| Breathable      | ❌ No           |
| Pressure        | 0              |
| Temperature     | Extremely Cold |
| Oxygen Loss     | Enabled        |
| Fire Spread     | Disabled       |
| Water Stability | Frozen         |

Implementation hook:

```java
AtmosphereRegistry.register(
   DEEP_SPACE,
   AtmosphereType.VACUUM
);
```

---

# 3. Oxygen System Integration

## Player Oxygen Consumption

Deep Space uses Ad Astra’s native oxygen drain system.

No custom oxygen logic should be written.

Player oxygen drains when:

* not inside sealed oxygenated area
* not wearing full space suit

---

## Supported Oxygen Machines

Must function normally:

* Oxygen Generator
* Oxygen Distributor
* Oxygen Loader
* Oxygen Tank
* Oxygen Gear

Deep Space must NOT override machine behavior.

---

## Oxygen Distributor Compatibility

Distributor must:

* detect enclosed rooms normally
* consume oxygen at standard rate
* allow crop growth when active

---

# 4. Plant Growth Rules

Plants ONLY grow when:

```
oxygen_level > threshold
pressure_enabled == true
```

Hook into Ad Astra sealed-room detection:

```java
OxygenSystemAPI.isAreaOxygenated(pos)
```

If false:

* crops halt growth
* bonsai pots pause ticking

---

# 5. Gravity Configuration

Gravity multiplier:

```
0.18 – 0.25 (Moon-like)
```

Implementation:

```java
GravityAPI.registerGravity(DEEP_SPACE, 0.2f);
```

Effects:

* higher jump height
* slower fall speed
* jetpacks more efficient

---

# 6. Vacuum Fluid Behavior

## Water Exposure Rule

When water exists in vacuum:

```
water → ice
```

Conversion delay:

```
5 ticks
```

Trigger conditions:

```java
if atmosphere == VACUUM && fluid == WATER
```

Purpose:

* prevents infinite water exploits
* reinforces survival gameplay

---

## Gas Behavior

Gas asteroids use:

* normal GT/Ad Astra fluid blocks
* sealed pockets only

No custom gas physics required.

---

# 7. Temperature Effects

Optional (recommended):

Players without suit experience:

* freezing damage
* slower mining speed

Hook:

```java
TemperatureSystem.applyColdExposure(player);
```

---

# 8. Space Suit Compatibility

All Ad Astra suits must work automatically:

| Suit        | Expected Behavior   |
| ----------- | ------------------- |
| Space Suit  | Prevent oxygen loss |
| Jet Suit    | Normal thrust       |
| Oxygen Tank | Consumable oxygen   |

No overrides allowed.

---

# 9. Rocket & Vehicle Compatibility

Ad Astra rockets MUST:

* recognize Deep Space as valid destination
* allow launch & landing
* obey fuel requirements

Register planet-like entry:

```java
PlanetRegistry.register(
   "deep_space",
   orbitLevel = SPACE
);
```

Deep Space behaves as orbital region rather than planet surface.
If a player is landing in a ad astra rocket on deep_space, and the player doesn't have an establised crashed site on an asteroid, generate a new crashed site on an asteroid and let the rocket land above the site. if the player has an existing crashed site on an asteroid, and they are travelling to deep space in an ad astra rocket, they should be landing above their existing crashed site / asteroid

---

# 10. Oxygen Network Automation

GregTech automation may pipe fluids into:

```
Ad Astra Oxygen Generator
```

Required compatibility:

| Input        | Result            |
| ------------ | ----------------- |
| Water        | Oxygen production |
| Oxygen fluid | Distributor fuel  |

No conversion overrides required.

---

# 11. Chunk Safety Rules

Oxygen distributors MUST continue working when:

* chunk force-loaded
* player offline

Integration requirement:

Respect Ad Astra ticking systems.

---

# 12. Visual Integration

Skybox:

* starfield
* no sun/moon
* deep black fog

Lighting:

```
ambientLight = 0.02
```

---

# 13. Gameplay Outcome

After integration:

Players experience:

✅ vacuum survival
✅ oxygen engineering
✅ sealed habitat gameplay
✅ industrial life support loops
✅ Ad Astra machines functioning natively

Deep Space becomes a seamless extension of Ad Astra space.

---

# 14. Explicit Non-Goals

This integration MUST NOT:

* modify Ad Astra configs globally
* replace oxygen logic
* alter other planets
* inject mixins into unrelated systems

All hooks must be dimension-scoped.

---

END OF AD ASTRA INTEGRATION SPEC
