package io.github.clarenced.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.platform.engine.TestExecutionResult;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.Objects;

@JsonAutoDetect(
    fieldVisibility  = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
public final class Test {
    private final String name;
    private String status;
    private long started;
    private long finished;
    private boolean isSkipped;
    private Failure failure;

    public Test(String name) {
        this.name = name;
        this.status = "";
        this.started = System.currentTimeMillis();
        this.finished = 0;
        this.isSkipped = false;
    }

    public boolean isPassed()  { return status.equals("passed"); }
    public boolean isFailed()  { return status.equals("failed"); }
    public boolean isAborted() { return status.equals("aborted"); }
    public boolean isSkipped() { return isSkipped; }

    @JsonProperty("name")
    public String name() { return name; }

    @JsonProperty("status")
    public String effectiveStatus() { return isSkipped ? "skipped" : status; }

    @JsonProperty("failure")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Failure failure() { return failure; }

    public void started() { this.started = System.currentTimeMillis(); }

    public void skipped() { this.isSkipped = true; }

    public void finished(TestExecutionResult testExecutionResult) {
        this.finished = System.currentTimeMillis();
        TestExecutionResult.Status resultStatus = testExecutionResult.getStatus();
        status = switch (resultStatus) {
            case SUCCESSFUL -> "passed";
            case FAILED     -> "failed";
            case ABORTED    -> "aborted";
        };
        if (resultStatus == TestExecutionResult.Status.FAILED) {
            testExecutionResult.getThrowable().ifPresent(throwable ->
                failure = new Failure(throwable.getMessage(), convertStackTrace(throwable))
            );
        }
    }

    public long duration() {
        return Duration.ofMillis(finished - started).toMillis();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Test) obj;
        return Objects.equals(this.name, that.name) &&
               Objects.equals(this.status, that.status) &&
               Objects.equals(this.failure, that.failure);
    }

    @Override
    public int hashCode() { return Objects.hash(name, status, failure); }

    private String convertStackTrace(Throwable stackTrace) {
        StringWriter sw = new StringWriter();
        stackTrace.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
