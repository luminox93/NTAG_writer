package ntagwriter;

import ntagwriter.reader.IdentivReader;
import ntagwriter.reader.ReaderException;
import ntagwriter.service.Ntag424AutoSetupService;
import ntagwriter.service.Ntag424SetupService;
import ntagwriter.util.ConsoleHelper;
import ntagwriter.util.HexUtils;

public class NtagWriterApplication {
    public static void main(String[] args) {
        ConsoleHelper.printSection("NTAG Writer - NFC 리더기 애플리케이션");

        // 모드 선택
        ConsoleHelper.println();
        ConsoleHelper.printInfo("실행 모드를 선택하세요:");
        ConsoleHelper.printInfo("  1. NTAG424 자동 설정 (프로덕션 모드 - 기본값 자동 적용)");
        ConsoleHelper.printInfo("  2. 간단 테스트 (리더기 연결 및 UID 읽기)");
        ConsoleHelper.printInfo("  3. NTAG424 대화형 설정 (디버그 모드 - 단계별 설정)");

        String mode = ConsoleHelper.input("\n모드를 선택하세요 (1, 2, 또는 3)", "1");

        IdentivReader reader = new IdentivReader();

        try {
            switch (mode) {
                case "1" -> runAutoSetup(reader);
                case "2" -> runSimpleTest(reader);
                case "3" -> runInteractiveSetup(reader);
                default -> {
                    ConsoleHelper.printWarning("잘못된 모드 선택. 자동 설정 모드로 실행합니다.");
                    runAutoSetup(reader);
                }
            }

        } catch (Exception e) {
            ConsoleHelper.printError("오류 발생: " + e.getMessage());
            if (e.getCause() != null) {
                ConsoleHelper.printError("원인: " + e.getCause().getMessage());
            }
        } finally {
            reader.disconnect();
        }
    }

    /**
     * 간단 테스트 모드
     */
    private static void runSimpleTest(IdentivReader reader) throws ReaderException {
        ConsoleHelper.printSection("간단 테스트 모드");

        // 리더기 연결
        ConsoleHelper.printProgress("리더기 연결 시도 중...");
        reader.connect();
        ConsoleHelper.printSuccess("연결 성공: " + reader.getReaderName());

        // 태그 UID 읽기
        ConsoleHelper.printProgress("태그 UID 읽기 중...");
        byte[] uid = reader.readUid();
        ConsoleHelper.printSuccess("UID: " + HexUtils.bytesToHex(uid));

        ConsoleHelper.println();
        ConsoleHelper.printSuccess("테스트 완료!");
    }

    /**
     * NTAG424 자동 설정 모드 (프로덕션)
     */
    private static void runAutoSetup(IdentivReader reader) {
        Ntag424AutoSetupService setupService = new Ntag424AutoSetupService(reader);
        setupService.setupTag();
    }

    /**
     * NTAG424 대화형 설정 모드 (디버그)
     */
    private static void runInteractiveSetup(IdentivReader reader) {
        ConsoleHelper.printSection("대화형 설정 모드 (디버그)");

        Ntag424SetupService setupService = new Ntag424SetupService(reader);

        boolean success = setupService.startSetup();

        if (success) {
            ConsoleHelper.println();
            ConsoleHelper.printSection("설정 요약");
            ConsoleHelper.printInfo("태그 UID: " + setupService.getTagUid());
            ConsoleHelper.printInfo("Base URL: " + setupService.getBaseUrl());
            ConsoleHelper.printInfo("AES 키: " + HexUtils.bytesToHex(setupService.getAesKey()));

            ConsoleHelper.println();
            ConsoleHelper.printWarning("이 정보를 안전한 곳에 저장하세요!");
        } else {
            ConsoleHelper.println();
            ConsoleHelper.printError("설정이 완료되지 않았습니다.");
        }
    }
}
