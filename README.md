<p align="center">
  <img src="branding/stillwander-icon-source.png" width="360" alt="Still Wander cinematic landscape icon">
</p>

# Still Wander

**Stand still. See your world differently.**

[![Build](https://github.com/Prasanna163/StillWander/actions/workflows/build.yml/badge.svg)](https://github.com/Prasanna163/StillWander/actions/workflows/build.yml)
[![GitHub release](https://img.shields.io/github/v/release/Prasanna163/StillWander?display_name=tag)](https://github.com/Prasanna163/StillWander/releases)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-62B47A)
![Fabric](https://img.shields.io/badge/Loader-Fabric-DBD0B4)
![Environment](https://img.shields.io/badge/Environment-Client--side-5965F2)

Still Wander turns idle Minecraft moments into smooth, environment-aware cinematic camera sequences. It moves between player, entity, cave, interior, passage, forest, and landscape compositions instead of repeatedly orbiting the player.

## Features

- Automatic activation after the configured AFK delay.
- Immediate cinematic toggle with `B`.
- Manual cut to the next composition with `N`.
- Slow panning, tracking, static, close-detail, cave, and wide landscape shots.
- Environment-aware framing for open terrain and confined spaces.
- Smooth entity following and independent cinematic FOV changes.
- Configurable exit when the player takes damage.
- Compatibility hooks that keep cinematic rendering active through Minecraft's inactivity limiter and supported Dynamic FPS behavior.

## Requirements

- Minecraft Java Edition 1.21.11
- Fabric Loader 0.19.3 or newer
- Fabric API
- Java 21 or newer

Still Wander is a client-side mod. It does not need to be installed on a multiplayer server.

## Installation

1. Install Fabric Loader for Minecraft 1.21.11.
2. Install Fabric API.
3. Remove any older Cinecraft or IdleLens JAR from the instance.
4. Download the Still Wander JAR from [GitHub Releases](https://github.com/Prasanna163/StillWander/releases).
5. Place it in the instance's `mods` folder and start Minecraft.

## Controls

| Default key | Action |
| --- | --- |
| `B` | Start or stop the cinematic immediately |
| `N` | Cut to the next shot |
| `F7` | Open Still Wander settings |
| `F8` | Toggle uninterrupted camera-path capture |
| `F9` | Toggle the diagnostic director overlay |

The camera also starts automatically after the configured idle delay. Normal player input exits the cinematic, and damage exits it when **Exit on damage** is enabled.

Settings are stored in `config/stillwander.json`. Users upgrading from the previous test build can copy `config/cinecraft.json` to that filename while Minecraft is closed.

## Building

The project uses a checksummed Gradle wrapper and Java 21:

```powershell
.\gradlew.bat clean build
```

The distributable is created at `build/libs/stillwander-1.0.0+1.21.11.jar`.

This repository preserves the tested Cinecraft 1.1.1 runtime as a checksummed vendored baseline and applies the Still Wander release identity during the build. The legacy `com.cinecraft` Java package remains an internal binary-compatibility detail; the public Fabric ID is `stillwander`.

## Support and contribution

- Report reproducible defects through [GitHub Issues](https://github.com/Prasanna163/StillWander/issues).
- Read [SUPPORT.md](SUPPORT.md) before reporting installation or compatibility problems.
- Development guidance is in [CONTRIBUTING.md](CONTRIBUTING.md).
- Release history is in [CHANGELOG.md](CHANGELOG.md).

## Transparency and license

Still Wander was developed iteratively by Prasanna163 with generative-AI coding and publishing assistance. Its current branding icon is AI-generated. Details are recorded in [AI_DISCLOSURE.md](AI_DISCLOSURE.md).

Copyright © 2026 Prasanna Kulkarni. All rights reserved. See [LICENSE](LICENSE).
