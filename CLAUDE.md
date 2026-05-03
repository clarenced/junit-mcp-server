# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build fat/shaded jar (required — thin jar will fail at runtime)
mvn package

# Run the server against a target project
java -jar target/junit-mcp-server-1.0-SNAPSHOT.jar /absolute/path/to/gradle/project
```

This is a MCP server written on top of the JUnit Api. Instead of the LLM relying on command line tool (maven, gradle) to run tests, 
the MCP provides a set uniques tools on top of the JUnit API to discover tests, launch tests.

## MCP Server Development

- The JUnit MCP server and fixture projects run on **independent Java versions** — do not change fixture Java versions to match the server or vice versa.
- When MCP tools fail, debug WHY they failed rather than falling back to direct Bash/Gradle commands. The goal is to make the MCP tools work, not to get results by other means.
- The server **must** be packaged as a fat/shaded jar (maven-shade-plugin is configured). A thin jar will fail at startup due to missing dependencies.

## Architecture

The server is a single class for now (`App.java`) that acts as a stdio-based MCP server using the MCP Java SDK.

**Startup flow:**
1. Takes a Gradle project path as `args[0]`
2. Extracts `testRuntimeClasspath` from the target project by running `./gradlew -q --init-script <tmpfile> printClasspath` (the init script is bundled at `src/main/resources/init.gradle`)
3. Loads test classes + main classes + the full classpath into a `URLClassLoader` parented to `App.class.getClassLoader()`
4. Registers MCP tools and blocks on the stdio transport

**Classpath loading is critical for JUnit discovery.** Before each JUnit launcher call, the code sets `Thread.currentThread().setContextClassLoader(urlClassLoader)` — this is how JUnit's `LauncherFactory` discovers the `TestEngine` via `ServiceLoader`. If this is missing or the classloader doesn't include `junit-jupiter-engine`, discovery returns empty.

**Currently only Gradle projects are supported.** The `detectClassPath` method hard-codes `./gradlew` and the `testRuntimeClasspath` configuration. Maven support is not yet implemented.

**Test class resolution** uses `DiscoverySelectors.selectClasspathRoots(Set.of(testClassPath))` where `testClassPath` is hardcoded to `build/classes/java/test` relative to the project root. The compiled classes must already exist before the server is started.

## Exposed MCP Tools

| Tool | Description |
|------|-------------|
| `list_tests_classes` | Discovers all test classes under `build/classes/java/test` |
| `list_tests_in_classes` | Lists all test methods in a given fully-qualified class name |
| `run_test_method_in_class` | (commented out) Runs a specific test method |

Both active tools return a JSON array of `TestDiscovery` records: `{displayName, type, container, isTest}`.

## Test Fixtures

Three calculator fixture projects live in `src/test/resources/test-fixtures/`. They are **intentionally on different build configurations** to validate the MCP server against varied setups — do not normalize their versions:

- `calculator-gradle-project`: Pure Gradle project
- `calculator-maven-project`: Pure Maven project  
- `calculator-project`: Hybrid Spring Boot REST project (both Gradle and Maven wrappers)

## Debugging Checklist

When `list_tests_classes` returns empty or classpath errors, check in order:
1. Jar is the shaded/fat jar (`dependency-reduced-pom.xml` present after build)
2. `junit-jupiter-engine` is bundled in the jar (needed for `ServiceLoader` TestEngine discovery)
3. Target project has been compiled (`build/classes/java/test` exists and is non-empty)
4. Server is using an absolute path to the target project
5. `Thread.currentThread().setContextClassLoader(urlClassLoader)` is called before the `LauncherFactory` call
6. Check `/tmp/tests.log` for detailed error output
