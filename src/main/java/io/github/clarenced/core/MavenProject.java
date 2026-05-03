package io.github.clarenced.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class MavenProject extends AbstractProject {

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
    protected Path getMainClassesPath(Path projectRoot) {
        return projectRoot.resolve("target/classes");
    }

    @Override
    protected String detectClasspath(Path projectRoot) throws IOException, InterruptedException {
        Path outputFile = Files.createTempFile("junit-mcp-maven-cp", ".txt");

        String mvnw = Files.exists(projectRoot.resolve("mvnw")) ? "./mvnw" : "mvn";
        Process process = new ProcessBuilder(
                mvnw, "-q", "dependency:build-classpath",
                "-Dmdep.outputFile=" + outputFile.toAbsolutePath(),
                "-DincludeScope=test")
                .directory(projectRoot.toFile())
                .start();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            throw new RuntimeException("Maven exited with code " + exitCode + ": " + error);
        }

        String classpath = Files.readString(outputFile).trim();
        if (classpath.isEmpty()) {
            throw new RuntimeException("Maven dependency:build-classpath returned an empty classpath");
        }
        return classpath;
    }
}
