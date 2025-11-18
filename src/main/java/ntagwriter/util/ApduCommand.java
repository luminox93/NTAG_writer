package ntagwriter.util;

import ntagwriter.comm.CommandApdu;

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
        return CommandApdu.builder(CLA_ISO, INS_SELECT)
                .p1((byte) 0x04)
                .p2((byte) 0x00)
                .data(aid)
                .build()
                .toBytes();
    }

    /**
     * GET DATA 명령어 생성
     *
     * @param tag Data object tag
     * @return APDU 명령어 바이트 배열
     */
    public static byte[] getData(byte tag) {
        return CommandApdu.builder(CLA_ISO, INS_GET_DATA)
                .p1((byte) 0x00)
                .p2(tag)
                .le(0)
                .build()
                .toBytes();
    }

    /**
     * READ BINARY 명령어 생성
     *
     * @param offset 읽기 시작 오프셋
     * @param length 읽을 데이터 길이
     * @return APDU 명령어 바이트 배열
     */
    public static byte[] readBinary(int offset, int length) {
        return CommandApdu.builder(CLA_ISO, INS_READ_BINARY)
                .p1((byte) (offset >> 8))
                .p2((byte) (offset & 0xFF))
                .le(length)
                .build()
                .toBytes();
    }

    /**
     * UPDATE BINARY 명령어 생성
     *
     * @param offset 쓰기 시작 오프셋
     * @param data   쓸 데이터
     * @return APDU 명령어 바이트 배열
     */
    public static byte[] updateBinary(int offset, byte[] data) {
        return CommandApdu.builder(CLA_ISO, INS_UPDATE_BINARY)
                .p1((byte) (offset >> 8))
                .p2((byte) (offset & 0xFF))
                .data(data)
                .build()
                .toBytes();
    }

    /**
     * NTAG424 AUTHENTICATE EV2 First 명령어 생성
     * AN12196 Section 3.6
     *
     * @param keyNo 인증할 키 번호
     * @param lenCap capability data 길이 (보통 0x00)
     * @return APDU 명령어 바이트 배열
     */
    public static byte[] authenticateEV2First(byte keyNo, byte lenCap) {
        int capLen = Byte.toUnsignedInt(lenCap);
        if (capLen == 0) {
            return authenticateEV2First(keyNo, (byte[]) null);
        }
        byte[] capabilities = new byte[capLen];
        return authenticateEV2First(keyNo, capabilities);
    }

    /**
     * NTAG424 AUTHENTICATE EV2 First 명령어 생성 (PCDcap2 포함)
     *
     * @param keyNo         인증할 키 번호
     * @param pcdCapabilities PCDcap2 데이터 (LenCap = length)
     * @return APDU 명령어 바이트 배열
     */
    public static byte[] authenticateEV2First(byte keyNo, byte[] pcdCapabilities) {
        int capLen = pcdCapabilities == null ? 0 : pcdCapabilities.length;
        byte[] payload = new byte[2 + capLen];
        payload[0] = keyNo;
        payload[1] = (byte) capLen;
        if (capLen > 0) {
            System.arraycopy(pcdCapabilities, 0, payload, 2, capLen);
        }

        return CommandApdu.builder(CLA_PROPRIETARY, INS_AUTHENTICATE_EV2_FIRST)
                .header((byte) 0x00, (byte) 0x00)
                .data(payload)
                .le(0)
                .build()
                .toBytes();
    }

    /**
     * NTAG424 GET VERSION 명령어 생성
     *
     * @return APDU 명령어 바이트 배열
     */
    public static byte[] getVersion() {
        return CommandApdu.builder(CLA_PROPRIETARY, INS_GET_VERSION)
                .header((byte) 0x00, (byte) 0x00)
                .le(0)
                .build()
                .toBytes();
    }

    /**
     * NTAG424 GET CARD UID 명령어 생성
     *
     * @return APDU 명령어 바이트 배열
     */
    public static byte[] getCardUid() {
        return CommandApdu.builder(CLA_PROPRIETARY, INS_GET_CARD_UID)
                .header((byte) 0x00, (byte) 0x00)
                .le(0)
                .build()
                .toBytes();
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
        byte[] payload = new byte[1 + encryptedKeyData.length + mac.length];
        int idx = 0;
        payload[idx++] = keyNo;
        System.arraycopy(encryptedKeyData, 0, payload, idx, encryptedKeyData.length);
        idx += encryptedKeyData.length;
        System.arraycopy(mac, 0, payload, idx, mac.length);

        return CommandApdu.builder(CLA_PROPRIETARY, INS_CHANGE_KEY)
                .header((byte) 0x00, (byte) 0x00)
                .data(payload)
                .le(0)
                .build()
                .toBytes();
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
        byte[] payload = new byte[1 + encryptedSettings.length + mac.length];
        int idx = 0;
        payload[idx++] = fileNo;
        System.arraycopy(encryptedSettings, 0, payload, idx, encryptedSettings.length);
        idx += encryptedSettings.length;
        System.arraycopy(mac, 0, payload, idx, mac.length);

        return CommandApdu.builder(CLA_PROPRIETARY, INS_CHANGE_FILE_SETTINGS)
                .header((byte) 0x00, (byte) 0x00)
                .data(payload)
                .le(0)
                .build()
                .toBytes();
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
        byte[] payload = new byte[1 + 3 + 3 + encryptedData.length + mac.length];
        int idx = 0;
        payload[idx++] = fileNo;
        // Offset little-endian
        payload[idx++] = (byte) (offset & 0xFF);
        payload[idx++] = (byte) ((offset >> 8) & 0xFF);
        payload[idx++] = (byte) ((offset >> 16) & 0xFF);
        // Length little-endian
        payload[idx++] = (byte) (length & 0xFF);
        payload[idx++] = (byte) ((length >> 8) & 0xFF);
        payload[idx++] = (byte) ((length >> 16) & 0xFF);
        System.arraycopy(encryptedData, 0, payload, idx, encryptedData.length);
        idx += encryptedData.length;
        System.arraycopy(mac, 0, payload, idx, mac.length);

        return CommandApdu.builder(CLA_PROPRIETARY, INS_WRITE_DATA)
                .header((byte) 0x00, (byte) 0x00)
                .data(payload)
                .le(0)
                .build()
                .toBytes();
    }

    /**
     * NTAG424 WRITE DATA 명령어 생성 (CommMode.Plain)
     * 인증은 되어 있지만 데이터와 MAC 없이 Native WriteData를 사용할 때 사용
     */
    public static byte[] writeData(byte fileNo, int offset, byte[] data) {
        byte[] payload = new byte[1 + 3 + 3 + data.length];
        int idx = 0;
        payload[idx++] = fileNo;
        payload[idx++] = (byte) (offset & 0xFF);
        payload[idx++] = (byte) ((offset >> 8) & 0xFF);
        payload[idx++] = (byte) ((offset >> 16) & 0xFF);

        int length = data.length;
        payload[idx++] = (byte) (length & 0xFF);
        payload[idx++] = (byte) ((length >> 8) & 0xFF);
        payload[idx++] = (byte) ((length >> 16) & 0xFF);
        System.arraycopy(data, 0, payload, idx, data.length);

        return CommandApdu.builder(CLA_PROPRIETARY, INS_WRITE_DATA)
                .header((byte) 0x00, (byte) 0x00)
                .data(payload)
                .le(0)
                .build()
                .toBytes();
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
