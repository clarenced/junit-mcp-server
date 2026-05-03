package io.github.clarenced.core;

import io.github.clarenced.model.TestDiscovery;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

public class JUnitDiscoverer {

    private final URLClassLoader classLoader;

    public JUnitDiscoverer(URLClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public List<TestDiscovery> discoverAll(Path testClassPath) {
        Thread.currentThread().setContextClassLoader(classLoader);
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.discoveryRequest()
                .selectors(DiscoverySelectors.selectClasspathRoots(Set.of(testClassPath)))
                .build();
        return discover(request);
    }

    public List<TestDiscovery> discoverInClass(String fullClassName) {
        Thread.currentThread().setContextClassLoader(classLoader);
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.discoveryRequest()
                .selectors(selectClass(fullClassName))
                .build();
        return discover(request);
    }

    private List<TestDiscovery> discover(LauncherDiscoveryRequest request) {
        List<TestDiscovery> discoveries = new ArrayList<>();
        try (LauncherSession session = LauncherFactory.openSession()) {
            TestPlan testPlan = session.getLauncher().discover(request);
            for (TestIdentifier root : testPlan.getRoots()) {
                for (TestIdentifier child : testPlan.getChildren(root)) {
                    collect(testPlan, child, discoveries);
                }
            }
        }
        return discoveries;
    }

    private void collect(TestPlan testPlan, TestIdentifier node, List<TestDiscovery> discoveries) {
        discoveries.add(new TestDiscovery(node.getUniqueId(), node.getType().name(), node.isContainer(), node.isTest()));
        for (TestIdentifier child : testPlan.getChildren(node)) {
            collect(testPlan, child, discoveries);
        }
    }
}
