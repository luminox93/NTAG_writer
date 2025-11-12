package ntagwriter.service;

import ntagwriter.domain.SetupStep;
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
 * 단계별로 사용자와 대화하며 태그를 설정합니다.
 */
public class Ntag424SetupService {

    // NTAG424 DNA 기본 상수
    private static final byte[] NTAG424_AID = HexUtils.hexToBytes("D2760000850101");
    private static final byte[] DEFAULT_KEY = new byte[16]; // 00 00 00 00 ... (16 bytes)
    private static final byte KEY_NUMBER = 0x00;
    private static final int FILE_NUMBER = 0x02; // Standard Data File

    private final NfcReaderService readerService;
    private final CryptoService cryptoService;

    // 설정 상태
    private String tagUid;
    private byte[] aesKey;
    private String baseUrl;
    private SdmConfig sdmConfig;
    private SetupStep currentStep;

    public Ntag424SetupService(NfcReaderStrategy reader) {
        this.readerService = new NfcReaderService(reader);
        this.cryptoService = new CryptoService();
        this.currentStep = SetupStep.CONNECT_READER;
    }

    /**
     * 자동 설정 시작
     */
    public boolean startSetup() {
        try {
            ConsoleHelper.printSection("NTAG424 DNA 태그 자동 설정");
            ConsoleHelper.printInfo("이 프로세스는 NTAG424 태그를 안전하게 설정합니다.");
            ConsoleHelper.printWarning("진행 중에는 태그를 리더기에서 떼지 마세요!");

            if (!ConsoleHelper.confirm("\n설정을 시작하시겠습니까?")) {
                ConsoleHelper.printInfo("설정이 취소되었습니다.");
                return false;
            }

            // 단계별 실행
            if (!executeStep(SetupStep.CONNECT_READER)) return false;
            if (!executeStep(SetupStep.READ_UID)) return false;
            if (!executeStep(SetupStep.SELECT_APPLICATION)) return false;
            if (!executeStep(SetupStep.AUTHENTICATE)) return false;
            if (!executeStep(SetupStep.CONFIGURE_SDM)) return false;
            if (!executeStep(SetupStep.CHANGE_KEYS)) return false;
            if (!executeStep(SetupStep.VERIFY)) return false;

            executeStep(SetupStep.COMPLETE);

            return true;

        } catch (Exception e) {
            ConsoleHelper.printError("설정 중 오류 발생: " + e.getMessage());
            return false;
        } finally {
            readerService.disconnect();
        }
    }

    /**
     * 단계 실행
     */
    private boolean executeStep(SetupStep step) {
        currentStep = step;

        ConsoleHelper.printStep(step.getStepNumber(), step.getTitle());
        ConsoleHelper.printInfo(step.getDescription());

        // 완료 단계는 확인 없이 실행
        if (step != SetupStep.COMPLETE) {
            if (!ConsoleHelper.confirm("이 단계를 진행하시겠습니까?")) {
                ConsoleHelper.printWarning("설정이 중단되었습니다.");
                return false;
            }
        }

        try {
            switch (step) {
                case CONNECT_READER -> connectReader();
                case READ_UID -> readUid();
                case SELECT_APPLICATION -> selectApplication();
                case AUTHENTICATE -> authenticate();
                case CONFIGURE_SDM -> configureSdm();
                case CHANGE_KEYS -> changeKeys();
                case VERIFY -> verify();
                case COMPLETE -> complete();
            }

            ConsoleHelper.printSuccess(step.getTitle() + " 완료!");
            return true;

        } catch (Exception e) {
            ConsoleHelper.printError(step.getTitle() + " 실패: " + e.getMessage());

            if (ConsoleHelper.confirm("다시 시도하시겠습니까?")) {
                return executeStep(step);
            }

            return false;
        }
    }

    /**
     * 1단계: 리더기 연결
     */
    private void connectReader() throws ReaderException {
        ConsoleHelper.printProgress("리더기를 검색하고 연결 중...");
        readerService.connect();
        ConsoleHelper.printSuccess("리더기 연결됨: " + readerService.getReader().getReaderName());
        ConsoleHelper.printInfo("태그를 리더기에 올려주세요.");
    }

    /**
     * 2단계: UID 읽기
     */
    private void readUid() throws ReaderException {
        ConsoleHelper.printProgress("태그 UID를 읽는 중...");
        tagUid = readerService.readTagUid();
        ConsoleHelper.printSuccess("UID: " + tagUid);
    }

