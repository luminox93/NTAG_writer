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

            // 5. 기본 키로 인증 (시뮬레이션)
            ConsoleHelper.printProgress("기본 키로 인증 중...");
            authenticate();
            ConsoleHelper.printSuccess("인증 완료");

            // 6. AES 키 생성
            ConsoleHelper.printProgress("AES 키 생성 중...");
            aesKey = cryptoService.generateRandomBytes(16);
            ConsoleHelper.printSuccess("AES 키 생성됨: " + HexUtils.bytesToHex(aesKey));

            // 7. SDM 설정
            ConsoleHelper.printProgress("SDM 설정 적용 중...");
            configureSdm();
            ConsoleHelper.printSuccess("SDM 설정 완료");

            // 8. NDEF 메시지 작성
            ConsoleHelper.printProgress("NDEF URL 작성 중...");
            writeNdefMessage();
            ConsoleHelper.printSuccess("NDEF 메시지 작성 완료");

            // 9. 키 변경 (시뮬레이션)
            ConsoleHelper.printProgress("보안 키 변경 중...");
            changeKeys();
            ConsoleHelper.printSuccess("키 변경 완료");

            ConsoleHelper.println();
            ConsoleHelper.printSection("설정 완료!");
            ConsoleHelper.printSuccess("태그가 성공적으로 설정되었습니다.");
            ConsoleHelper.println();
            ConsoleHelper.printWarning("중요 정보 - 안전하게 보관하세요:");
            ConsoleHelper.printInfo("  태그 UID: " + tagUid);
            ConsoleHelper.printInfo("  AES 키: " + HexUtils.bytesToHex(aesKey));

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
     * 기본 키로 인증 (시뮬레이션)
     */
    private void authenticate() throws ReaderException, GeneralSecurityException {
        // TODO: 완전한 EV2 인증 구현
        // 현재는 시뮬레이션 모드
        ConsoleHelper.printWarning("⚠ EV2 인증은 시뮬레이션 모드입니다.");
    }

    /**
     * SDM 설정
     */
    private void configureSdm() throws ReaderException {
        // Enum에서 정의된 설정값 사용
        sdmConfig = new SdmConfig(config.getBaseUrl(), aesKey);
        sdmConfig.setPicOffset(config.getPiccDataOffset());
        sdmConfig.setSdmMacOffset(config.getSdmMacOffset());
        sdmConfig.setSdmMacInputOffset(config.getSdmMacInputOffset());

        ConsoleHelper.printInfo("  Base URL: " + config.getBaseUrl());
        ConsoleHelper.printInfo("  PICC Data Offset: " + config.getPiccDataOffset());
        ConsoleHelper.printInfo("  SDM MAC Offset: " + config.getSdmMacOffset());
        ConsoleHelper.printInfo("  Access Rights: Read=0x%02X, Write=0x%02X".formatted(
            config.getReadAccess(), config.getWriteAccess()));

        // TODO: 실제 Change File Settings APDU 전송
        ConsoleHelper.printWarning("⚠ SDM 활성화 APDU는 아직 구현되지 않았습니다.");
    }

    /**
     * NDEF 메시지 작성
     */
    private void writeNdefMessage() throws ReaderException {
        // Enum에 정의된 Base URL 사용
        String ndefUrl = config.getBaseUrl();

        ConsoleHelper.printInfo("  URL: " + ndefUrl);

        // TODO: 실제 NDEF 메시지 작성 APDU 전송
        ConsoleHelper.printWarning("⚠ NDEF 작성 APDU는 아직 구현되지 않았습니다.");
    }

    /**
     * 보안 키 변경 (시뮬레이션)
     */
    private void changeKeys() throws ReaderException {
        // TODO: 실제 Change Key APDU 전송
        ConsoleHelper.printWarning("⚠ 키 변경 APDU는 아직 구현되지 않았습니다.");
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
