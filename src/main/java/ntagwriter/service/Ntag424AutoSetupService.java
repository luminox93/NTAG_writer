package ntagwriter.service;

import ntagwriter.domain.NtagDefaultConfig;
import ntagwriter.domain.SdmConfig;
import ntagwriter.reader.NfcReaderStrategy;
import ntagwriter.reader.ReaderException;
import ntagwriter.util.*;

import javax.smartcardio.ResponseAPDU;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

/**
 * NTAG424 DNA 태그 자동 설정 서비스 (리팩토링)
 * 미리 정의된 enum 기본값을 사용하여 태그를 자동으로 설정합니다.
 */
public class Ntag424AutoSetupService {

    // NTAG424 DNA 기본 상수
    private static final byte[] NTAG424_AID = HexUtils.hexToBytes("D2760000850101");
    private static final byte[] DEFAULT_KEY = new byte[16]; // 00 00 00 00 ... (16 bytes)

    private final NfcReaderService readerService;
    private final CryptoService cryptoService;
    private final Ev2AuthenticationService ev2AuthService;
    private final SdmConfigurationService sdmService;
    private final NdefWriteService ndefService;
    private final KeyManagementService keyService;
    private final NtagDefaultConfig config;

    // 설정 상태
    private String tagUid;
    private byte[] aesKey;
    private SdmConfig sdmConfig;
    private SessionContext sessionContext;

    public Ntag424AutoSetupService(NfcReaderStrategy reader) {
        this(reader, NtagDefaultConfig.WALKD_PRODUCTION);
    }

    public Ntag424AutoSetupService(NfcReaderStrategy reader, NtagDefaultConfig config) {
        this.readerService = new NfcReaderService(reader);
        this.cryptoService = new CryptoService();
        this.ev2AuthService = new Ev2AuthenticationService(readerService, cryptoService);
        this.sdmService = new SdmConfigurationService(readerService, cryptoService);
        this.ndefService = new NdefWriteService(readerService);
        this.keyService = new KeyManagementService(readerService, cryptoService);
        this.config = config;
    }

