package ntagwriter.debug;

import ntagwriter.util.HexUtils;

/**
 * DTO holding the reference values from AN12196 Table 18.
 */
public class ChangeFileSettingsExample {

    private final byte[] kSesAuthEnc;
    private final byte[] kSesAuthMac;
    private final byte[] ti;
    private final byte[] cmdCtr;
    private final byte fileNumber;
    private final byte[] cmdData;
    private final byte[] expectedEncryptedSettings;
    private final byte[] expectedMacT;

    private ChangeFileSettingsExample(Builder builder) {
        this.kSesAuthEnc = builder.kSesAuthEnc;
        this.kSesAuthMac = builder.kSesAuthMac;
        this.ti = builder.ti;
        this.cmdCtr = builder.cmdCtr;
        this.fileNumber = builder.fileNumber;
        this.cmdData = builder.cmdData;
        this.expectedEncryptedSettings = builder.expectedEncryptedSettings;
        this.expectedMacT = builder.expectedMacT;
    }

    public static ChangeFileSettingsExample docExample() {
        return builder()
            .kSesAuthEnc("1309C877509E5A215007FF0ED19CA564")
            .kSesAuthMac("4C6626F5E72EA694202139295C7A7FC7")
            .ti("9D00C4DF")
            .cmdCtr("0100")
            .fileNumber((byte) 0x02)
            .cmdData("4000E0C1F121200000430000430000")
            .expectedEncryptedSettings("61B6D97903566E84C3AE5274467E89EA")
            .expectedMacT("D799B7C1A0EF7A04")
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public byte[] getKSesAuthEnc() {
        return kSesAuthEnc;
    }

    public byte[] getKSesAuthMac() {
        return kSesAuthMac;
    }

    public byte[] getTi() {
        return ti;
    }

    public byte[] getCmdCtr() {
        return cmdCtr;
    }

    public byte getFileNumber() {
        return fileNumber;
    }

    public byte[] getCmdData() {
        return cmdData;
    }

    public byte[] getExpectedEncryptedSettings() {
        return expectedEncryptedSettings;
    }

    public byte[] getExpectedMacT() {
        return expectedMacT;
    }

    public static class Builder {
        private byte[] kSesAuthEnc;
        private byte[] kSesAuthMac;
        private byte[] ti;
        private byte[] cmdCtr;
        private byte fileNumber;
        private byte[] cmdData;
        private byte[] expectedEncryptedSettings;
        private byte[] expectedMacT;

        public Builder kSesAuthEnc(String hex) {
            this.kSesAuthEnc = HexUtils.hexToBytes(hex);
            return this;
        }

        public Builder kSesAuthMac(String hex) {
            this.kSesAuthMac = HexUtils.hexToBytes(hex);
            return this;
        }

        public Builder ti(String hex) {
            this.ti = HexUtils.hexToBytes(hex);
            return this;
        }

        public Builder cmdCtr(String hexLittleEndian) {
            this.cmdCtr = HexUtils.hexToBytes(hexLittleEndian);
            return this;
        }

        public Builder fileNumber(byte fileNumber) {
            this.fileNumber = fileNumber;
            return this;
        }

        public Builder cmdData(String hex) {
            this.cmdData = HexUtils.hexToBytes(hex);
            return this;
        }

        public Builder expectedEncryptedSettings(String hex) {
            this.expectedEncryptedSettings = HexUtils.hexToBytes(hex);
            return this;
        }

        public Builder expectedMacT(String hex) {
            this.expectedMacT = HexUtils.hexToBytes(hex);
            return this;
        }

        public ChangeFileSettingsExample build() {
            return new ChangeFileSettingsExample(this);
        }
    }
}
