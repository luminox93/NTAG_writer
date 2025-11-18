package ntagwriter.service;

import ntagwriter.domain.NtagDefaultConfig;
import ntagwriter.domain.SdmConfig;
import ntagwriter.reader.NfcReaderStrategy;
import ntagwriter.reader.ReaderException;
import ntagwriter.util.ApduCommand;
import ntagwriter.util.ConsoleHelper;
import ntagwriter.util.HexUtils;

import javax.smartcardio.ResponseAPDU;
import java.security.GeneralSecurityException;

/**
 * NTAG424 DNA 태그 자동 설정 서비스
 * 미리 정의된 enum 기본값을 사용하여 태그를 자동으로 설정합니다.
 */
public class Ntag424AutoSetupService {

    // NTAG424 DNA 기본 상수
    private static final byte[] NTAG424_AID = HexUtils.hexToBytes("D2760000850101");
    private static final byte[] DEFAULT_KEY = new byte[16]; // 00 00 00 00 ... (16 bytes)
    private static final byte KEY_NUMBER = 0x00;
    private static final int FILE_NUMBER = 0x02; // Standard Data File

    private final NfcReaderService readerService;
    private final CryptoService cryptoService;
    private final NtagDefaultConfig config;

    // 설정 상태
    private String tagUid;
    private byte[] aesKey;
    private SdmConfig sdmConfig;

    // 인증 세션 정보 (시뮬레이션용)
    private byte[] kSesAuthENC;
    private byte[] kSesAuthMAC;
    private byte[] transactionId;  // TI (4 bytes)
    private byte[] commandCounter; // CmdCtr (2 bytes)

    public Ntag424AutoSetupService(NfcReaderStrategy reader) {
        this(reader, NtagDefaultConfig.WALKD_PRODUCTION);
    }

    public Ntag424AutoSetupService(NfcReaderStrategy reader, NtagDefaultConfig config) {
        this.readerService = new NfcReaderService(reader);
        this.cryptoService = new CryptoService();
        this.config = config;
    }

    /**
     * 태그 접촉 후 자동 설정 실행
     */
    public boolean setupTag() {
        try {
            ConsoleHelper.printSection("NTAG424 자동 설정");
            ConsoleHelper.printInfo("설정값: " + config.name());
            ConsoleHelper.println();

            // 1. 리더기 연결
            ConsoleHelper.printProgress("리더기 연결 중...");
            readerService.connect();
            ConsoleHelper.printSuccess("리더기 연결됨: " + readerService.getReader().getReaderName());

            // 2. 태그 대기
            ConsoleHelper.printInfo("태그를 리더기에 올려주세요...");
            ConsoleHelper.println();

            // 3. UID 읽기
            ConsoleHelper.printProgress("태그 UID 읽기 중...");
            tagUid = readerService.readTagUid();
            ConsoleHelper.printSuccess("UID: " + tagUid);

            // 4. 애플리케이션 선택
            ConsoleHelper.printProgress("NTAG424 DNA 애플리케이션 선택 중...");
            selectApplication();
            ConsoleHelper.printSuccess("애플리케이션 선택 완료");

            // 5. 기본 키로 인증
            ConsoleHelper.printProgress("기본 키로 인증 중...");
            authenticate();
            ConsoleHelper.printSuccess("인증 완료");

            // 6. SDM 설정
            ConsoleHelper.printProgress("SDM 설정 적용 중...");
            configureSdm();
            ConsoleHelper.printSuccess("SDM 설정 완료");

            // 7. NDEF 메시지 작성
            ConsoleHelper.printProgress("NDEF URL 작성 중...");
            writeNdefMessage();
            ConsoleHelper.printSuccess("NDEF 메시지 작성 완료");

            ConsoleHelper.println();
            ConsoleHelper.printSection("설정 완료!");
            ConsoleHelper.printSuccess("태그가 성공적으로 설정되었습니다.");
            ConsoleHelper.println();
            ConsoleHelper.printInfo("태그 UID: " + tagUid);

            return true;

        } catch (Exception e) {
            ConsoleHelper.printError("설정 실패: " + e.getMessage());
            if (e.getCause() != null) {
                ConsoleHelper.printError("원인: " + e.getCause().getMessage());
            }
            return false;
        } finally {
            readerService.disconnect();
        }
    }

