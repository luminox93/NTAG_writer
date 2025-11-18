package ntagwriter.crypto;

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
        byte[] sv1 = SessionVectorBuilder.build((byte) 0xA5, (byte) 0x5A, rndA, rndB);
        byte[] sv2 = SessionVectorBuilder.build((byte) 0x5A, (byte) 0xA5, rndA, rndB);

        // 세션 키 생성 (PRF = AES-CMAC)
        byte[] sesAuthENCKey = MacUtils.prfCmac(authKey, sv1);
        byte[] sesAuthMACKey = MacUtils.prfCmac(authKey, sv2);

        return new byte[][] {sesAuthENCKey, sesAuthMACKey};
    }

    /**
     * 디버그용: SV 값을 출력
     */
    public static void printSessionVectors(byte[] rndA, byte[] rndB) {
        byte[] sv1 = SessionVectorBuilder.build((byte) 0xA5, (byte) 0x5A, rndA, rndB);
        byte[] sv2 = SessionVectorBuilder.build((byte) 0x5A, (byte) 0xA5, rndA, rndB);

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
