# Still Wander provenance

This document exists because the first public repository was too minimal to
show how the mod was made. It contained release JARs and artwork, but omitted
the source and began its visible Git history after the implementation already
existed. That was a poor publication decision and made reasonable verification
needlessly difficult.

## Development timeline

- **August 20, 2026:** development began under the working title **Cinecraft**.
  The initial Fabric project and camera implementation were created in a
  recorded, iterative development session.
- **August 20-21, 2026:** the mod was expanded through repeated builds and
  user testing: damage exit, smooth paths, environmental analysis, varied
  framing, entity tracking, independent FOV, Dynamic FPS handling, settings,
  and manual controls.
- **August 21, 2026:** the canonical `1.1.1+1.21.11` implementation was built.
- **August 24, 2026:** that implementation was renamed and published as
  **Still Wander 1.0.0**. Internal Java packages retain the original
  `com.cinecraft` working-title namespace; changing those names was unnecessary
  and would have made binary verification harder.
- **August 25, 2026:** the authored 1.1.1 source snapshot was restored from the
  original development record, rebuilt, verified, branded at source level, and
  added to this repository.

A short-lived earlier recovery commit on August 25 published CFR-decompiled
Java because the authored files were not in Git history. Once the original
development record was located and shown to reproduce the release exactly,
that decompiled tree and its regeneration workflow were superseded by the
authored, buildable source now under `source/1.21.11`. The decompilation commits
remain visible in Git history rather than being rewritten away.

## Verification performed

The restored authored source was first built without branding changes. It
reproduced the archived Cinecraft 1.1.1 JAR exactly:

`16D35350387917DA9CDACB8D4E96DB54E6DA3DB548032575974D767C78300C25`

All 65 compiled class entries were byte-for-byte identical.

The user-facing Still Wander branding was then applied directly in the source
and resources. A clean source build produced all 70 file entries inside the
published `stillwander-1.0.0+1.21.11.jar` byte-for-byte identically. The outer
ZIP/JAR hash can differ because ZIP timestamps and container metadata are not
normalized; the contents loaded by Fabric are identical.

The published 1.21.11 JAR has SHA-256:

`5267B076EA5579D989A84F4D9AB49AF8833294B0BE7B2759E251F1173C4AC422`

## Relationship to IDLE

[IDLE - Inactive Dynamic Lens Exploration](https://www.curseforge.com/minecraft/mc-mods/idle)
is a separate NeoForge mod. Still Wander is not a fork or port of IDLE. No IDLE
source or binary was consulted during the recorded August 20-21 Still Wander
development session. Both projects implement familiar cinematic-camera ideas,
so high-level overlap such as inactivity timers, orbiting cameras, collision
checks, smoothing, and manual toggles is expected.

For additional checking, the August 11 IDLE 1.0.2 binary and the Still Wander
1.21.11 binary were independently decompiled and compared. They use different
loaders, packages, class structures, and implementations; no distinctive exact
implementation lines were shared. Decompiled IDLE code is not included here.

## Authorship and AI disclosure

Prasanna Kulkarni directed the product, selected and refined the behavior,
tested builds in Minecraft, reported failures, and made the release decisions.
OpenAI Codex provided substantial implementation and tooling assistance during
the recorded development. The project does not claim that every line was typed
without generative-AI assistance.

The source is published for inspection and verification. See [LICENSE](LICENSE)
for the current usage terms.

