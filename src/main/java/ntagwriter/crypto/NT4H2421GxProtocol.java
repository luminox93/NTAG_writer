package ntagwriter.crypto;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.macs.CMac;
import org.bouncycastle.crypto.params.KeyParameter;
import ntagwriter.util.HexUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * NT4H2421Gx (NTAG 424 DNA) 완전한 암호화 프로토콜 구현
 *
 * 데이터시트 기반 구현:
 * - Session Key Generation (9.1.7)
 * - MAC Truncation for NT4H2421Gx
 * - AuthenticateEV2First 3-Pass Protocol
 * - Secure Messaging with Full CommMode
 */
public class NT4H2421GxProtocol {

    // ===== 1단계: 암호화 프리미티브 정의 =====

    /**
     * PRF (Pseudo Random Function) = AES-CMAC
     * NIST SP 800-38B
     */
    public static byte[] PRF(byte[] key, byte[] data) {
        try {
            BlockCipher cipher = new AESEngine();
            CMac cmac = new CMac(cipher);
            KeyParameter keyParam = new KeyParameter(key);

            cmac.init(keyParam);
            cmac.update(data, 0, data.length);

            byte[] output = new byte[16];
            cmac.doFinal(output, 0);
            return output;
        } catch (Exception e) {
            throw new RuntimeException("PRF 계산 실패: " + e.getMessage(), e);
        }
    }

    // ===== 2단계: 핵심 암호화 보조 함수 =====

    /**
     * MAC 트렁케이션 (NT4H2421Gx 전용)
     * 16바이트 CMAC에서 짝수 번째 바이트 8개 추출
     * 인덱스: 1, 3, 5, 7, 9, 11, 13, 15
     */
    public static byte[] truncateMAC(byte[] cmac) {
        byte[] truncated = new byte[8];
        for (int i = 0; i < 8; i++) {
            truncated[i] = cmac[i * 2 + 1];
        }
        return truncated;
    }

    /**
     * 세션 벡터 SV 구성 (데이터시트 9.1.7)
     *
     * SV = prefix1 || prefix2 || 00 01 00 80 ||
     *      RndA[15..14] || (RndA[13..8] XOR RndB[15..10]) ||
     *      RndB[9..0] || RndA[7..0]
     */
    public static byte[] constructSV(byte prefix1, byte prefix2, byte[] rndA, byte[] rndB) {
        byte[] sv = new byte[32];
        int idx = 0;

        // 헤더
        sv[idx++] = prefix1;
        sv[idx++] = prefix2;
        sv[idx++] = 0x00;
        sv[idx++] = 0x01;
        sv[idx++] = 0x00;
        sv[idx++] = (byte) 0x80;

        // RndA[15..14]
        sv[idx++] = rndA[15];
        sv[idx++] = rndA[14];

        // RndA[13..8] XOR RndB[15..10]
        for (int i = 0; i < 6; i++) {
            sv[idx++] = (byte)(rndA[13 - i] ^ rndB[15 - i]);
        }

        // RndB[9..0]
        for (int i = 9; i >= 0; i--) {
            sv[idx++] = rndB[i];
        }

        // RndA[7..0]
        for (int i = 7; i >= 0; i--) {
            sv[idx++] = rndA[i];
        }

        return sv;
    }

