package io.github.clarenced.tools;

import io.github.clarenced.core.JUnitDiscoverer;
import io.github.clarenced.model.TestDiscovery;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import tools.jackson.databind.ObjectMapper;

import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * MCP tool that discovers all JUnit test classes in the project's compiled test output directory.
 *
 * <p>Exposed as the {@code list_tests_classes} MCP tool. Takes no parameters and returns a JSON
 * array of {@link io.github.clarenced.model.TestDiscovery} objects, one per test class found.
 *
 * <p>Example output for a project containing {@code CalculatorTest}:
 * <pre>{@code
 * [
 *   {"displayName": "Calculator Tests", "type": "CLASS", "container": true, "test": false}
 * ]
 * }</pre>
 *
 * <p>This tool should be called first to discover what test classes are available before
 * invoking {@link ListTestsInClassTool} or {@link RunTestsInClassTool}.
 */
public record ListTestClassesTool(Path testClassPath, JUnitDiscoverer discoverer) implements McpTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public ListTestClassesTool(Path testClassPath, URLClassLoader discoverer) {
        this(testClassPath, new JUnitDiscoverer(discoverer));
    }

    @Override
    public McpServerFeatures.SyncToolSpecification specification() {
        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder()
                .name("list_tests_classes")
                .description("List all test classes in the project.")
                .inputSchema(new McpSchema.JsonSchema("object", Map.of(), List.of(), null, null, null))
                .build())
            .callHandler((exchange, request) -> handle())
            .build();
    }

    private McpSchema.CallToolResult handle() {
        try {
            List<TestDiscovery> discoveries = discoverer.discoverAll(testClassPath);
            return McpTool.textResult(MAPPER.writeValueAsString(discoveries));
        } catch (Exception e) {
            return McpTool.errorResult(e.getMessage());
        }
    }
}
