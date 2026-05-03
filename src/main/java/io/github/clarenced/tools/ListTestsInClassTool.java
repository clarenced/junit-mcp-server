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

public class ListTestsInClassTool implements McpTool {

    private static final Logger LOG = LoggerFactory.getLogger(ListTestsInClassTool.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JUnitDiscoverer discoverer;

    public ListTestsInClassTool(URLClassLoader classLoader) {
        this.discoverer = new JUnitDiscoverer(classLoader);
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
