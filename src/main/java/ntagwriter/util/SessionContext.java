package ntagwriter.util;

/**
 * 세션 정보 컨텍스트 (불변 객체)
 * 인증 후 세션 정보를 일관되게 관리
 */
public final class SessionContext {

    public final byte[] kSesAuthENC;
    public final byte[] kSesAuthMAC;
    public final byte[] transactionId;
    public final byte[] commandCounter;

    /**
     * 전체 세션 정보 생성자
     */
    public SessionContext(byte[] kSesAuthENC, byte[] kSesAuthMAC,
                         byte[] transactionId, byte[] commandCounter) {
        this.kSesAuthENC = kSesAuthENC;
        this.kSesAuthMAC = kSesAuthMAC;
        this.transactionId = transactionId;
        this.commandCounter = commandCounter;
    }

    /**
     * 키 관리용 세션 컨텍스트 생성 (최소 정보)
     */
    public static SessionContext forKeyManagement(byte[] kSesAuthENC, byte[] commandCounter) {
        return new SessionContext(kSesAuthENC, null, null, commandCounter);
    }

    /**
     * SDM 설정용 세션 컨텍스트 생성 (전체 정보)
     */
    public static SessionContext forSdmConfiguration(byte[] kSesAuthENC, byte[] kSesAuthMAC,
                                                     byte[] transactionId, byte[] commandCounter) {
        return new SessionContext(kSesAuthENC, kSesAuthMAC, transactionId, commandCounter);
    }

    /**
     * Command Counter 증가 (새 인스턴스 반환)
     */
    public SessionContext withIncrementedCounter() {
        byte[] newCounter = new byte[2];
        System.arraycopy(commandCounter, 0, newCounter, 0, 2);
        CommandCounterManager.increment(newCounter);
        return new SessionContext(kSesAuthENC, kSesAuthMAC, transactionId, newCounter);
    }

    /**
     * MAC 키가 있는지 확인
     */
    public boolean hasMacKey() {
        return kSesAuthMAC != null;
    }

    /**
     * Transaction ID가 있는지 확인
     */
    public boolean hasTransactionId() {
        return transactionId != null;
    }
}