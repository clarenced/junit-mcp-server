package io.github.clarenced.spi;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Path;

public interface Project {
    boolean supports(Path path);
    URLClassLoader getClassLoader(Path projectRoot) throws IOException, InterruptedException;
    Path getTestClassesPath(Path projectRoot);
    int priority();
}
