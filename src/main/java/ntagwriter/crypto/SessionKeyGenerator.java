package ntagwriter.crypto;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.macs.CMac;
import org.bouncycastle.crypto.params.KeyParameter;
import ntagwriter.util.HexUtils;

/**
 * NTAG 424 DNA Session Key Generator
 *
 * 데이터 시트 9.1.7 Session Key Generation 섹션 구현
 * NIST SP 800-108 Counter Mode에 따른 키 유도 함수(KDF) 구현
 *
 * 세션 키 생성 공식:
 * - SesAuthENCKey = PRF(Kx, SV1)
 * - SesAuthMACKey = PRF(Kx, SV2)
 *
 * 여기서 PRF는 AES-CMAC (NIST SP 800-38B)
 */
public class SessionKeyGenerator {

    // 데이터 시트에서 정의한 레이블 상수
    private static final byte[] LABEL_ENC = {(byte) 0xA5, (byte) 0x5A}; // Encryption key label
    private static final byte[] LABEL_MAC = {(byte) 0x5A, (byte) 0xA5}; // MAC key label

    // 고정 카운터와 길이 (128비트 키 생성용)
    private static final byte[] COUNTER = {0x00, 0x01}; // 0001h
    private static final byte[] LENGTH = {0x00, (byte) 0x80}; // 0080h (128 bits)

    /**
     * 세션 키 쌍 생성 (인증 후)
     *
     * @param authKey 인증에 사용된 키 (Kx)
     * @param rndA 리더가 생성한 랜덤 값 (16 bytes)
     * @param rndB 태그가 생성한 랜덤 값 (16 bytes)
     * @return [SesAuthENCKey, SesAuthMACKey] 배열
     */
    public static byte[][] generateSessionKeys(byte[] authKey, byte[] rndA, byte[] rndB) {
        if (authKey == null || authKey.length != 16) {
            throw new IllegalArgumentException("인증 키는 16바이트여야 합니다");
        }
        if (rndA == null || rndA.length != 16) {
            throw new IllegalArgumentException("RndA는 16바이트여야 합니다");
        }
        if (rndB == null || rndB.length != 16) {
            throw new IllegalArgumentException("RndB는 16바이트여야 합니다");
        }

        // SV1, SV2 생성
        byte[] sv1 = generateSV1(rndA, rndB);
        byte[] sv2 = generateSV2(rndA, rndB);

        // 세션 키 생성 (PRF = AES-CMAC)
        byte[] sesAuthENCKey = calculatePRF(authKey, sv1);
        byte[] sesAuthMACKey = calculatePRF(authKey, sv2);

        return new byte[][] {sesAuthENCKey, sesAuthMACKey};
    }

    /**
     * SV1 생성 (암호화 키용)
     *
     * SV1 = A5h || 5Ah || 00h || 01h || 00h || 80h || RndA[15..14] ||
     *       (RndA[13..8] XOR RndB[15..10]) || RndB[9..0] || RndA[7..0]
     *
     * @param rndA 리더 랜덤 값
     * @param rndB 태그 랜덤 값
     * @return 32바이트 SV1
     */
    private static byte[] generateSV1(byte[] rndA, byte[] rndB) {
        byte[] sv1 = new byte[32];

        // 헤더: A5h || 5Ah || 00h || 01h || 00h || 80h
        sv1[0] = (byte) 0xA5;
        sv1[1] = (byte) 0x5A;
        sv1[2] = 0x00;
        sv1[3] = 0x01;
        sv1[4] = 0x00;
        sv1[5] = (byte) 0x80;

        // RndA[15..14] (2 bytes)
        sv1[6] = rndA[15];
        sv1[7] = rndA[14];

        // RndA[13..8] XOR RndB[15..10] (6 bytes)
        for (int i = 0; i < 6; i++) {
            sv1[8 + i] = (byte) (rndA[13 - i] ^ rndB[15 - i]);
        }

        // RndB[9..0] (10 bytes)
        for (int i = 0; i < 10; i++) {
            sv1[14 + i] = rndB[9 - i];
        }

        // RndA[7..0] (8 bytes)
        for (int i = 0; i < 8; i++) {
            sv1[24 + i] = rndA[7 - i];
        }

        return sv1;
    }

