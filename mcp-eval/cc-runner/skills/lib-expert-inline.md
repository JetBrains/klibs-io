# KMP Klibs Version Guide (inline)

Use `klibs.io` MCP as the primary authority for Kotlin Multiplatform library discovery
and version selection. Do all lookups yourself in this conversation.

## Tools
- `searchProjects(query, platforms, targetFilters)` — find KMP projects by keywords /
  platforms / targets. `platforms`: common, jvm, androidJvm, native, wasm, js.
  `targetFilters`: Map<TargetGroup, Set<target>>, e.g. `{"JVM":["17"],"IOS":[]}`
  (empty set = any target in that group).
- `getLatestVersion(groupId, artifactId)` — latest + latest-stable for an exact GAV;
  returns a `packageFound` flag.

## Rules
1. Treat klibs.io as source of truth for versions and KMP support. Never invent a
   version number or coordinates.
2. `group:artifact` question → `getLatestVersion`. Capability / "which library" question
   → `searchProjects`. Translate platform/target words into the filter shape above.
3. Recommend the newest **stable** release unless the user explicitly wants pre-releases.
4. If a package is not found (`packageFound:false`), say so — do not fall back to memory.
5. When comparing, only compare libraries klibs shows as KMP-capable for the required
   targets. Keep a recommendation shortlist short; include the exact version.

## Response
```
<group>:<artifact>:<version>   (stable)
Targets: <relevant targets>
implementation("<group>:<artifact>:<version>")
```
