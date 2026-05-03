# junit-mcp-server

An MCP (Model Context Protocol) server that exposes JUnit test discovery and execution as tools,
allowing AI coding agents to run tests directly without relying on shell commands.

## Why

When an LLM needs to run tests today, it typically shells out to `mvn test` or `./gradlew test`,
captures the raw terminal output, parses it as plain text, and forwards that interpretation to the
user. This approach has three problems: the output is verbose and wastes tokens, text parsing is
fragile and error-prone, and the result leaves room for misinterpretation.

**junit-mcp-server connects the LLM directly to the JUnit Platform API** instead of going through
the command line. Test results come back as structured JSON with exact pass/fail counts, failure
causes, and stack traces — nothing more. The benefits:

- **Fewer tokens consumed** — the agent receives only the data it needs, not pages of build logs
- **More precise responses** — structured output leaves no room for parsing ambiguity or misreading
- **No dependency on build tool output format** — works regardless of how Gradle or Maven formats its console output

## How it works

On startup the server detects the build tool (Gradle or Maven), extracts the full
`testRuntimeClasspath` by invoking the build tool once, loads everything into a `URLClassLoader`,
and registers MCP tools backed by the JUnit Platform Launcher API.
The agent never needs to call `gradle test` or `mvn test` — it calls the MCP tools instead.

## Requirements

- Java 21+
- The target project must be **pre-compiled** before starting the server (`./gradlew testClasses` or `mvn test-compile`)

## Build

```bash
mvn package
```

This produces a fat/shaded jar at `target/junit-mcp-server-1.0-SNAPSHOT.jar`.

## Available tools

| Tool | Description | Parameters |
|------|-------------|------------|
| `list_tests_classes` | Discovers all test classes in the project | — |
| `list_tests_in_classes` | Lists all test methods in a given class | `fullClassName` |
| `run_tests_in_class` | Runs all tests in a given class and returns a report | `fullClassName` |
| `run_test_method_in_class` | Runs a single test method and returns a report | `fullClassName`, `methodName` |

All run tools return a JSON report:

```json
[{
  "className": "com.example.CalculatorTest",
  "total": 3,
  "passed": 2,
  "failed": 1,
  "skipped": 0,
  "tests": [
    { "name": "shouldAdd", "status": "passed" },
    { "name": "shouldSubtract", "status": "passed" },
    { "name": "shouldDivide", "status": "failed", "failure": { "cause": "/ by zero", "stackTrace": "..." } }
  ]
}]
```

## Configuration

Replace `/absolute/path/to/your/project` with the actual path to the Gradle or Maven project
you want the agent to test, and `/absolute/path/to/junit-mcp-server.jar` with the path to the
built jar.

### Claude Code (CLI)

Add to your project's `.claude/mcp.json` or to the global `~/.claude/mcp.json`:

```json
{
  "mcpServers": {
    "junit": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/junit-mcp-server-1.0-SNAPSHOT.jar",
        "/absolute/path/to/your/project"
      ]
    }
  }
}
```

Or via the CLI:

```bash
claude mcp add junit -- java -jar /absolute/path/to/junit-mcp-server-1.0-SNAPSHOT.jar /absolute/path/to/your/project
```

### Claude Desktop (claude.ai)

Edit `~/Library/Application Support/Claude/claude_desktop_config.json` (macOS)
or `%APPDATA%\Claude\claude_desktop_config.json` (Windows):

```json
{
  "mcpServers": {
    "junit": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/junit-mcp-server-1.0-SNAPSHOT.jar",
        "/absolute/path/to/your/project"
      ]
    }
  }
}
```

### Cursor

Open **Settings → MCP** and add a new server entry, or edit `~/.cursor/mcp.json`:

```json
{
  "mcpServers": {
    "junit": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/junit-mcp-server-1.0-SNAPSHOT.jar",
        "/absolute/path/to/your/project"
      ]
    }
  }
}
```

### VS Code (GitHub Copilot / Continue)

For **Continue** (`~/.continue/config.json`):

```json
{
  "mcpServers": [
    {
      "name": "junit",
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/junit-mcp-server-1.0-SNAPSHOT.jar",
        "/absolute/path/to/your/project"
      ]
    }
  ]
}
```

For **GitHub Copilot** (VS Code `settings.json`):

```json
{
  "github.copilot.chat.mcp.servers": {
    "junit": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/junit-mcp-server-1.0-SNAPSHOT.jar",
        "/absolute/path/to/your/project"
      ]
    }
  }
}
```

### Windsurf

Edit `~/.codeium/windsurf/mcp_config.json`:

```json
{
  "mcpServers": {
    "junit": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/junit-mcp-server-1.0-SNAPSHOT.jar",
        "/absolute/path/to/your/project"
      ]
    }
  }
}
```

### OpenAI Codex CLI

```bash
codex --mcp-server "java -jar /absolute/path/to/junit-mcp-server-1.0-SNAPSHOT.jar /absolute/path/to/your/project"
```

Or in your Codex config file (`~/.codex/config.json`):

```json
{
  "mcpServers": [
    {
      "name": "junit",
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/junit-mcp-server-1.0-SNAPSHOT.jar",
        "/absolute/path/to/your/project"
      ]
    }
  ]
}
```

## Supported project types

| Build tool | Detection | Classpath extraction |
|------------|-----------|----------------------|
| Gradle | `gradlew` present | `./gradlew printClasspath` via init script |
| Maven | `pom.xml` present | `./mvnw dependency:build-classpath` |

If both `gradlew` and `pom.xml` are present (hybrid project), Gradle takes priority.

## Debugging

Logs are written to `/tmp/tests.log`. If tools return empty results, check in order:

1. The jar is the shaded fat jar (`dependency-reduced-pom.xml` exists after `mvn package`)
2. The target project has been compiled (`build/classes/java/test` or `target/test-classes` is non-empty)
3. The path passed to the server is absolute
4. `/tmp/tests.log` for detailed error output
