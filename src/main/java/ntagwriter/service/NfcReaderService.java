package ntagwriter.service;

import ntagwriter.reader.ApduStatusWord;
import ntagwriter.reader.NfcReaderStrategy;
import ntagwriter.reader.ReaderException;
import ntagwriter.util.ConsoleHelper;
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
            ConsoleHelper.printWarning("이미 리더기가 연결되어 있습니다.");
            return;
        }

        reader.connect();
        connected = true;
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
     * 응답 확인 (SW = 9000 또는 9100)
     */
    public boolean isSuccess(ResponseAPDU response) {
        return ApduStatusWord.isSuccessSw(response.getSW());
    }

    /**
     * 에러 메시지 생성
     */
    public String getErrorMessage(ResponseAPDU response) {
        return ApduStatusWord.describe(response.getSW());
    }

    public boolean isConnected() {
        return connected;
    }

    public NfcReaderStrategy getReader() {
        return reader;
    }
}
