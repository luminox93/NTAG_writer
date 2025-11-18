package ntagwriter.infrastructure.nfc.apdu;

import java.nio.ByteBuffer;

/**
 * Select Application APDU 명령
 * ISO 7816-4 SELECT 명령
 */
public final class SelectApplicationCommand implements ApduCommand {

    private static final byte CLA = (byte) 0x00;  // ISO 7816-4
    private static final byte INS = (byte) 0xA4;  // SELECT
    private static final byte P1_SELECT_BY_NAME = 0x04;
    private static final byte P2_FIRST_OR_ONLY = 0x00;

    public final byte[] aid;

    public SelectApplicationCommand(byte[] aid) {
        if (aid == null || aid.length == 0) {
            throw new IllegalArgumentException("AID cannot be null or empty");
        }
        this.aid = aid.clone();
    }

    /**
     * NTAG424 DNA 애플리케이션 선택
     */
    public static SelectApplicationCommand ntag424() {
        return new SelectApplicationCommand(new byte[] {
            (byte) 0xD2, 0x76, 0x00, 0x00, (byte) 0x85, 0x01, 0x01
        });
    }

    @Override
    public byte[] build() {
        ByteBuffer buffer = ByteBuffer.allocate(6 + aid.length);
        buffer.put(CLA);
        buffer.put(INS);
        buffer.put(P1_SELECT_BY_NAME);
        buffer.put(P2_FIRST_OR_ONLY);
        buffer.put((byte) aid.length);  // Lc
        buffer.put(aid);                 // Data
        buffer.put((byte) 0x00);         // Le (expect maximum response)
        return buffer.array();
    }

    @Override
    public String name() {
        return "SELECT_APPLICATION";
    }

    @Override
    public String description() {
        return String.format("Select application with AID: %s", bytesToHex(aid));
    }

    @Override
    public int expectedResponseLength() {
        return 256; // Maximum response for application selection
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}