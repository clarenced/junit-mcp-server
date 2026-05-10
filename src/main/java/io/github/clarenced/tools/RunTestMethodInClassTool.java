package io.github.clarenced.tools;

import io.github.clarenced.core.JUnitRunner;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * MCP tool that runs a single JUnit test method in a given class and returns its result.
 *
 * <p>Exposed as the {@code run_test_method_in_class} MCP tool. Requires two parameters:
 * <ul>
 *   <li>{@code fullClassName} – fully qualified class name, e.g.
 *       {@code io.github.clarenced.calculator.CalculatorTest}</li>
 *   <li>{@code methodName} – name of the test method to execute, e.g. {@code testDivideByZero}</li>
 * </ul>
 *
 * <p>Returns a JSON array of {@link io.github.clarenced.model.TestClass} objects in the same
 * format as {@link RunTestsInClassTool}, but scoped to the single targeted method
 * ({@code total} will be 1).
 *
 * <p>Example: running {@code testDivideByZero} on {@code CalculatorTest} — the test asserts that
 * dividing by zero throws an {@code ArithmeticException}; when the implementation is correct the
 * report shows {@code passed=1, failed=0}.
 *
 * <p>Use {@link ListTestsInClassTool} to discover available method names before calling this tool.
 */
public record RunTestMethodInClassTool(JUnitRunner runner) implements McpTool {

    private static final Logger LOG = LoggerFactory.getLogger(RunTestMethodInClassTool.class);

    @Override
    public McpServerFeatures.SyncToolSpecification specification() {
        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder()
                .name("run_test_method_in_class")
                .description("Run a specific test method in a class")
                .inputSchema(new McpSchema.JsonSchema(
                    "object",
                    Map.of(
                        "fullClassName", Map.of("type", "string", "description", "The fully qualified className"),
                        "methodName", Map.of("type", "string", "description", "The method to run")),
                    List.of("fullClassName", "methodName"),
                    null, null, null))
                .build())
            .callHandler((exchange, request) -> handle(request))
            .build();
    }

    private McpSchema.CallToolResult handle(McpSchema.CallToolRequest request) {
        if (!request.arguments().containsKey("fullClassName")) {
            return McpTool.errorResult("fullClassName is empty");
        }
        if (!request.arguments().containsKey("methodName")) {
            return McpTool.errorResult("methodName is empty");
        }
        String fullClassName = request.arguments().get("fullClassName").toString();
        String methodName = request.arguments().get("methodName").toString();
        String report = runner.run(LauncherDiscoveryRequestBuilder.discoveryRequest()
            .selectors(DiscoverySelectors.selectMethod(fullClassName, methodName))
            .build()).report();
        LOG.debug("TestReport: {}", report);
        return McpTool.textResult(report);
    }
}