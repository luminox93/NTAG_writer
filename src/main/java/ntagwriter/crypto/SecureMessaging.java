package ntagwriter.crypto;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.macs.CMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.paddings.ISO7816d4Padding;
import ntagwriter.util.HexUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * NTAG 424 DNA Secure Messaging 구현
 *
 * 데이터 시트의 보안 메시징 사양 구현:
 * - MAC 생성 및 검증 (8바이트 트렁케이션)
 * - IV 생성 (명령/응답)
 * - 암호화/복호화
 * - Command Counter 관리
 */
public class SecureMessaging {

    // IV 레이블 (데이터 시트 정의)
    private static final byte[] IV_LABEL_COMMAND = {(byte) 0xA5, (byte) 0x5A}; // 명령용
    private static final byte[] IV_LABEL_RESPONSE = {(byte) 0x5A, (byte) 0xA5}; // 응답용

    // 세션 정보
    private final byte[] sesAuthMACKey;
    private final byte[] sesAuthENCKey;
    private final byte[] transactionId; // 4 bytes TI
    private int commandCounter; // 2 bytes CmdCtr

    /**
     * SecureMessaging 인스턴스 생성
     *
     * @param sesAuthMACKey MAC용 세션 키 (16 bytes)
     * @param sesAuthENCKey 암호화용 세션 키 (16 bytes)
     * @param transactionId 트랜잭션 ID (4 bytes)
     */
    public SecureMessaging(byte[] sesAuthMACKey, byte[] sesAuthENCKey, byte[] transactionId) {
        if (sesAuthMACKey == null || sesAuthMACKey.length != 16) {
            throw new IllegalArgumentException("MAC 세션 키는 16바이트여야 합니다");
        }
        if (sesAuthENCKey == null || sesAuthENCKey.length != 16) {
            throw new IllegalArgumentException("ENC 세션 키는 16바이트여야 합니다");
        }
        if (transactionId == null || transactionId.length != 4) {
            throw new IllegalArgumentException("Transaction ID는 4바이트여야 합니다");
        }

        this.sesAuthMACKey = Arrays.copyOf(sesAuthMACKey, 16);
        this.sesAuthENCKey = Arrays.copyOf(sesAuthENCKey, 16);
        this.transactionId = Arrays.copyOf(transactionId, 4);
        this.commandCounter = 0; // 인증 성공 후 0000h로 시작
    }

    /**
     * MAC 계산 (트렁케이션 포함)
     *
     * NT4H2421Gx에서는 16바이트 CMAC 출력 중 8개의 짝수 번째 바이트만 사용
     *
     * @param data MAC을 계산할 데이터
     * @return 8바이트 트렁케이티드 MAC
     */
    public byte[] calculateMAC(byte[] data) {
        // Full CMAC 계산 후 짝수 인덱스 바이트만 추출
        byte[] fullMAC = calculateFullCMAC(sesAuthMACKey, data);
        return MacUtils.truncateMac(fullMAC);
    }

    /**
     * 명령용 MAC 계산 (TI와 CmdCtr 포함)
     *
     * @param cmdHeader 명령 헤더 (CMD || CmdCtr)
     * @param cmdData 명령 데이터
     * @return 8바이트 MAC
     */
    public byte[] calculateCommandMAC(byte cmdCode, byte[] cmdData) {
        // MAC 입력 구성: CMD || CmdCtr || TI || CmdData
        ByteBuffer buffer = ByteBuffer.allocate(1 + 2 + 4 + (cmdData != null ? cmdData.length : 0));
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.put(cmdCode);
        buffer.putShort((short) commandCounter);
        buffer.put(transactionId);
        if (cmdData != null) {
            buffer.put(cmdData);
        }

        return calculateMAC(buffer.array());
    }

    /**
     * 응답용 MAC 계산
     *
     * @param statusCode 상태 코드 (2 bytes)
     * @param responseData 응답 데이터
     * @return 8바이트 MAC
     */
    public byte[] calculateResponseMAC(byte[] statusCode, byte[] responseData) {
        // MAC 입력 구성: StatusCode || CmdCtr || TI || ResponseData
        ByteBuffer buffer = ByteBuffer.allocate(2 + 2 + 4 + (responseData != null ? responseData.length : 0));
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.put(statusCode);
        buffer.putShort((short) commandCounter);
        buffer.put(transactionId);
        if (responseData != null) {
            buffer.put(responseData);
        }

        return calculateMAC(buffer.array());
    }

