package io.github.clarenced.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class MavenProject extends AbstractProject {

    private static final boolean IS_WINDOWS =
            System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");

    @Override
    public boolean supports(Path path) {
        return Files.exists(path.resolve("pom.xml"));
    }

    @Override
    public int priority() {
        return 5;
    }

    @Override
    public Path getTestClassesPath(Path projectRoot) {
        return projectRoot.resolve("target/test-classes");
    }

    @Override
    public Path getMainClassesPath(Path projectRoot) {
        return projectRoot.resolve("target/classes");
    }

    @Override
    public String detectClasspath(Path projectRoot) throws IOException, InterruptedException {
        Path outputFile = Files.createTempFile("junit-mcp-maven-cp", ".txt");
        try {
            List<String> command = new ArrayList<>(mavenCommand(projectRoot));
            command.addAll(List.of(
                    "-q", "-B", "dependency:build-classpath",
                    "-Dmdep.outputFile=" + outputFile.toAbsolutePath(),
                    "-DincludeScope=test"));

            Process process = new ProcessBuilder(command)
                    .directory(projectRoot.toFile())
                    // Maven output must never pollute the MCP server's stdout (JSON-RPC channel)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();

            // Drain stderr concurrently so a full pipe buffer cannot block Maven
            CompletableFuture<String> stderr = CompletableFuture.supplyAsync(() -> {
                try {
                    return new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    return "";
                }
            });

            if (!process.waitFor(5, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                throw new RuntimeException("Maven timed out after 5 minutes: " + String.join(" ", command));
            }
            if (process.exitValue() != 0) {
                throw new RuntimeException("Maven exited with code " + process.exitValue()
                        + " (" + String.join(" ", command) + "): " + stderr.join());
            }

            String classpath = Files.readString(outputFile).trim();
            if (classpath.isEmpty()) {
                throw new RuntimeException("Maven dependency:build-classpath returned an empty classpath");
            }
            return classpath;
        } finally {
            Files.deleteIfExists(outputFile);
        }
    }

    /**
     * Resolution order:
     * 1. The project's Maven wrapper (mvnw.cmd on Windows, mvnw elsewhere)
     * 2. Maven from MAVEN_HOME / M2_HOME
     * 3. mvn found on the PATH
     */
    private List<String> mavenCommand(Path projectRoot) {
        if (IS_WINDOWS) {
            Path wrapper = projectRoot.resolve("mvnw.cmd");
            if (Files.isRegularFile(wrapper)) {
                return List.of("cmd.exe", "/c", wrapper.toAbsolutePath().toString());
            }
        } else {
            Path wrapper = projectRoot.resolve("mvnw");
            if (Files.isRegularFile(wrapper)) {
                // Going through sh avoids "Permission denied" when the +x bit is missing
                return Files.isExecutable(wrapper)
                        ? List.of(wrapper.toAbsolutePath().toString())
                        : List.of("sh", wrapper.toAbsolutePath().toString());
            }
        }

        for (String env : List.of("MAVEN_HOME", "M2_HOME")) {
            String home = System.getenv(env);
            if (home != null && !home.isBlank()) {
                Path mvn = Path.of(home, "bin", IS_WINDOWS ? "mvn.cmd" : "mvn");
                if (Files.isRegularFile(mvn)) {
                    return launcher(mvn);
                }
            }
        }

        Path fromPath = findInPath(IS_WINDOWS ? "mvn.cmd" : "mvn");
        if (fromPath != null) {
            return launcher(fromPath);
        }

        throw new IllegalStateException(
                "Maven not found: no wrapper (mvnw/mvnw.cmd) in " + projectRoot
                        + ", no MAVEN_HOME/M2_HOME, and no 'mvn' on the PATH");
    }

    private static List<String> launcher(Path executable) {
        return IS_WINDOWS
                ? List.of("cmd.exe", "/c", executable.toString())
                : List.of(executable.toString());
    }

    private static Path findInPath(String executable) {
        String path = System.getenv("PATH");
        if (path == null) return null;
        for (String dir : path.split(File.pathSeparator)) {
            if (dir.isBlank()) continue;
            try {
                Path candidate = Path.of(dir.trim()).resolve(executable);
                if (Files.isRegularFile(candidate)) return candidate;
            } catch (RuntimeException ignored) {
                // malformed PATH entry
            }
        }
        return null;
    }
}