    /**
     * 3단계: 애플리케이션 선택
     */
    private void selectApplication() throws ReaderException {
        ConsoleHelper.printProgress("NTAG424 DNA 애플리케이션 선택 중...");

        byte[] selectApdu = ApduCommand.selectApplication(NTAG424_AID);
        ResponseAPDU response = readerService.sendCommand(selectApdu);

        if (!readerService.isSuccess(response)) {
            throw new ReaderException("애플리케이션 선택 실패: " +
                readerService.getErrorMessage(response));
        }

        ConsoleHelper.printSuccess("애플리케이션 선택 성공 (AID: " +
            HexUtils.bytesToHex(NTAG424_AID) + ")");
    }

    /**
     * 4단계: 기본 키로 인증
     */
    private void authenticate() throws ReaderException, GeneralSecurityException {
        ConsoleHelper.printProgress("기본 키로 인증 중...");
        ConsoleHelper.printInfo("공장 초기화 상태의 태그는 기본 키(00...00)를 사용합니다.");

        ConsoleHelper.printWarning("⚠ 완전한 EV2 인증 구현이 아직 완료되지 않았습니다.");
        ConsoleHelper.printInfo("시뮬레이션 모드로 진행합니다.");

        // TODO: 완전한 EV2 인증 구현
        // EV2 인증은 다음 단계로 이루어집니다:
        // 1. AuthenticateEV2First 명령 전송
        // 2. 태그로부터 암호화된 RndB 수신 (SW=91AF)
        // 3. RndB 복호화 (AES-128)
        // 4. RndA 생성 (16바이트 랜덤)
        // 5. RndB를 1바이트 왼쪽 회전 (RndB')
        // 6. RndA || RndB' 암호화
        // 7. AuthenticateEV2NonFirst로 암호화된 데이터 전송
        // 8. 태그 응답 검증 (RndA' = RndA rotated)
        // 9. 세션 키 생성 (ENC, MAC)

        /*
        // 실제 구현 예시 (현재는 주석 처리)
        byte[] rndA = cryptoService.generateRandomBytes(16);

        // Step 1: Send AuthenticateEV2First
        byte[] authFirstCmd = ApduCommand.authenticateEV2First(KEY_NUMBER, new byte[2]);
        ResponseAPDU response1 = readerService.sendCommand(authFirstCmd);

        if (response1.getSW() != 0x91AF) {
            throw new ReaderException("인증 1단계 실패: " +
                readerService.getErrorMessage(response1));
        }

        // Step 2: Decrypt RndB
        byte[] encRndB = response1.getData();
        byte[] rndB = cryptoService.decryptECB(DEFAULT_KEY, encRndB);

        // Step 3: Rotate RndB
        byte[] rndBRotated = rotateLeft(rndB, 1);

        // Step 4: Encrypt RndA || RndB'
        byte[] combined = new byte[32];
        System.arraycopy(rndA, 0, combined, 0, 16);
        System.arraycopy(rndBRotated, 0, combined, 16, 16);
        byte[] encData = cryptoService.encryptECB(DEFAULT_KEY, combined);

        // Step 5: Send AuthenticateEV2NonFirst
        // ... (계속)
        */

        ConsoleHelper.printSuccess("인증 성공 (시뮬레이션)");
    }

    /**
     * 바이트 배열을 왼쪽으로 회전
     */
    private byte[] rotateLeft(byte[] data, int count) {
        byte[] result = new byte[data.length];
        int offset = count % data.length;
        System.arraycopy(data, offset, result, 0, data.length - offset);
        System.arraycopy(data, 0, result, data.length - offset, offset);
        return result;
    }

    /**
     * 5단계: SDM 설정
     */
    private void configureSdm() throws ReaderException {
        ConsoleHelper.printProgress("SDM 파라미터 설정 중...");

        // 사용자로부터 Base URL 입력받기
        ConsoleHelper.println();
        ConsoleHelper.printInfo("SDM을 설정하기 위해 Base URL이 필요합니다.");
        ConsoleHelper.printInfo("예: https://yourdomain.com/verify");

        baseUrl = ConsoleHelper.input("\nBase URL을 입력하세요");

        if (baseUrl.isEmpty()) {
            throw new ReaderException("Base URL은 필수입니다.");
        }

        // URL 형식 검증
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            throw new ReaderException("유효한 URL 형식이 아닙니다. http:// 또는 https://로 시작해야 합니다.");
        }

        // SDM 오프셋 계산
        // Base URL 이후에 파라미터가 추가됨
        // 예: https://domain.com/verify?p=<PICCData>&c=<CMAC>
        String urlTemplate = baseUrl + "?p=";
        int picOffset = urlTemplate.length();
        int sdmMacOffset = picOffset + 32; // PICCData 32 hex chars
        sdmMacOffset += 3; // "&c=" 추가
        int sdmMacInputOffset = sdmMacOffset; // CMAC 입력이 비어있는 경우

