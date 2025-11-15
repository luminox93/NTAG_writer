package ntagwriter.util;

/**
 * APDU (Application Protocol Data Unit) 명령어를 생성하는 유틸리티 클래스
 * NTAG424 DNA와 통신하기 위한 ISO 7816-4 표준 APDU 명령어를 생성합니다.
 */
public class ApduCommand {

    // ISO 7816-4 Class byte
    public static final byte CLA_ISO = (byte) 0x00;
    public static final byte CLA_PROPRIETARY = (byte) 0x90;

    // ISO 7816-4 Instruction bytes
    public static final byte INS_SELECT = (byte) 0xA4;
    public static final byte INS_READ_BINARY = (byte) 0xB0;
    public static final byte INS_UPDATE_BINARY = (byte) 0xD6;
    public static final byte INS_GET_DATA = (byte) 0xCA;

    // NTAG424 specific instruction bytes
    public static final byte INS_AUTHENTICATE_EV2_FIRST = (byte) 0x71;
    public static final byte INS_AUTHENTICATE_EV2_NON_FIRST = (byte) 0x77;
    public static final byte INS_CHANGE_FILE_SETTINGS = (byte) 0x5F;
    public static final byte INS_CHANGE_KEY = (byte) 0xC4;
    public static final byte INS_WRITE_DATA = (byte) 0x8D;
    public static final byte INS_GET_VERSION = (byte) 0x60;
    public static final byte INS_GET_CARD_UID = (byte) 0x51;

    /**
     * SELECT 명령어 생성
     *
     * @param aid Application Identifier
     * @return APDU 명령어 바이트 배열
     */
    public static byte[] selectApplication(byte[] aid) {
        byte[] apdu = new byte[5 + aid.length];
        apdu[0] = CLA_ISO;
        apdu[1] = INS_SELECT;
        apdu[2] = 0x04; // P1: Select by name
        apdu[3] = 0x00; // P2: First or only occurrence
        apdu[4] = (byte) aid.length; // Lc: Length of data
        System.arraycopy(aid, 0, apdu, 5, aid.length);
        return apdu;
    }

    /**
     * GET DATA 명령어 생성
     *
     * @param tag Data object tag
     * @return APDU 명령어 바이트 배열
     */
    public static byte[] getData(byte tag) {
        return new byte[]{CLA_ISO, INS_GET_DATA, 0x00, tag, 0x00};
    }

    /**
     * READ BINARY 명령어 생성
     *
     * @param offset 읽기 시작 오프셋
     * @param length 읽을 데이터 길이
     * @return APDU 명령어 바이트 배열
     */
    public static byte[] readBinary(int offset, int length) {
        return new byte[]{
                CLA_ISO,
                INS_READ_BINARY,
                (byte) (offset >> 8),
                (byte) (offset & 0xFF),
                (byte) length
        };
    }

    /**
     * UPDATE BINARY 명령어 생성
     *
     * @param offset 쓰기 시작 오프셋
     * @param data   쓸 데이터
     * @return APDU 명령어 바이트 배열
     */
    public static byte[] updateBinary(int offset, byte[] data) {
        byte[] apdu = new byte[5 + data.length];
        apdu[0] = CLA_ISO;
        apdu[1] = INS_UPDATE_BINARY;
        apdu[2] = (byte) (offset >> 8);
        apdu[3] = (byte) (offset & 0xFF);
        apdu[4] = (byte) data.length;
        System.arraycopy(data, 0, apdu, 5, data.length);
        return apdu;
    }

    /**
     * NTAG424 AUTHENTICATE EV2 First 명령어 생성
     *
     * @param keyNo 인증할 키 번호
     * @param data  인증 데이터 (랜덤 챌린지)
     * @return APDU 명령어 바이트 배열
     */
    public static byte[] authenticateEV2First(byte keyNo, byte[] data) {
        byte[] apdu = new byte[7 + data.length];
        apdu[0] = CLA_PROPRIETARY;
        apdu[1] = INS_AUTHENTICATE_EV2_FIRST;
        apdu[2] = 0x00;
        apdu[3] = 0x00;
        apdu[4] = (byte) (2 + data.length); // Lc
        apdu[5] = keyNo;
        apdu[6] = (byte) data.length;
        System.arraycopy(data, 0, apdu, 7, data.length);
        return apdu;
    }

    /**
     * NTAG424 GET VERSION 명령어 생성
     *
     * @return APDU 명령어 바이트 배열
     */
    public static byte[] getVersion() {
        return new byte[]{CLA_PROPRIETARY, INS_GET_VERSION, 0x00, 0x00, 0x00};
    }

    /**
     * NTAG424 GET CARD UID 명령어 생성
     *
     * @return APDU 명령어 바이트 배열
     */
    public static byte[] getCardUid() {
        return new byte[]{CLA_PROPRIETARY, INS_GET_CARD_UID, 0x00, 0x00, 0x00};
    }

    /**
     * NTAG424 CHANGE KEY 명령어 생성 (CommMode.FULL)
     * AN12196 Section 5.16 참조
     *
     * @param keyNo        변경할 키 번호
     * @param encryptedKeyData 암호화된 키 데이터 (KeyData를 KSesAuthENC로 암호화한 값)
     * @param mac          CMAC 값 (8 bytes, MACt)
     * @return APDU 명령어 바이트 배열
     */
    public static byte[] changeKey(byte keyNo, byte[] encryptedKeyData, byte[] mac) {
        // APDU 구조: CLA INS P1 P2 Lc KeyNo EncData MACt Le
        int dataLen = 1 + encryptedKeyData.length + mac.length;
        byte[] apdu = new byte[5 + dataLen + 1];

        apdu[0] = CLA_PROPRIETARY;  // 90
        apdu[1] = INS_CHANGE_KEY;    // C4
        apdu[2] = 0x00;              // P1
        apdu[3] = 0x00;              // P2
        apdu[4] = (byte) dataLen;    // Lc

        int offset = 5;
        apdu[offset++] = keyNo;
        System.arraycopy(encryptedKeyData, 0, apdu, offset, encryptedKeyData.length);
        offset += encryptedKeyData.length;
        System.arraycopy(mac, 0, apdu, offset, mac.length);
        offset += mac.length;

        apdu[offset] = 0x00;  // Le

        return apdu;
    }