    /**
     * 태그 설정 실행
     * @return 설정 성공 여부
     */
    public boolean setupTag() {
        try {
            printHeader();

            // 1. 리더기 연결 및 태그 감지
            tagUid = readerService.connectAndReadUid();
            ConsoleHelper.printSuccess("✓ 태그 감지됨: UID = " + tagUid);

            // 2. 애플리케이션 선택
            selectApplication();

            // 3. 인증
            authenticate();

            // 4. SDM 설정
            configureSdm();

            // 5. NDEF 메시지 작성
            writeNdefMessage();

            // 6. 키 변경
            changeKeys();

            // 7. 설정 검증
            verifySetup();

            printSuccess();
            return true;

        } catch (Exception e) {
            ConsoleHelper.printError("✗ 설정 실패: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            readerService.disconnect();
        }
    }

    private void printHeader() {
        ConsoleHelper.printLine('=');
        ConsoleHelper.printInfo(" NTAG424 DNA 태그 자동 설정");
        ConsoleHelper.printLine('=');
        ConsoleHelper.printInfo("ℹ 설정 구성: " + config.name());
        ConsoleHelper.printInfo("ℹ Base URL: " + config.getBaseUrl());
        ConsoleHelper.printInfo("");
    }

    private void selectApplication() throws ReaderException {
        ConsoleHelper.printInfo("→ NTAG424 애플리케이션 선택 중...");

        byte[] apdu = new byte[5 + NTAG424_AID.length + 1];
        apdu[0] = 0x00; // CLA
        apdu[1] = (byte) 0xA4; // INS (SELECT)
        apdu[2] = 0x04; // P1 (Select by AID)
        apdu[3] = 0x00; // P2
        apdu[4] = (byte) NTAG424_AID.length; // Lc
        System.arraycopy(NTAG424_AID, 0, apdu, 5, NTAG424_AID.length);
        apdu[5 + NTAG424_AID.length] = 0x00; // Le

        ResponseAPDU response = readerService.sendCommand(apdu);
        ApduResponseValidator.validateSuccess(response, "애플리케이션 선택");

        ConsoleHelper.printSuccess("✓ NTAG424 애플리케이션 선택됨");
    }

    private void authenticate() throws ReaderException, GeneralSecurityException {
        ConsoleHelper.printInfo("→ 태그 인증 중...");

        // EV2 인증 수행
        Ev2AuthenticationService.AuthenticationResult authResult =
            ev2AuthService.authenticateEV2First((byte) 0x00, DEFAULT_KEY);

        if (!authResult.isSuccess()) {
            throw new ReaderException("인증 실패");
        }

        // 세션 컨텍스트 생성
        this.sessionContext = SessionContext.forSdmConfiguration(
            authResult.getKSesAuthENC(),
            authResult.getKSesAuthMAC(),
            authResult.getTransactionId(),
            authResult.getCommandCounter()
        );

        ConsoleHelper.printSuccess("✓ 인증 성공");
        ConsoleHelper.printInfo("  Transaction ID: " +
            HexUtils.bytesToHex(sessionContext.transactionId));
    }

    private void configureSdm() throws ReaderException, GeneralSecurityException {
        // SDM 서비스에 세션 컨텍스트 전달
        sdmService.setSession(sessionContext);

        // SDM 설정 실행
        sdmService.configureSdm(
            config.getSdmAccessRightsBytes(),
            config.getPiccDataOffset(),
            config.getSdmMacInputOffset(),
            config.getSdmMacOffset()
        );

        // Command Counter 증가
        CommandCounterManager.increment(sessionContext.commandCounter);

        // SdmConfig 객체 생성
        this.sdmConfig = new SdmConfig(
            config.getBaseUrl(),
            config.getPiccDataOffset(),
            config.getSdmMacInputOffset(),
            config.getSdmMacOffset()
        );
    }

    private void writeNdefMessage() throws ReaderException {
        ndefService.writeNdefMessage(config.getBaseUrl());
        CommandCounterManager.increment(sessionContext.commandCounter);
    }

    private void changeKeys() throws ReaderException, GeneralSecurityException {
        // AES 키 생성
        this.aesKey = generateAesKey();
        ConsoleHelper.printInfo("  생성된 AES 키: " + HexUtils.bytesToHex(aesKey));

        // 키 서비스에 세션 컨텍스트 전달
        keyService.setSession(sessionContext);

        // 모든 키 변경 (내부에서 CommandCounter 증가 처리)
        keyService.changeAllKeys(aesKey);
    }

    private void verifySetup() {
        ConsoleHelper.printLine('-');
        ConsoleHelper.printSuccess("✓ 설정 검증 완료");
        ConsoleHelper.printInfo("  - SDM이 활성화되었습니다");
        ConsoleHelper.printInfo("  - NDEF 메시지가 작성되었습니다");
        ConsoleHelper.printInfo("  - 보안 키가 변경되었습니다");
    }

    private void printSuccess() {
        ConsoleHelper.printLine('=');
        ConsoleHelper.printSuccess(" 설정 완료!");
        ConsoleHelper.printLine('=');
        ConsoleHelper.printSuccess("✓ NTAG424 태그가 성공적으로 설정되었습니다.");
        ConsoleHelper.printInfo("");
        ConsoleHelper.printInfo("ℹ 태그 정보:");
        ConsoleHelper.printInfo("  UID: " + tagUid);
        ConsoleHelper.printInfo("  Base URL: " + config.getBaseUrl());
        ConsoleHelper.printInfo("  AES Key: " + HexUtils.bytesToHex(aesKey));
        ConsoleHelper.printInfo("");
        ConsoleHelper.printWarning("⚠ 중요: AES 키를 안전한 곳에 보관하세요!");
    }

    private byte[] generateAesKey() {
        SecureRandom random = new SecureRandom();
        byte[] key = new byte[16];
        random.nextBytes(key);
        return key;
    }

    // Getters (불변 필드 직접 반환)
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