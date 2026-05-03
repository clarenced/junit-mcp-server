package io.github.clarenced.core;

import io.github.clarenced.App;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class GradleProject extends AbstractProject {

    @Override
    public boolean supports(Path path) {
        return Files.exists(path.resolve("gradlew"));
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public Path getTestClassesPath(Path projectRoot) {
        return projectRoot.resolve("build/classes/java/test");
    }

    @Override
    protected Path getMainClassesPath(Path projectRoot) {
        return projectRoot.resolve("build/classes/java/main");
    }

    @Override
    protected String detectClasspath(Path projectRoot) throws IOException, InterruptedException {
        InputStream is = App.class.getResourceAsStream("/init.gradle");
        Path initScript = Files.createTempFile("junit-mcp-init", ".gradle");
        Files.copy(is, initScript, StandardCopyOption.REPLACE_EXISTING);

        Process process = new ProcessBuilder("./gradlew", "-q", "--init-script", initScript.toString(), "printClasspath")
                .directory(projectRoot.toFile())
                .start();

        String classpath = new String(process.getInputStream().readAllBytes()).trim();
        int exitCode = process.waitFor();
        if (exitCode != 0 || classpath.isEmpty()) {
            String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            throw new RuntimeException("Gradle exited with code " + exitCode + ": " + error);
        }
        return classpath;
    }
}
