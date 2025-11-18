package ntagwriter.util;

/**
 * 디버그 로깅 유틸리티
 * 반복적인 HEX 데이터 로깅 패턴을 간소화
 */
public class DebugLogger {

    private static boolean debugEnabled = true;

    /**
     * HEX 데이터 로깅 (기본 2칸 들여쓰기)
     */
    public static void logHex(String label, byte[] data) {
        if (!debugEnabled || data == null) return;
        ConsoleHelper.printInfo("  [DEBUG] " + label + ": " + HexUtils.bytesToHex(data));
    }

    /**
     * HEX 데이터 로깅 (커스텀 들여쓰기)
     */
    public static void logHex(String label, byte[] data, int indentLevel) {
        if (!debugEnabled || data == null) return;
        String indent = "  ".repeat(indentLevel);
        ConsoleHelper.printInfo(indent + "[DEBUG] " + label + ": " + HexUtils.bytesToHex(data));
    }

    /**
     * 문자열 디버그 로깅
     */
    public static void log(String message) {
        if (!debugEnabled) return;
        ConsoleHelper.printInfo("  [DEBUG] " + message);
    }

    /**
     * 문자열 디버그 로깅 (커스텀 들여쓰기)
     */
    public static void log(String message, int indentLevel) {
        if (!debugEnabled) return;
        String indent = "  ".repeat(indentLevel);
        ConsoleHelper.printInfo(indent + "[DEBUG] " + message);
    }

    /**
     * 디버그 모드 설정
     */
    public static void setEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    /**
     * 디버그 모드 확인
     */
    public static boolean isEnabled() {
        return debugEnabled;
    }
}