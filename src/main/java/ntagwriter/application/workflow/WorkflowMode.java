package ntagwriter.application.workflow;

/**
 * 워크플로우 실행 모드
 */
public enum WorkflowMode {
    INTERACTIVE("대화형 모드 - 각 단계마다 사용자 확인"),
    AUTOMATIC("자동 모드 - 사용자 개입 없이 실행"),
    STEP_BY_STEP("단계별 모드 - 각 단계 후 일시 정지");

    public final String description;

    WorkflowMode(String description) {
        this.description = description;
    }
}