    /**
     * IV 구성 (보안 메시징용)
     * IV = E(KSesAuthENC, Label || TI || CmdCtr || ZeroPadding)
     */
    public static byte[] constructIV(byte[] sesAuthENCKey, byte[] label,
                                     byte[] ti, byte[] cmdCtr) throws Exception {
        byte[] ivInput = new byte[16];
        System.arraycopy(label, 0, ivInput, 0, 2);
        System.arraycopy(ti, 0, ivInput, 2, 4);
        System.arraycopy(cmdCtr, 0, ivInput, 6, 2);
        // 나머지 8바이트는 0

        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(sesAuthENCKey, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        return cipher.doFinal(ivInput);
    }

    // ===== 3단계: AuthenticateEV2First 프로토콜 =====

    public static class AuthenticationProtocol {
        private byte[] authKey;
        private byte[] rndA;
        private byte[] rndB;
        private byte[] transactionId;
        private byte[] sesAuthENCKey;
        private byte[] sesAuthMACKey;
        private byte[] commandCounter = new byte[]{0x01, 0x00}; // 인증 후 첫 명령은 0x0001

        private SecureRandom random = new SecureRandom();

        public AuthenticationProtocol(byte[] authKey) {
            this.authKey = authKey;
        }

        /**
         * Pass 1: 암호화된 RndB 복호화
         */
        public void processEncryptedRndB(byte[] encRndB) throws Exception {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(authKey, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(new byte[16]); // IV = 0
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            this.rndB = cipher.doFinal(encRndB);
        }

        /**
         * Pass 2: RndA || RndB' 생성 및 암호화
         */
        public byte[] generateAuthResponse() throws Exception {
            // RndA 생성
            this.rndA = new byte[16];
            random.nextBytes(rndA);

            // RndB' 생성
            byte[] rndBRotated = ByteRotation.rotateLeft(rndB);

            // 연결
            byte[] combined = new byte[32];
            System.arraycopy(rndA, 0, combined, 0, 16);
            System.arraycopy(rndBRotated, 0, combined, 16, 16);

            // 암호화
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(authKey, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(new byte[16]);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            return cipher.doFinal(combined);
        }

        /**
         * Pass 3: TI 수신 및 세션 키 생성
         */
        public void processFinalResponse(byte[] encResponse) throws Exception {
            // 복호화
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(authKey, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(new byte[16]);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decrypted = cipher.doFinal(encResponse);

            // TI 추출
            this.transactionId = Arrays.copyOfRange(decrypted, 0, 4);

            // RndA' 검증
            byte[] rndAPrime = Arrays.copyOfRange(decrypted, 4, 20);
            byte[] expectedRndAPrime = ByteRotation.rotateLeft(rndA);
            if (!Arrays.equals(rndAPrime, expectedRndAPrime)) {
                throw new SecurityException("RndA' 검증 실패");
            }

            // 세션 키 생성
            generateSessionKeys();
        }

        private void generateSessionKeys() {
            // SV1 → SesAuthENCKey
            byte[] sv1 = constructSV((byte)0xA5, (byte)0x5A, rndA, rndB);
            this.sesAuthENCKey = PRF(authKey, sv1);

            // SV2 → SesAuthMACKey
            byte[] sv2 = constructSV((byte)0x5A, (byte)0xA5, rndA, rndB);
            this.sesAuthMACKey = PRF(authKey, sv2);
        }

        public void incrementCommandCounter() {
            int counter = ((commandCounter[1] & 0xFF) << 8) | (commandCounter[0] & 0xFF);
            counter++;
            commandCounter[0] = (byte)(counter & 0xFF);
            commandCounter[1] = (byte)((counter >> 8) & 0xFF);
        }

        // Getters
        public byte[] getTransactionId() { return transactionId; }
        public byte[] getSesAuthENCKey() { return sesAuthENCKey; }
        public byte[] getSesAuthMACKey() { return sesAuthMACKey; }
        public byte[] getCommandCounter() { return commandCounter; }
    }

    // ===== 4단계: 보안 메시징을 통한 명령 실행 =====

    public static class SecureMessaging {
        private AuthenticationProtocol auth;

        public SecureMessaging(AuthenticationProtocol auth) {
            this.auth = auth;
        }

        /**
         * ChangeFileSettings 명령 구성 (CommMode.Full)
         */
        public byte[] buildChangeFileSettings(byte fileNo, byte[] fileSettings) throws Exception {
            byte cmdCode = 0x5F;

            // 데이터 구성: FileNo || Settings
            ByteBuffer plainData = ByteBuffer.allocate(1 + fileSettings.length);
            plainData.put(fileNo);
            plainData.put(fileSettings);

            // 패딩 추가
            byte[] paddedData = addPadding(plainData.array());

            // IV 생성 (명령용)
            byte[] ivLabel = new byte[]{(byte)0xA5, (byte)0x5A};
            byte[] iv = constructIV(auth.getSesAuthENCKey(), ivLabel,
                                   auth.getTransactionId(), auth.getCommandCounter());

            // 암호화
            byte[] encryptedData = encryptCBC(auth.getSesAuthENCKey(), iv, paddedData);

            // MAC 계산
            ByteBuffer macInput = ByteBuffer.allocate(1 + 2 + 4 + 1 + encryptedData.length);
            macInput.put(cmdCode);
            macInput.put(auth.getCommandCounter());
            macInput.put(auth.getTransactionId());
            macInput.put(fileNo);
            macInput.put(encryptedData);

            byte[] fullMAC = PRF(auth.getSesAuthMACKey(), macInput.array());
            byte[] truncatedMAC = truncateMAC(fullMAC);

            // APDU 구성
            ByteBuffer apdu = ByteBuffer.allocate(5 + 1 + encryptedData.length + 8 + 1);
            apdu.put((byte)0x90);                           // CLA
            apdu.put(cmdCode);                               // INS
            apdu.put((byte)0x00);                           // P1
            apdu.put((byte)0x00);                           // P2
            apdu.put((byte)(1 + encryptedData.length + 8)); // Lc
            apdu.put(fileNo);                                // File number
            apdu.put(encryptedData);                         // Encrypted settings
            apdu.put(truncatedMAC);                          // MAC
            apdu.put((byte)0x00);                           // Le

            // Command Counter 증가
            auth.incrementCommandCounter();

            return apdu.array();
        }

        private byte[] addPadding(byte[] data) {
            int blockSize = 16;
            int paddedLength = ((data.length + 1 + blockSize - 1) / blockSize) * blockSize;
            byte[] padded = new byte[paddedLength];
            System.arraycopy(data, 0, padded, 0, data.length);
            padded[data.length] = (byte)0x80;
            return padded;
        }

        private byte[] encryptCBC(byte[] key, byte[] iv, byte[] data) throws Exception {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            return cipher.doFinal(data);
        }
    }

    /**
     * 테스트 메인
     */
    public static void main(String[] args) {
        try {
            System.out.println("===== NT4H2421Gx Protocol Implementation =====\n");

            // 기본 키
            byte[] authKey = new byte[16];

            // 인증 프로토콜 시작
            AuthenticationProtocol auth = new AuthenticationProtocol(authKey);

            // Pass 1: RndB 시뮬레이션
            byte[] rndB = HexUtils.hexToBytes("1234567890ABCDEF1234567890ABCDEF");
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,
                       new SecretKeySpec(authKey, "AES"),
                       new IvParameterSpec(new byte[16]));
            byte[] encRndB = cipher.doFinal(rndB);

            auth.processEncryptedRndB(encRndB);
            System.out.println("1. RndB 복호화 완료");

            // Pass 2
            byte[] authResponse = auth.generateAuthResponse();
            System.out.println("2. Auth Response: " + HexUtils.bytesToHex(authResponse));

            // Pass 3: TI 시뮬레이션
            byte[] ti = HexUtils.hexToBytes("AABBCCDD");
            byte[] finalResponse = new byte[32];
            System.arraycopy(ti, 0, finalResponse, 0, 4);
            System.arraycopy(ByteRotation.rotateLeft(auth.rndA), 0, finalResponse, 4, 16);

            cipher.init(Cipher.ENCRYPT_MODE,
                       new SecretKeySpec(authKey, "AES"),
                       new IvParameterSpec(new byte[16]));
            byte[] encFinalResponse = cipher.doFinal(finalResponse);

            auth.processFinalResponse(encFinalResponse);
            System.out.println("3. 세션 키 생성 완료");
            System.out.println("   TI: " + HexUtils.bytesToHex(auth.getTransactionId()));
            System.out.println("   SesAuthENCKey: " + HexUtils.bytesToHex(auth.getSesAuthENCKey()));
            System.out.println("   SesAuthMACKey: " + HexUtils.bytesToHex(auth.getSesAuthMACKey()));
            System.out.println("   Initial CmdCtr: " + HexUtils.bytesToHex(auth.getCommandCounter()));

            // 보안 명령 테스트
            SecureMessaging sm = new SecureMessaging(auth);
            byte[] settings = HexUtils.hexToBytes("4000E0000100C1F1212B000051000051");
            byte[] apdu = sm.buildChangeFileSettings((byte)0x02, settings);
            System.out.println("\n4. ChangeFileSettings APDU:");
            System.out.println("   " + HexUtils.bytesToHex(apdu));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
