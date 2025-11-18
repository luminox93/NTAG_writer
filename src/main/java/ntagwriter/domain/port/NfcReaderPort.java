package ntagwriter.domain.port;

import ntagwriter.domain.tag.Tag;

/**
 * NFC 리더기 포트 인터페이스
 * 도메인 레이어에서 인프라스트럭처의 NFC 리더기를 추상화
 */
public interface NfcReaderPort {

    /**
     * 리더기 연결
     *
     * @return 연결 성공 여부
     */
    boolean connect();

    /**
     * 리더기 연결 해제
     */
    void disconnect();

    /**
     * 연결 상태 확인
     *
     * @return 연결되어 있으면 true
     */
    boolean isConnected();

    /**
     * 태그 감지 대기
     *
     * @param timeoutMs 타임아웃 (밀리초)
     * @return 감지된 태그, 없으면 null
     */
    Tag waitForTag(long timeoutMs);

    /**
     * 태그 존재 여부 확인
     *
     * @return 태그가 있으면 true
     */
    boolean isTagPresent();

    /**
     * APDU 명령 전송
     *
     * @param command APDU 명령 바이트 배열
     * @return 응답 바이트 배열
     * @throws ReaderException 통신 오류 시
     */
    byte[] sendCommand(byte[] command) throws ReaderException;

    /**
     * 리더기 정보 조회
     *
     * @return 리더기 이름
     */
    String getReaderName();

    /**
     * 리더기 통신 예외
     */
    class ReaderException extends Exception {
        public ReaderException(String message) {
            super(message);
        }

        public ReaderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}