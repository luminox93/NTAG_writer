package ntagwriter.util;

/**
 * 16진수 문자열과 바이트 배열 간 변환 유틸리티
 */
public class HexUtils {

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    /**
     * 바이트 배열을 16진수 문자열로 변환
     *
     * @param bytes 변환할 바이트 배열
     * @return 16진수 문자열 (예: "A1B2C3")
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = HEX_ARRAY[v >>> 4];
            hexChars[i * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * 16진수 문자열을 바이트 배열로 변환
     *
     * @param hexString 16진수 문자열 (예: "A1B2C3" 또는 "A1 B2 C3")
     * @return 바이트 배열
     */
    public static byte[] hexToBytes(String hexString) {
        if (hexString == null || hexString.isEmpty()) {
            return new byte[0];
        }

        // 공백 제거
        hexString = hexString.replaceAll("\\s+", "");

        // 홀수 길이면 앞에 0 추가
        if (hexString.length() % 2 != 0) {
            hexString = "0" + hexString;
        }

        int len = hexString.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }

        return data;
    }

    /**
     * 바이트 배열을 공백으로 구분된 16진수 문자열로 변환
     *
     * @param bytes 변환할 바이트 배열
     * @return 공백으로 구분된 16진수 문자열 (예: "A1 B2 C3")
     */
    public static String bytesToHexSpaced(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            int v = bytes[i] & 0xFF;
            sb.append(HEX_ARRAY[v >>> 4]);
            sb.append(HEX_ARRAY[v & 0x0F]);
        }
        return sb.toString();
    }

    /**
     * 바이트 값을 16진수 문자열로 변환
     *
     * @param b 변환할 바이트
     * @return 16진수 문자열 (예: "A1")
     */
    public static String byteToHex(byte b) {
        int v = b & 0xFF;
        return "" + HEX_ARRAY[v >>> 4] + HEX_ARRAY[v & 0x0F];
    }

    /**
     * 정수를 지정된 길이의 바이트 배열로 변환 (Big Endian)
     *
     * @param value  변환할 정수
     * @param length 바이트 배열 길이
     * @return 바이트 배열
     */
    public static byte[] intToBytes(int value, int length) {
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[length - 1 - i] = (byte) (value >> (i * 8));
        }
        return result;
    }

    /**
     * 바이트 배열을 정수로 변환 (Big Endian)
     *
     * @param bytes 변환할 바이트 배열
     * @return 정수 값
     */
    public static int bytesToInt(byte[] bytes) {
        int result = 0;
        for (byte b : bytes) {
            result = (result << 8) | (b & 0xFF);
        }
        return result;
    }
}
