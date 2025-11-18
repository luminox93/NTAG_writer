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
    private static final int NDEF_FILE_NUMBER = 0x02; // NDEF file (SDM 설정 대상)

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
    private byte[] transactionId; // TI (4 bytes)
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
            ConsoleHelper.println();

            // 3. UID 읽기
            ConsoleHelper.printProgress("태그 UID 읽기 중...");
            tagUid = readerService.readTagUid();
            ConsoleHelper.printSuccess("UID: " + tagUid);

            // 4. 애플리케이션 선택
            ConsoleHelper.printProgress("NTAG424 DNA 애플리케이션 선택 중...");
            selectApplication();
            ConsoleHelper.printSuccess("애플리케이션 선택 완료");

            // 5. 기본 키로 인증 (SDM 설정 및 NDEF 쓰기용 EV2 세션)
            ConsoleHelper.printProgress("기본 키로 인증 중...");
            authenticate();
            ConsoleHelper.printSuccess("인증 완료");

            // 6. SDM 설정을 먼저! (일부 구현에서는 이 순서가 중요함)
            ConsoleHelper.printProgress("SDM 설정 적용 중...");
            configureSdm();
            ConsoleHelper.printSuccess("SDM 설정 완료");

            // 7. NDEF 메시지 작성 (SDM 설정 후 WriteData)
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
     * 기본 키로 인증 (EV2First)
     * AN12196 Section 3.6 참조
     */
    private void authenticate() throws ReaderException, GeneralSecurityException {
        // 1. AuthenticateEV2First 명령 전송 (RndB 요청)
        byte[] cmd1 = ApduCommand.authenticateEV2First(KEY_NUMBER, (byte) 0x03);
        ResponseAPDU response1 = readerService.sendCommand(cmd1);

        if (response1.getSW() != 0x91AF) {
            throw new ReaderException("EV2 인증 시작 실패: " +
                    readerService.getErrorMessage(response1));
        }

        // 2. 암호화된 RndB 수신 및 복호화
        byte[] encRndB = response1.getData();
        byte[] rndB = cryptoService.decryptCBC(DEFAULT_KEY, new byte[16], encRndB);

        // 3. RndA 생성
        byte[] rndA = cryptoService.generateRandomBytes(16);

        // 4. RndB를 왼쪽으로 1바이트 rotate
        byte[] rndBRotated = rotateLeft(rndB);

        // 5. RndA || RndB' 연결 및 암호화
        byte[] combined = new byte[32];
        System.arraycopy(rndA, 0, combined, 0, 16);
        System.arraycopy(rndBRotated, 0, combined, 16, 16);

        byte[] encCombined = cryptoService.encryptCBC(DEFAULT_KEY, new byte[16], combined);

        // 6. AF 명령으로 암호화된 데이터 전송
        byte[] cmd2 = new byte[5 + encCombined.length + 1];
        cmd2[0] = (byte) 0x90;
        cmd2[1] = (byte) 0xAF;
        cmd2[2] = 0x00;
        cmd2[3] = 0x00;
        cmd2[4] = (byte) encCombined.length;
        System.arraycopy(encCombined, 0, cmd2, 5, encCombined.length);
        cmd2[cmd2.length - 1] = 0x00;

        ResponseAPDU response2 = readerService.sendCommand(cmd2);

        if (response2.getSW() != 0x9100 && response2.getSW() != 0x9000) {
            throw new ReaderException("EV2 인증 실패: " +
                    readerService.getErrorMessage(response2));
        }

        // 7. 응답 복호화 및 검증
        byte[] encResponse = response2.getData();
        byte[] decResponse = cryptoService.decryptCBC(DEFAULT_KEY, new byte[16], encResponse);

        // TI 추출 (첫 4 바이트)
        transactionId = new byte[4];
        System.arraycopy(decResponse, 0, transactionId, 0, 4);

        // RndA' 추출 (4~19 바이트)
        byte[] rndARotated = new byte[16];
        System.arraycopy(decResponse, 4, rndARotated, 0, 16);

        // RndA' 검증
        byte[] rndAVerify = rotateRight(rndARotated);
        if (!java.util.Arrays.equals(rndA, rndAVerify)) {
            throw new ReaderException("EV2 인증 검증 실패: RndA 불일치");
        }

        // 8. 세션 키 유도
        byte[] sv1 = buildSessionVector((byte) 0xA5, (byte) 0x5A, rndA, rndB);
        kSesAuthENC = cryptoService.calculateCmac(DEFAULT_KEY, sv1);

        byte[] sv2 = buildSessionVector((byte) 0x5A, (byte) 0xA5, rndA, rndB);
        kSesAuthMAC = cryptoService.calculateCmac(DEFAULT_KEY, sv2);

        // 9. 커맨드 카운터 초기화
        // AuthenticateEV2First 성공 후 CmdCtr은 0000h로 초기화되지만,
        // 인증 응답 처리 후 증가시켜야 함 (다음 명령은 0001h)
        commandCounter = new byte[] { 0x01, 0x00 }; // Little-endian: 0x0001

        ConsoleHelper.printInfo("  TI: " + HexUtils.bytesToHex(transactionId));
        ConsoleHelper.printInfo("  Initial CmdCtr: " + HexUtils.bytesToHex(commandCounter));
    }

    /**
     * 세션 벡터 생성
     * SV = prefix1 prefix2 00 01 00 80 || RndA[15:14] || (RndA[13:8] XOR
     * RndB[15:10]) || RndB[9:0] || RndA[7:0]
     */
    private byte[] buildSessionVector(byte prefix1, byte prefix2, byte[] rndA, byte[] rndB) {
        byte[] sv = new byte[32];
        int idx = 0;

        // Prefix
        sv[idx++] = prefix1;
        sv[idx++] = prefix2;
        sv[idx++] = 0x00;
        sv[idx++] = 0x01;
        sv[idx++] = 0x00;
        sv[idx++] = (byte) 0x80;

        // RndA[15:14]
        sv[idx++] = rndA[15];
        sv[idx++] = rndA[14];

        // RndA[13:8] XOR RndB[15:10]
        for (int i = 0; i < 6; i++) {
            sv[idx++] = (byte) (rndA[13 - i] ^ rndB[15 - i]);
        }

        // RndB[9:0]
        for (int i = 9; i >= 0; i--) {
            sv[idx++] = rndB[i];
        }

        // RndA[7:0]
        for (int i = 7; i >= 0; i--) {
            sv[idx++] = rndA[i];
        }

        return sv;
    }

    /**
     * 바이트 배열을 왼쪽으로 1바이트 rotate
     */
    private byte[] rotateLeft(byte[] data) {
        byte[] rotated = new byte[data.length];
        System.arraycopy(data, 1, rotated, 0, data.length - 1);
        rotated[data.length - 1] = data[0];
        return rotated;
    }

    /**
     * 바이트 배열을 오른쪽으로 1바이트 rotate
     */
    private byte[] rotateRight(byte[] data) {
        byte[] rotated = new byte[data.length];
        rotated[0] = data[data.length - 1];
        System.arraycopy(data, 0, rotated, 1, data.length - 1);
        return rotated;
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
        return new byte[] {
                (byte) (crc & 0xFF),
                (byte) ((crc >> 8) & 0xFF),
                (byte) ((crc >> 16) & 0xFF),
                (byte) ((crc >> 24) & 0xFF)
        };
    }

    /**
     * CMAC을 MACt로 변환
     * NT4H2421Gx에서 MAC은 16바이트 CMAC 출력 중 8개의 짝수 번째 바이트만 사용
     * (인덱스 1, 3, 5, 7, 9, 11, 13, 15)
     */
    private byte[] truncateMac(byte[] cmac) {
        byte[] mact = new byte[8];
        for (int i = 0; i < 8; i++) {
            mact[i] = cmac[i * 2 + 1]; // 짝수 인덱스 (0-based이므로 1, 3, 5...)
        }
        ConsoleHelper.printInfo("  [DEBUG] CMAC bytes used for truncation: " +
            String.format("[1]=%02X [3]=%02X [5]=%02X [7]=%02X [9]=%02X [11]=%02X [13]=%02X [15]=%02X",
                cmac[1], cmac[3], cmac[5], cmac[7], cmac[9], cmac[11], cmac[13], cmac[15]));
        return mact;
    }

    /**
     * IVc 입력 데이터 생성
     * AN12196 Figure 10: A55A || TI || CmdCtr || 8바이트 0
     */
    private byte[] buildIVcInput(byte[] ti, byte[] cmdCtr) {
        byte[] ivInput = new byte[16];
        ivInput[0] = (byte) 0xA5;
        ivInput[1] = (byte) 0x5A;
        System.arraycopy(ti, 0, ivInput, 2, 4);
        System.arraycopy(cmdCtr, 0, ivInput, 6, 2);
        // 나머지는 0 유지
        return ivInput;
    }

    /**
     * IVc 생성
     * IVc = E(KSesAuthENC, TI || CmdCtr || 0000000000000000)
     */
    private byte[] generateIVc(byte[] kSesAuthENC, byte[] ti, byte[] cmdCtr) throws GeneralSecurityException {
        byte[] ivInput = buildIVcInput(ti, cmdCtr);
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

            // File Number는 평문으로, Settings만 암호화
            byte[] paddedData = cryptoService.addPadding(settingsData, 16);
            ConsoleHelper.printInfo("  [DEBUG] Settings Data: " + HexUtils.bytesToHex(settingsData));
            ConsoleHelper.printInfo("  [DEBUG] Padded Data: " + HexUtils.bytesToHex(paddedData));

            byte[] ivc = generateIVc(kSesAuthENC, transactionId, commandCounter);
            ConsoleHelper.printInfo(
                    "  [DEBUG] IVc Input: " + HexUtils.bytesToHex(buildIVcInput(transactionId, commandCounter)));
            ConsoleHelper.printInfo("  [DEBUG] KSesAuthENC: " + HexUtils.bytesToHex(kSesAuthENC));
            ConsoleHelper.printInfo("  [DEBUG] IVc: " + HexUtils.bytesToHex(ivc));

            byte[] encryptedSettings = cryptoService.encryptCBC(kSesAuthENC, ivc, paddedData);

            // CMAC 계산
            // 올바른 구조: Cmd || CmdCtr || TI || FileNo(평문) || EncryptedData
            byte[] cmacData = new byte[1 + 2 + 4 + 1 + encryptedSettings.length];
            int idx = 0;
            cmacData[idx++] = (byte) 0x5F; // INS
            System.arraycopy(commandCounter, 0, cmacData, idx, 2);
            idx += 2;
            System.arraycopy(transactionId, 0, cmacData, idx, 4);
            idx += 4;
            cmacData[idx++] = (byte) NDEF_FILE_NUMBER; // File Number (평문)
            System.arraycopy(encryptedSettings, 0, cmacData, idx, encryptedSettings.length);

            ConsoleHelper.printInfo("  [DEBUG] CMAC Input: " + HexUtils.bytesToHex(cmacData));
            ConsoleHelper.printInfo("  [DEBUG] KSesAuthMAC: " + HexUtils.bytesToHex(kSesAuthMAC));
            ConsoleHelper.printInfo("  [DEBUG] EncryptedSettings: " + HexUtils.bytesToHex(encryptedSettings));

            byte[] cmac = cryptoService.calculateCmac(kSesAuthMAC, cmacData);
            ConsoleHelper.printInfo("  [DEBUG] CMAC (full 16 bytes): " + HexUtils.bytesToHex(cmac));

            byte[] mact = truncateMac(cmac);
            ConsoleHelper.printInfo("  [DEBUG] MACt (truncated 8 bytes): " + HexUtils.bytesToHex(mact));

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
     * NTAG 424 DNA 실제 사양
     */
    private byte[] buildFileSettingsData() {
        // 더 간단한 구조로 시도
        byte[] data = new byte[13];  // 축소된 크기
        int idx = 0;

        // FileOption: 0x40 (SDM enabled)
        data[idx++] = 0x40;

        ConsoleHelper.printInfo("  [DEBUG] Building File Settings Data (Simplified - 13 bytes):");

        // AccessRights: 2 bytes only
        data[idx++] = (byte) 0xE0;  // Read (0xE) / Write (0x0)
        data[idx++] = 0x00;

        // SDMOptions: 0xC1 (UID mirror + SDMReadCtr mirror, ASCII encoding)
        data[idx++] = (byte) 0xC1;

        // SDMAccessRights: 2 bytes
        data[idx++] = (byte) 0xF1;  // Reserved=F, SDMCtrRet=1
        data[idx++] = 0x21;  // SDMMetaRead=2, SDMFileRead=1

        // PICCDataOffset (3 bytes, little-endian)
        int piccOffset = 43;  // 0x2B
        data[idx++] = (byte) (piccOffset & 0xFF);
        data[idx++] = (byte) ((piccOffset >> 8) & 0xFF);
        data[idx++] = (byte) ((piccOffset >> 16) & 0xFF);

        // SDMMACOffset (3 bytes, little-endian)
        int macOffset = 81;  // 0x51
        data[idx++] = (byte) (macOffset & 0xFF);
        data[idx++] = (byte) ((macOffset >> 8) & 0xFF);
        data[idx++] = (byte) ((macOffset >> 16) & 0xFF);

        ConsoleHelper.printInfo("  [DEBUG] CmdData: " + HexUtils.bytesToHex(data));
        ConsoleHelper.printInfo("  [DEBUG] CmdData Length: " + data.length + " bytes");

        return data;
    }

    /**
     * NDEF 메시지 작성
     */
    private void writeNdefMessage() throws ReaderException {
        // Enum에 정의된 Base URL 사용
        String ndefUrl = config.getBaseUrl();
        ConsoleHelper.printInfo("  URL: " + ndefUrl);

        byte[] ndefMessage = buildNdefMessage(ndefUrl);

        byte[] apdu = ApduCommand.writeData((byte) NDEF_FILE_NUMBER, 0, ndefMessage);
        ResponseAPDU response = readerService.sendCommand(apdu);

        if (!readerService.isSuccess(response)) {
            throw new ReaderException("NDEF 메시지 작성 실패: " + readerService.getErrorMessage(response));
        }

        // 태그는 Plain WriteData 시 CmdCtr을 증가시키지 않으므로 로컬 카운터도 유지해야 함
        // incrementCommandCounter(); // REMOVED: Plain mode does not increment CmdCtr

        // 테스트: WriteData 후에 인증이 유지되는지 확인
        ConsoleHelper.printInfo("  [DEBUG] WriteData 후 인증 상태 확인 필요");
    }

    /**
     * NDEF 메시지 구성
     */
    private byte[] buildNdefMessage(String url) {
        // NTAG424(NFC Type 4) NDEF 파일은 NLEN(2바이트) + NDEF 메시지 구조를 사용한다.
        String normalizedUrl = url;
        byte uriIdentifier = 0x00;

        if (normalizedUrl.startsWith("https://")) {
            uriIdentifier = 0x04;
            normalizedUrl = normalizedUrl.substring("https://".length());
        } else if (normalizedUrl.startsWith("http://")) {
            uriIdentifier = 0x03;
            normalizedUrl = normalizedUrl.substring("http://".length());
        }

        byte[] urlBytes = normalizedUrl.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int payloadLen = 1 + urlBytes.length; // URI prefix + URL

        byte[] record = new byte[1 + 1 + 1 + 1 + payloadLen];
        int idx = 0;
        record[idx++] = (byte) 0xD1; // Header (MB/ME/SR set, TNF=0x01)
        record[idx++] = 0x01; // Type Length
        record[idx++] = (byte) payloadLen; // Payload Length (short RECORD)
        record[idx++] = 0x55; // Type "U"
        record[idx++] = uriIdentifier;
        System.arraycopy(urlBytes, 0, record, idx, urlBytes.length);

        int nlen = record.length;
        byte[] ndef = new byte[2 + nlen];
        ndef[0] = (byte) ((nlen >> 8) & 0xFF);
        ndef[1] = (byte) (nlen & 0xFF);
        System.arraycopy(record, 0, ndef, 2, nlen);

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
        System.arraycopy(commandCounter, 0, cmacData, idx, 2);
        idx += 2;
        System.arraycopy(transactionId, 0, cmacData, idx, 4);
        idx += 4;
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
