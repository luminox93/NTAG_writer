package ntagwriter.service;

import ntagwriter.reader.NfcReaderStrategy;
import ntagwriter.reader.ReaderException;
import ntagwriter.util.HexUtils;

import javax.smartcardio.ResponseAPDU;

/**
 * NFC 리더기 서비스
 * 리더기 연결, UID 읽기, APDU 명령 전송 등을 관리
 */
public class NfcReaderService {

    private final NfcReaderStrategy reader;
    private boolean connected;

    public NfcReaderService(NfcReaderStrategy reader) {
        this.reader = reader;
        this.connected = false;
    }

    /**
     * 리더기 연결
     */
    public void connect() throws ReaderException {
        if (connected) {
            System.out.println("이미 리더기가 연결되어 있습니다.");
            return;
        }

        reader.connect();
        connected = true;
        System.out.println("리더기 연결됨: " + reader.getReaderName());
    }

    /**
     * 리더기 연결 해제
     */
    public void disconnect() {
        if (!connected) {
            return;
        }

        reader.disconnect();
        connected = false;
        System.out.println("리더기 연결 해제됨");
    }

    /**
     * 태그 UID 읽기
     */
    public String readTagUid() throws ReaderException {
        if (!connected) {
            throw new ReaderException("리더기가 연결되지 않았습니다.");
        }

        byte[] uid = reader.readUid();
        return HexUtils.bytesToHex(uid);
    }

    /**
     * APDU 명령 전송
     */
    public ResponseAPDU sendCommand(byte[] apdu) throws ReaderException {
        if (!connected) {
            throw new ReaderException("리더기가 연결되지 않았습니다.");
        }

        return reader.sendCommand(apdu);
    }

    /**
     * 응답 확인 (SW = 9000)
     */
    public boolean isSuccess(ResponseAPDU response) {
        return response.getSW() == 0x9000;
    }

    /**
     * 에러 메시지 생성
     */
    public String getErrorMessage(ResponseAPDU response) {
        int sw = response.getSW();
        return switch (sw) {
            case 0x9000 -> "성공";
            case 0x6300 -> "검증 실패";
            case 0x6700 -> "잘못된 길이";
            case 0x6982 -> "보안 상태 불만족";
            case 0x6985 -> "사용 조건 불만족";
            case 0x6A80 -> "잘못된 데이터";
            case 0x6A82 -> "파일을 찾을 수 없음";
            case 0x6A86 -> "잘못된 P1/P2";
            case 0x6D00 -> "명령어를 지원하지 않음";
            case 0x6E00 -> "클래스를 지원하지 않음";
            default -> String.format("알 수 없는 오류 (SW=%04X)", sw);
        };
    }

    public boolean isConnected() {
        return connected;
    }

    public NfcReaderStrategy getReader() {
        return reader;
    }
}