    /**
     * 애플리케이션 선택
     */
    private void selectApplication() throws ReaderException {
        byte[] selectApdu = ApduCommand.selectApplication(NTAG424_AID);
        ResponseAPDU response = readerService.sendCommand(selectApdu);

        if (!readerService.isSuccess(response)) {
            throw new ReaderException("애플리케이션 선택 실패: " +
                readerService.getErrorMessage(response));
        }
    }

    /**
     * 기본 키로 인증
     */
    private void authenticate() throws ReaderException, GeneralSecurityException {
        // TODO: 완전한 EV2 인증 구현
        // 현재는 공장 기본 키를 세션 키로 직접 사용
        kSesAuthENC = DEFAULT_KEY.clone();
        kSesAuthMAC = DEFAULT_KEY.clone();
        transactionId = new byte[]{0x00, 0x00, 0x00, 0x00};
        commandCounter = new byte[]{0x00, 0x00};
    }

    /**
     * CRC32 계산 (ISO/IEC 13239)
     */
    private byte[] calculateCrc32(byte[] data) {
        long crc = 0xFFFFFFFFL;
        for (byte b : data) {
            crc ^= (b & 0xFF);
            for (int i = 0; i < 8; i++) {
                if ((crc & 1) != 0) {
                    crc = (crc >>> 1) ^ 0xEDB88320L;
                } else {
                    crc = crc >>> 1;
                }
            }
        }
        crc = ~crc;

        // Little-endian 4 bytes
        return new byte[]{
            (byte) (crc & 0xFF),
            (byte) ((crc >> 8) & 0xFF),
            (byte) ((crc >> 16) & 0xFF),
            (byte) ((crc >> 24) & 0xFF)
        };
    }

    /**
     * CMAC을 MACt로 변환 (짝수 바이트만 추출, 8 bytes)
     * AN12196: MACt = CMAC[1,3,5,7,9,11,13,15]
     */
    private byte[] truncateMac(byte[] cmac) {
        byte[] mact = new byte[8];
        for (int i = 0; i < 8; i++) {
            mact[i] = cmac[i * 2 + 1];  // 1, 3, 5, 7, 9, 11, 13, 15
        }
        return mact;
    }

    /**
     * IVc 생성
     * IVc = E(KSesAuthENC, A55A || TI || CmdCtr || 0000000000000000)
     */
    private byte[] generateIVc(byte[] kSesAuthENC, byte[] ti, byte[] cmdCtr) throws GeneralSecurityException {
        byte[] ivInput = new byte[16];
        ivInput[0] = (byte) 0xA5;
        ivInput[1] = (byte) 0x5A;
        System.arraycopy(ti, 0, ivInput, 2, 4);
        System.arraycopy(cmdCtr, 0, ivInput, 6, 2);
        // Remaining bytes are 0x00

        return cryptoService.encryptECB(kSesAuthENC, ivInput);
    }

    /**
     * SDM 설정
     */
    private void configureSdm() throws ReaderException {
        try {
            // Enum에서 정의된 설정값 사용
            sdmConfig = new SdmConfig(config.getBaseUrl(), DEFAULT_KEY);
            sdmConfig.setPicOffset(config.getPiccDataOffset());
            sdmConfig.setSdmMacOffset(config.getSdmMacOffset());
            sdmConfig.setSdmMacInputOffset(config.getSdmMacInputOffset());

            ConsoleHelper.printInfo("  Base URL: " + config.getBaseUrl());
            ConsoleHelper.printInfo("  PICC Data Offset: " + config.getPiccDataOffset());
            ConsoleHelper.printInfo("  SDM MAC Offset: " + config.getSdmMacOffset());
            ConsoleHelper.printInfo("  Access Rights: Read=0x%02X, Write=0x%02X".formatted(
                config.getReadAccess(), config.getWriteAccess()));

            // Change File Settings APDU 생성 및 전송
            byte[] settingsData = buildFileSettingsData();
            byte[] ivc = generateIVc(kSesAuthENC, transactionId, commandCounter);
            byte[] encryptedSettings = cryptoService.encryptCBC(kSesAuthENC, ivc,
                cryptoService.addPadding(settingsData, 16));

            // CMAC 계산
            byte[] cmacData = new byte[1 + 2 + 4 + 1 + encryptedSettings.length];
            int idx = 0;
            cmacData[idx++] = (byte) 0x5F; // INS
            System.arraycopy(commandCounter, 0, cmacData, idx, 2); idx += 2;
            System.arraycopy(transactionId, 0, cmacData, idx, 4); idx += 4;
            cmacData[idx++] = (byte) FILE_NUMBER;
            System.arraycopy(encryptedSettings, 0, cmacData, idx, encryptedSettings.length);

            byte[] cmac = cryptoService.calculateCmac(kSesAuthMAC, cmacData);
            byte[] mact = truncateMac(cmac);

            byte[] apdu = ApduCommand.changeFileSettings((byte) FILE_NUMBER, encryptedSettings, mact);
            ResponseAPDU response = readerService.sendCommand(apdu);

            if (!readerService.isSuccess(response)) {
                throw new ReaderException("SDM 설정 실패: " + readerService.getErrorMessage(response));
            }

            incrementCommandCounter();

        } catch (GeneralSecurityException e) {
            throw new ReaderException("SDM 설정 암호화 오류: " + e.getMessage());
        }
    }

