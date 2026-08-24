# Still Wander distribution guide

## Release identity

| Field | Value |
| --- | --- |
| Project name | Still Wander |
| Suggested slug | `still-wander` |
| Release title | Still Wander 1.0.0 for Minecraft 1.21.11 |
| Version number | `1.0.0+1.21.11` |
| Mod loader | Fabric |
| Environment | Client-side |
| Minecraft version | 1.21.11 |
| Required dependency | Fabric API |
| Java version | 21 or newer |
| Release file | `build/libs/stillwander-1.0.0+1.21.11.jar` |

## Installation text

1. Install Fabric Loader for Minecraft 1.21.11.
2. Install Fabric API.
3. Remove any older Cinecraft or IdleLens JAR.
4. Place the Still Wander JAR in the Minecraft `mods` folder.
5. Start Minecraft and configure controls under **Still Wander**.

## Marketplace setup

Use the short and full descriptions from `BRANDING.md` and the release JAR as the only downloadable file. Platform-specific, copy-ready fields are in `marketplace/MODRINTH.md` and `marketplace/CURSEFORGE.md`.

For Modrinth and CurseForge, mark Fabric API as a required dependency. Mark the project as client-side only and select only Minecraft 1.21.11 for this release. Do not advertise support for another game version until a build has been compiled and runtime-tested against it.

The project currently uses an All Rights Reserved proprietary license. Marketplace settings must match `LICENSE` and `fabric.mod.json`.

### Modrinth policy restriction

Modrinth's August 2026 rules prohibit AI-generated images anywhere on a project page and may prevent primarily AI-generated projects from being listed publicly. Do not upload the current generated icon to Modrinth. Still Wander must declare AI-generated code and text, and should be submitted only after the owner has reviewed the current eligibility rules and is prepared for moderation to reject or restrict the project.

### CurseForge disclosure

CurseForge permits AI-modified showcase imagery when it is clearly disclosed and not used to misrepresent gameplay. If the current icon is used, keep the AI disclaimer in the prepared CurseForge description and add actual in-game screenshots before submission.

## Release checklist

- Run `gradlew.bat clean build` with Java 21.
- Confirm the JAR reports mod ID `stillwander` and version `1.0.0+1.21.11`.
- Launch a client with Fabric API and reach the main menu without entrypoint or mixin errors.
- Test `B`, `N`, `F7`, automatic AFK activation, normal input exit, and damage exit in a world.
- Record at least one outdoor, cave/interior, player, and entity-follow sequence for the listing video.
- Upload only `stillwander-1.0.0+1.21.11.jar`, not the development JAR under `build/devlibs`.
