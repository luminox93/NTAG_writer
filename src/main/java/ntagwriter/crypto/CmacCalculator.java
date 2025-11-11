package ntagwriter.crypto;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.macs.CMac;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 * AES-CMAC 계산 유틸리티
 * NTAG424 DNA의 보안 메시징을 위한 CMAC 생성 및 검증
 */
public class CmacCalculator {

    /**
     * AES-CMAC 계산
     *
     * @param key  16바이트 AES 키
     * @param data CMAC을 계산할 데이터
     * @return 16바이트 CMAC 값
     */
    public static byte[] calculateCmac(byte[] key, byte[] data) {
        return calculateCmac(key, data, 16);
    }

    /**
     * AES-CMAC 계산 (길이 지정)
     *
     * @param key        16바이트 AES 키
     * @param data       CMAC을 계산할 데이터
     * @param macLength  출력할 CMAC 길이 (바이트)
     * @return 지정된 길이의 CMAC 값
     */
    public static byte[] calculateCmac(byte[] key, byte[] data, int macLength) {
        try {
            BlockCipher cipher = new AESEngine();
            CMac cmac = new CMac(cipher);
            KeyParameter keyParam = new KeyParameter(key);

            cmac.init(keyParam);
            cmac.update(data, 0, data.length);

            byte[] output = new byte[cmac.getMacSize()];
            cmac.doFinal(output, 0);

            // 요청된 길이만큼 반환
            if (macLength < output.length) {
                byte[] truncated = new byte[macLength];
                System.arraycopy(output, 0, truncated, 0, macLength);
                return truncated;
            }

            return output;
        } catch (Exception e) {
            throw new RuntimeException("CMAC 계산 실패: " + e.getMessage(), e);
        }
    }

    /**
     * CMAC 검증
     *
     * @param key          16바이트 AES 키
     * @param data         원본 데이터
     * @param expectedMac  예상되는 CMAC 값
     * @return 검증 성공 여부
     */
    public static boolean verifyCmac(byte[] key, byte[] data, byte[] expectedMac) {
        byte[] calculatedMac = calculateCmac(key, data, expectedMac.length);
        return constantTimeEquals(calculatedMac, expectedMac);
    }

    /**
     * 타이밍 공격을 방지하는 상수 시간 비교
     *
     * @param a 첫 번째 배열
     * @param b 두 번째 배열
     * @return 두 배열이 동일한지 여부
     */
    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    /**
     * NTAG424 세션 키 생성을 위한 SV (Session Vector) 계산
     *
     * @param key     마스터 키
     * @param rndA    리더의 랜덤 값
     * @param rndB    태그의 랜덤 값
     * @param svType  SV 타입 (0x00: ENC, 0x01: MAC)
     * @return 세션 키
     */
    public static byte[] deriveSessionKey(byte[] key, byte[] rndA, byte[] rndB, byte svType) {
        // SV = 0xA5 || 0x5A || 0x00 || 0x01 || 0x00 || svType || RndA[0..1] || RndB[0..1] || ...
        byte[] sv = new byte[32];
        sv[0] = (byte) 0xA5;
        sv[1] = (byte) 0x5A;
        sv[2] = 0x00;
        sv[3] = 0x01;
        sv[4] = 0x00;
        sv[5] = svType;

        // RndA와 RndB의 처음 2바이트씩 추가
        System.arraycopy(rndA, 0, sv, 6, 2);
        System.arraycopy(rndB, 0, sv, 8, 2);

        // 나머지는 0으로 채움

        return calculateCmac(key, sv);
    }
}