    /**
     * SV2 생성 (MAC 키용)
     *
     * SV2 = 5Ah || A5h || 00h || 01h || 00h || 80h || RndA[15..14] ||
     *       (RndA[13..8] XOR RndB[15..10]) || RndB[9..0] || RndA[7..0]
     *
     * @param rndA 리더 랜덤 값
     * @param rndB 태그 랜덤 값
     * @return 32바이트 SV2
     */
    private static byte[] generateSV2(byte[] rndA, byte[] rndB) {
        byte[] sv2 = new byte[32];

        // 헤더: 5Ah || A5h || 00h || 01h || 00h || 80h (SV1과 처음 2바이트만 반대)
        sv2[0] = (byte) 0x5A;
        sv2[1] = (byte) 0xA5;
        sv2[2] = 0x00;
        sv2[3] = 0x01;
        sv2[4] = 0x00;
        sv2[5] = (byte) 0x80;

        // 나머지는 SV1과 동일
        // RndA[15..14] (2 bytes)
        sv2[6] = rndA[15];
        sv2[7] = rndA[14];

        // RndA[13..8] XOR RndB[15..10] (6 bytes)
        for (int i = 0; i < 6; i++) {
            sv2[8 + i] = (byte) (rndA[13 - i] ^ rndB[15 - i]);
        }

        // RndB[9..0] (10 bytes)
        for (int i = 0; i < 10; i++) {
            sv2[14 + i] = rndB[9 - i];
        }

        // RndA[7..0] (8 bytes)
        for (int i = 0; i < 8; i++) {
            sv2[24 + i] = rndA[7 - i];
        }

        return sv2;
    }

    /**
     * PRF(Pseudo Random Function) 계산 - AES-CMAC 사용
     * NIST SP 800-38B에 따른 CMAC 알고리즘
     *
     * @param key 인증 키 (Kx)
     * @param data 입력 데이터 (SV1 또는 SV2)
     * @return 16바이트 세션 키
     */
    private static byte[] calculatePRF(byte[] key, byte[] data) {
        try {
            BlockCipher cipher = new AESEngine();
            CMac cmac = new CMac(cipher);
            KeyParameter keyParam = new KeyParameter(key);

            cmac.init(keyParam);
            cmac.update(data, 0, data.length);

            byte[] output = new byte[16]; // 128-bit key
            cmac.doFinal(output, 0);

            return output;
        } catch (Exception e) {
            throw new RuntimeException("PRF(AES-CMAC) 계산 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 디버그용: SV 값을 출력
     */
    public static void printSessionVectors(byte[] rndA, byte[] rndB) {
        byte[] sv1 = generateSV1(rndA, rndB);
        byte[] sv2 = generateSV2(rndA, rndB);

        System.out.println("=== Session Vector Generation Debug ===");
        System.out.println("RndA: " + HexUtils.bytesToHex(rndA));
        System.out.println("RndB: " + HexUtils.bytesToHex(rndB));
        System.out.println();
        System.out.println("SV1 (for ENC): " + HexUtils.bytesToHex(sv1));
        System.out.println("SV2 (for MAC): " + HexUtils.bytesToHex(sv2));
    }

    /**
     * 디버그용: 세션 키 생성 과정 출력
     */
    public static void debugSessionKeyGeneration(byte[] authKey, byte[] rndA, byte[] rndB) {
        System.out.println("=== NTAG 424 DNA Session Key Generation ===");
        System.out.println("Auth Key (Kx): " + HexUtils.bytesToHex(authKey));

        printSessionVectors(rndA, rndB);

        byte[][] sessionKeys = generateSessionKeys(authKey, rndA, rndB);
        System.out.println();
        System.out.println("=== Generated Session Keys ===");
        System.out.println("SesAuthENCKey: " + HexUtils.bytesToHex(sessionKeys[0]));
        System.out.println("SesAuthMACKey: " + HexUtils.bytesToHex(sessionKeys[1]));
    }
}