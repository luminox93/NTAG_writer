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

    /**
     * PRF(K, SV) = AES-CMAC(K, SV)
     *
     * 데이터시트 9.1.7 절에 명시된 세션 키 생성용 PRF.
     */
    public static byte[] prfCmac(byte[] key, byte[] sessionVector) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(sessionVector, "sessionVector");
        if (key.length != 16) {
            throw new IllegalArgumentException("PRF 키는 16바이트 AES 키여야 합니다.");
        }
        return CmacCalculator.calculateCmac(key, sessionVector);
    }
}
