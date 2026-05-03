package io.github.clarenced.tools;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;

public interface McpTool {

    McpServerFeatures.SyncToolSpecification specification();

    static McpSchema.CallToolResult errorResult(String message) {
        return McpSchema.CallToolResult.builder()
                .isError(true)
                .content(List.of(new McpSchema.TextContent(message)))
                .build();
    }

    static McpSchema.CallToolResult textResult(String content) {
        return McpSchema.CallToolResult.builder()
                .content(List.of(new McpSchema.TextContent(content)))
                .build();
    }
}
