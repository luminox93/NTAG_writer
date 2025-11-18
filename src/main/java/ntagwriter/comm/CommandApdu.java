package ntagwriter.comm;

import java.util.Objects;

/**
 * ISO/IEC 7816-4 표준에 따른 Command APDU 표현 클래스.
 * CLA/INS/P1/P2 헤더와 선택적인 데이터(Lc) 및 Le 필드를 관리한다.
 */
public final class CommandApdu {

    private final byte cla;
    private final byte ins;
    private final byte p1;
    private final byte p2;
    private final byte[] data;
    private final Integer le; // null이면 Le 없음

    private CommandApdu(Builder builder) {
        this.cla = builder.cla;
        this.ins = builder.ins;
        this.p1 = builder.p1;
        this.p2 = builder.p2;
        this.data = builder.data == null ? new byte[0] : builder.data.clone();
        this.le = builder.le;
    }

    public byte getCla() {
        return cla;
    }

    public byte getIns() {
        return ins;
    }

    public byte getP1() {
        return p1;
    }

    public byte getP2() {
        return p2;
    }

    public int getLc() {
        return data.length;
    }

    public byte[] getData() {
        return data.clone();
    }

    public boolean hasLe() {
        return le != null;
    }

    public Integer getLe() {
        return le;
    }

    /**
     * APDU 전체 바이트 배열로 직렬화한다.
     */
    public byte[] toBytes() {
        int totalLength = 4; // CLA, INS, P1, P2
        if (data.length > 0) {
            totalLength += 1 + data.length; // Lc + 데이터
        }
        if (le != null) {
            totalLength += 1;
        }

        byte[] apdu = new byte[totalLength];
        int idx = 0;
        apdu[idx++] = cla;
        apdu[idx++] = ins;
        apdu[idx++] = p1;
        apdu[idx++] = p2;

        if (data.length > 0) {
            apdu[idx++] = (byte) data.length;
            System.arraycopy(data, 0, apdu, idx, data.length);
            idx += data.length;
        }

        if (le != null) {
            apdu[idx] = (byte) le.intValue();
        }

        return apdu;
    }

    @Override
    public String toString() {
        return "CommandApdu{" +
                "cla=" + String.format("0x%02X", cla & 0xFF) +
                ", ins=" + String.format("0x%02X", ins & 0xFF) +
                ", p1=" + String.format("0x%02X", p1 & 0xFF) +
                ", p2=" + String.format("0x%02X", p2 & 0xFF) +
                ", lc=" + getLc() +
                ", le=" + (le == null ? "-" : String.format("0x%02X", le & 0xFF)) +
                '}';
    }

    public static Builder builder(byte cla, byte ins) {
        return new Builder(cla, ins);
    }

    /**
     * Command APDU 빌더.
     */
    public static final class Builder {
        private final byte cla;
        private final byte ins;
        private byte p1;
        private byte p2;
        private byte[] data = new byte[0];
        private Integer le;

        private Builder(byte cla, byte ins) {
            this.cla = cla;
            this.ins = ins;
        }

        public Builder p1(byte value) {
            this.p1 = value;
            return this;
        }

        public Builder p2(byte value) {
            this.p2 = value;
            return this;
        }

        public Builder header(byte p1, byte p2) {
            this.p1 = p1;
            this.p2 = p2;
            return this;
        }

        public Builder data(byte[] value) {
            if (value == null || value.length == 0) {
                this.data = new byte[0];
                return this;
            }
            this.data = value.clone();
            return this;
        }

        public Builder le(Integer value) {
            if (value == null) {
                this.le = null;
            } else {
                if (value < 0 || value > 0xFF) {
                    throw new IllegalArgumentException("Le는 0~255 범위여야 합니다.");
                }
                this.le = value;
            }
            return this;
        }

        public CommandApdu build() {
            Objects.requireNonNull(data, "data");
            if (data.length > 0xFF) {
                throw new IllegalArgumentException("Lc(Data 길이)는 255 바이트를 초과할 수 없습니다.");
            }
            return new CommandApdu(this);
        }
    }
}
