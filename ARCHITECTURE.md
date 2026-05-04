# Architecture

## Overview

JUnit MCP Server is a stdio-based MCP server that exposes JUnit 5 test discovery and execution as MCP tools. An LLM client connects over stdin/stdout and calls these tools instead of shelling out to `gradle test` or `mvn test`.

## Package Structure

```
io.github.clarenced
├── JUnitMcpServer          # Entry point — wires everything together
├── spi/
│   └── Project             # Interface for build-tool adapters
├── core/
│   ├── AbstractProject     # Shared URLClassLoader construction logic
│   ├── GradleProject       # Gradle adapter (detects gradlew, priority=10)
│   ├── MavenProject        # Maven adapter (detects pom.xml, priority=5)
│   ├── ProjectDetectors    # ServiceLoader-based adapter selection
│   ├── JUnitDiscoverer     # Wraps JUnit Launcher for discovery
│   └── JUnitRunner         # Wraps JUnit Launcher for execution + report collection
├── model/
│   ├── TestReport          # Aggregates TestClass instances; serializes to JSON
│   ├── TestClass           # One test class: counts + list of Test results
│   ├── Test                # One test method: name + status + optional Failure
│   ├── Failure             # Cause message + stack trace
│   └── TestDiscovery       # Discovery result record: uniqueId, type, isContainer, isTest
└── tools/
    ├── McpTool             # Common interface / helper factory methods
    ├── ListTestClassesTool     # MCP tool: list_tests_classes
    ├── ListTestsInClassTool    # MCP tool: list_tests_in_class
    ├── RunTestsInClassTool     # MCP tool: run_tests_in_class
    └── RunTestMethodInClassTool # MCP tool: run_test_method_in_class
```

## Startup Flow

```
args[0] = /path/to/project
       │
       ▼
ProjectDetectors.detect()          ← ServiceLoader finds highest-priority Project
       │                              that returns supports(path) = true
       ▼
project.getClassLoader()
  ├── detectClasspath()            ← runs gradlew / mvn to get testRuntimeClasspath
  └── builds URLClassLoader        ← classpath JARs + test-classes dir + main-classes dir
       │                              parented to JUnitMcpServer's own classloader
       ▼
JUnitRunner(urlClassLoader)        ← owns launcher lifecycle
       │
       ▼
McpServer (stdio transport)
  └── registers tools              ← each tool holds its own schema + handler
```

## Project SPI

`Project` is a service-provider interface loaded via `ServiceLoader`. Implementations are declared in `META-INF/services/io.github.clarenced.spi.Project`.

| Implementation | Detection condition | Priority | Test classes path              | Classpath extraction                            |
|----------------|---------------------|----------|--------------------------------|-------------------------------------------------|
| `GradleProject`| `gradlew` present   | 10       | `build/classes/java/test`      | `./gradlew printClasspath` (via `init.gradle`)  |
| `MavenProject` | `pom.xml` present   | 5        | `target/test-classes`          | `mvn dependency:build-classpath`                |

When a project has both (e.g. the Spring Boot fixture), `GradleProject` wins because it has higher priority.

## ClassLoader Design

```
JUnitMcpServer.class.getClassLoader()   ← parent (contains JUnit Launcher API, MCP SDK, …)
        │
        └── URLClassLoader              ← child, built per target project
              ├── target project test-classes dir
              ├── target project main-classes dir
              └── all testRuntimeClasspath JARs
                    (includes junit-jupiter-engine for ServiceLoader TestEngine discovery)
```

`Thread.currentThread().setContextClassLoader(urlClassLoader)` must be called before any `LauncherFactory` call so JUnit's `ServiceLoader` can find `junit-jupiter-engine` inside the target project's classpath.

## Discovery

`JUnitDiscoverer` builds a `LauncherDiscoveryRequest` and walks the resulting `TestPlan` tree, collecting `TestDiscovery` records. Two modes:

- **`discoverAll(testClassPath)`** — scans an entire classpath root for test classes
- **`discoverInClass(fullClassName)`** — scans a single class by name

Each `TestDiscovery` record contains: `uniqueId`, `type` (CLASS / METHOD / …), `isContainer`, `isTest`.

## Execution & Reporting

`JUnitRunner` opens a `LauncherSession`, registers a `TestExecutionListener`, and executes the discovered plan. The listener delegates to `TestReport`:

```
TestExecutionListener events
  executionStarted(container)  → TestReport.reportExecutionStarted  → new TestClass added
  executionStarted(test)       → TestReport.reportTestStarted       → new Test added to TestClass
  executionFinished(test)      → TestReport.reportTestFinished      → Test.status set
  executionFinished(container) → TestReport.reportExecutionFinished → TestClass.status set
  executionSkipped(test)       → TestReport.reportTestSkipped       → Test.status = skipped
  executionSkipped(container)  → TestReport.reportExecutionSkipped  → TestClass.status = skipped
```

`TestReport` and `TestClass` use `CopyOnWriteArrayList` to be safe for concurrent test execution.

## Model / JSON Output

```
TestReport
└── List<TestClass>               (CopyOnWriteArrayList — thread-safe)
      ├── className   String       (JUnit uniqueId of the container)
      ├── total       long         (derived: tests.size())
      ├── passed      long         (derived: count SUCCESSFUL)
      ├── failed      long         (derived: count FAILED)
      ├── skipped     long         (derived: count SKIPPED)
      └── tests       List<Test>
            ├── name      String   (JUnit uniqueId of the test)
            ├── status    String   passed | failed | skipped | aborted
            └── failure   Failure? (only when status = failed)
                  ├── cause      String
                  └── stackTrace String
```

Example JSON:

```json
[{
  "className": "[engine:junit-jupiter]/[class:io.example.CalculatorTest]",
  "total": 3,
  "passed": 2,
  "failed": 1,
  "skipped": 0,
  "tests": [
    { "name": "[engine:junit-jupiter]/[class:io.example.CalculatorTest]/[method:shouldAdd()]", "status": "passed" },
    { "name": "[engine:junit-jupiter]/[class:io.example.CalculatorTest]/[method:shouldDivide()]", "status": "failed",
      "failure": { "cause": "/ by zero", "stackTrace": "..." } }
  ]
}]
```

## MCP Tools

| Tool name                  | Input                              | Returns              |
|----------------------------|------------------------------------|----------------------|
| `list_tests_classes`       | _(none)_                           | `TestDiscovery[]` JSON |
| `list_tests_in_class`      | `fullClassName: string`            | `TestDiscovery[]` JSON |
| `run_tests_in_class`       | `fullClassName: string`            | `TestReport` JSON    |
| `run_test_method_in_class` | `fullClassName`, `methodName`      | `TestReport` JSON    |

All tools implement the `McpTool` interface, which provides `specification()` returning a `SyncToolSpecification` (name + JSON schema + handler).

## Test Fixtures

Three independent calculator projects under `src/test/resources/test-fixtures/`. They are on **intentionally different** build configurations and Java versions — do not normalize them:

| Fixture                       | Build tool | Notes                                |
|-------------------------------|------------|--------------------------------------|
| `calculator-gradle-project`   | Gradle     | Plain Gradle project                 |
| `calculator-maven-project`    | Maven      | Plain Maven project                  |
| `calculator-project`          | Gradle+Maven | Spring Boot REST app (hybrid wrappers) |
