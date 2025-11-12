package ntagwriter.service;

import ntagwriter.crypto.AesEncryption;
import ntagwriter.crypto.CmacCalculator;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;

/**
 * 암호화 서비스
 * AES-CMAC 계산 및 세션 키 생성 등을 담당
 */
public class CryptoService {

    private final SecureRandom random;

    public CryptoService() {
        this.random = new SecureRandom();
    }

    /**
     * 랜덤 바이트 생성
     */
    public byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    /**
     * CMAC 계산
     */
    public byte[] calculateCmac(byte[] key, byte[] data) {
        return CmacCalculator.calculateCmac(key, data);
    }

    /**
     * CMAC 계산 (길이 지정)
     */
    public byte[] calculateCmac(byte[] key, byte[] data, int length) {
        return CmacCalculator.calculateCmac(key, data, length);
    }

    /**
     * CMAC 검증
     */
    public boolean verifyCmac(byte[] key, byte[] data, byte[] expectedMac) {
        return CmacCalculator.verifyCmac(key, data, expectedMac);
    }

    /**
     * 세션 키 생성
     */
    public byte[] deriveSessionKey(byte[] masterKey, byte[] rndA, byte[] rndB, byte svType) {
        return CmacCalculator.deriveSessionKey(masterKey, rndA, rndB, svType);
    }

    /**
     * AES ECB 암호화
     */
    public byte[] encryptECB(byte[] key, byte[] data) throws GeneralSecurityException {
        return AesEncryption.encryptECB(key, data);
    }

    /**
     * AES ECB 복호화
     */
    public byte[] decryptECB(byte[] key, byte[] data) throws GeneralSecurityException {
        return AesEncryption.decryptECB(key, data);
    }

    /**
     * AES CBC 암호화
     */
    public byte[] encryptCBC(byte[] key, byte[] iv, byte[] data) throws GeneralSecurityException {
        return AesEncryption.encryptCBC(key, iv, data);
    }

    /**
     * AES CBC 복호화
     */
    public byte[] decryptCBC(byte[] key, byte[] iv, byte[] data) throws GeneralSecurityException {
        return AesEncryption.decryptCBC(key, iv, data);
    }

    /**
     * 패딩 추가
     */
    public byte[] addPadding(byte[] data, int blockSize) {
        return AesEncryption.addPadding(data, blockSize);
    }

    /**
     * 패딩 제거
     */
    public byte[] removePadding(byte[] data) {
        return AesEncryption.removePadding(data);
    }
}
