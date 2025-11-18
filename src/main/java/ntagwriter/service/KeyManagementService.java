package ntagwriter.service;

import ntagwriter.reader.ReaderException;
import ntagwriter.util.*;

import javax.smartcardio.ResponseAPDU;
import java.security.GeneralSecurityException;

/**
 * 키 관리 서비스
 * NTAG424 DNA의 키 변경 기능을 담당
 */
public class KeyManagementService {

    private final NfcReaderService readerService;
    private final CryptoService cryptoService;
    private SessionContext session;

    public KeyManagementService(NfcReaderService readerService, CryptoService cryptoService) {
        this.readerService = readerService;
        this.cryptoService = cryptoService;
    }

    /**
     * 세션 컨텍스트 설정
     */
    public void setSession(SessionContext session) {
        this.session = session;
    }

    /**
     * 모든 키를 변경
     */
    public void changeAllKeys(byte[] newKey) throws ReaderException, GeneralSecurityException {
        ConsoleHelper.printInfo("→ 보안 키 변경 중...");

        // Key 0부터 Key 4까지 모두 동일한 키로 변경
        for (byte keyNo = 0; keyNo < 5; keyNo++) {
            changeKey(keyNo, new byte[16], newKey, (byte) 0x00);
            CommandCounterManager.increment(session.commandCounter);
        }

        ConsoleHelper.printSuccess("✓ 모든 키가 변경되었습니다!");
        ConsoleHelper.printWarning("⚠ 중요: 새 키를 안전한 곳에 보관하세요!");
        ConsoleHelper.printInfo("  새 키 (HEX): " + HexUtils.bytesToHex(newKey));
    }

    /**
     * 특정 키 변경
     * @param keyNo 변경할 키 번호
     * @param oldKey 현재 키
     * @param newKey 새 키
     * @param newKeyVersion 새 키 버전
     */
    public void changeKey(byte keyNo, byte[] oldKey, byte[] newKey, byte newKeyVersion)
            throws ReaderException, GeneralSecurityException {

        ConsoleHelper.printInfo("  → Key " + keyNo + " 변경 중...");

        // 키 데이터 준비 (newKey + newKeyVersion + CRC32 of newKey)
        byte[] keyData = new byte[16 + 1 + 4];
        System.arraycopy(newKey, 0, keyData, 0, 16);
        keyData[16] = newKeyVersion;

        // CRC32는 나중에 계산 (필요시)
        // 현재는 0으로 채움
        keyData[17] = 0x00;
        keyData[18] = 0x00;
        keyData[19] = 0x00;
        keyData[20] = 0x00;

        // 키 데이터 암호화
        byte[] encryptedKeyData = cryptoService.encryptECB(session.kSesAuthENC, keyData);

        // ChangeKey APDU
        byte[] apdu = new byte[6 + encryptedKeyData.length];
        apdu[0] = (byte) 0x90; // CLA
        apdu[1] = (byte) 0xC4; // INS (ChangeKey)
        apdu[2] = 0x00; // P1
        apdu[3] = 0x00; // P2
        apdu[4] = (byte) (1 + encryptedKeyData.length); // Lc
        apdu[5] = keyNo; // Key Number
        System.arraycopy(encryptedKeyData, 0, apdu, 6, encryptedKeyData.length);

        DebugLogger.logHex("Key Data", keyData, 2);
        DebugLogger.logHex("Encrypted", encryptedKeyData, 2);

        ResponseAPDU response = readerService.sendCommand(apdu);
        ApduResponseValidator.validateSuccess(response, "Key " + keyNo + " 변경");

        ConsoleHelper.printSuccess("  ✓ Key " + keyNo + " 변경 완료");
    }
}