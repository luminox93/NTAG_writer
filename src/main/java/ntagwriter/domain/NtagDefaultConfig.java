package ntagwriter.domain;

import ntagwriter.util.HexUtils;

/**
 * NTAG424 DNA 기본 설정값
 * docs/ntag_todo.md에 정의된 프로그래밍 기본값을 enum으로 관리
 */
public enum NtagDefaultConfig {
    /**
     * walkd.co.kr 프로젝트용 기본 설정
     */
    WALKD_PRODUCTION(
        "https://challenge.walkd.co.kr/dashboard?" +
        "enc=00000000000000000000000000000000&" +
        "cmac=0000000000000000",
        (byte) 0x0E,  // Read: Free access (0E)
        (byte) 0x00,  // Write: Key 0
        (byte) 0x00,  // ReadWrite: Key 0
        (byte) 0x00,  // Change: Key 0
        (byte) 0x02,  // SDM Meta Read: Key 2
        (byte) 0x01,  // SDM File Read: Key 1
        (byte) 0x01,  // SDM Counter Ret: Key 1
        43,           // PICC Data Offset
        81,           // SDM MAC Input Offset
        81            // SDM MAC Offset
    );

    private final String baseUrl;

    // Access Rights
    private final byte readAccess;
    private final byte writeAccess;
    private final byte readWriteAccess;
    private final byte changeAccess;

    // SDM Access Rights
    private final byte sdmMetaRead;
    private final byte sdmFileRead;
    private final byte sdmCounterRet;

    // Mirroring Offset Bytes
    private final int piccDataOffset;
    private final int sdmMacInputOffset;
    private final int sdmMacOffset;

    NtagDefaultConfig(String baseUrl,
                      byte readAccess, byte writeAccess, byte readWriteAccess, byte changeAccess,
                      byte sdmMetaRead, byte sdmFileRead, byte sdmCounterRet,
                      int piccDataOffset, int sdmMacInputOffset, int sdmMacOffset) {
        this.baseUrl = baseUrl;
        this.readAccess = readAccess;
        this.writeAccess = writeAccess;
        this.readWriteAccess = readWriteAccess;
        this.changeAccess = changeAccess;
        this.sdmMetaRead = sdmMetaRead;
        this.sdmFileRead = sdmFileRead;
        this.sdmCounterRet = sdmCounterRet;
        this.piccDataOffset = piccDataOffset;
        this.sdmMacInputOffset = sdmMacInputOffset;
        this.sdmMacOffset = sdmMacOffset;
    }

    // Getters
    public String getBaseUrl() {
        return baseUrl;
    }

    public byte getReadAccess() {
        return readAccess;
    }

    public byte getWriteAccess() {
        return writeAccess;
    }

    public byte getReadWriteAccess() {
        return readWriteAccess;
    }

    public byte getChangeAccess() {
        return changeAccess;
    }

    public byte getSdmMetaRead() {
        return sdmMetaRead;
    }

    public byte getSdmFileRead() {
        return sdmFileRead;
    }

    public byte getSdmCounterRet() {
        return sdmCounterRet;
    }

    public int getPiccDataOffset() {
        return piccDataOffset;
    }

    public int getSdmMacInputOffset() {
        return sdmMacInputOffset;
    }

    public int getSdmMacOffset() {
        return sdmMacOffset;
    }

    /**
     * Access Rights를 4바이트로 반환 (RRWW RRWW CRWU)
     * RR = Read, WW = Write, C = Change, R/W = ReadWrite, U = Unused
     */
    public byte[] getAccessRightsBytes() {
        // NTAG424 Access Rights 포맷:
        // Byte 0: Read/Write access (4 bits each)
        // Byte 1: Change/ReadWrite access (4 bits each)
        byte byte0 = (byte) ((readAccess << 4) | writeAccess);
        byte byte1 = (byte) ((changeAccess << 4) | readWriteAccess);
        return new byte[] { byte0, byte1 };
    }

    /**
     * SDM Access Rights를 반환
     */
    public byte[] getSdmAccessRightsBytes() {
        return new byte[] { sdmMetaRead, sdmFileRead, sdmCounterRet };
    }

    @Override
    public String toString() {
        return String.format("""
            NtagDefaultConfig {
              Base URL: %s
              Access Rights:
                - Read: 0x%02X
                - Write: 0x%02X
                - ReadWrite: 0x%02X
                - Change: 0x%02X
              SDM Access Rights:
                - Meta Read: 0x%02X
                - File Read: 0x%02X
                - Counter Ret: 0x%02X
              Mirroring Offsets:
                - PICC Data: %d
                - SDM MAC Input: %d
                - SDM MAC: %d
            }""",
            baseUrl,
            readAccess, writeAccess, readWriteAccess, changeAccess,
            sdmMetaRead, sdmFileRead, sdmCounterRet,
            piccDataOffset, sdmMacInputOffset, sdmMacOffset
        );
    }
}
