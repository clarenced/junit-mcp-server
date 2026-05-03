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

public class RunTestMethodInClassTool implements McpTool {

    private static final Logger LOG = LoggerFactory.getLogger(RunTestMethodInClassTool.class);

    private final JUnitRunner runner;

    public RunTestMethodInClassTool(JUnitRunner runner) {
        this.runner = runner;
    }

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