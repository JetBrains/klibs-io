# klibs.io MCP Server

This module exposes [klibs.io](https://klibs.io) — the catalog of Kotlin Multiplatform
libraries — as an [MCP (Model Context Protocol)](https://modelcontextprotocol.io) server.
Once connected, your AI agent can search for Kotlin Multiplatform projects and look up the
latest published versions of packages directly from the klibs.io index.

## Endpoint

The server is hosted publicly and requires no authentication:

```
https://api.klibs.io/mcp
```

It speaks MCP over streamable HTTP (stateless transport), so any MCP client that supports
remote HTTP servers can connect by URL.

## Adding the Server to Your Agent

All configurations point the client at the same URL. Use the snippet that matches your tool.

### IntelliJ IDEA (AI Assistant)

Open **Settings | Tools | AI Assistant | Model Context Protocol (MCP)**, click **Add**,
and either fill in the fields manually or switch to **As JSON** and paste:

```json
{
  "mcpServers": {
    "klibs": {
      "url": "https://api.klibs.io/mcp"
    }
  }
}
```

Apply the settings — the `klibs` tools become available in the AI Assistant chat.

### Junie

Junie reads the same MCP configuration as the JetBrains AI Assistant. Open
**Settings | Tools | Junie | MCP Settings** (or **Settings | Tools | AI Assistant |
Model Context Protocol (MCP)**) and add:

```json
{
  "mcpServers": {
    "klibs": {
      "url": "https://api.klibs.io/mcp"
    }
  }
}
```

After applying, Junie can call the `klibs` tools while working on your tasks.

### Claude Code

```bash
claude mcp add --transport http klibs https://api.klibs.io/mcp
```

### Cursor

Add to `~/.cursor/mcp.json` (global) or `.cursor/mcp.json` (project):

```json
{
  "mcpServers": {
    "klibs": {
      "url": "https://api.klibs.io/mcp"
    }
  }
}
```

### VS Code

Add to `.vscode/mcp.json`:

```json
{
  "servers": {
    "klibs": {
      "type": "http",
      "url": "https://api.klibs.io/mcp"
    }
  }
}
```

## Verifying the Connection

After adding the server, ask your agent something that requires the index, for example:

> Find Kotlin Multiplatform libraries for JSON serialization that support iOS and JVM.

or

> What is the latest stable version of org.jetbrains.kotlinx:kotlinx-serialization-json?

The agent should invoke the `searchProjects` / `getLatestVersion` tools and answer using
live data from klibs.io.