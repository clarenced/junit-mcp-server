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

public class JUnitMcpServer {

    private static final Logger LOG = LoggerFactory.getLogger(JUnitMcpServer.class);

    static void main(String[] args) throws IOException, InterruptedException {
        String projectPath = getProjectPath(args);
        LOG.info("Starting server at: {}", projectPath);

        // Step 1 — detect the build tool (Gradle or Maven) from the project directory structure.
        // ProjectDetectors uses ServiceLoader to find the highest-priority Project implementation
        // that claims to support the given path.
        Path projectDir = Path.of(projectPath).toAbsolutePath().normalize();
        Project project = ProjectDetectors.detect(projectDir);
        LOG.info("Detected project type: {}", project.getClass().getSimpleName());

        // Step 2 — build the URLClassLoader for the target project.
        // This runs the build tool (gradlew/mvnw) to extract the testRuntimeClasspath, then
        // loads test classes, main classes, and all dependencies into a single URLClassLoader.
        // The classloader is parented to JUnitMcpServer's classloader so JUnit's ServiceLoader
        // can still find the TestEngine (junit-jupiter-engine) bundled inside this jar.
        URLClassLoader urlClassLoader = project.getClassLoader(projectDir);
        Path testClassPath = project.getTestClassesPath(projectDir);

        // Step 3 — create the JUnitRunner, which owns the execution lifecycle:
        // set context classloader → open LauncherSession → discover → execute → collect report.
        JUnitRunner runner = new JUnitRunner(urlClassLoader);

        // Step 4 — create the MCP server over stdio transport.
        McpSyncServer junitServer = McpServer.sync(new StdioServerTransportProvider(McpJsonDefaults.getMapper()))
                .serverInfo("junit-mcp-server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .prompts(false)
                        .logging()
                        .build())
                .build();

        // Step 5 — register all tools. Each tool is self-contained: it holds its own
        // name, description, input schema, and handler logic.
        List.of(
                new ListTestClassesTool(testClassPath, urlClassLoader),
                new ListTestsInClassTool(urlClassLoader),
                new RunTestMethodInClassTool(runner),
                new RunTestsInClassTool(runner)
        ).forEach(tool -> junitServer.addTool(tool.specification()));

        // Step 6 — register a shutdown hook so the MCP server is cleanly closed on JVM exit.
        Runtime.getRuntime().addShutdownHook(new Thread(junitServer::close));
    }

    private static String getProjectPath(String[] args) {
        if (args.length == 0 || args[0] == null || args[0].isBlank()) {
            throw new IllegalArgumentException("Please specify project path");
        }
        return args[0];
    }
}
