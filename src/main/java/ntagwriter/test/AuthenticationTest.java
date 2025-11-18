package ntagwriter.test;

import ntagwriter.crypto.AuthenticateEV2;
import ntagwriter.crypto.ByteRotation;
import ntagwriter.crypto.SecureMessaging;
import ntagwriter.crypto.SessionKeyGenerator;
import ntagwriter.util.HexUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * NTAG 424 DNA 인증 및 보안 메시징 통합 테스트
 */
public class AuthenticationTest {

    public static void main(String[] args) {
        System.out.println("===== NTAG 424 DNA Authentication & Secure Messaging Test =====\n");

        // 테스트 1: 인증 프로토콜 시뮬레이션
        testAuthenticationProtocol();

        System.out.println("\n" + "=".repeat(70) + "\n");

        // 테스트 2: 보안 메시징 (MAC, IV, 암호화)
        testSecureMessaging();

        System.out.println("\n" + "=".repeat(70) + "\n");

        // 테스트 3: 완전한 인증 시나리오
        testFullScenario();
    }

    /**
     * 테스트 1: 인증 프로토콜 시뮬레이션
     */
    private static void testAuthenticationProtocol() {
        System.out.println("Test 1: Authentication Protocol Simulation");
        System.out.println("-".repeat(50));

        try {
            // 키 설정
            byte[] authKey = HexUtils.hexToBytes("00000000000000000000000000000000");

            // 태그 시뮬레이션: RndB 생성 및 암호화
            SecureRandom random = new SecureRandom();
            byte[] rndB = new byte[16];
            random.nextBytes(rndB);

            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(authKey, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(new byte[16]); // IV = 0
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encryptedRndB = cipher.doFinal(rndB);

            System.out.println("Tag's RndB:      " + HexUtils.bytesToHex(rndB));
            System.out.println("Encrypted RndB:  " + HexUtils.bytesToHex(encryptedRndB));

            // PCD 인증 수행
            AuthenticateEV2 auth = new AuthenticateEV2();
            AuthenticateEV2.AuthenticationResult result = auth.performAuthentication(
                    (byte) 0x00,
                    authKey,
                    encryptedRndB
            );

            System.out.println("\n인증 결과:");
            System.out.println("  PCD → Tag command: " + HexUtils.bytesToHex(result.pcdCommand2));
            System.out.println("  세션 키 생성 완료");

            // 태그 응답 시뮬레이션
            byte[] ti = HexUtils.hexToBytes("12345678"); // 예제 TI
            byte[] rndAPrime = ByteRotation.rotateLeft(result.rndA);
            byte[] pdCap2 = new byte[6];
            byte[] pcdCap2 = new byte[6];

            // 최종 응답 구성 및 암호화
            byte[] finalResponse = new byte[32];
            System.arraycopy(ti, 0, finalResponse, 0, 4);
            System.arraycopy(rndAPrime, 0, finalResponse, 4, 16);
            System.arraycopy(pdCap2, 0, finalResponse, 20, 6);
            System.arraycopy(pcdCap2, 0, finalResponse, 26, 6);

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encryptedFinalResponse = cipher.doFinal(finalResponse);

            // 최종 응답 파싱
            AuthenticateEV2.FinalResponse parsed = auth.parseFinalResponse(
                    authKey,
                    encryptedFinalResponse,
                    result.rndA
            );

            System.out.println("\n최종 인증 완료:");
            System.out.println("  Transaction ID: " + HexUtils.bytesToHex(parsed.transactionId));
            System.out.println("  인증 성공!");

        } catch (Exception e) {
            System.err.println("인증 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 테스트 2: 보안 메시징 테스트
     */
    private static void testSecureMessaging() {
        System.out.println("Test 2: Secure Messaging (MAC, IV, Encryption)");
        System.out.println("-".repeat(50));

        // 세션 키와 TI 설정
        byte[] sesAuthMACKey = HexUtils.hexToBytes("A324C2127B3EEAB673D5B2E4CAAD91C7");
        byte[] sesAuthENCKey = HexUtils.hexToBytes("06851EDBE9EB2ED7B9CD08593AF25052");
        byte[] transactionId = HexUtils.hexToBytes("12345678");

        SecureMessaging sm = new SecureMessaging(sesAuthMACKey, sesAuthENCKey, transactionId);

        // MAC 테스트
        System.out.println("MAC Truncation Test:");
        byte[] testData = HexUtils.hexToBytes("0102030405060708090A0B0C0D0E0F10");
        byte[] mac = sm.calculateMAC(testData);
        System.out.println("  Input data: " + HexUtils.bytesToHex(testData));
        System.out.println("  Truncated MAC (8 bytes): " + HexUtils.bytesToHex(mac));

        // Command MAC 테스트
        System.out.println("\nCommand MAC Test:");
        byte cmdCode = (byte) 0x51; // ChangeFileSettings
        byte[] cmdData = HexUtils.hexToBytes("02404000E0");
        byte[] cmdMac = sm.calculateCommandMAC(cmdCode, cmdData);
        System.out.println("  Command: 0x" + String.format("%02X", cmdCode));
        System.out.println("  Data: " + HexUtils.bytesToHex(cmdData));
        System.out.println("  Command MAC: " + HexUtils.bytesToHex(cmdMac));

        // IV 생성 테스트
        System.out.println("\nIV Generation Test:");
        byte[] commandIV = sm.generateIV(true);
        byte[] responseIV = sm.generateIV(false);
        System.out.println("  Command IV:  " + HexUtils.bytesToHex(commandIV));
        System.out.println("  Response IV: " + HexUtils.bytesToHex(responseIV));

        // 암호화/복호화 테스트
        System.out.println("\nEncryption/Decryption Test:");
        byte[] plaintext = HexUtils.hexToBytes("48656C6C6F20576F726C6421"); // "Hello World!"
        byte[] encrypted = sm.encrypt(plaintext, true);
        byte[] decrypted = sm.decrypt(encrypted, true);

        System.out.println("  Plaintext:  " + HexUtils.bytesToHex(plaintext));
        System.out.println("  Encrypted:  " + HexUtils.bytesToHex(encrypted));
        System.out.println("  Decrypted:  " + HexUtils.bytesToHex(decrypted));
        System.out.println("  Match: " + Arrays.equals(plaintext, decrypted));

        // Command Counter 테스트
        System.out.println("\nCommand Counter Test:");
        System.out.println("  Initial CmdCtr: 0x" + String.format("%04X", sm.getCommandCounter()));
        sm.incrementCommandCounter();
        System.out.println("  After increment: 0x" + String.format("%04X", sm.getCommandCounter()));
        sm.resetCommandCounter();
        System.out.println("  After reset: 0x" + String.format("%04X", sm.getCommandCounter()));
    }

    /**
     * 테스트 3: 완전한 시나리오
     */
    private static void testFullScenario() {
        System.out.println("Test 3: Full Authentication & Secure Messaging Scenario");
        System.out.println("-".repeat(50));

        try {
            // 1. 인증 수행
            byte[] authKey = new byte[16]; // 기본 키
            SecureRandom random = new SecureRandom();

            // 태그의 RndB 시뮬레이션
            byte[] rndB = new byte[16];
            random.nextBytes(rndB);

            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(authKey, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(new byte[16]);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encryptedRndB = cipher.doFinal(rndB);

            // 인증
            AuthenticateEV2 auth = new AuthenticateEV2();
            AuthenticateEV2.AuthenticationResult authResult = auth.performAuthentication(
                    (byte) 0x00,
                    authKey,
                    encryptedRndB
            );

            // TI 설정 (태그로부터 받았다고 가정)
            authResult.transactionId = HexUtils.hexToBytes("AABBCCDD");

            // 2. SecureMessaging 설정
            SecureMessaging sm = authResult.createSecureMessaging();

            System.out.println("1. 인증 완료");
            System.out.println("   TI: " + HexUtils.bytesToHex(authResult.transactionId));
            System.out.println("   SesAuthENCKey: " + HexUtils.bytesToHex(authResult.sesAuthENCKey));
            System.out.println("   SesAuthMACKey: " + HexUtils.bytesToHex(authResult.sesAuthMACKey));

            // 3. 보안 명령 전송 시뮬레이션 (ChangeFileSettings)
            System.out.println("\n2. ChangeFileSettings 명령 준비");

            byte cmdCode = (byte) 0x5F; // ChangeFileSettings
            byte fileNo = 0x02; // NDEF file
            byte[] fileOption = {0x40}; // SDM enabled
            byte[] accessRights = {(byte) 0xE0, 0x00}; // Read: Free, Write: Key 0
            byte[] sdmAccessRights = {(byte) 0xF1, 0x21}; // 예제
            byte[] sdmOptions = new byte[21]; // SDM 옵션들

            // 명령 데이터 구성
            ByteBuffer cmdBuffer = ByteBuffer.allocate(1 + 1 + 2 + 2 + 21);
            cmdBuffer.put(fileNo);
            cmdBuffer.put(fileOption);
            cmdBuffer.put(accessRights);
            cmdBuffer.put(sdmAccessRights);
            cmdBuffer.put(sdmOptions);
            byte[] cmdData = cmdBuffer.array();

            // MAC 생성
            byte[] mac = sm.calculateCommandMAC(cmdCode, cmdData);

            System.out.println("   Command: 0x" + String.format("%02X", cmdCode));
            System.out.println("   File No: 0x" + String.format("%02X", fileNo));
            System.out.println("   Data: " + HexUtils.bytesToHex(cmdData));
            System.out.println("   MAC: " + HexUtils.bytesToHex(mac));

            // 4. Command Counter 증가
            sm.incrementCommandCounter();

            System.out.println("\n3. 명령 전송 후");
            System.out.println("   Command Counter: 0x" + String.format("%04X", sm.getCommandCounter()));

            System.out.println("\n시나리오 완료!");

        } catch (Exception e) {
            System.err.println("시나리오 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
