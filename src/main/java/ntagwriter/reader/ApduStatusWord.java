package ntagwriter.reader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * NTAG424 DNA 및 ISO/IEC 7816-4 상태 워드(SW1/SW2) 모음.
 * 데이터시트와 docs/error_code.md를 기반으로 한다.
 */
public enum ApduStatusWord {

    // NTAG 고유 (CLA = 0x90, SW1 = 0x91)
    OPERATION_OK(0x9100, "OPERATION_OK", "NTAG 명령이 정상 완료되었습니다.", true),
    COMMAND_NOT_FOUND(0x910B, "COMMAND_NOT_FOUND", "지원하지 않는 명령 코드입니다.", false),
    COMMAND_FORMAT_ERROR(0x910C, "COMMAND_FORMAT_ERROR", "명령 길이 혹은 주소 형식이 잘못되었습니다.", false),
    ILLEGAL_COMMAND_CODE(0x911C, "ILLEGAL_COMMAND_CODE", "CLA=0x90 범위에서 허용되지 않는 INS입니다.", false),
    INTEGRITY_ERROR(0x911E, "INTEGRITY_ERROR", "CMAC/CRC/패딩 검증 실패 또는 암호문 구조 오류입니다.", false),
    NO_SUCH_KEY(0x9140, "NO_SUCH_KEY", "지정한 키 번호가 존재하지 않습니다.", false),
    LENGTH_ERROR(0x917E, "LENGTH_ERROR", "전송한 데이터 길이가 명령 정의와 다릅니다.", false),
    PERMISSION_DENIED(0x919D, "PERMISSION_DENIED", "현재 권한으로 해당 명령이 허용되지 않습니다.", false),
    PARAMETER_ERROR(0x919E, "PARAMETER_ERROR", "P1/P2/Data 값이 허용 범위를 벗어났습니다.", false),
    AUTHENTICATION_DELAY(0x91AD, "AUTHENTICATION_DELAY", "인증 실패 제한으로 지연 상태입니다.", false),
    AUTHENTICATION_ERROR(0x91AE, "AUTHENTICATION_ERROR", "인증 또는 세션 상태가 요구 조건과 다릅니다.", false),
    ADDITIONAL_FRAME(0x91AF, "ADDITIONAL_FRAME", "추가 프레임을 전송해야 합니다.", false),
    BOUNDARY_ERROR(0x91BE, "BOUNDARY_ERROR", "파일 경계를 넘어 접근했습니다.", false),
    COMMAND_ABORTED(0x91CA, "COMMAND_ABORTED", "체인 명령이 정상적으로 완료되지 않았습니다.", false),
    MEMORY_ERROR(0x91EE, "MEMORY_ERROR", "비휘발성 메모리 접근 중 오류가 발생했습니다.", false),
    FILE_NOT_FOUND(0x91F0, "FILE_NOT_FOUND", "지정한 파일 번호를 찾을 수 없습니다.", false),

    // ISO/IEC 7816-4 공통 상태 (CLA = 0x00)
    NORMAL_PROCESSING(0x9000, "NORMAL_PROCESSING", "명령이 성공했습니다.", true),
    VERIFICATION_FAILED(0x6300, "VERIFICATION_FAILED", "검증에 실패했습니다.", false),
    ISO_MEMORY_FAILURE(0x6581, "MEMORY_FAILURE", "NVM 접근 중 메모리 오류가 발생했습니다.", false),
    WRONG_LENGTH(0x6700, "WRONG_LENGTH", "APDU 길이가 사양과 다릅니다.", false),
    SECURITY_STATUS_NOT_SATISFIED(0x6982, "SECURITY_STATUS_NOT_SATISFIED", "보안 조건이 충족되지 않았습니다.", false),
    CONDITIONS_OF_USE_NOT_SATISFIED(0x6985, "CONDITIONS_OF_USE_NOT_SATISFIED", "사용 조건이 충족되지 않았습니다.", false),
    INCORRECT_PARAMETERS_IN_DATA(0x6A80, "INCORRECT_PARAMETERS_IN_DATA", "Data 필드 값이 잘못되었습니다.", false),
    FILE_OR_APPLICATION_NOT_FOUND(0x6A82, "FILE_OR_APPLICATION_NOT_FOUND", "파일 또는 애플리케이션이 존재하지 않습니다.", false),
    INCORRECT_P1P2(0x6A86, "INCORRECT_P1P2", "P1 혹은 P2 값이 잘못되었습니다.", false),
    LC_INCONSISTENT_WITH_P1P2(0x6A87, "LC_INCONSISTENT_WITH_P1P2", "Lc 값이 P1/P2 조합과 일치하지 않습니다.", false),
    WRONG_LE(0x6C00, "WRONG_LE", "Le 값이 잘못되었습니다.", false),
    WRONG_LE_WITH_HINT(0x6C00, 0xFF00, "WRONG_LE_WITH_HINT", "Le 값이 잘못되었으며 SW2가 허용 길이를 제공합니다.", false) {
        @Override
        public String formatDescription(int sw) {
            int expectedLength = sw & 0xFF;
            if (expectedLength == 0) {
                return super.formatDescription(sw);
            }
            return super.formatDescription(sw) + String.format(" (허용 길이: 0x%02X)", expectedLength);
        }
    },
    INSTRUCTION_NOT_SUPPORTED(0x6D00, "INSTRUCTION_NOT_SUPPORTED", "지원하지 않는 INS입니다.", false),
    CLASS_NOT_SUPPORTED(0x6E00, "CLASS_NOT_SUPPORTED", "지원하지 않는 CLA입니다.", false);

    private final int code;
    private final int mask;
    private final String displayName;
    private final String description;
    private final boolean success;

    private static final Map<Integer, ApduStatusWord> EXACT_MATCHES;
    private static final List<ApduStatusWord> PATTERN_MATCHES;

    static {
        Map<Integer, ApduStatusWord> exact = new HashMap<>();
        List<ApduStatusWord> patterns = new ArrayList<>();

        for (ApduStatusWord statusWord : values()) {
            if (statusWord.mask == 0xFFFF) {
                exact.put(statusWord.code, statusWord);
            } else {
                patterns.add(statusWord);
            }
        }

        EXACT_MATCHES = Collections.unmodifiableMap(exact);
        PATTERN_MATCHES = Collections.unmodifiableList(patterns);
    }

    ApduStatusWord(int code, String displayName, String description, boolean success) {
        this(code, 0xFFFF, displayName, description, success);
    }

    ApduStatusWord(int code, int mask, String displayName, String description, boolean success) {
        this.code = code;
        this.mask = mask;
        this.displayName = displayName;
        this.description = description;
        this.success = success;
    }

    public int getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean matches(int sw) {
        return (sw & mask) == code;
    }

    public String formatDescription(int sw) {
        return description;
    }

    public static Optional<ApduStatusWord> fromSw(int sw) {
        ApduStatusWord exact = EXACT_MATCHES.get(sw);
        if (exact != null) {
            return Optional.of(exact);
        }

        for (ApduStatusWord candidate : PATTERN_MATCHES) {
            if (candidate.matches(sw)) {
                return Optional.of(candidate);
            }
        }

        return Optional.empty();
    }

    public static boolean isSuccessSw(int sw) {
        return fromSw(sw).map(ApduStatusWord::isSuccess).orElse(false);
    }

    public static String describe(int sw) {
        return fromSw(sw)
                .map(status -> String.format("%s - %s", status.getDisplayName(), status.formatDescription(sw)))
                .orElseGet(() -> String.format("알 수 없는 상태 (SW=%04X)", sw));
    }
}
