package ntagwriter.crypto;

import java.util.Objects;

/**
 * NTAG424 DNA Session Vector(SV) 생성기.
 * 데이터시트 9.1.7 공식: prefix1 || prefix2 || 00 01 00 80 || RndA[15:14] ||
 * (RndA[13:8] XOR RndB[15:10]) || RndB[9:0] || RndA[7:0]
 */
public final class SessionVectorBuilder {

    private SessionVectorBuilder() {
    }

    public static byte[] build(byte prefix1, byte prefix2, byte[] rndA, byte[] rndB) {
        Objects.requireNonNull(rndA, "rndA");
        Objects.requireNonNull(rndB, "rndB");
        if (rndA.length != 16 || rndB.length != 16) {
            throw new IllegalArgumentException("RndA/RndB는 16바이트여야 합니다.");
        }

        byte[] sv = new byte[32];
        int idx = 0;

        sv[idx++] = prefix1;
        sv[idx++] = prefix2;
        sv[idx++] = 0x00;
        sv[idx++] = 0x01;
        sv[idx++] = 0x00;
        sv[idx++] = (byte) 0x80;

        sv[idx++] = rndA[15];
        sv[idx++] = rndA[14];

        for (int i = 0; i < 6; i++) {
            sv[idx++] = (byte) (rndA[13 - i] ^ rndB[15 - i]);
        }

        for (int i = 9; i >= 0; i--) {
            sv[idx++] = rndB[i];
        }

        for (int i = 7; i >= 0; i--) {
            sv[idx++] = rndA[i];
        }

        return sv;
    }
}
