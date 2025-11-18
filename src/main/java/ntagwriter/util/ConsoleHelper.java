package ntagwriter.util;

import java.util.Scanner;

/**
 * 콘솔 대화형 UI 헬퍼
 * 사용자와의 대화형 상호작용을 돕는 유틸리티 클래스
 */
public class ConsoleHelper {

    private static final Scanner scanner = new Scanner(System.in);

    private static final String INFO_PREFIX = "[INFO] ";
    private static final String SUCCESS_PREFIX = "[OK] ";
    private static final String WARNING_PREFIX = "[WARN] ";
    private static final String ERROR_PREFIX = "[ERROR] ";
    private static final String PROGRESS_PREFIX = "-> ";

    /**
     * 사용자에게 예/아니오 질문
     * @param message 질문 메시지
     * @return true (예), false (아니오)
     */
    public static boolean confirm(String message) {
        System.out.print("\n" + message + " (y/n): ");
        String input = scanner.nextLine().trim().toLowerCase();
        return input.equals("y") || input.equals("yes") || input.equals("예");
    }

    /**
     * 사용자 입력 받기
     * @param prompt 프롬프트 메시지
     * @return 사용자 입력 문자열
     */
    public static String input(String prompt) {
        System.out.print(prompt + ": ");
        return scanner.nextLine().trim();
    }

    /**
     * 사용자 입력 받기 (기본값 포함)
     * @param prompt 프롬프트 메시지
     * @param defaultValue 기본값
     * @return 사용자 입력 문자열 또는 기본값
     */
    public static String input(String prompt, String defaultValue) {
        System.out.print(prompt + " [기본값: " + defaultValue + "]: ");
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? defaultValue : input;
    }

    /**
     * 섹션 헤더 출력
     * @param title 섹션 제목
     */
    public static void printSection(String title) {
        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println(" " + title);
        System.out.println("=".repeat(60));
    }

    /**
     * 단계 헤더 출력
     * @param stepNumber 단계 번호
     * @param stepName 단계 이름
     */
    public static void printStep(int stepNumber, String stepName) {
        System.out.println();
        System.out.println("-".repeat(60));
        System.out.println(String.format("[단계 %d] %s", stepNumber, stepName));
        System.out.println("-".repeat(60));
    }

    /**
     * 정보 메시지 출력
     * @param message 메시지
     */
    public static void printInfo(String message) {
        System.out.println(INFO_PREFIX + message);
    }

    /**
     * 성공 메시지 출력
     * @param message 메시지
     */
    public static void printSuccess(String message) {
        System.out.println(SUCCESS_PREFIX + message);
    }

    /**
     * 경고 메시지 출력
     * @param message 메시지
     */
    public static void printWarning(String message) {
        System.out.println(WARNING_PREFIX + message);
    }

    /**
     * 오류 메시지 출력
     * @param message 메시지
     */
    public static void printError(String message) {
        System.err.println(ERROR_PREFIX + message);
    }

    /**
     * 진행 메시지 출력
     * @param message 메시지
     */
    public static void printProgress(String message) {
        System.out.println(PROGRESS_PREFIX + message);
    }

    /**
     * 빈 줄 출력
     */
    public static void println() {
        System.out.println();
    }

    /**
     * 계속하려면 Enter 키 대기
     */
    public static void pressEnterToContinue() {
        System.out.print("\n계속하려면 Enter 키를 누르세요...");
        scanner.nextLine();
    }
}