    /**
     * File Settings 데이터 구성
     * AN12196 Table 18 참조
     */
    private byte[] buildFileSettingsData() {
        // FileOption || AccessRights(2) || SDMOptions || SDMAccessRights(2) ||
        // ENCPICCDataOffset(3) || SDMMACOffset(3) || SDMMACInputOffset(3)
        byte[] data = new byte[15];
        int idx = 0;

        // FileOption: 0x40 (SDM enabled, CommMode.Plain)
        data[idx++] = 0x40;

        // AccessRights: 2 bytes (little-endian)
        int accessRights = (config.getWriteAccess() << 12) | (config.getReadAccess() << 8) |
                           (config.getReadAccess() << 4) | config.getReadAccess();
        data[idx++] = (byte) (accessRights & 0xFF);
        data[idx++] = (byte) ((accessRights >> 8) & 0xFF);

        // SDMOptions: 0xC1 (UID mirror, SDMReadCtr, ASCII encoding)
        data[idx++] = (byte) 0xC1;

        // SDMAccessRights: 0xF121 (little-endian)
        data[idx++] = 0x21;
        data[idx++] = (byte) 0xF1;

        // ENCPICCDataOffset (3 bytes, little-endian)
        int piccOffset = config.getPiccDataOffset();
        data[idx++] = (byte) (piccOffset & 0xFF);
        data[idx++] = (byte) ((piccOffset >> 8) & 0xFF);
        data[idx++] = (byte) ((piccOffset >> 16) & 0xFF);

        // SDMMACOffset (3 bytes, little-endian)
        int macOffset = config.getSdmMacOffset();
        data[idx++] = (byte) (macOffset & 0xFF);
        data[idx++] = (byte) ((macOffset >> 8) & 0xFF);
        data[idx++] = (byte) ((macOffset >> 16) & 0xFF);

        // SDMMACInputOffset (3 bytes, little-endian)
        int macInputOffset = config.getSdmMacInputOffset();
        data[idx++] = (byte) (macInputOffset & 0xFF);
        data[idx++] = (byte) ((macInputOffset >> 8) & 0xFF);
        data[idx++] = (byte) ((macInputOffset >> 16) & 0xFF);

        return data;
    }

    /**
     * NDEF 메시지 작성
     */
    private void writeNdefMessage() throws ReaderException {
        // Enum에 정의된 Base URL 사용
        String ndefUrl = config.getBaseUrl();
        ConsoleHelper.printInfo("  URL: " + ndefUrl);

        // NDEF 메시지 구성
        byte[] ndefMessage = buildNdefMessage(ndefUrl);

        // CommMode.PLAIN으로 Write Data 전송
        byte[] apdu = ApduCommand.writeDataPlain((byte) FILE_NUMBER, 0, ndefMessage);
        ResponseAPDU response = readerService.sendCommand(apdu);

        if (!readerService.isSuccess(response)) {
            throw new ReaderException("NDEF 메시지 작성 실패: " + readerService.getErrorMessage(response));
        }
    }

