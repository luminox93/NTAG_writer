package ntagwriter.application.handler;

import ntagwriter.application.workflow.WorkflowContext;
import ntagwriter.application.workflow.WorkflowState;

/**
 * 워크플로우 단계 핸들러 인터페이스
 * 각 단계의 실행 로직을 캡슐화
 */
public interface StepHandler {

    /**
     * 이 핸들러가 처리할 수 있는 상태인지 확인
     *
     * @param state 워크플로우 상태
     * @return 처리 가능하면 true
     */
    boolean canHandle(WorkflowState state);

    /**
     * 단계 실행
     *
     * @param context 워크플로우 컨텍스트
     * @throws StepExecutionException 실행 중 오류 발생 시
     */
    void execute(WorkflowContext context) throws StepExecutionException;

    /**
     * 단계 이름
     *
     * @return 단계 이름
     */
    String name();

    /**
     * 다음 상태
     *
     * @return 성공 시 전환할 다음 상태
     */
    WorkflowState nextState();

    /**
     * 단계 실행 예외
     */
    class StepExecutionException extends Exception {
        public StepExecutionException(String message) {
            super(message);
        }

        public StepExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}