package ntagwriter.domain;

/**
 * NTAG424 자동 설정 단계
 */
public enum SetupStep {
    CONNECT_READER(1, "리더기 연결", "NFC 리더기를 연결하고 태그를 인식합니다."),
    READ_UID(2, "태그 UID 읽기", "태그의 고유 식별자(UID)를 읽습니다."),
    SELECT_APPLICATION(3, "애플리케이션 선택", "NTAG424 DNA 애플리케이션을 선택합니다."),
    AUTHENTICATE(4, "기본 키 인증", "공장 초기화된 기본 키로 인증합니다."),
    CONFIGURE_SDM(5, "SDM 설정", "Secure Dynamic Messaging 파라미터를 설정합니다."),
    CHANGE_KEYS(6, "보안 키 변경", "새로운 AES 키로 변경합니다."),
    VERIFY(7, "설정 검증", "설정이 올바르게 적용되었는지 확인합니다."),
    COMPLETE(8, "완료", "NTAG424 태그 설정이 완료되었습니다.");

    private final int stepNumber;
    private final String title;
    private final String description;

    SetupStep(int stepNumber, String title, String description) {
        this.stepNumber = stepNumber;
        this.title = title;
        this.description = description;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return String.format("[단계 %d] %s: %s", stepNumber, title, description);
    }
}
