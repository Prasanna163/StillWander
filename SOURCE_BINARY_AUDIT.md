# Source / binary authentication audit

**Audit input commit:** `744b27ad390f8f2c28d983caab3e0317c57b4770`  
**Historical baseline commit:** `fde823d0dce0a105a1b95461f796b73d00e1e746`  
**Historical baseline JAR SHA-256:** `16D35350387917DA9CDACB8D4E96DB54E6DA3DB548032575974D767C78300C25`  
**Historical build.gradle SHA-256:** `5806AF05D1D84612D54FC7B345DBD168C4BBDDA4BC0FB0C3BF12D6403F275ACD`  
**Decompiler used for reproducibility check:** CFR `0.152`

## Verdict

- **Recovered source reproducibility: PASS.** All **50 / 50** committed Java files are byte-for-byte identical to a fresh CFR 0.152 decompilation of the repository's cryptographically verified historical Cinecraft 1.1.1 JAR.
- **Still Wander 1.0.0 + Minecraft 1.21.11 runtime-bytecode lineage: PASS.** All **65 / 65** project class files match the historical Cinecraft binary exactly after applying only the explicit branding constant-pool substitutions documented in the original `build.gradle`.
- **The published version JARs are not all byte-for-byte identical at the project-class level.** The per-version table below reports the exact class-byte match count. No claim of byte-for-byte identity is made for a version whose count is below the total.

### What “exact” means here

There are two different questions, and this audit keeps them separate:

1. **Source reconstruction exactness:** whether the committed readable `.java` tree is exactly reproducible from the preserved binary using the same decompiler version. This is a byte-for-byte file comparison.
2. **Release runtime exactness:** whether the compiled project `.class` files in a release JAR are byte-for-byte the same as the authenticated baseline, allowing only the branding string substitutions explicitly performed by the historical build recipe.

This does **not** claim that CFR recovered the original handwritten comments, formatting, or original local-variable names. Those source-level details are not present in Java bytecode. The repository source is an exact reproducible **decompilation of the preserved implementation**, not a claim that decompiled formatting is the lost original authoring text.

## Evidence chain

1. Git history contains `vendor/cinecraft-1.1.1+1.21.11.jar` at commit `fde823d0dce0a105a1b95461f796b73d00e1e746`.
2. Its SHA-256 is independently checked as `16D35350387917DA9CDACB8D4E96DB54E6DA3DB548032575974D767C78300C25` before any comparison runs.
3. The historical `build.gradle` names that JAR as `legacyBaseline`, extracts its `com/cinecraft/**/*.class` runtime implementation, rewrites only a defined list of branding strings in six classes, and packages those classes into Still Wander.
4. The present `src/main/java/com/cinecraft/` tree is regenerated from that exact binary with CFR 0.152 and compared file-for-file.
5. The 1.21.11 Still Wander JAR is compared class-for-class against the historical baseline after reproducing the historical branding rewrite at the Java class-file constant-pool level.
6. Every published version JAR is hashed in full and its `com/cinecraft/**/*.class` payload is compared byte-for-byte with the authenticated 1.21.11 runtime payload.

## Recovered source reproducibility

- committed Java files: **50**
- freshly decompiled Java files: **50**
- byte-for-byte identical files: **50**
- mismatched or missing files: **0**

## Authenticated 1.21.11 runtime-bytecode comparison

- historical baseline project classes: **65**
- Still Wander 1.21.11 project classes: **65**
- exact expected class matches after documented branding substitution: **65**
- mismatched or missing classes: **0**

## Published JAR inventory and exact comparisons

| Published JAR | SHA-256 | Bytes | Project classes | Exact class bytes vs 1.21.11 | Strict decompiled Java match vs recovered source* |
| --- | --- | ---: | ---: | ---: | ---: |
| `stillwander-1.0.0+1.20.1.jar` | `F8BB20918FF8CCE40878D87769BF0AB9547DB8EC106CEDBC16898931185B678F` | 592897 | 64 | 0/65 | 34/50 |
| `stillwander-1.0.0+1.20.4.jar` | `564A1374FDDE80234187D47E9F2239D106B805B2630761582A422F310175875D` | 592893 | 64 | 0/65 | 34/50 |
| `stillwander-1.0.0+1.21.1.jar` | `4F8205C6204636A0F31992541DE035B2E6C23AF14B18C726818CE3E2A7276E15` | 593013 | 64 | 0/65 | 37/50 |
| `stillwander-1.0.0+1.21.11.jar` | `5267B076EA5579D989A84F4D9AB49AF8833294B0BE7B2759E251F1173C4AC422` | 592457 | 65 | 65/65 | 50/50 |
| `stillwander-1.0.0+1.21.4.jar` | `EDCE5166D556BA4D65D84CC9713953DA071F4960091BB4D15F853AEBD6D71F4A` | 594138 | 65 | 0/65 | 38/50 |
| `stillwander-1.0.0+1.21.8.jar` | `40F31749E54C3256347416F3D4B177E3FF3B7E805522998CC5D0D42CB1B60198` | 594060 | 65 | 0/65 | 39/50 |
| `stillwander-1.0.0+26.1.1.jar` | `E07AB69A7339CF34652623FE032818238A839B140F932CD9C6E192107526161A` | 592974 | 65 | 0/65 | 24/50 |
| `stillwander-1.0.0+26.1.2.jar` | `996DFBD0BDD8BBC6BFD63679E75C6DC5C1F397342819F80FED9C6ADC3FBE10CF` | 592976 | 65 | 0/65 | 24/50 |
| `stillwander-1.0.0+26.1.jar` | `1DA0F0357EECD48FA9C16EFFD408CF6FAD79026C62AF738F1A088C9C34932157` | 592971 | 65 | 0/65 | 24/50 |
| `stillwander-1.0.0+26.2.jar` | `7ED574E71F712E36818152AC49AC7728BFAE50B50EEA146C0DEACA43FB7C90F1` | 593167 | 65 | 0/65 | 24/50 |

