package io.github.clarenced.model;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
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
            testClasses.add(new TestClass(testIdentifier.getUniqueId(), extractClassName(testIdentifier)));
        }
    }

    public void reportExecutionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        if (testIdentifier.isContainer()) {
            testClasses.stream()
                .filter(testClass -> testClass.uniqueId().equals(testIdentifier.getUniqueId()))
                .findFirst()
                .ifPresent(testClass -> testClass.finished(testExecutionResult));
        }
    }

    public void reportExecutionSkipped(TestIdentifier testIdentifier) {
        if (testIdentifier.isContainer()) {
            TestClass testClass = new TestClass(testIdentifier.getUniqueId(), extractClassName(testIdentifier));
            testClass.markAsSkipped();
            testClasses.add(testClass);
        }
    }

    public void reportTestStarted(TestIdentifier testIdentifier) {
        if (testIdentifier.isTest() && testIdentifier.getParentId().isPresent()) {
            testClasses.stream()
                .filter(testClass -> Objects.equals(testClass.uniqueId(), testIdentifier.getParentId().get()))
                .findFirst()
                .ifPresent(testClass -> testClass.testIsStarted(testIdentifier.getUniqueId(), extractMethodName(testIdentifier)));
        }
    }

    public void reportTestFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        if (testIdentifier.isTest() && testIdentifier.getParentId().isPresent()) {
            testClasses.stream()
                .filter(testClass -> Objects.equals(testClass.uniqueId(), testIdentifier.getParentId().get()))
                .findFirst()
                .ifPresent(testClass -> testClass.testIsFinished(testIdentifier.getUniqueId(), testExecutionResult));
        }
    }

    public void reportTestSkipped(TestIdentifier testIdentifier) {
        if (testIdentifier.isTest() && testIdentifier.getParentId().isPresent()) {
            testClasses.stream()
                .filter(testClass -> testClass.uniqueId().equals(testIdentifier.getParentId().get()))
                .findFirst()
                .ifPresent(testClass -> testClass.testIsSkipped(testIdentifier.getUniqueId()));
        }
    }

    private String extractClassName(TestIdentifier testIdentifier) {
        return testIdentifier.getSource()
            .filter(source -> source instanceof ClassSource)
            .map(source -> ((ClassSource) source).getClassName())
            .map(className -> className.substring(className.lastIndexOf(".") + 1))
            .orElseGet(() -> extractFromUniqueId(testIdentifier.getUniqueId(), "class", testIdentifier.getDisplayName()));
    }

    private String extractMethodName(TestIdentifier testIdentifier) {
        return testIdentifier.getSource()
            .filter(source -> source instanceof MethodSource)
            .map(source -> ((MethodSource) source).getMethodName())
            .orElseGet(() -> extractFromUniqueId(testIdentifier.getUniqueId(), "method", testIdentifier.getDisplayName()));
    }

    private String extractFromUniqueId(String uniqueId, String segment, String fallback) {
        String prefix = "[" + segment + ":";
        int start = uniqueId.lastIndexOf(prefix);
        if (start < 0) return fallback;
        int end = uniqueId.indexOf("]", start);
        if (end < 0) return fallback;
        String value = uniqueId.substring(start + prefix.length(), end);
        int paren = value.indexOf("(");
        return paren >= 0 ? value.substring(0, paren) : value;
    }

    public String report() {
        try {
            return MAPPER.writeValueAsString(testClasses);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize test report", e);
        }
    }
}
