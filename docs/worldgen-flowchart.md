# Deep Space Worldgen — Execution Flowchart

Defines the **exact execution order**, threading rules, caching layers,
and failure containment ("crash bubbles") for deterministic space world generation.

This document is machine-oriented and intended to be consumed directly
by automation or coding AIs.

---

# 0. DESIGN GOALS

Worldgen must be:

- Deterministic per seed
- Async-safe
- Crash-contained (never corrupt chunk pipeline)
- Registry-driven (no hardcoded planets/asteroids)
- Cache-efficient
- Incrementally extensible

---

# 1. HIGH-LEVEL PIPELINE

Chunk generation executes in ordered **ChunkStatus phases**:
