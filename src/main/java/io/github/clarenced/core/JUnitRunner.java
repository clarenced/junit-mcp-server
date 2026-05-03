package io.github.clarenced.core;

import io.github.clarenced.model.TestReport;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.LauncherFactory;

import java.net.URLClassLoader;

public class JUnitRunner {

    private final URLClassLoader classLoader;

    public JUnitRunner(URLClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public TestReport run(LauncherDiscoveryRequest request) {
        Thread.currentThread().setContextClassLoader(classLoader);
        Listener listener = new Listener();
        try (LauncherSession session = LauncherFactory.openSession()) {
            Launcher launcher = session.getLauncher();
            launcher.registerTestExecutionListeners(listener);
            launcher.execute(launcher.discover(request));
        }
        return listener.testReport;
    }

    private static class Listener implements TestExecutionListener {

        private final TestReport testReport = new TestReport();

        @Override
        public void executionStarted(TestIdentifier testIdentifier) {
            if (testIdentifier.isContainer()) {
                testReport.reportExecutionStarted(testIdentifier);
            }
            if (testIdentifier.isTest()) {
                testReport.reportTestStarted(testIdentifier);
            }
        }

        @Override
        public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult result) {
            if (testIdentifier.isContainer()) {
                testReport.reportExecutionFinished(testIdentifier, result);
            }
            if (testIdentifier.isTest()) {
                testReport.reportTestFinished(testIdentifier, result);
            }
        }

        @Override
        public void executionSkipped(TestIdentifier testIdentifier, String reason) {
            if (testIdentifier.isContainer()) {
                testReport.reportExecutionSkipped(testIdentifier);
            }
            if (testIdentifier.isTest()) {
                testReport.reportTestSkipped(testIdentifier);
            }
        }
    }
}