package ntagwriter.util;

import ntagwriter.reader.ReaderException;
import javax.smartcardio.ResponseAPDU;

/**
 * APDU Response 검증 유틸리티
 * 공통적인 응답 검증 로직을 제공
 */
public class ApduResponseValidator {

    // 성공 상태 코드
    private static final int SW_SUCCESS = 0x9000;
    private static final int SW_SUCCESS_WITH_DATA = 0x9100;

    /**
     * 응답이 성공인지 검증
     * @param response APDU 응답
     * @param operationName 작업 이름 (에러 메시지용)
     * @throws ReaderException 응답이 실패인 경우
     */
    public static void validateSuccess(ResponseAPDU response, String operationName)
            throws ReaderException {
        if (!isSuccess(response)) {
            throw new ReaderException(
                String.format("%s 실패: %04X", operationName, response.getSW())
            );
        }
    }

    /**
     * 응답이 성공인지 확인
     * @param response APDU 응답
     * @return 성공 여부
     */
    public static boolean isSuccess(ResponseAPDU response) {
        int sw = response.getSW();
        return sw == SW_SUCCESS || sw == SW_SUCCESS_WITH_DATA;
    }

    /**
     * 상태 워드를 포맷된 문자열로 반환
     * @param response APDU 응답
     * @return 포맷된 상태 워드 (예: "9000")
     */
    public static String formatStatusWord(ResponseAPDU response) {
        return String.format("%04X", response.getSW());
    }

    /**
     * 에러 메시지 생성
     * @param response APDU 응답
     * @param operationName 작업 이름
     * @return 에러 메시지
     */
    public static String getErrorMessage(ResponseAPDU response, String operationName) {
        return String.format("%s 실패: %s",
            operationName,
            formatStatusWord(response)
        );
    }
}