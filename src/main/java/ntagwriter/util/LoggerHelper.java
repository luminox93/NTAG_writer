package ntagwriter.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 로깅 유틸리티 클래스
 * SLF4J와 Logback을 사용한 통합 로깅 관리
 */
public class LoggerHelper {

    /**
     * 클래스에 맞는 Logger 인스턴스 생성
     *
     * @param clazz 로거를 생성할 클래스
     * @return Logger 인스턴스
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    /**
     * 이름으로 Logger 인스턴스 생성
     *
     * @param name 로거 이름
     * @return Logger 인스턴스
     */
    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }

    /**
     * Hex 문자열을 포함한 디버그 메시지 로깅
     *
     * @param logger Logger 인스턴스
     * @param message 로그 메시지
     * @param bytes 바이트 배열
     */
    public static void logHexDebug(Logger logger, String message, byte[] bytes) {
        if (logger.isDebugEnabled()) {
            logger.debug("{}: {}", message, HexUtils.bytesToHex(bytes));
        }
    }

    /**
     * 구분선 로깅 (디버그 레벨)
     *
     * @param logger Logger 인스턴스
     * @param title 구분선 제목
     */
    public static void logSection(Logger logger, String title) {
        if (logger.isDebugEnabled()) {
            logger.debug("=== {} ===", title);
        }
    }
}