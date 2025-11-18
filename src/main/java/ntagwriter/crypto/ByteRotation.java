package ntagwriter.crypto;

import java.util.Objects;

/**
 * 바이트 배열 회전(순환 이동) 유틸리티.
 * EV2 인증 단계에서 RndA/RndB를 1바이트 회전할 때 사용한다.
 */
public final class ByteRotation {

    private ByteRotation() {
    }

    /**
     * 1바이트 좌측 회전.
     */
    public static byte[] rotateLeft(byte[] data) {
        return rotateLeft(data, 1);
    }

    /**
     * 지정한 바이트 수만큼 좌측 회전.
     */
    public static byte[] rotateLeft(byte[] data, int count) {
        Objects.requireNonNull(data, "data");
        if (data.length == 0) {
            return new byte[0];
        }
        int normalized = normalize(count, data.length);
        if (normalized == 0) {
            return data.clone();
        }

        byte[] rotated = new byte[data.length];
        System.arraycopy(data, normalized, rotated, 0, data.length - normalized);
        System.arraycopy(data, 0, rotated, data.length - normalized, normalized);
        return rotated;
    }

    /**
     * 1바이트 우측 회전.
     */
    public static byte[] rotateRight(byte[] data) {
        return rotateRight(data, 1);
    }

    /**
     * 지정한 바이트 수만큼 우측 회전.
     */
    public static byte[] rotateRight(byte[] data, int count) {
        Objects.requireNonNull(data, "data");
        if (data.length == 0) {
            return new byte[0];
        }
        int normalized = normalize(count, data.length);
        if (normalized == 0) {
            return data.clone();
        }
        // 우측 회전은 좌측 회전의 역연산
        int leftShift = data.length - normalized;
        return rotateLeft(data, leftShift);
    }

    private static int normalize(int count, int length) {
        int mod = count % length;
        if (mod < 0) {
            mod += length;
        }
        return mod;
    }
}
