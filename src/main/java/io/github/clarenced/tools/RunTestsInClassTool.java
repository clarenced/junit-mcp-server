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

public class RunTestsInClassTool implements McpTool {

    private static final Logger LOG = LoggerFactory.getLogger(RunTestsInClassTool.class);

    private final JUnitRunner runner;

    public RunTestsInClassTool(JUnitRunner runner) {
        this.runner = runner;
    }

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