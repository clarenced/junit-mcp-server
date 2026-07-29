package io.github.clarenced.core;

import io.github.clarenced.spi.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Owns the lifecycle of the {@link URLClassLoader} used to discover and run tests in the target
 * project, and keeps it fresh as the project's compiled output changes.
 *
 * <p>A {@link URLClassLoader} can never "unload" or re-read an already-loaded class, and its jar
 * list is fixed at construction time. This class works around that by:
 * <ul>
 *   <li>caching the classpath string resolved by the build tool (gradlew/mvnw) so it doesn't have
 *       to be re-resolved on every access, and</li>
 *   <li>on every {@link #get()} call, cheaply comparing a snapshot (file count + max last-modified
 *       time) of the project's compiled {@code test}/{@code main} class directories against the
 *       last known snapshot, rebuilding the classloader (reusing the cached classpath, no
 *       gradle/mvn invocation) whenever it has changed.</li>
 * </ul>
 *
 * <p>A brand new dependency (e.g. an edit to {@code build.gradle}/{@code pom.xml}) is only picked
 * up via {@link #reload()}, which re-invokes the build tool to re-resolve the classpath.
 */
public class ManagedClassLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ManagedClassLoader.class);

    private final Project project;
    private final Path projectRoot;

    private String cachedClasspath;
    private URLClassLoader classLoader;
    private DirSnapshot snapshot;

    public ManagedClassLoader(Project project, Path projectRoot) throws IOException, InterruptedException {
        this.project = project;
        this.projectRoot = projectRoot;
        this.cachedClasspath = project.detectClasspath(projectRoot);
        this.classLoader = project.getClassLoader(projectRoot, cachedClasspath);
        this.snapshot = snapshot();
    }

    /**
     * Returns the current classloader, transparently rebuilding it first if the project's
     * compiled {@code test}/{@code main} classes have changed since the last access (new or
     * recompiled classes). Does not re-invoke the build tool — use {@link #reload()} for that.
     */
    public synchronized URLClassLoader get() {
        DirSnapshot current = snapshot();
        if (!current.equals(snapshot)) {
            LOG.info("Detected compiled class changes, rebuilding classloader");
            rebuild(cachedClasspath);
            snapshot = current;
        }
        return classLoader;
    }

    /**
     * Fully reloads the project: re-invokes the build tool (gradlew/mvnw) to re-resolve the
     * classpath, then rebuilds the classloader. Call this after adding a new dependency, since
     * routine recompiles of existing/new classes are already picked up automatically by
     * {@link #get()}.
     */
    public synchronized void reload() throws IOException, InterruptedException {
        cachedClasspath = project.detectClasspath(projectRoot);
        rebuild(cachedClasspath);
        snapshot = snapshot();
    }

    public synchronized void close() {
        try {
            classLoader.close();
        } catch (IOException e) {
            LOG.warn("Failed to close classloader", e);
        }
    }

    private void rebuild(String classpath) {
        try {
            URLClassLoader newLoader = project.getClassLoader(projectRoot, classpath);
            URLClassLoader old = classLoader;
            classLoader = newLoader;
            old.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to rebuild classloader", e);
        }
    }

    private DirSnapshot snapshot() {
        DirSnapshot testSnapshot = snapshotOf(project.getTestClassesPath(projectRoot));
        DirSnapshot mainSnapshot = snapshotOf(project.getMainClassesPath(projectRoot));
        return new DirSnapshot(
                testSnapshot.fileCount() + mainSnapshot.fileCount(),
                Math.max(testSnapshot.maxModifiedMillis(), mainSnapshot.maxModifiedMillis()));
    }

    private DirSnapshot snapshotOf(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return new DirSnapshot(0, 0);
        }
        try (var stream = Files.walk(dir)) {
            long[] count = {0};
            long[] maxModified = {0};
            stream.filter(Files::isRegularFile).forEach(file -> {
                count[0]++;
                try {
                    long modified = Files.getLastModifiedTime(file).toMillis();
                    if (modified > maxModified[0]) {
                        maxModified[0] = modified;
                    }
                } catch (IOException e) {
                    LOG.warn("Failed to read last-modified time for {}", file, e);
                }
            });
            return new DirSnapshot(count[0], maxModified[0]);
        } catch (IOException e) {
            LOG.warn("Failed to snapshot directory {}", dir, e);
            return new DirSnapshot(0, 0);
        }
    }

    private record DirSnapshot(long fileCount, long maxModifiedMillis) {
    }
}