    /**
     * IV (Initialization Vector) 생성
     *
     * IV = AES_ECB(SesAuthENCKey, Label || TI || CmdCtr || ZeroPadding)
     *
     * @param isCommand true면 명령용, false면 응답용
     * @return 16바이트 IV
     */
    public byte[] generateIV(boolean isCommand) {
        // IV 입력 데이터 구성: Label(2) || TI(4) || CmdCtr(2) || ZeroPadding(8)
        byte[] ivInput = new byte[16];
        ByteBuffer buffer = ByteBuffer.wrap(ivInput);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // 레이블 (2 bytes)
        buffer.put(isCommand ? IV_LABEL_COMMAND : IV_LABEL_RESPONSE);

        // Transaction ID (4 bytes)
        buffer.put(transactionId);

        // Command Counter (2 bytes, LSB first)
        buffer.putShort((short) commandCounter);

        // 나머지는 0으로 자동 패딩됨 (8 bytes)

        // ECB 모드로 암호화
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(sesAuthENCKey, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return cipher.doFinal(ivInput);
        } catch (Exception e) {
            throw new RuntimeException("IV 생성 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 데이터 암호화 (CBC 모드)
     *
     * @param plainData 평문 데이터
     * @param isCommand true면 명령, false면 응답
     * @return 암호화된 데이터
     */
    public byte[] encrypt(byte[] plainData, boolean isCommand) {
        try {
            // IV 생성
            byte[] iv = generateIV(isCommand);

            // ISO/IEC 7816-4 패딩 적용 및 CBC 암호화
            PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(
                    new CBCBlockCipher(new AESEngine()),
                    new ISO7816d4Padding()
            );
            ParametersWithIV params = new ParametersWithIV(new KeyParameter(sesAuthENCKey), iv);
            cipher.init(true, params);

            byte[] output = new byte[cipher.getOutputSize(plainData.length)];
            int len = cipher.processBytes(plainData, 0, plainData.length, output, 0);
            len += cipher.doFinal(output, len);

            return Arrays.copyOf(output, len);
        } catch (Exception e) {
            throw new RuntimeException("암호화 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 데이터 복호화 (CBC 모드)
     *
     * @param encryptedData 암호문 데이터
     * @param isCommand true면 명령, false면 응답
     * @return 복호화된 데이터
     */
    public byte[] decrypt(byte[] encryptedData, boolean isCommand) {
        try {
            // IV 생성
            byte[] iv = generateIV(isCommand);

            // CBC 복호화 및 ISO/IEC 7816-4 패딩 제거
            PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(
                    new CBCBlockCipher(new AESEngine()),
                    new ISO7816d4Padding()
            );
            ParametersWithIV params = new ParametersWithIV(new KeyParameter(sesAuthENCKey), iv);
            cipher.init(false, params);

            byte[] output = new byte[cipher.getOutputSize(encryptedData.length)];
            int len = cipher.processBytes(encryptedData, 0, encryptedData.length, output, 0);
            len += cipher.doFinal(output, len);

            return Arrays.copyOf(output, len);
        } catch (Exception e) {
            throw new RuntimeException("복호화 실패: " + e.getMessage(), e);
        }
    }

    /**
     * Command Counter 증가
     * 각 명령/응답 후에 호출해야 함
     */
    public void incrementCommandCounter() {
        commandCounter = (commandCounter + 1) & 0xFFFF; // 16비트 래핑
    }

    /**
     * Command Counter 리셋
     * AuthenticateEV2First 성공 후 호출
     */
    public void resetCommandCounter() {
        commandCounter = 0;
    }

    /**
     * 현재 Command Counter 값 반환
     */
    public int getCommandCounter() {
        return commandCounter;
    }

    /**
     * Transaction ID 반환
     */
    public byte[] getTransactionId() {
        return Arrays.copyOf(transactionId, 4);
    }

    /**
     * Full CMAC 계산 (내부 사용)
     */
    private static byte[] calculateFullCMAC(byte[] key, byte[] data) {
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
            throw new RuntimeException("CMAC 계산 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 디버그: 보안 메시징 상태 출력
     */
    public void printDebugInfo() {
        System.out.println("=== Secure Messaging State ===");
        System.out.println("Transaction ID: " + HexUtils.bytesToHex(transactionId));
        System.out.println("Command Counter: " + String.format("0x%04X", commandCounter));
        System.out.println("SesAuthMACKey: " + HexUtils.bytesToHex(sesAuthMACKey));
        System.out.println("SesAuthENCKey: " + HexUtils.bytesToHex(sesAuthENCKey));
    }
}
