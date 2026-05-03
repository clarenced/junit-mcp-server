package io.github.clarenced.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.platform.engine.TestExecutionResult;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@JsonAutoDetect(
    fieldVisibility  = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
public final class TestClass {
    private final String className;
    private final List<Test> tests;
    private final long started;
    private long finished;
    private String status;

    public TestClass(String className) {
        this(className, new CopyOnWriteArrayList<>(), System.currentTimeMillis(), 0, "");
    }

    private TestClass(String className, List<Test> tests, long started, long finished, String status) {
        this.className = className;
        this.tests = tests;
        this.started = started;
        this.finished = finished;
        this.status = status;
    }

    @JsonProperty("className")
    public String className() { return className; }

    @JsonProperty("total")
    public long total() { return tests.size(); }

    @JsonProperty("passed")
    public long passed() { return tests.stream().filter(Test::isPassed).count(); }

    @JsonProperty("failed")
    public long failed() { return tests.stream().filter(Test::isFailed).count(); }

    @JsonProperty("skipped")
    public long skipped() { return tests.stream().filter(Test::isSkipped).count(); }

    @JsonProperty("tests")
    public List<Test> tests() { return tests; }

    public long aborted() { return tests.stream().filter(Test::isAborted).count(); }
    public String status() { return status; }

    public void markAsSkipped() {
        finished = System.currentTimeMillis();
        status = "skipped";
    }

    public void finished(TestExecutionResult testExecutionResult) {
        finished = System.currentTimeMillis();
        TestExecutionResult.Status resultStatus = testExecutionResult.getStatus();
        status = switch (resultStatus) {
            case SUCCESSFUL -> "passed";
            case FAILED     -> "failed";
            case ABORTED    -> "aborted";
        };
    }

    public void testIsStarted(String uniqueId) { tests.add(new Test(uniqueId)); }

    public void testIsFinished(String uniqueId, TestExecutionResult testExecutionResult) {
        tests.stream()
            .filter(test -> test.name().equals(uniqueId))
            .findFirst()
            .ifPresent(test -> test.finished(testExecutionResult));
    }

    public void testIsSkipped(String uniqueId) {
        tests.stream()
            .filter(test -> test.name().equals(uniqueId))
            .findFirst()
            .ifPresent(Test::skipped);
    }

    public long duration() {
        return Duration.ofMillis(finished - started).toMillis();
    }
}