    /**
     * NTAG424 CHANGE FILE SETTINGS 명령어 생성 (CommMode.FULL)
     * AN12196 Section 5.9, Table 18 참조
     *
     * @param fileNo       파일 번호
     * @param encryptedSettings 암호화된 파일 설정 데이터
     * @param mac          CMAC 값 (8 bytes, MACt)
     * @return APDU 명령어 바이트 배열
     */
    public static byte[] changeFileSettings(byte fileNo, byte[] encryptedSettings, byte[] mac) {
        // APDU 구조: CLA INS P1 P2 Lc FileNo EncSettings MACt Le
        int dataLen = 1 + encryptedSettings.length + mac.length;
        byte[] apdu = new byte[5 + dataLen + 1];

        apdu[0] = CLA_PROPRIETARY;        // 90
        apdu[1] = INS_CHANGE_FILE_SETTINGS; // 5F
        apdu[2] = 0x00;                    // P1
        apdu[3] = 0x00;                    // P2
        apdu[4] = (byte) dataLen;          // Lc

        int offset = 5;
        apdu[offset++] = fileNo;
        System.arraycopy(encryptedSettings, 0, apdu, offset, encryptedSettings.length);
        offset += encryptedSettings.length;
        System.arraycopy(mac, 0, apdu, offset, mac.length);
        offset += mac.length;

        apdu[offset] = 0x00;  // Le

        return apdu;
    }

    /**
     * NTAG424 WRITE DATA 명령어 생성 (CommMode.FULL)
     * AN12196 Section 5.8.2, Table 17 참조
     *
     * @param fileNo       파일 번호
     * @param offset       쓰기 시작 오프셋 (3 bytes, little-endian)
     * @param length       데이터 길이 (3 bytes, little-endian)
     * @param encryptedData 암호화된 데이터
     * @param mac          CMAC 값 (8 bytes, MACt)
     * @return APDU 명령어 바이트 배열
     */
    public static byte[] writeDataFull(byte fileNo, int offset, int length, byte[] encryptedData, byte[] mac) {
        // APDU 구조: CLA INS P1 P2 Lc FileNo Offset(3) Length(3) EncData MACt Le
        int headerLen = 1 + 3 + 3; // FileNo + Offset + Length
        int dataLen = headerLen + encryptedData.length + mac.length;
        byte[] apdu = new byte[5 + dataLen + 1];

        apdu[0] = CLA_PROPRIETARY;  // 90
        apdu[1] = INS_WRITE_DATA;   // 8D
        apdu[2] = 0x00;             // P1
        apdu[3] = 0x00;             // P2
        apdu[4] = (byte) dataLen;   // Lc

        int idx = 5;
        apdu[idx++] = fileNo;

        // Offset (3 bytes, little-endian)
        apdu[idx++] = (byte) (offset & 0xFF);
        apdu[idx++] = (byte) ((offset >> 8) & 0xFF);
        apdu[idx++] = (byte) ((offset >> 16) & 0xFF);

        // Length (3 bytes, little-endian)
        apdu[idx++] = (byte) (length & 0xFF);
        apdu[idx++] = (byte) ((length >> 8) & 0xFF);
        apdu[idx++] = (byte) ((length >> 16) & 0xFF);

        // Encrypted Data
        System.arraycopy(encryptedData, 0, apdu, idx, encryptedData.length);
        idx += encryptedData.length;

        // MAC
        System.arraycopy(mac, 0, apdu, idx, mac.length);
        idx += mac.length;

        apdu[idx] = 0x00;  // Le

        return apdu;
    }

    /**
     * NTAG424 WRITE DATA 명령어 생성 (CommMode.PLAIN)
     * AN12196 Section 5.8.1, Table 16 참조 (ISOUpdateBinary 사용)
     *
     * @param fileNo       파일 번호
     * @param offset       쓰기 시작 오프셋
     * @param data         쓸 데이터 (평문)
     * @return APDU 명령어 바이트 배열
     */
    public static byte[] writeDataPlain(byte fileNo, int offset, byte[] data) {
        // CommMode.PLAIN에서는 ISOUpdateBinary 사용
        // P1에 파일 번호를 OR
        byte[] apdu = new byte[5 + data.length];

        apdu[0] = CLA_ISO;              // 00
        apdu[1] = INS_UPDATE_BINARY;    // D6
        apdu[2] = (byte) (0x80 | fileNo); // P1: Short EF identifier
        apdu[3] = (byte) (offset & 0xFF); // P2: Offset (low byte)
        apdu[4] = (byte) data.length;     // Lc

        System.arraycopy(data, 0, apdu, 5, data.length);

        return apdu;
    }

    /**
     * APDU 명령어를 읽기 쉬운 문자열로 변환
     *
     * @param apdu APDU 명령어 바이트 배열
     * @return 16진수 문자열
     */
    public static String toString(byte[] apdu) {
        return HexUtils.bytesToHexSpaced(apdu);
    }
}
