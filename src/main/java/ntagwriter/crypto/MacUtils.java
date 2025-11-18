package ntagwriter.crypto;

import java.util.Objects;

/**
 * NT4H2421Gx MAC 관련 유틸리티.
 */
public final class MacUtils {

    private MacUtils() {
    }

    /**
     * CMAC 16바이트 결과에서 인덱스 0, 2, 4...14만 추출해 8바이트 MACt 생성.
     */
    public static byte[] truncateMac(byte[] fullMac) {
        Objects.requireNonNull(fullMac, "fullMac");
        if (fullMac.length < 16) {
            throw new IllegalArgumentException("CMAC 결과는 16바이트 이상이어야 합니다.");
        }
        byte[] truncated = new byte[8];
        for (int i = 0; i < 8; i++) {
            truncated[i] = fullMac[i * 2];
        }
        return truncated;
    }
}
