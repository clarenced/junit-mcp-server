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

public class ListTestClassesTool implements McpTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Path testClassPath;
    private final JUnitDiscoverer discoverer;

    public ListTestClassesTool(Path testClassPath, URLClassLoader classLoader) {
        this.testClassPath = testClassPath;
        this.discoverer = new JUnitDiscoverer(classLoader);
    }

    @Override
    public McpServerFeatures.SyncToolSpecification specification() {
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(McpSchema.Tool.builder()
                        .name("list_tests_classes")
                        .description("List all test classes")
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
