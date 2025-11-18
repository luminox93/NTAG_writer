package ntagwriter.test;

import ntagwriter.crypto.SessionKeyGenerator;
import ntagwriter.util.HexUtils;

/**
 * SessionKeyGenerator 테스트 클래스
 *
 * NTAG 424 DNA 데이터 시트의 세션 키 생성 로직을 테스트합니다.
 * 실제 태그와의 통신에서 얻은 RndA, RndB 값으로 테스트하거나
 * 예제 값을 사용하여 검증할 수 있습니다.
 */
public class SessionKeyGeneratorTest {

    public static void main(String[] args) {
        System.out.println("===== NTAG 424 DNA Session Key Generation Test =====\n");

        // 테스트 케이스 1: 기본 키와 예제 랜덤 값
        testCase1();
        System.out.println("\n" + "=".repeat(60) + "\n");

        // 테스트 케이스 2: 사용자 정의 키와 랜덤 값
        testCase2();
        System.out.println("\n" + "=".repeat(60) + "\n");

        // 테스트 케이스 3: 데이터 시트 예제 값 (있다면)
        testCase3();
    }

    /**
     * 테스트 케이스 1: 기본 키 (all zeros)와 예제 랜덤 값
     */
    private static void testCase1() {
        System.out.println("Test Case 1: Default Key with Example Random Values");
        System.out.println("-".repeat(50));

        // 기본 키 (16 bytes of 0x00)
        byte[] authKey = new byte[16];

        // 예제 랜덤 값들
        byte[] rndA = HexUtils.hexToBytes("0123456789ABCDEF0123456789ABCDEF");
        byte[] rndB = HexUtils.hexToBytes("FEDCBA9876543210FEDCBA9876543210");

        // 세션 키 생성 및 디버그 출력
        SessionKeyGenerator.debugSessionKeyGeneration(authKey, rndA, rndB);

        // 세션 키 생성
        byte[][] sessionKeys = SessionKeyGenerator.generateSessionKeys(authKey, rndA, rndB);

        System.out.println("\n검증:");
        System.out.println("- SesAuthENCKey 길이: " + sessionKeys[0].length + " bytes (예상: 16)");
        System.out.println("- SesAuthMACKey 길이: " + sessionKeys[1].length + " bytes (예상: 16)");
        System.out.println("- 두 키가 다른가? " + (!java.util.Arrays.equals(sessionKeys[0], sessionKeys[1])));
    }

    /**
     * 테스트 케이스 2: 사용자 정의 키와 랜덤 값
     */
    private static void testCase2() {
        System.out.println("Test Case 2: Custom Key with Custom Random Values");
        System.out.println("-".repeat(50));

        // 사용자 정의 키
        byte[] authKey = HexUtils.hexToBytes("00112233445566778899AABBCCDDEEFF");

        // 사용자 정의 랜덤 값
        byte[] rndA = HexUtils.hexToBytes("1111111111111111AAAAAAAAAAAAAAAA");
        byte[] rndB = HexUtils.hexToBytes("2222222222222222BBBBBBBBBBBBBBBB");

        // 세션 키 생성
        byte[][] sessionKeys = SessionKeyGenerator.generateSessionKeys(authKey, rndA, rndB);

        System.out.println("Input:");
        System.out.println("  Auth Key: " + HexUtils.bytesToHex(authKey));
        System.out.println("  RndA:     " + HexUtils.bytesToHex(rndA));
        System.out.println("  RndB:     " + HexUtils.bytesToHex(rndB));

        System.out.println("\nOutput:");
        System.out.println("  SesAuthENCKey: " + HexUtils.bytesToHex(sessionKeys[0]));
        System.out.println("  SesAuthMACKey: " + HexUtils.bytesToHex(sessionKeys[1]));

        // SV 벡터도 확인
        System.out.println("\nSession Vectors:");
        SessionKeyGenerator.printSessionVectors(rndA, rndB);
    }

    /**
     * 테스트 케이스 3: 실제 태그 통신에서 캡처한 값 (예제)
     */
    private static void testCase3() {
        System.out.println("Test Case 3: Realistic Example (Simulated Tag Communication)");
        System.out.println("-".repeat(50));

        // 실제 통신에서 일반적으로 볼 수 있는 패턴의 값들
        byte[] authKey = HexUtils.hexToBytes("00000000000000000000000000000000");

        // 실제 태그가 생성할 법한 랜덤 값
        byte[] rndA = generatePseudoRandomBytes(16, 0x42);
        byte[] rndB = generatePseudoRandomBytes(16, 0x84);

        System.out.println("Simulated Authentication Session:");
        System.out.println("  Auth Key: " + HexUtils.bytesToHex(authKey));
        System.out.println("  RndA (Reader): " + HexUtils.bytesToHex(rndA));
        System.out.println("  RndB (Tag):    " + HexUtils.bytesToHex(rndB));

        // 세션 키 생성
        byte[][] sessionKeys = SessionKeyGenerator.generateSessionKeys(authKey, rndA, rndB);

        System.out.println("\nDerived Session Keys:");
        System.out.println("  SesAuthENCKey: " + HexUtils.bytesToHex(sessionKeys[0]));
        System.out.println("  SesAuthMACKey: " + HexUtils.bytesToHex(sessionKeys[1]));

        // 세션 키의 특성 분석
        analyzeSessionKeys(sessionKeys[0], sessionKeys[1]);
    }

    /**
     * 의사 랜덤 바이트 생성 (테스트용)
     */
    private static byte[] generatePseudoRandomBytes(int length, int seed) {
        byte[] bytes = new byte[length];
        java.util.Random random = new java.util.Random(seed);
        random.nextBytes(bytes);
        return bytes;
    }

    /**
     * 세션 키 특성 분석
     */
    private static void analyzeSessionKeys(byte[] encKey, byte[] macKey) {
        System.out.println("\n세션 키 분석:");

        // Hamming distance 계산
        int hammingDistance = 0;
        for (int i = 0; i < encKey.length; i++) {
            int xor = encKey[i] ^ macKey[i];
            hammingDistance += Integer.bitCount(xor & 0xFF);
        }

        System.out.println("  Hamming Distance: " + hammingDistance + " bits");
        System.out.println("  키 독립성: " + (hammingDistance > 64 ? "양호" : "주의 필요"));

        // 엔트로피 간단 체크
        int uniqueBytesEnc = countUniqueBytes(encKey);
        int uniqueBytesMac = countUniqueBytes(macKey);

        System.out.println("  ENC 키 고유 바이트: " + uniqueBytesEnc + "/16");
        System.out.println("  MAC 키 고유 바이트: " + uniqueBytesMac + "/16");
    }

    /**
     * 고유 바이트 수 계산
     */
    private static int countUniqueBytes(byte[] data) {
        boolean[] seen = new boolean[256];
        int count = 0;
        for (byte b : data) {
            int index = b & 0xFF;
            if (!seen[index]) {
                seen[index] = true;
                count++;
            }
        }
        return count;
    }
}