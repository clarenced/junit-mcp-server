package io.github.clarenced.tools;

import io.github.clarenced.core.JUnitRunner;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasspathRoots;

/**
 * MCP tool that runs all JUnit tests in the project and returns a full test report.
 *
 * <p>Exposed as the {@code run_all_tests} MCP tool. Takes no parameters.
 *
 * <p>Returns a JSON array of {@link io.github.clarenced.model.TestClass} objects, one per test
 * class executed, each containing aggregated counts ({@code total}, {@code passed}, {@code failed},
 * {@code skipped}) and an array of individual {@link io.github.clarenced.model.Test} results.
 *
 * <p>Example output for a project containing {@code CalculatorTest} (8 tests, all passing):
 * <pre>{@code
 * [
 *   {
 *     "className": "[engine:junit-jupiter]/[class:io.github.clarenced.calculator.CalculatorTest]",
 *     "total": 8, "passed": 8, "failed": 0, "skipped": 0,
 *     "tests": [...]
 *   }
 * ]
 * }</pre>
 */
public record RunAllTestsTool(JUnitRunner runner, Path testClassPath) implements McpTool {

    private static final Logger LOG = LoggerFactory.getLogger(RunAllTestsTool.class);

    @Override
    public McpServerFeatures.SyncToolSpecification specification() {
        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder()
                .name("run_all_tests")
                .description("Run all tests in the project.")
                .inputSchema(new McpSchema.JsonSchema("object", Map.of(), List.of(), null, null, null))
                .build())
            .callHandler((exchange, request) -> handle())
            .build();
    }

    private McpSchema.CallToolResult handle() {
        String report = runner.run(LauncherDiscoveryRequestBuilder.discoveryRequest()
            .selectors(selectClasspathRoots(Set.of(testClassPath)))
            .build()).report();
        LOG.debug("TestReport: {}", report);
        return McpTool.textResult(report);
    }
}