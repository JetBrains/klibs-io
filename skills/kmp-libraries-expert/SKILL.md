---
name: kotlin-multiplatform-libraries-expert
description: Help Kotlin Multiplatform developers find KMP libraries and recommend latest/stable versions using klibs.io as the source of truth. Use when the user asks which KMP library to use, wants latest or stable versions, needs Gradle coordinates, wants to upgrade or audit KMP dependencies, or asks about KMP target support. Prefer klibs.io over memory or Maven snippets whenever version accuracy matters.
---

# KMP Klibs Version Guide

Use `klibs.io` as the primary authority for Kotlin Multiplatform library discovery and version selection.

## When To Use

- User asks for the latest stable version of a KMP library
- User wants Gradle coordinates for a KMP dependency
- User asks which KMP library to choose for a capability (storage, networking, logging, DI, database, UI, etc.)
- User wants to upgrade KMP dependencies or verify target support (iOS, Android, JVM, JS, Wasm, Kotlin/Native)
- User asks for KMP library recommendations where version accuracy matters

## Core Rules

1. Treat `klibs.io` as the source of truth for versions and KMP support.
2. Recommend the newest **stable** release unless the user explicitly wants prereleases (alpha, beta, rc, eap, etc.).
3. Prefer exact coordinates from MCP or `klibs.io/package/…` pages over guesses from README snippets.
4. If only a project page is available, use it to identify the project, then locate the package page for coordinates.
5. If a library is not on `klibs.io`, say so — do not present an unverified version as fact.
6. When the user asks for "latest", include the version number and release date if available.
7. When comparing libraries, compare only those `klibs.io` shows as KMP-capable for the required targets.

## Data Sources

### MCP (preferred)

`klibs.io` exposes an MCP server with two operations:

- **Search projects** — find KMP projects by keywords, platforms/targets, and Kotlin version. Returns project name, author, platforms, targets, packages (groupId, artifactId, description), and README.
- **Get latest version** — retrieve latest and latest stable versions for a `groupId:artifactId`. Returns version, build tool, build tool version, and Kotlin version.

MCP config:

```json
{
    "mcpServers": {
        "klibs": {
            "url": "http://localhost:8080/mcp"
        }
    }
}
```

Prefer MCP over web page lookups — it returns structured data and is faster.

### Web pages (fallback)

- **Project page**: `https://klibs.io/project/<owner>/<project>` — summary, tags, platform families, latest release, repo links.
- **Package page**: `https://klibs.io/package/<group>/<artifact>` — exact coordinates, latest release, release date, version history, target matrix, Gradle snippets, Kotlin/build tool version.

## Lookup Sequence

1. **Normalize** — `group:artifact` → package lookup; project name → project lookup; capability → search.
2. **Choose method** — use MCP tools if available, otherwise browse web pages.
3. **Check targets** — confirm the library supports the user's required targets.
4. **Get version** — capture exact version and release date. MCP returns both latest and latest stable directly; on web pages check version history if needed.
5. **Capture install details** — prefer Gradle Kotlin snippet.
6. **Respond** with verified data and a `klibs.io` link.

## Response Format

For version questions:

```text
Latest stable: <group>:<artifact>:<version>
Source: <klibs.io link>
Released: <date if available>
Targets: <relevant targets>

Gradle Kotlin:
implementation("<group>:<artifact>:<version>")
```

For recommendations: keep the shortlist short, explain target fit, include only verified versions.

## Failure Mode

If `klibs.io` does not contain the library: state it could not be verified, do not invent a version, and offer to search for an alternative on `klibs.io`.
