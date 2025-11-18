package ntagwriter.infrastructure.nfc.apdu;

import java.nio.ByteBuffer;

/**
 * AuthenticateEV2First APDU 명령
 * NTAG424 DNA EV2 인증 시작
 */
public final class AuthenticateEV2FirstCommand implements ApduCommand {

    private static final byte CLA = (byte) 0x90;
    private static final byte INS = (byte) 0x71;

    public final byte keyNumber;
    public final byte[] lengthCapability;

    public AuthenticateEV2FirstCommand(byte keyNumber) {
        this(keyNumber, null);
    }

    public AuthenticateEV2FirstCommand(byte keyNumber, byte[] lengthCapability) {
        if (keyNumber < 0 || keyNumber > 0x0D) {
            throw new IllegalArgumentException("Key number must be between 0x00 and 0x0D");
        }
        this.keyNumber = keyNumber;
        this.lengthCapability = lengthCapability;
    }

    @Override
    public byte[] build() {
        int dataLength = (lengthCapability != null) ? lengthCapability.length : 0;
        ByteBuffer buffer = ByteBuffer.allocate(7 + dataLength);

        buffer.put(CLA);
        buffer.put(INS);
        buffer.put(keyNumber);          // P1: Key number
        buffer.put((byte) 0x00);        // P2: 0x00 for first authentication

        if (lengthCapability != null && lengthCapability.length > 0) {
            buffer.put((byte) lengthCapability.length);  // Lc
            buffer.put(lengthCapability);                 // Data (PCDcap2)
        } else {
            buffer.put((byte) 0x00);                      // Lc = 0
        }

        buffer.put((byte) 0x00);        // Le: expect maximum response

        return buffer.array();
    }

    @Override
    public String name() {
        return "AUTHENTICATE_EV2_FIRST";
    }

    @Override
    public String description() {
        return String.format("Authenticate EV2 First with key #%02X", keyNumber);
    }

    @Override
    public int expectedResponseLength() {
        return 17; // 16 bytes encrypted RndB + 1 byte status
    }
}