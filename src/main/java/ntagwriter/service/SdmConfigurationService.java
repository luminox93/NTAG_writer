package ntagwriter.service;

import ntagwriter.crypto.MacUtils;
import ntagwriter.reader.ReaderException;
import ntagwriter.util.*;

import javax.smartcardio.ResponseAPDU;
import java.security.GeneralSecurityException;

/**
 * SDM (Secure Dynamic Messaging) 설정 서비스
 * NTAG424 DNA의 SDM 기능 설정을 담당
 */
public class SdmConfigurationService {

    private static final int NDEF_FILE_NUMBER = 0x02;

    private final NfcReaderService readerService;
    private final CryptoService cryptoService;
    private SessionContext session;

    public SdmConfigurationService(NfcReaderService readerService, CryptoService cryptoService) {
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
     * SDM 설정 수행
     */
    public void configureSdm(byte[] sdmAccessRights, int piccDataOffset,
                           int sdmMacInputOffset, int sdmMacOffset)
            throws ReaderException, GeneralSecurityException {

        ConsoleHelper.printInfo("→ NDEF 파일에 SDM 설정 중...");

        // Change File Settings APDU 생성 및 전송
        byte[] settingsData = buildFileSettingsData(
            sdmAccessRights, piccDataOffset, sdmMacInputOffset, sdmMacOffset
        );

        // Settings 암호화
        byte[] paddedData = cryptoService.addPadding(settingsData, 16);
        DebugLogger.logHex("Settings Data", settingsData);
        DebugLogger.logHex("Padded Data", paddedData);

        byte[] ivc = generateIVc(session.kSesAuthENC, session.transactionId, session.commandCounter);
        byte[] ivcInput = buildIVcInput(session.transactionId, session.commandCounter);
        DebugLogger.logHex("IVc Input", ivcInput);
        DebugLogger.logHex("KSesAuthENC", session.kSesAuthENC);
        DebugLogger.logHex("IVc", ivc);

        byte[] encryptedSettings = cryptoService.encryptCBC(session.kSesAuthENC, ivc, paddedData);

        // CMAC 계산
        byte[] cmac = calculateCmac(encryptedSettings);
        byte[] mact = MacUtils.truncateMac(cmac);

        // APDU 전송
        byte[] apdu = buildChangeFileSettingsApdu(encryptedSettings, mact);
        ResponseAPDU response = readerService.sendCommand(apdu);

        ApduResponseValidator.validateSuccess(response, "SDM 설정");

        // Command Counter 증가
        CommandCounterManager.increment(session.commandCounter);
        ConsoleHelper.printSuccess("✓ SDM 설정 완료!");
    }

    /**
     * File Settings 데이터 생성
     */
    private byte[] buildFileSettingsData(byte[] sdmAccessRights, int piccDataOffset,
                                        int sdmMacInputOffset, int sdmMacOffset) {
        // File Option: 0x40 = SDM and Mirroring enabled
        // Access Rights: 2 bytes
        // SDM Access Rights: 변수
        // ASCII Mirror: piccData만 활성화 (0x01)
        // SDM Options: 0xC1 (UID mirror + Read Counter + Read Counter Limit)

        byte[] settings = new byte[10 + sdmAccessRights.length];
        int idx = 0;

        // File Option
        settings[idx++] = 0x40;

        // Access Rights (2 bytes) - Read: Free, Others: Key 0
        settings[idx++] = (byte) 0xE0;  // Read: 0xE (Free), Write: 0x0
        settings[idx++] = 0x00;         // RW: 0x0, Change: 0x0

        // SDM Access Rights
        System.arraycopy(sdmAccessRights, 0, settings, idx, sdmAccessRights.length);
        idx += sdmAccessRights.length;

        // ASCII Mirror Configuration
        settings[idx++] = 0x01;  // Mirror PICC Data

        // SDM Options
        settings[idx++] = (byte) 0xC1;  // UID mirror + Read Counter + Limit

        // PICC Data Offset (3 bytes, little endian)
        writeLittleEndian3Bytes(settings, idx, piccDataOffset);
        idx += 3;

        // SDM MAC Input Offset (3 bytes, little endian)
        writeLittleEndian3Bytes(settings, idx, sdmMacInputOffset);
        idx += 3;

        // SDM MAC Offset (3 bytes, little endian)
        writeLittleEndian3Bytes(settings, idx, sdmMacOffset);

        return settings;
    }

    /**
     * 3바이트 little-endian 쓰기 헬퍼
     */
    private void writeLittleEndian3Bytes(byte[] buffer, int offset, int value) {
        buffer[offset] = (byte) (value & 0xFF);
        buffer[offset + 1] = (byte) ((value >> 8) & 0xFF);
        buffer[offset + 2] = (byte) ((value >> 16) & 0xFF);
    }

    /**
     * IVc 생성
     */
    private byte[] generateIVc(byte[] kSesAuthENC, byte[] ti, byte[] cmdCtr)
            throws GeneralSecurityException {
        byte[] ivInput = buildIVcInput(ti, cmdCtr);
        return cryptoService.encryptECB(kSesAuthENC, ivInput);
    }

    /**
     * IVc 입력 데이터 생성
     */
    private byte[] buildIVcInput(byte[] ti, byte[] cmdCtr) {
        byte[] ivInput = new byte[16];
        System.arraycopy(ti, 0, ivInput, 0, 4);      // TI (4 bytes)
        System.arraycopy(cmdCtr, 0, ivInput, 4, 2);  // CmdCtr (2 bytes)
        // 나머지는 0x00으로 패딩
        return ivInput;
    }

    /**
     * CMAC 계산
     */
    private byte[] calculateCmac(byte[] encryptedSettings)
            throws GeneralSecurityException {
        // 올바른 구조: Cmd || CmdCtr || TI || FileNo(평문) || EncryptedData
        byte[] cmacData = new byte[1 + 2 + 4 + 1 + encryptedSettings.length];
        int idx = 0;
        cmacData[idx++] = (byte) 0x5F; // INS
        System.arraycopy(session.commandCounter, 0, cmacData, idx, 2);
        idx += 2;
        System.arraycopy(session.transactionId, 0, cmacData, idx, 4);
        idx += 4;
        cmacData[idx++] = (byte) NDEF_FILE_NUMBER; // File Number (평문)
        System.arraycopy(encryptedSettings, 0, cmacData, idx, encryptedSettings.length);

        DebugLogger.logHex("CMAC Input", cmacData);
        DebugLogger.logHex("KSesAuthMAC", session.kSesAuthMAC);
        DebugLogger.logHex("EncryptedSettings", encryptedSettings);

        byte[] cmac = cryptoService.calculateCmac(session.kSesAuthMAC, cmacData);
        DebugLogger.logHex("CMAC (full 16 bytes)", cmac);

        return cmac;
    }

    /**
     * Change File Settings APDU 생성
     */
    private byte[] buildChangeFileSettingsApdu(byte[] encryptedSettings, byte[] mact) {
        // APDU 구조: CLA INS P1 P2 Lc FileNo(평문) EncData MACt Le
        byte[] apdu = new byte[5 + 1 + encryptedSettings.length + mact.length + 1];
        apdu[0] = (byte) 0x90; // CLA
        apdu[1] = (byte) 0x5F; // INS (ChangeFileSettings)
        apdu[2] = 0x00; // P1
        apdu[3] = 0x00; // P2
        apdu[4] = (byte)(1 + encryptedSettings.length + mact.length); // Lc
        apdu[5] = (byte) NDEF_FILE_NUMBER; // File Number (평문)
        System.arraycopy(encryptedSettings, 0, apdu, 6, encryptedSettings.length);
        System.arraycopy(mact, 0, apdu, 6 + encryptedSettings.length, mact.length);
        apdu[apdu.length - 1] = 0x00; // Le

        DebugLogger.logHex("MACt (truncated 8 bytes)", mact);

        return apdu;
    }
}