package io.github.clarenced.core;

import io.github.clarenced.spi.Project;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.ServiceLoader;

public class ProjectDetectors {

    public static Project detect(Path path) {
        return ServiceLoader.load(Project.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .filter(project -> project.supports(path))
                .max(Comparator.comparing(Project::priority))
                .orElseThrow(NoProjectFoundException::new);
    }
}
