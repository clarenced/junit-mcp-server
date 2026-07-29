package io.github.clarenced.spi;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Path;

public interface Project {
    boolean supports(Path path);
    URLClassLoader getClassLoader(Path projectRoot) throws IOException, InterruptedException;
    URLClassLoader getClassLoader(Path projectRoot, String classpath) throws IOException;
    Path getTestClassesPath(Path projectRoot);
    Path getMainClassesPath(Path projectRoot);
    String detectClasspath(Path projectRoot) throws IOException, InterruptedException;
    int priority();
}