    /**
     * NDEF 메시지 구성
     */
    private byte[] buildNdefMessage(String url) {
        // NDEF URI Record 구성
        // Type 3: https://
        byte[] urlBytes = url.replace("https://", "").getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // NDEF Message: TLV(03) + Length + NDEF Record
        // NDEF Record: Header + Type Length + Payload Length + Type + Payload
        int payloadLen = 1 + urlBytes.length; // URI prefix + URL
        int recordLen = 1 + 1 + 1 + 1 + payloadLen; // Header + TypeLen + PayloadLen + Type + Payload

        byte[] ndef = new byte[2 + recordLen + 1]; // TLV + Record + Terminator TLV
        int idx = 0;

        // NDEF Message TLV
        ndef[idx++] = 0x03; // NDEF Message TLV
        ndef[idx++] = (byte) recordLen;

        // NDEF Record Header: MB=1, ME=1, CF=0, SR=1, IL=0, TNF=1 (Well-known)
        ndef[idx++] = (byte) 0xD1;

        // Type Length
        ndef[idx++] = 0x01;

        // Payload Length (short record)
        ndef[idx++] = (byte) payloadLen;

        // Type: "U" (URI)
        ndef[idx++] = 0x55;

        // Payload: URI identifier code (0x04 = https://) + URL
        ndef[idx++] = 0x04;
        System.arraycopy(urlBytes, 0, ndef, idx, urlBytes.length);
        idx += urlBytes.length;

        // Terminator TLV
        ndef[idx++] = (byte) 0xFE;

        return ndef;
    }

    /**
     * 보안 키 변경
     */
    private void changeKeys() throws ReaderException {
        try {
            // Key 0번 변경 (Application Master Key)
            changeKey((byte) 0x00, DEFAULT_KEY, aesKey, (byte) 0x01);
            ConsoleHelper.printInfo("  Key 0 변경 완료");

        } catch (GeneralSecurityException e) {
            throw new ReaderException("키 변경 암호화 오류: " + e.getMessage());
        }
    }

    /**
     * 개별 키 변경
     * AN12196 Section 5.16 참조
     */
    private void changeKey(byte keyNo, byte[] oldKey, byte[] newKey, byte newKeyVersion)
            throws ReaderException, GeneralSecurityException {

        // Case 2: KeyNo to change = AuthKey (Key 0)
        // KeyData = New Key || New Key Version || Padding
        byte[] keyData = new byte[32];
        System.arraycopy(newKey, 0, keyData, 0, 16);
        keyData[16] = newKeyVersion;
        // Remaining bytes are padding (0x00)

        // IVc 생성 및 암호화
        byte[] ivc = generateIVc(kSesAuthENC, transactionId, commandCounter);
        byte[] encryptedKeyData = cryptoService.encryptCBC(kSesAuthENC, ivc, keyData);

        // CMAC 계산
        // Cmd || CmdCtr || TI || KeyNo || E(KeyData)
        byte[] cmacData = new byte[1 + 2 + 4 + 1 + encryptedKeyData.length];
        int idx = 0;
        cmacData[idx++] = (byte) 0xC4; // INS_CHANGE_KEY
        System.arraycopy(commandCounter, 0, cmacData, idx, 2); idx += 2;
        System.arraycopy(transactionId, 0, cmacData, idx, 4); idx += 4;
        cmacData[idx++] = keyNo;
        System.arraycopy(encryptedKeyData, 0, cmacData, idx, encryptedKeyData.length);

        byte[] cmac = cryptoService.calculateCmac(kSesAuthMAC, cmacData);
        byte[] mact = truncateMac(cmac);

        // APDU 전송
        byte[] apdu = ApduCommand.changeKey(keyNo, encryptedKeyData, mact);
        ResponseAPDU response = readerService.sendCommand(apdu);

        if (!readerService.isSuccess(response)) {
            throw new ReaderException("키 변경 실패: " + readerService.getErrorMessage(response));
        }

        incrementCommandCounter();
    }

    /**
     * 커맨드 카운터 증가
     */
    private void incrementCommandCounter() {
        int counter = ((commandCounter[1] & 0xFF) << 8) | (commandCounter[0] & 0xFF);
        counter++;
        commandCounter[0] = (byte) (counter & 0xFF);
        commandCounter[1] = (byte) ((counter >> 8) & 0xFF);
    }

    // Getters
    public String getTagUid() {
        return tagUid;
    }

    public byte[] getAesKey() {
        return aesKey;
    }

    public SdmConfig getSdmConfig() {
        return sdmConfig;
    }

    public NtagDefaultConfig getConfig() {
        return config;
    }
}