\*The strict Java comparison removes only the CFR header comment and reverses the documented Still-Wander branding strings. It deliberately does **not** normalize Minecraft-version-specific class/method/field identifiers. A result below 100% therefore means the source text is not byte-for-byte equivalent after those limited normalizations; it must not be described as exact source identity for that version.

## Per-version mismatch notes

### `stillwander-1.0.0+1.20.1.jar`

- project class mismatches/missing vs 1.21.11: **65**
- examples: `com/cinecraft/CinecraftClient.class`, `com/cinecraft/camera/ArcLengthSplinePath.class`, `com/cinecraft/camera/CameraPath.class`, `com/cinecraft/camera/CameraPose.class`, `com/cinecraft/camera/CinematicShot.class`, `com/cinecraft/camera/FovPath.class`, `com/cinecraft/camera/LookAt.class`, `com/cinecraft/camera/MovingShot.class`
- strict decompiled Java mismatches/missing: **16**
- examples: `CinecraftClient.java`, `camera/ArcLengthSplinePath.java`, `config/CinecraftConfig.java`, `config/CinecraftSettingsScreen.java`, `debug/CinecraftDebugHud.java`, `director/CinematicDirector.java`, `director/SceneScanner.java`, `director/SceneSubject.java`

### `stillwander-1.0.0+1.20.4.jar`

- project class mismatches/missing vs 1.21.11: **65**
- examples: `com/cinecraft/CinecraftClient.class`, `com/cinecraft/camera/ArcLengthSplinePath.class`, `com/cinecraft/camera/CameraPath.class`, `com/cinecraft/camera/CameraPose.class`, `com/cinecraft/camera/CinematicShot.class`, `com/cinecraft/camera/FovPath.class`, `com/cinecraft/camera/LookAt.class`, `com/cinecraft/camera/MovingShot.class`
- strict decompiled Java mismatches/missing: **16**
- examples: `CinecraftClient.java`, `camera/ArcLengthSplinePath.java`, `config/CinecraftConfig.java`, `config/CinecraftSettingsScreen.java`, `debug/CinecraftDebugHud.java`, `director/CinematicDirector.java`, `director/SceneScanner.java`, `director/SceneSubject.java`

### `stillwander-1.0.0+1.21.1.jar`

- project class mismatches/missing vs 1.21.11: **65**
- examples: `com/cinecraft/CinecraftClient.class`, `com/cinecraft/camera/ArcLengthSplinePath.class`, `com/cinecraft/camera/CameraPath.class`, `com/cinecraft/camera/CameraPose.class`, `com/cinecraft/camera/CinematicShot.class`, `com/cinecraft/camera/FovPath.class`, `com/cinecraft/camera/LookAt.class`, `com/cinecraft/camera/MovingShot.class`
- strict decompiled Java mismatches/missing: **13**
- examples: `CinecraftClient.java`, `config/CinecraftConfig.java`, `config/CinecraftSettingsScreen.java`, `debug/CinecraftDebugHud.java`, `director/CinematicDirector.java`, `director/SceneScanner.java`, `director/SceneSubject.java`, `director/ShotPlanner.java`

### `stillwander-1.0.0+1.21.11.jar`

- project class payload vs 1.21.11: **exact byte-for-byte match**
- strict decompiled Java vs recovered source: **exact match after branding reversal/header removal**

### `stillwander-1.0.0+1.21.4.jar`

- project class mismatches/missing vs 1.21.11: **65**
- examples: `com/cinecraft/CinecraftClient.class`, `com/cinecraft/camera/ArcLengthSplinePath.class`, `com/cinecraft/camera/CameraPath.class`, `com/cinecraft/camera/CameraPose.class`, `com/cinecraft/camera/CinematicShot.class`, `com/cinecraft/camera/FovPath.class`, `com/cinecraft/camera/LookAt.class`, `com/cinecraft/camera/MovingShot.class`
- strict decompiled Java mismatches/missing: **12**
- examples: `CinecraftClient.java`, `config/CinecraftConfig.java`, `config/CinecraftSettingsScreen.java`, `debug/CinecraftDebugHud.java`, `director/CinematicDirector.java`, `director/SceneScanner.java`, `director/SceneSubject.java`, `director/ShotPlanner.java`

