package io.github.clarenced.model;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestIdentifier;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class TestReport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final List<TestClass> testClasses = new CopyOnWriteArrayList<>();

    public void reportExecutionStarted(TestIdentifier testIdentifier) {
        if (testIdentifier.isContainer()) {
            testClasses.add(new TestClass(testIdentifier.getUniqueId()));
        }
    }

    public void reportExecutionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        if (testIdentifier.isContainer()) {
            testClasses.stream()
                .filter(testClass -> testClass.className().equals(testIdentifier.getUniqueId()))
                .findFirst()
                .ifPresent(testClass -> testClass.finished(testExecutionResult));
        }
    }

    public void reportExecutionSkipped(TestIdentifier testIdentifier) {
        if (testIdentifier.isContainer()) {
            TestClass testClass = new TestClass(testIdentifier.getUniqueId());
            testClass.markAsSkipped();
            testClasses.add(testClass);
        }
    }

    public void reportTestStarted(TestIdentifier testIdentifier) {
        if (testIdentifier.isTest() && testIdentifier.getParentId().isPresent()) {
            testClasses.stream()
                .filter(testClass -> Objects.equals(testClass.className(), testIdentifier.getParentId().get()))
                .findFirst()
                .ifPresent(testClass -> testClass.testIsStarted(testIdentifier.getUniqueId()));
        }
    }

    public void reportTestFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        if (testIdentifier.isTest() && testIdentifier.getParentId().isPresent()) {
            testClasses.stream()
                .filter(testClass -> Objects.equals(testClass.className(), testIdentifier.getParentId().get()))
                .findFirst()
                .ifPresent(testClass -> testClass.testIsFinished(testIdentifier.getUniqueId(), testExecutionResult));
        }
    }

    public void reportTestSkipped(TestIdentifier testIdentifier) {
        if (testIdentifier.isTest() && testIdentifier.getParentId().isPresent()) {
            testClasses.stream()
                .filter(testClass -> testClass.className().equals(testIdentifier.getParentId().get()))
                .findFirst()
                .ifPresent(testClass -> testClass.testIsSkipped(testIdentifier.getUniqueId()));
        }
    }

    public String report() {
        try {
            return MAPPER.writeValueAsString(testClasses);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize test report", e);
        }
    }
}
