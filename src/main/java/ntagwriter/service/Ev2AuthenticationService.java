package ntagwriter.service;

import ntagwriter.crypto.ByteRotation;
import ntagwriter.crypto.SessionVectorBuilder;
import ntagwriter.reader.ReaderException;

import javax.smartcardio.ResponseAPDU;
import java.security.GeneralSecurityException;
import java.util.Arrays;

/**
 * EV2 인증 시퀀스 구현 (NT4H2421Gx, Section 9.1).
 * Table 26/28/29를 기반으로 AuthenticateEV2First Part1/Part2를 처리한다.
 */
public class Ev2AuthenticationService {

    private static final byte[] ZERO_IV = new byte[16];
    private static final byte[] DEFAULT_PCD_CAP2 = new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

    private final NfcReaderService readerService;
    private final CryptoService cryptoService;

    public Ev2AuthenticationService(NfcReaderService readerService, CryptoService cryptoService) {
        this.readerService = readerService;
        this.cryptoService = cryptoService;
    }

    /**
     * AuthenticateEV2First Part1/Part2 전체 프로세스 수행.
     *
     * @param keyNumber 인증에 사용할 키 번호
     * @param authKey   16바이트 인증 키 (Kx)
     * @return EV2 세션 정보
     */
    public Ev2Session authenticate(byte keyNumber, byte[] authKey)
            throws ReaderException, GeneralSecurityException {

        // Part 1: AuthenticateEV2First (Table 26)
        ResponseAPDU response1 = readerService.sendCommand(
                ntagwriter.util.ApduCommand.authenticateEV2First(keyNumber, DEFAULT_PCD_CAP2));

        if (response1.getSW() != 0x91AF) {
            throw new ReaderException("EV2 Part1 실패: " + readerService.getErrorMessage(response1));
        }

        byte[] encRndB = response1.getData();
        byte[] rndB = cryptoService.decryptCBC(authKey, ZERO_IV, encRndB);

        // Part 2 요청 데이터 생성 (Table 28)
        byte[] rndA = cryptoService.generateRandomBytes(16);
        byte[] rndBPrime = ByteRotation.rotateLeft(rndB);

        byte[] plainPart2 = new byte[32];
        System.arraycopy(rndA, 0, plainPart2, 0, 16);
        System.arraycopy(rndBPrime, 0, plainPart2, 16, 16);

        byte[] encPart2 = cryptoService.encryptCBC(authKey, ZERO_IV, plainPart2);

        byte[] part2Command = ntagwriter.comm.CommandApdu.builder((byte) 0x90, (byte) 0xAF)
                .header((byte) 0x00, (byte) 0x00)
                .data(encPart2)
                .le(0)
                .build()
                .toBytes();

        ResponseAPDU response2 = readerService.sendCommand(part2Command);

        int sw = response2.getSW();
        if (sw != 0x9100 && sw != 0x9000) {
            throw new ReaderException("EV2 Part2 실패: " + readerService.getErrorMessage(response2));
        }

        // Part 2 응답 복호화 및 검증 (Table 29)
        byte[] decrypted = cryptoService.decryptCBC(authKey, ZERO_IV, response2.getData());
        if (decrypted.length < 32) {
            throw new ReaderException("EV2 응답 길이 오류");
        }

        int offset = 0;
        byte[] transactionId = Arrays.copyOfRange(decrypted, offset, offset + 4);
        offset += 4;

        byte[] rndAPrime = Arrays.copyOfRange(decrypted, offset, offset + 16);
        offset += 16;

        byte[] pdCapabilities = Arrays.copyOfRange(decrypted, offset, offset + 6);
        offset += 6;

        byte[] pcdCapabilities = Arrays.copyOfRange(decrypted, offset, offset + 6);

        byte[] rndAOriginal = ByteRotation.rotateRight(rndAPrime);
        if (!Arrays.equals(rndAOriginal, rndA)) {
            throw new ReaderException("EV2 검증 실패: RndA 불일치");
        }

        byte[] kSesAuthEnc = deriveSessionKey(authKey, rndA, rndB, (byte) 0xA5, (byte) 0x5A);
        byte[] kSesAuthMac = deriveSessionKey(authKey, rndA, rndB, (byte) 0x5A, (byte) 0xA5);

        return new Ev2Session(transactionId, rndA, rndB, kSesAuthEnc, kSesAuthMac,
                pcdCapabilities, pdCapabilities);
    }

    private byte[] deriveSessionKey(byte[] authKey, byte[] rndA, byte[] rndB,
                                    byte prefix1, byte prefix2) {
        byte[] sv = SessionVectorBuilder.build(prefix1, prefix2, rndA, rndB);
        return cryptoService.calculateCmac(authKey, sv);
    }

    /**
     * EV2 인증 결과 (세션 정보).
     */
    public static final class Ev2Session {
        private final byte[] transactionId;
        private final byte[] rndA;
        private final byte[] rndB;
        private final byte[] kSesAuthEnc;
        private final byte[] kSesAuthMac;
        private final byte[] pcdCapabilities;
        private final byte[] pdCapabilities;
        private final byte[] commandCounter = new byte[] {0x00, 0x00}; // CmdCtr 초기화

        private Ev2Session(byte[] transactionId,
                           byte[] rndA,
                           byte[] rndB,
                           byte[] kSesAuthEnc,
                           byte[] kSesAuthMac,
                           byte[] pcdCapabilities,
                           byte[] pdCapabilities) {
            this.transactionId = transactionId.clone();
            this.rndA = rndA.clone();
            this.rndB = rndB.clone();
            this.kSesAuthEnc = kSesAuthEnc.clone();
            this.kSesAuthMac = kSesAuthMac.clone();
            this.pcdCapabilities = pcdCapabilities.clone();
            this.pdCapabilities = pdCapabilities.clone();
        }

        public byte[] transactionId() {
            return transactionId.clone();
        }

        public byte[] rndA() {
            return rndA.clone();
        }

        public byte[] rndB() {
            return rndB.clone();
        }

        public byte[] kSesAuthEnc() {
            return kSesAuthEnc.clone();
        }

        public byte[] kSesAuthMac() {
            return kSesAuthMac.clone();
        }

        public byte[] pcdCapabilities() {
            return pcdCapabilities.clone();
        }

        public byte[] pdCapabilities() {
            return pdCapabilities.clone();
        }

        public byte[] commandCounter() {
            return commandCounter.clone();
        }
    }
}
