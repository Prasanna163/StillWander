# Source provenance

`Cinecraft` was the original development name of the project that is now published as **Still Wander**.

The first Still Wander GitHub release preserved the already-built Cinecraft 1.1.1 implementation as a vendored JAR while the public identifiers and branding were changed. The repository was later simplified to release downloads, which left the Java source tree absent from the public branch.

The Java files now under `src/main/java/com/cinecraft/` were recovered from the exact Cinecraft 1.1.1 binary that exists in this repository's own first release commit:

- historical commit: `fde823d0dce0a105a1b95461f796b73d00e1e746`
- historical path: `vendor/cinecraft-1.1.1+1.21.11.jar`
- SHA-256: `16D35350387917DA9CDACB8D4E96DB54E6DA3DB548032575974D767C78300C25`
- reconstruction tool: CFR 0.152

This recovery makes the implementation readable and auditable again. Because the original `.java` files were not present in this Git repository's history, decompilation cannot reproduce comments, formatting, or every original local-variable name exactly. The recovered files therefore represent the implementation contained in the project's own previously distributed Cinecraft binary, rather than a claim that decompiler formatting is the original handwritten formatting.

The `com.cinecraft` package and Cinecraft identifiers are intentionally preserved in this recovered tree because they document the project's pre-Still-Wander identity and make the lineage directly inspectable.
