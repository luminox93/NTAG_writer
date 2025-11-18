package ntagwriter.crypto;

import ntagwriter.util.HexUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * NTAG 424 DNA AuthenticateEV2First/NonFirst 프로토콜 구현
 *
 * 3-Pass 인증 절차:
 * 1. PCD → PICC: AuthenticateEV2First 명령 전송
 * 2. PICC → PCD: 암호화된 RndB 전송
 * 3. PCD → PICC: 암호화된 (RndA || RndB') 전송
 * 4. PICC → PCD: 암호화된 (TI || RndA' || PDcap2 || PCDcap2) 응답
 */
public class AuthenticateEV2 {

    // APDU 명령 코드
    public static final byte CMD_AUTHENTICATE_EV2_FIRST = (byte) 0x71;
    public static final byte CMD_AUTHENTICATE_EV2_NON_FIRST = (byte) 0x77;

    // 기능 바이트 (Capability bytes)
    private static final byte[] DEFAULT_PDCAP2 = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00}; // 6 bytes
    private static final byte[] DEFAULT_PCDCAP2 = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00}; // 6 bytes

    private final SecureRandom random;

    public AuthenticateEV2() {
        this.random = new SecureRandom();
    }

    /**
     * AuthenticateEV2First 전체 프로세스 실행
     *
     * @param keyNumber 키 번호
     * @param authKey 인증 키 (16 bytes)
     * @param tagResponse1 태그의 첫 번째 응답 (암호화된 RndB)
     * @return 인증 결과 (세션 키, TI 등)
     */
    public AuthenticationResult performAuthentication(
            byte keyNumber,
            byte[] authKey,
            byte[] tagResponse1) throws Exception {

        if (authKey.length != 16) {
            throw new IllegalArgumentException("인증 키는 16바이트여야 합니다");
        }

        // Step 1: 태그로부터 받은 암호화된 RndB를 복호화
        byte[] encryptedRndB = tagResponse1;
        byte[] rndB = decryptAES(authKey, encryptedRndB, new byte[16]); // IV = 0

        System.out.println("=== Authentication Step 1 ===");
        System.out.println("Encrypted RndB: " + HexUtils.bytesToHex(encryptedRndB));
        System.out.println("Decrypted RndB: " + HexUtils.bytesToHex(rndB));

        // Step 2: RndA 생성 및 RndB' 계산
        byte[] rndA = generateRandom(16);
        byte[] rndBRotated = SecureMessaging.rotateLeft(rndB); // RndB'

        System.out.println("\n=== Authentication Step 2 ===");
        System.out.println("Generated RndA: " + HexUtils.bytesToHex(rndA));
        System.out.println("RndB' (rotated): " + HexUtils.bytesToHex(rndBRotated));

        // Step 3: RndA || RndB' 암호화
        byte[] toEncrypt = new byte[32];
        System.arraycopy(rndA, 0, toEncrypt, 0, 16);
        System.arraycopy(rndBRotated, 0, toEncrypt, 16, 16);

        // CBC 모드, IV는 암호화된 RndB 사용
        byte[] encrypted = encryptAES_CBC(authKey, toEncrypt, encryptedRndB);

        System.out.println("To encrypt: " + HexUtils.bytesToHex(toEncrypt));
        System.out.println("Encrypted: " + HexUtils.bytesToHex(encrypted));

        // 이것이 PCD가 태그로 보낼 두 번째 메시지
        byte[] pcdCommand2 = encrypted;

        // Step 4: 세션 키 생성
        byte[][] sessionKeys = SessionKeyGenerator.generateSessionKeys(authKey, rndA, rndB);
        byte[] sesAuthENCKey = sessionKeys[0];
        byte[] sesAuthMACKey = sessionKeys[1];

        System.out.println("\n=== Session Keys ===");
        System.out.println("SesAuthENCKey: " + HexUtils.bytesToHex(sesAuthENCKey));
        System.out.println("SesAuthMACKey: " + HexUtils.bytesToHex(sesAuthMACKey));

        // 결과 반환 (실제 TI는 태그의 최종 응답에서 받아야 함)
        AuthenticationResult result = new AuthenticationResult();
        result.sesAuthENCKey = sesAuthENCKey;
        result.sesAuthMACKey = sesAuthMACKey;
        result.rndA = rndA;
        result.rndB = rndB;
        result.pcdCommand2 = pcdCommand2;

        return result;
    }

    /**
     * 태그의 최종 응답 파싱
     * 암호화된 (TI || RndA' || PDcap2 || PCDcap2) 복호화
     *
     * @param authKey 인증 키
     * @param encryptedResponse 태그의 암호화된 응답
     * @param rndA 원본 RndA (검증용)
     * @return 파싱된 응답
     */
    public FinalResponse parseFinalResponse(
            byte[] authKey,
            byte[] encryptedResponse,
            byte[] rndA) throws Exception {

        // 복호화 (IV = 0)
        byte[] decrypted = decryptAES(authKey, encryptedResponse, new byte[16]);

        System.out.println("\n=== Final Response Parsing ===");
        System.out.println("Encrypted response: " + HexUtils.bytesToHex(encryptedResponse));
        System.out.println("Decrypted response: " + HexUtils.bytesToHex(decrypted));

        // 파싱: TI(4) || RndA'(16) || PDcap2(6) || PCDcap2(6)
        if (decrypted.length < 32) {
            throw new IllegalArgumentException("응답 길이가 잘못되었습니다");
        }

        FinalResponse response = new FinalResponse();
        int offset = 0;

        // TI 추출 (4 bytes)
        response.transactionId = Arrays.copyOfRange(decrypted, offset, offset + 4);
        offset += 4;

        // RndA' 추출 및 검증 (16 bytes)
        byte[] rndAPrime = Arrays.copyOfRange(decrypted, offset, offset + 16);
        offset += 16;

        // RndA'가 RndA를 1바이트 왼쪽 회전한 것인지 검증
        byte[] expectedRndAPrime = SecureMessaging.rotateLeft(rndA);
        if (!Arrays.equals(rndAPrime, expectedRndAPrime)) {
            throw new SecurityException("RndA' 검증 실패 - 인증 실패");
        }

        // PDcap2 추출 (6 bytes)
        response.pdCap2 = Arrays.copyOfRange(decrypted, offset, offset + 6);
        offset += 6;

        // PCDcap2 추출 (6 bytes)
        response.pcdCap2 = Arrays.copyOfRange(decrypted, offset, offset + 6);

        System.out.println("Transaction ID: " + HexUtils.bytesToHex(response.transactionId));
        System.out.println("RndA' verified: OK");
        System.out.println("PDcap2: " + HexUtils.bytesToHex(response.pdCap2));
        System.out.println("PCDcap2: " + HexUtils.bytesToHex(response.pcdCap2));

        return response;
    }

    /**
     * AuthenticateEV2First 명령 APDU 생성
     *
     * @param keyNumber 키 번호
     * @param lenCap 길이 capability (옵션)
     * @return APDU 바이트 배열
     */
    public static byte[] buildAuthenticateEV2FirstCommand(byte keyNumber, byte[] lenCap) {
        ByteBuffer buffer = ByteBuffer.allocate(7 + (lenCap != null ? lenCap.length : 0));

        buffer.put((byte) 0x90); // CLA
        buffer.put(CMD_AUTHENTICATE_EV2_FIRST); // INS
        buffer.put(keyNumber); // P1 (key number)
        buffer.put((byte) 0x00); // P2

        if (lenCap != null && lenCap.length > 0) {
            buffer.put((byte) lenCap.length); // Lc
            buffer.put(lenCap); // Data
        } else {
            buffer.put((byte) 0x00); // Lc = 0
        }

        buffer.put((byte) 0x00); // Le

        return buffer.array();
    }

    /**
     * 랜덤 바이트 생성
     */
    private byte[] generateRandom(int length) {
        byte[] random = new byte[length];
        this.random.nextBytes(random);
        return random;
    }

    /**
     * AES ECB 모드 복호화 (인증 초기 단계용)
     */
    private byte[] decryptAES(byte[] key, byte[] data, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        return cipher.doFinal(data);
    }

    /**
     * AES CBC 모드 암호화
     */
    private byte[] encryptAES_CBC(byte[] key, byte[] data, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        return cipher.doFinal(data);
    }

    /**
     * 인증 결과 클래스
     */
    public static class AuthenticationResult {
        public byte[] sesAuthENCKey;
        public byte[] sesAuthMACKey;
        public byte[] transactionId;
        public byte[] rndA;
        public byte[] rndB;
        public byte[] pcdCommand2; // PCD가 태그로 보낼 두 번째 명령

        public SecureMessaging createSecureMessaging() {
            if (transactionId == null) {
                throw new IllegalStateException("Transaction ID가 설정되지 않았습니다");
            }
            return new SecureMessaging(sesAuthMACKey, sesAuthENCKey, transactionId);
        }
    }

    /**
     * 최종 응답 클래스
     */
    public static class FinalResponse {
        public byte[] transactionId;
        public byte[] pdCap2;
        public byte[] pcdCap2;
    }

    /**
     * 디버그: 인증 프로세스 시뮬레이션
     */
    public static void simulateAuthentication() {
        try {
            System.out.println("===== NTAG 424 DNA Authentication Simulation =====\n");

            // 테스트용 키
            byte[] authKey = new byte[16]; // 기본 키 (all zeros)

            // 시뮬레이션: 태그가 보낸 암호화된 RndB (예제)
            SecureRandom random = new SecureRandom();
            byte[] rndB = new byte[16];
            random.nextBytes(rndB);

            // RndB 암호화 (태그가 수행)
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(authKey, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(new byte[16]);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encryptedRndB = cipher.doFinal(rndB);

            System.out.println("Simulated RndB: " + HexUtils.bytesToHex(rndB));
            System.out.println("Encrypted RndB: " + HexUtils.bytesToHex(encryptedRndB));
            System.out.println();

            // 인증 수행
            AuthenticateEV2 auth = new AuthenticateEV2();
            AuthenticationResult result = auth.performAuthentication(
                    (byte) 0x00, // key number
                    authKey,
                    encryptedRndB
            );

            System.out.println("\n=== Authentication Complete ===");
            System.out.println("PCD should send: " + HexUtils.bytesToHex(result.pcdCommand2));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}