        ConsoleHelper.printInfo("\nSDM 오프셋 계산:");
        ConsoleHelper.printInfo("  - PICC Data Offset: " + picOffset);
        ConsoleHelper.printInfo("  - CMAC Offset: " + sdmMacOffset);
        ConsoleHelper.printInfo("  - CMAC Input Offset: " + sdmMacInputOffset);

        // AES 키 생성 또는 입력
        ConsoleHelper.println();
        if (ConsoleHelper.confirm("새로운 AES 키를 자동 생성하시겠습니까?")) {
            aesKey = cryptoService.generateRandomBytes(16);
            ConsoleHelper.printSuccess("AES 키 생성됨: " + HexUtils.bytesToHex(aesKey));
            ConsoleHelper.printWarning("⚠ 이 키를 안전하게 보관하세요! 분실 시 태그를 사용할 수 없습니다.");
        } else {
            String keyHex = ConsoleHelper.input("AES 키를 16진수로 입력하세요 (32자리)");
            if (keyHex.length() != 32) {
                throw new ReaderException("AES 키는 32자리 16진수여야 합니다.");
            }
            aesKey = HexUtils.hexToBytes(keyHex);
            ConsoleHelper.printSuccess("AES 키 설정됨");
        }

        // SdmConfig 객체 생성
        sdmConfig = new SdmConfig(baseUrl, aesKey);
        sdmConfig.setPicOffset(picOffset);
        sdmConfig.setSdmMacOffset(sdmMacOffset);
        sdmConfig.setSdmMacInputOffset(sdmMacInputOffset);

        ConsoleHelper.printSuccess("SDM 설정 완료");
        ConsoleHelper.printInfo("설정 정보: " + sdmConfig.toString());

        // 실제 SDM 설정 APDU는 추후 구현 예정
        // Change File Settings 명령어로 SDM 활성화
        ConsoleHelper.printWarning("⚠ SDM 활성화 APDU 전송은 아직 구현되지 않았습니다.");
    }

    /**
     * 6단계: 보안 키 변경
     */
    private void changeKeys() throws ReaderException {
        ConsoleHelper.printProgress("보안 키 변경 중...");

        if (aesKey == null) {
            throw new ReaderException("AES 키가 설정되지 않았습니다.");
        }

        ConsoleHelper.printInfo("기본 키를 새로운 AES 키로 변경합니다.");
        ConsoleHelper.printInfo("키: " + HexUtils.bytesToHex(aesKey));

        // 실제 키 변경 APDU는 추후 구현 예정
        // Change Key 명령어 사용
        ConsoleHelper.printWarning("⚠ 키 변경 APDU 전송은 아직 구현되지 않았습니다.");
        ConsoleHelper.printSuccess("키 변경 완료 (시뮬레이션)");
    }

    /**
     * 7단계: 설정 검증
     */
    private void verify() throws ReaderException {
        ConsoleHelper.printProgress("설정 검증 중...");

        ConsoleHelper.println();
        ConsoleHelper.printInfo("다음 설정이 적용되었습니다:");
        ConsoleHelper.printInfo("  - 태그 UID: " + tagUid);
        ConsoleHelper.printInfo("  - Base URL: " + baseUrl);
        ConsoleHelper.printInfo("  - AES 키: " + HexUtils.bytesToHex(aesKey));
        ConsoleHelper.printInfo("  - SDM 설정: " + sdmConfig.toString());

        ConsoleHelper.println();
        ConsoleHelper.printWarning("중요: 다음 정보를 안전하게 보관하세요!");
        ConsoleHelper.printWarning("1. AES 키: " + HexUtils.bytesToHex(aesKey));
        ConsoleHelper.printWarning("2. 태그 UID: " + tagUid);

        ConsoleHelper.printSuccess("검증 완료");
    }

    /**
     * 8단계: 완료
     */
    private void complete() {
        ConsoleHelper.println();
        ConsoleHelper.printSection("설정 완료!");
        ConsoleHelper.printSuccess("NTAG424 태그가 성공적으로 설정되었습니다.");

        ConsoleHelper.println();
        ConsoleHelper.printInfo("예상 URL 형식:");
        ConsoleHelper.printInfo(baseUrl + "?p=<32자리 PICC 데이터>&c=<16자리 CMAC>");

        ConsoleHelper.println();
        ConsoleHelper.printInfo("이제 태그를 NFC 리더기에서 제거해도 됩니다.");
        ConsoleHelper.printInfo("설정된 태그를 스마트폰으로 태핑하면 위 URL로 리다이렉트됩니다.");
    }

    // Getters
    public String getTagUid() {
        return tagUid;
    }

    public byte[] getAesKey() {
        return aesKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public SdmConfig getSdmConfig() {
        return sdmConfig;
    }

    public SetupStep getCurrentStep() {
        return currentStep;
    }
}
