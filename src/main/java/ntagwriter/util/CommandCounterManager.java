package ntagwriter.util;

/**
 * Command Counter 관리 유틸리티
 * NTAG424 DNA의 2바이트 little-endian 커맨드 카운터를 관리
 */
public class CommandCounterManager {

    /**
     * Command Counter 증가
     * @param commandCounter 2바이트 카운터 배열
     */
    public static void increment(byte[] commandCounter) {
        if (commandCounter == null || commandCounter.length != 2) {
            throw new IllegalArgumentException("Command counter must be 2 bytes");
        }

        int counter = toInt(commandCounter);
        counter++;
        fromInt(counter, commandCounter);
    }

    /**
     * 바이트 배열을 정수로 변환 (little-endian)
     * @param commandCounter 2바이트 카운터 배열
     * @return 정수값
     */
    public static int toInt(byte[] commandCounter) {
        if (commandCounter == null || commandCounter.length != 2) {
            throw new IllegalArgumentException("Command counter must be 2 bytes");
        }
        return (commandCounter[0] & 0xFF) | ((commandCounter[1] & 0xFF) << 8);
    }

    /**
     * 정수를 바이트 배열로 변환 (little-endian)
     * @param value 카운터 값
     * @param commandCounter 결과를 저장할 2바이트 배열
     */
    public static void fromInt(int value, byte[] commandCounter) {
        if (commandCounter == null || commandCounter.length != 2) {
            throw new IllegalArgumentException("Command counter must be 2 bytes");
        }
        commandCounter[0] = (byte) (value & 0xFF);
        commandCounter[1] = (byte) ((value >> 8) & 0xFF);
    }

    /**
     * 새로운 Command Counter 생성
     * @param initialValue 초기값
     * @return 2바이트 배열
     */
    public static byte[] create(int initialValue) {
        byte[] counter = new byte[2];
        fromInt(initialValue, counter);
        return counter;
    }
}