package io.github.clarenced.tools;

import io.github.clarenced.core.JUnitDiscoverer;
import io.github.clarenced.model.TestDiscovery;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;

/**
 * MCP tool that lists all test methods declared in a given JUnit test class.
 *
 * <p>Exposed as the {@code list_tests_in_classes} MCP tool. Requires one parameter:
 * <ul>
 *   <li>{@code fullClassName} – fully qualified class name, e.g.
 *       {@code io.github.clarenced.calculator.CalculatorTest}</li>
 * </ul>
 *
 * <p>Returns a JSON array of {@link io.github.clarenced.model.TestDiscovery} objects, one per
 * test method found in the class.
 *
 * <p>Example output for {@code io.github.clarenced.calculator.CalculatorTest}:
 * <pre>{@code
 * [
 *   {"displayName": "Adding two positive numbers",              "type": "METHOD", "container": false, "test": true},
 *   {"displayName": "Adding a positive and a negative number",  "type": "METHOD", "container": false, "test": true},
 *   {"displayName": "Division by zero throws ArithmeticException", "type": "METHOD", "container": false, "test": true}
 * ]
 * }</pre>
 *
 * <p>Use {@link ListTestClassesTool} first to obtain the fully qualified class names available in
 * the project.
 */
public record ListTestsInClassTool(JUnitDiscoverer discoverer) implements McpTool {

    private static final Logger LOG = LoggerFactory.getLogger(ListTestsInClassTool.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public ListTestsInClassTool(URLClassLoader discoverer) {
        this(new JUnitDiscoverer(discoverer));
    }

    @Override
    public McpServerFeatures.SyncToolSpecification specification() {
        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder()
                .name("list_tests_in_classes")
                .description("List all tests that are in a specific class")
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
        String fullClassName = (String) request.arguments().get("fullClassName");
        if (fullClassName == null || fullClassName.isBlank()) {
            return McpTool.errorResult("fullClassName is empty");
        }
        LOG.info("Listing tests in class: {}", fullClassName);
        try {
            List<TestDiscovery> discoveries = discoverer.discoverInClass(fullClassName.trim());
            return McpTool.textResult(MAPPER.writeValueAsString(discoveries));
        } catch (Exception e) {
            LOG.error("Error listing tests in class: {}", fullClassName, e);
            throw new RuntimeException(e);
        }
    }
}
