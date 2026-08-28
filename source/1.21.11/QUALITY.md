# Still Wander quality gates

This file defines the development contract for the 1.21.11 cinematic director. A feature is not complete merely because it compiles.

## Automated gate

Run from the repository root:

```powershell
.\gradlew.bat -p source/1.21.11 clean test build --no-daemon --console=plain
```

The automated suite covers deterministic continuity rules, bounded state, camera-path invariants, diagnostic traces, and the scene-fixture catalogue. Development CI runs this gate on every relevant push and pull request. Published-JAR parity is a separate release-only workflow.

## Safety invariants

- Collision and visibility validation remains authoritative over editorial scoring.
- Camera and focus samples must be finite.
- FOV tracks must remain inside the planner's supported range.
- Damage, activity, manual controls, HUD restoration, and Dynamic FPS bypass semantics must not be changed by director work.
- The scanner must inspect loaded world state only and must not mutate the world.
- The existing planner remains the fallback when no continuity-aware candidate is safe.

## Fixed visual matrix

Milestone builds are reviewed in these environments before release:

1. Open plains or desert
2. Dense forest
3. Small interior
4. Cave
5. Ocean or coast
6. Village or player build
7. Nether
8. Moving nearby entity

For each environment, review smoothness, composition, continuity, variety, collision safety, FOV changes, and subject tracking. Visual review is a milestone gate; ordinary code changes use automated validation and do not require launching Minecraft.

## Performance baseline

The opt-in debug overlay reports scene-survey and shot-planning time in microseconds. The director also retains the latest 32 accepted `ShotTrace` records for diagnostic integrations. Compare median and worst observed values over a fifteen-minute session with the last accepted milestone. A regression must be investigated before release; raising scan or candidate budgets requires its own reviewed change.

## Phase 1 continuity contract

Continuity is a soft scoring layer. It may reward an adjacent scale change or a short subject continuation and penalize repeated scale, repeated orbit, large lens jumps, uncontrolled action-axis crossings, and overused subjects. It may never approve a candidate rejected by collision, visibility, or framing checks.
