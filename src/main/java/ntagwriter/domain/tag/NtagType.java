package ntagwriter.domain.tag;

/**
 * NTAG 태그 타입 열거형
 */
public enum NtagType {
    NTAG_424_DNA("NTAG 424 DNA", 416, true),
    NTAG_424_DNA_TT("NTAG 424 DNA TagTamper", 416, true),
    NTAG_213("NTAG 213", 180, false),
    NTAG_215("NTAG 215", 504, false),
    NTAG_216("NTAG 216", 888, false),
    UNKNOWN("Unknown", 0, false);

    public final String displayName;
    public final int totalMemory;
    public final boolean supportsSdm;

    NtagType(String displayName, int totalMemory, boolean supportsSdm) {
        this.displayName = displayName;
        this.totalMemory = totalMemory;
        this.supportsSdm = supportsSdm;
    }

    public boolean supportsSdm() {
        return supportsSdm;
    }

    /**
     * UID 기반으로 태그 타입 식별
     *
     * @param uid UID 바이트 배열
     * @return 식별된 태그 타입
     */
    public static NtagType identifyByUid(byte[] uid) {
        if (uid == null || uid.length < 7) {
            return UNKNOWN;
        }

        // NXP 제조사 코드 확인 (0x04)
        if (uid[0] != 0x04) {
            return UNKNOWN;
        }

        // TODO: 실제 태그 타입 식별 로직 구현
        // 현재는 NTAG 424 DNA로 가정
        return NTAG_424_DNA;
    }
}