package ntagwriter.reader;

import javax.smartcardio.ResponseAPDU;

/**
 * NFC 리더기 전략 인터페이스
 * 다양한 NFC 리더기를 지원하기 위한 전략 패턴 구현
 */
public interface NfcReaderStrategy {

    /**
     * 리더기에 연결
     *
     * @throws ReaderException 연결 실패 시
     */
    void connect() throws ReaderException;

    /**
     * 태그의 UID 읽기
     *
     * @return 태그 UID (바이트 배열)
     * @throws ReaderException 읽기 실패 시
     */
    byte[] readUid() throws ReaderException;

    /**
     * APDU 명령어 전송
     *
     * @param apdu APDU 명령어 바이트 배열
     * @return 응답 APDU
     * @throws ReaderException 전송 실패 시
     */
    ResponseAPDU sendCommand(byte[] apdu) throws ReaderException;

    /**
     * 리더기 연결 해제
     */
    void disconnect();

    /**
     * 리더기가 연결되어 있는지 확인
     *
     * @return 연결 상태
     */
    boolean isConnected();

    /**
     * 리더기 이름 가져오기
     *
     * @return 리더기 이름
     */
    String getReaderName();
}
