package ntagwriter.domain;

/**
 * NTAG424 SDM (Secure Dynamic Messaging) 설정 정보
 */
public class SdmConfig {
    private String baseUrl;
    private byte[] aesKey;
    private int picOffset;
    private int sdmMacOffset;
    private int sdmMacInputOffset;

    public SdmConfig() {
    }

    public SdmConfig(String baseUrl, byte[] aesKey) {
        this.baseUrl = baseUrl;
        this.aesKey = aesKey;
    }

    // Getters and Setters
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public byte[] getAesKey() {
        return aesKey;
    }

    public void setAesKey(byte[] aesKey) {
        this.aesKey = aesKey;
    }

    public int getPicOffset() {
        return picOffset;
    }

    public void setPicOffset(int picOffset) {
        this.picOffset = picOffset;
    }

    public int getSdmMacOffset() {
        return sdmMacOffset;
    }

    public void setSdmMacOffset(int sdmMacOffset) {
        this.sdmMacOffset = sdmMacOffset;
    }

    public int getSdmMacInputOffset() {
        return sdmMacInputOffset;
    }

    public void setSdmMacInputOffset(int sdmMacInputOffset) {
        this.sdmMacInputOffset = sdmMacInputOffset;
    }

    @Override
    public String toString() {
        return "SdmConfig{" +
                "baseUrl='" + baseUrl + '\'' +
                ", picOffset=" + picOffset +
                ", sdmMacOffset=" + sdmMacOffset +
                ", sdmMacInputOffset=" + sdmMacInputOffset +
                '}';
    }
}
