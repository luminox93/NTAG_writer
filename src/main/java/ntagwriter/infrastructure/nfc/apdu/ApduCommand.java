package ntagwriter.infrastructure.nfc.apdu;

/**
 * APDU 명령 인터페이스
 * Command 패턴 적용
 */
public interface ApduCommand {

    /**
     * APDU 바이트 배열 생성
     *
     * @return APDU 명령 바이트 배열
     */
    byte[] build();

    /**
     * 명령 이름
     *
     * @return 명령 이름
     */
    String name();

    /**
     * 명령 설명
     *
     * @return 명령에 대한 설명
     */
    String description();

    /**
     * 예상 응답 길이
     *
     * @return 예상 응답 바이트 수 (알 수 없으면 -1)
     */
    default int expectedResponseLength() {
        return -1;
    }
}