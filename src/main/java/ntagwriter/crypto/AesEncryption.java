package ntagwriter.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.Arrays;

/**
 * AES 암호화/복호화 유틸리티
 * NTAG424 DNA의 AES-128 암호화를 위한 클래스
 */
public class AesEncryption {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION_ECB = "AES/ECB/NoPadding";
    private static final String TRANSFORMATION_CBC = "AES/CBC/NoPadding";

    /**
     * AES-128 ECB 모드로 암호화
     *
     * @param key  16바이트 AES 키
     * @param data 암호화할 데이터 (16의 배수여야 함)
     * @return 암호화된 데이터
     * @throws GeneralSecurityException 암호화 실패 시
     */
    public static byte[] encryptECB(byte[] key, byte[] data) throws GeneralSecurityException {
        SecretKeySpec secretKey = new SecretKeySpec(key, ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION_ECB);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }

    /**
     * AES-128 ECB 모드로 복호화
     *
     * @param key  16바이트 AES 키
     * @param data 복호화할 데이터 (16의 배수여야 함)
     * @return 복호화된 데이터
     * @throws GeneralSecurityException 복호화 실패 시
     */
    public static byte[] decryptECB(byte[] key, byte[] data) throws GeneralSecurityException {
        SecretKeySpec secretKey = new SecretKeySpec(key, ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION_ECB);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }

    /**
     * AES-128 CBC 모드로 암호화
     *
     * @param key  16바이트 AES 키
     * @param iv   16바이트 초기화 벡터
     * @param data 암호화할 데이터 (16의 배수여야 함)
     * @return 암호화된 데이터
     * @throws GeneralSecurityException 암호화 실패 시
     */
    public static byte[] encryptCBC(byte[] key, byte[] iv, byte[] data) throws GeneralSecurityException {
        SecretKeySpec secretKey = new SecretKeySpec(key, ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION_CBC);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        return cipher.doFinal(data);
    }

    /**
     * AES-128 CBC 모드로 복호화
     *
     * @param key  16바이트 AES 키
     * @param iv   16바이트 초기화 벡터
     * @param data 복호화할 데이터 (16의 배수여야 함)
     * @return 복호화된 데이터
     * @throws GeneralSecurityException 복호화 실패 시
     */
    public static byte[] decryptCBC(byte[] key, byte[] iv, byte[] data) throws GeneralSecurityException {
        SecretKeySpec secretKey = new SecretKeySpec(key, ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION_CBC);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        return cipher.doFinal(data);
    }

    /**
     * 데이터에 PKCS7 패딩 추가
     *
     * @param data      원본 데이터
     * @param blockSize 블록 크기 (일반적으로 16)
     * @return 패딩이 추가된 데이터
     */
    public static byte[] addPadding(byte[] data, int blockSize) {
        int paddingLength = blockSize - (data.length % blockSize);
        byte[] padded = new byte[data.length + paddingLength];
        System.arraycopy(data, 0, padded, 0, data.length);
        Arrays.fill(padded, data.length, padded.length, (byte) paddingLength);
        return padded;
    }

    /**
     * 데이터에서 PKCS7 패딩 제거
     *
     * @param data 패딩이 포함된 데이터
     * @return 패딩이 제거된 데이터
     */
    public static byte[] removePadding(byte[] data) {
        if (data == null || data.length == 0) {
            return data;
        }
        int paddingLength = data[data.length - 1] & 0xFF;
        if (paddingLength > 0 && paddingLength <= 16) {
            return Arrays.copyOf(data, data.length - paddingLength);
        }
        return data;
    }

    /**
     * 16바이트 제로 IV 생성
     *
     * @return 제로로 초기화된 16바이트 배열
     */
    public static byte[] getZeroIV() {
        return new byte[16];
    }
}
