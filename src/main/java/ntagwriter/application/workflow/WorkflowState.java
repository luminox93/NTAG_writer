package ntagwriter.application.workflow;

/**
 * 워크플로우 상태 열거형
 */
public enum WorkflowState {
    INITIALIZED("초기화됨"),
    CONNECTING("리더기 연결 중"),
    CONNECTED("리더기 연결됨"),
    DETECTING_TAG("태그 감지 중"),
    TAG_DETECTED("태그 감지됨"),
    AUTHENTICATING("인증 중"),
    AUTHENTICATED("인증 완료"),
    CONFIGURING_SDM("SDM 설정 중"),
    SDM_CONFIGURED("SDM 설정 완료"),
    WRITING_NDEF("NDEF 쓰기 중"),
    NDEF_WRITTEN("NDEF 쓰기 완료"),
    CHANGING_KEYS("키 변경 중"),
    KEYS_CHANGED("키 변경 완료"),
    VERIFYING("검증 중"),
    COMPLETED("완료"),
    CANCELLED("취소됨"),
    ERROR("오류");

    public final String description;

    WorkflowState(String description) {
        this.description = description;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == ERROR;
    }

    public boolean canProceed() {
        return !isTerminal();
    }

    public boolean isInProgress() {
        return name().endsWith("ING");
    }
}