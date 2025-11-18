package ntagwriter.application.workflow;

import ntagwriter.domain.tag.Tag;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 워크플로우 실행 결과
 */
public final class WorkflowResult {

    public final boolean success;
    public final Tag finalTag;
    public final WorkflowState finalState;
    public final LocalDateTime startTime;
    public final LocalDateTime endTime;
    public final Duration duration;
    public final List<StepResult> stepResults;
    public final String errorMessage;

    private WorkflowResult(Builder builder) {
        this.success = builder.success;
        this.finalTag = builder.finalTag;
        this.finalState = builder.finalState;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.duration = Duration.between(startTime, endTime);
        this.stepResults = new ArrayList<>(builder.stepResults);
        this.errorMessage = builder.errorMessage;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean success;
        private Tag finalTag;
        private WorkflowState finalState;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private List<StepResult> stepResults = new ArrayList<>();
        private String errorMessage;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder finalTag(Tag tag) {
            this.finalTag = tag;
            return this;
        }

        public Builder finalState(WorkflowState state) {
            this.finalState = state;
            return this;
        }

        public Builder startTime(LocalDateTime time) {
            this.startTime = time;
            return this;
        }

        public Builder endTime(LocalDateTime time) {
            this.endTime = time;
            return this;
        }

        public Builder addStepResult(StepResult result) {
            this.stepResults.add(result);
            return this;
        }

        public Builder errorMessage(String message) {
            this.errorMessage = message;
            return this;
        }

        public WorkflowResult build() {
            if (startTime == null) startTime = LocalDateTime.now();
            if (endTime == null) endTime = LocalDateTime.now();
            return new WorkflowResult(this);
        }
    }

    /**
     * 단계별 실행 결과
     */
    public static final class StepResult {
        public final String stepName;
        public final boolean success;
        public final Duration duration;
        public final String message;

        public StepResult(String stepName, boolean success, Duration duration, String message) {
            this.stepName = stepName;
            this.success = success;
            this.duration = duration;
            this.message = message;
        }
    }

    @Override
    public String toString() {
        return String.format("WorkflowResult{success=%s, state=%s, duration=%s, steps=%d}",
            success, finalState, duration, stepResults.size());
    }
}