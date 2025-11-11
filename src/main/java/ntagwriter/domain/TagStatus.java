package ntagwriter.domain;

/**
 * NTAG424 태그의 프로그래밍 상태를 나타내는 enum
 */
public enum TagStatus {
    /**
     * 프로그래밍 대기 중
     */
    PENDING("대기"),

    /**
     * 프로그래밍 진행 중
     */
    IN_PROGRESS("진행중"),

    /**
     * 프로그래밍 성공
     */
    SUCCESS("성공"),

    /**
     * 프로그래밍 실패
     */
    FAILED("실패"),

    /**
     * 태그를 찾을 수 없음
     */
    NOT_FOUND("태그 없음");

    private final String displayName;

    TagStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
