# Contributing to Still Wander

## Development environment

- Java 21
- Minecraft 1.21.11
- Fabric Loader and Fabric API versions declared in `gradle.properties`

Build with:

```powershell
.\gradlew.bat clean build
```

Run a development client with:

```powershell
.\gradlew.bat runClient
```

## Repository architecture

Still Wander 1.0 is built from the checksummed binary baseline in `vendor/`. Gradle remaps that runtime for development and patches only release identifiers and user-facing branding. Camera behavior changes should be made only with a reproducible source or bytecode-level implementation and must be tested in Minecraft before release.

Do not commit Gradle caches, generated builds, runtime worlds, logs, crash reports, account tokens, or launcher credentials.

## Pull requests

1. Explain the user-facing problem and proposed behavior.
2. Keep unrelated formatting or dependency changes out of the pull request.
3. Run `gradlew.bat clean build`.
4. For camera changes, test automatic activation, `B`, `N`, normal input exit, damage exit, a confined scene, and an outdoor scene.
5. Include relevant logs and screenshots without personal account information.

By contributing, you confirm that you have the right to submit the contribution. Acceptance does not change the repository's license unless the owner agrees in writing.
