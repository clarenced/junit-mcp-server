package io.github.clarenced;

import io.github.clarenced.core.JUnitRunner;
import io.github.clarenced.core.ProjectDetectors;
import io.github.clarenced.spi.Project;
import io.github.clarenced.tools.*;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;

public class App {

    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        String projectPath = getProjectPath(args);
        LOG.info("Starting server at: {}", projectPath);

        Path projectDir = Path.of(projectPath).toAbsolutePath().normalize();
        Project project = ProjectDetectors.detect(projectDir);
        LOG.info("Detected project type: {}", project.getClass().getSimpleName());

        URLClassLoader urlClassLoader = project.getClassLoader(projectDir);
        Path testClassPath = project.getTestClassesPath(projectDir);
        JUnitRunner runner = new JUnitRunner(urlClassLoader);

        McpSyncServer junitServer = McpServer.sync(new StdioServerTransportProvider(McpJsonDefaults.getMapper()))
                .serverInfo("junit-mcp-server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .prompts(false)
                        .logging()
                        .build())
                .build();

        List.of(
                new ListTestClassesTool(testClassPath, urlClassLoader),
                new ListTestsInClassTool(urlClassLoader),
                new RunTestMethodInClassTool(runner),
                new RunTestsInClassTool(runner)
        ).forEach(tool -> junitServer.addTool(tool.specification()));

        Runtime.getRuntime().addShutdownHook(new Thread(junitServer::close));
    }

    private static String getProjectPath(String[] args) {
        if (args.length == 0 || args[0] == null || args[0].isBlank()) {
            throw new IllegalArgumentException("Please specify project path");
        }
        return args[0];
    }
}