package io.github.clarenced.tools;

import io.github.clarenced.core.ManagedClassLoader;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;
import java.util.Map;

/**
 * MCP tool that fully reloads the target project's classpath and rebuilds the JUnit classloader.
 *
 * <p>Exposed as the {@code reload_project} MCP tool. Takes no parameters.
 *
 * <p>Recompiling an existing test/main class, or adding a new one, is already picked up
 * automatically on the next tool call — no need to call this tool for that. This tool exists for
 * changes that a cheap file-count/mtime check on the compiled output directories cannot detect,
 * most notably a new or updated dependency (e.g. an edit to {@code build.gradle}/{@code pom.xml}).
 * Call it after such a change so the classpath is re-resolved by the build tool
 * (gradlew/mvnw) before the next discovery or run.
 *
 * <p>Example output on success:
 * <pre>{@code
 * Project reloaded successfully.
 * }</pre>
 */
public record ReloadProjectTool(ManagedClassLoader classLoader) implements McpTool {

    @Override
    public McpServerFeatures.SyncToolSpecification specification() {
        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder()
                .name("reload_project")
                .description("Reload the project's classpath and rebuild the JUnit classloader. " +
                        "Call after adding a new dependency; routine recompiles are picked up automatically.")
                .inputSchema(new McpSchema.JsonSchema("object", Map.of(), List.of(), null, null, null))
                .build())
            .callHandler((exchange, request) -> handle())
            .build();
    }

    private McpSchema.CallToolResult handle() {
        try {
            classLoader.reload();
            return McpTool.textResult("Project reloaded successfully.");
        } catch (Exception e) {
            return McpTool.errorResult(e.getMessage());
        }
    }
}