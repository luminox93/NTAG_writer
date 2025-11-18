package ntagwriter.domain.port;

/**
 * 암호화 관련 포트 인터페이스
 * 도메인 레이어에서 필요한 암호화 기능을 추상화
 */
public interface CryptoPort {

    /**
     * AES-128 암호화 (ECB 모드)
     *
     * @param key 암호화 키 (16 bytes)
     * @param data 평문 데이터
     * @return 암호화된 데이터
     */
    byte[] encryptAesEcb(byte[] key, byte[] data);

    /**
     * AES-128 복호화 (ECB 모드)
     *
     * @param key 복호화 키 (16 bytes)
     * @param data 암호화된 데이터
     * @return 복호화된 데이터
     */
    byte[] decryptAesEcb(byte[] key, byte[] data);

    /**
     * AES-128 암호화 (CBC 모드)
     *
     * @param key 암호화 키 (16 bytes)
     * @param iv 초기화 벡터 (16 bytes)
     * @param data 평문 데이터
     * @return 암호화된 데이터
     */
    byte[] encryptAesCbc(byte[] key, byte[] iv, byte[] data);

    /**
     * AES-128 복호화 (CBC 모드)
     *
     * @param key 복호화 키 (16 bytes)
     * @param iv 초기화 벡터 (16 bytes)
     * @param data 암호화된 데이터
     * @return 복호화된 데이터
     */
    byte[] decryptAesCbc(byte[] key, byte[] iv, byte[] data);

    /**
     * AES-CMAC 계산
     *
     * @param key CMAC 키 (16 bytes)
     * @param data 데이터
     * @return CMAC 값 (16 bytes)
     */
    byte[] calculateCmac(byte[] key, byte[] data);

    /**
     * 랜덤 바이트 생성
     *
     * @param length 생성할 바이트 수
     * @return 랜덤 바이트 배열
     */
    byte[] generateRandomBytes(int length);

    /**
     * CRC32 계산
     *
     * @param data 데이터
     * @return CRC32 값 (4 bytes)
     */
    byte[] calculateCrc32(byte[] data);

    /**
     * 세션 키 생성
     *
     * @param authKey 인증 키 (16 bytes)
     * @param rndA 랜덤 A (16 bytes)
     * @param rndB 랜덤 B (16 bytes)
     * @return [SesAuthENCKey, SesAuthMACKey] 배열
     */
    byte[][] generateSessionKeys(byte[] authKey, byte[] rndA, byte[] rndB);

    /**
     * 바이트 배열 왼쪽 회전
     *
     * @param data 원본 바이트 배열
     * @return 왼쪽으로 1바이트 회전된 배열
     */
    byte[] rotateLeft(byte[] data);

    /**
     * 바이트 배열 오른쪽 회전
     *
     * @param data 원본 바이트 배열
     * @return 오른쪽으로 1바이트 회전된 배열
     */
    byte[] rotateRight(byte[] data);

    /**
     * MAC 트렁케이션
     *
     * @param mac 원본 MAC (16 bytes)
     * @param length 트렁케이션할 길이
     * @return 트렁케이션된 MAC
     */
    byte[] truncateMac(byte[] mac, int length);
}