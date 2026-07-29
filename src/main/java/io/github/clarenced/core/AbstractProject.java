package io.github.clarenced.core;

import io.github.clarenced.JUnitMcpServer;
import io.github.clarenced.spi.Project;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

public abstract class AbstractProject implements Project {

    @Override
    public URLClassLoader getClassLoader(Path projectRoot) throws IOException, InterruptedException {
        return getClassLoader(projectRoot, detectClasspath(projectRoot));
    }

    @Override
    public URLClassLoader getClassLoader(Path projectRoot, String classpath) throws IOException {
        Path testClasses = getTestClassesPath(projectRoot);
        Path mainClasses = getMainClassesPath(projectRoot);

        String[] entries = classpath.split(File.pathSeparator);
        URL[] urls = new URL[entries.length + 2];
        for (int i = 0; i < entries.length; i++) {
            urls[i] = Path.of(entries[i]).toUri().toURL();
        }
        urls[entries.length]     = testClasses.toFile().toURI().toURL();
        urls[entries.length + 1] = mainClasses.toFile().toURI().toURL();
        return new URLClassLoader(urls, JUnitMcpServer.class.getClassLoader());
    }

    @Override
    public abstract String detectClasspath(Path projectRoot) throws IOException, InterruptedException;

    @Override
    public abstract Path getMainClassesPath(Path projectRoot);
}
