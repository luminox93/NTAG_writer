package ntagwriter.domain.tag;

/**
 * 태그 상태 열거형
 */
public enum TagStatus {
    DETECTED("태그가 감지됨"),
    CONNECTED("태그에 연결됨"),
    AUTHENTICATED("인증 완료"),
    CONFIGURING("설정 중"),
    CONFIGURED("설정 완료"),
    WRITING("데이터 쓰기 중"),
    WRITTEN("데이터 쓰기 완료"),
    VERIFYING("검증 중"),
    VERIFIED("검증 완료"),
    ERROR("오류 발생"),
    DISCONNECTED("연결 해제됨");

    public final String description;

    TagStatus(String description) {
        this.description = description;
    }

    public boolean isTerminal() {
        return this == ERROR || this == DISCONNECTED || this == VERIFIED;
    }

    public boolean canProceed() {
        return this != ERROR && this != DISCONNECTED;
    }
}