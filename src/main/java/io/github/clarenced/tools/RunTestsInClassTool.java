package io.github.clarenced.tools;

import io.github.clarenced.core.JUnitRunner;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * MCP tool that runs all JUnit test methods in a given class and returns a detailed test report.
 *
 * <p>Exposed as the {@code run_tests_in_class} MCP tool. Requires one parameter:
 * <ul>
 *   <li>{@code fullClassName} – fully qualified class name, e.g.
 *       {@code io.github.clarenced.calculator.CalculatorTest}</li>
 * </ul>
 *
 * <p>Returns a JSON array of {@link io.github.clarenced.model.TestClass} objects, each containing
 * aggregated counts ({@code total}, {@code passed}, {@code failed}, {@code skipped}) and an array
 * of individual {@link io.github.clarenced.model.Test} results.
 *
 * <p>Example output after running {@code CalculatorTest} (all 8 tests passing):
 * <pre>{@code
 * [
 *   {
 *     "className": "[engine:junit-jupiter]/[class:io.github.clarenced.calculator.CalculatorTest]",
 *     "total": 8, "passed": 8, "failed": 0, "skipped": 0,
 *     "tests": [...]
 *   }
 * ]
 * }</pre>
 *
 * <p>Use {@link ListTestClassesTool} to discover class names before calling this tool.
 */
public record RunTestsInClassTool(JUnitRunner runner) implements McpTool {

    private static final Logger LOG = LoggerFactory.getLogger(RunTestsInClassTool.class);

    @Override
    public McpServerFeatures.SyncToolSpecification specification() {
        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder()
                .name("run_tests_in_class")
                .description("Run all tests in a class")
                .inputSchema(new McpSchema.JsonSchema(
                    "object",
                    Map.of("fullClassName", Map.of("type", "string", "description", "The fully qualified className")),
                    List.of("fullClassName"),
                    null, null, null))
                .build())
            .callHandler((exchange, request) -> handle(request))
            .build();
    }

    private McpSchema.CallToolResult handle(McpSchema.CallToolRequest request) {
        String fullClassName = request.arguments().get("fullClassName") != null
            ? request.arguments().get("fullClassName").toString().trim()
            : null;
        if (fullClassName == null || fullClassName.isBlank()) {
            return McpTool.errorResult("fullClassName is empty");
        }
        String report = runner.run(LauncherDiscoveryRequestBuilder.discoveryRequest()
            .selectors(selectClass(fullClassName))
            .build()).report();
        LOG.debug("TestReport: {}", report);
        return McpTool.textResult(report);
    }
}