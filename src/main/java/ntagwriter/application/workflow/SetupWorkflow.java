package ntagwriter.application.workflow;

import ntagwriter.domain.tag.Tag;
import ntagwriter.domain.config.SdmConfiguration;

/**
 * NTAG424 설정 워크플로우 인터페이스
 * 전체 설정 프로세스의 오케스트레이션 담당
 */
public interface SetupWorkflow {

    /**
     * 워크플로우 실행
     *
     * @param context 워크플로우 컨텍스트
     * @return 실행 결과
     */
    WorkflowResult execute(WorkflowContext context);

    /**
     * 현재 상태 조회
     *
     * @return 현재 워크플로우 상태
     */
    WorkflowState currentState();

    /**
     * 워크플로우 중단
     */
    void cancel();

    /**
     * 워크플로우 재시작
     *
     * @param context 새로운 컨텍스트
     */
    void restart(WorkflowContext context);
}