### `stillwander-1.0.0+1.21.8.jar`

- project class mismatches/missing vs 1.21.11: **65**
- examples: `com/cinecraft/CinecraftClient.class`, `com/cinecraft/camera/ArcLengthSplinePath.class`, `com/cinecraft/camera/CameraPath.class`, `com/cinecraft/camera/CameraPose.class`, `com/cinecraft/camera/CinematicShot.class`, `com/cinecraft/camera/FovPath.class`, `com/cinecraft/camera/LookAt.class`, `com/cinecraft/camera/MovingShot.class`
- strict decompiled Java mismatches/missing: **11**
- examples: `CinecraftClient.java`, `config/CinecraftConfig.java`, `config/CinecraftSettingsScreen.java`, `debug/CinecraftDebugHud.java`, `director/CinematicDirector.java`, `director/SceneScanner.java`, `director/SceneSubject.java`, `director/ShotPlanner.java`

### `stillwander-1.0.0+26.1.1.jar`

- project class mismatches/missing vs 1.21.11: **65**
- examples: `com/cinecraft/CinecraftClient.class`, `com/cinecraft/camera/ArcLengthSplinePath.class`, `com/cinecraft/camera/CameraPath.class`, `com/cinecraft/camera/CameraPose.class`, `com/cinecraft/camera/CinematicShot.class`, `com/cinecraft/camera/FovPath.class`, `com/cinecraft/camera/LookAt.class`, `com/cinecraft/camera/MovingShot.class`
- strict decompiled Java mismatches/missing: **26**
- examples: `CinecraftClient.java`, `camera/ArcLengthSplinePath.java`, `camera/CameraPath.java`, `camera/CameraPose.java`, `camera/LookAt.java`, `camera/MovingShot.java`, `camera/PanPath.java`, `camera/RailPath.java`

### `stillwander-1.0.0+26.1.2.jar`

- project class mismatches/missing vs 1.21.11: **65**
- examples: `com/cinecraft/CinecraftClient.class`, `com/cinecraft/camera/ArcLengthSplinePath.class`, `com/cinecraft/camera/CameraPath.class`, `com/cinecraft/camera/CameraPose.class`, `com/cinecraft/camera/CinematicShot.class`, `com/cinecraft/camera/FovPath.class`, `com/cinecraft/camera/LookAt.class`, `com/cinecraft/camera/MovingShot.class`
- strict decompiled Java mismatches/missing: **26**
- examples: `CinecraftClient.java`, `camera/ArcLengthSplinePath.java`, `camera/CameraPath.java`, `camera/CameraPose.java`, `camera/LookAt.java`, `camera/MovingShot.java`, `camera/PanPath.java`, `camera/RailPath.java`

### `stillwander-1.0.0+26.1.jar`

- project class mismatches/missing vs 1.21.11: **65**
- examples: `com/cinecraft/CinecraftClient.class`, `com/cinecraft/camera/ArcLengthSplinePath.class`, `com/cinecraft/camera/CameraPath.class`, `com/cinecraft/camera/CameraPose.class`, `com/cinecraft/camera/CinematicShot.class`, `com/cinecraft/camera/FovPath.class`, `com/cinecraft/camera/LookAt.class`, `com/cinecraft/camera/MovingShot.class`
- strict decompiled Java mismatches/missing: **26**
- examples: `CinecraftClient.java`, `camera/ArcLengthSplinePath.java`, `camera/CameraPath.java`, `camera/CameraPose.java`, `camera/LookAt.java`, `camera/MovingShot.java`, `camera/PanPath.java`, `camera/RailPath.java`

### `stillwander-1.0.0+26.2.jar`

- project class mismatches/missing vs 1.21.11: **65**
- examples: `com/cinecraft/CinecraftClient.class`, `com/cinecraft/camera/ArcLengthSplinePath.class`, `com/cinecraft/camera/CameraPath.class`, `com/cinecraft/camera/CameraPose.class`, `com/cinecraft/camera/CinematicShot.class`, `com/cinecraft/camera/FovPath.class`, `com/cinecraft/camera/LookAt.class`, `com/cinecraft/camera/MovingShot.class`
- strict decompiled Java mismatches/missing: **26**
- examples: `CinecraftClient.java`, `camera/ArcLengthSplinePath.java`, `camera/CameraPath.java`, `camera/CameraPose.java`, `camera/LookAt.java`, `camera/MovingShot.java`, `camera/PanPath.java`, `camera/RailPath.java`

## Relevant repository locations

- Recovered source: `src/main/java/com/cinecraft/`
- Source provenance: `SOURCE_PROVENANCE.md`
- Published JARs: `versions/`
- Historical release commit: `fde823d0dce0a105a1b95461f796b73d00e1e746`
- Historical build recipe: `build.gradle` at the historical release commit
- Reproducibility workflow: `.github/workflows/restore-cinecraft-source.yml`
- This audit workflow: `.github/workflows/source-binary-audit.yml`

---

This report is generated from repository contents and Git history by deterministic hash, decompilation, class-file and byte comparisons. It intentionally distinguishes cryptographically/bytewise verified facts from claims that Java bytecode cannot support.
