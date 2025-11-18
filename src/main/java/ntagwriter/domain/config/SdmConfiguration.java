package ntagwriter.domain.config;

import java.util.Arrays;

/**
 * Secure Dynamic Messaging (SDM) 설정 도메인 값 객체
 * Immutable value object - 모든 필드는 public final
 */
public final class SdmConfiguration {
    public final String baseUrl;
    public final byte[] sdmMetaReadKey;
    public final byte[] sdmFileReadKey;
    public final boolean enableUidMirroring;
    public final boolean enableReadCounter;
    public final boolean enableEncryption;
    public final int picDataOffset;
    public final int sdmMacOffset;
    public final int sdmEncOffset;

    private SdmConfiguration(Builder builder) {
        this.baseUrl = builder.baseUrl;
        this.sdmMetaReadKey = builder.sdmMetaReadKey != null ?
            Arrays.copyOf(builder.sdmMetaReadKey, builder.sdmMetaReadKey.length) : null;
        this.sdmFileReadKey = builder.sdmFileReadKey != null ?
            Arrays.copyOf(builder.sdmFileReadKey, builder.sdmFileReadKey.length) : null;
        this.enableUidMirroring = builder.enableUidMirroring;
        this.enableReadCounter = builder.enableReadCounter;
        this.enableEncryption = builder.enableEncryption;
        this.picDataOffset = builder.picDataOffset;
        this.sdmMacOffset = builder.sdmMacOffset;
        this.sdmEncOffset = builder.sdmEncOffset;
    }

    /**
     * SDM이 활성화되어 있는지 확인
     */
    public boolean isSdmEnabled() {
        return baseUrl != null && !baseUrl.isEmpty() &&
               (enableUidMirroring || enableReadCounter || enableEncryption);
    }

    /**
     * 설정 검증
     */
    public boolean isValid() {
        if (baseUrl == null || baseUrl.isEmpty()) {
            return false;
        }
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            return false;
        }
        if (enableEncryption && (sdmFileReadKey == null || sdmFileReadKey.length != 16)) {
            return false;
        }
        if (picDataOffset < 0 || sdmMacOffset < 0 || sdmEncOffset < 0) {
            return false;
        }
        return true;
    }

    /**
     * SDM URL 템플릿 생성
     */
    public String buildUrlTemplate() {
        StringBuilder template = new StringBuilder(baseUrl);

        if (!baseUrl.contains("?")) {
            template.append("?");
        } else {
            template.append("&");
        }

        if (enableUidMirroring) {
            template.append("uid=00000000000000&");
        }
        if (enableReadCounter) {
            template.append("ctr=000000&");
        }
        if (enableEncryption) {
            template.append("enc=");
            template.append("0".repeat(32)); // Placeholder for encrypted data
            template.append("&");
        }

        template.append("cmac=");
        template.append("0".repeat(16)); // Placeholder for CMAC

        return template.toString();
    }

    // Builder
    public static class Builder {
        private String baseUrl;
        private byte[] sdmMetaReadKey;
        private byte[] sdmFileReadKey;
        private boolean enableUidMirroring = true;
        private boolean enableReadCounter = true;
        private boolean enableEncryption = false;
        private int picDataOffset;
        private int sdmMacOffset;
        private int sdmEncOffset;

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder sdmMetaReadKey(byte[] key) {
            this.sdmMetaReadKey = key != null ? Arrays.copyOf(key, key.length) : null;
            return this;
        }

        public Builder sdmFileReadKey(byte[] key) {
            this.sdmFileReadKey = key != null ? Arrays.copyOf(key, key.length) : null;
            return this;
        }

        public Builder enableUidMirroring(boolean enable) {
            this.enableUidMirroring = enable;
            return this;
        }

        public Builder enableReadCounter(boolean enable) {
            this.enableReadCounter = enable;
            return this;
        }

        public Builder enableEncryption(boolean enable) {
            this.enableEncryption = enable;
            return this;
        }

        public Builder picDataOffset(int offset) {
            this.picDataOffset = offset;
            return this;
        }

        public Builder sdmMacOffset(int offset) {
            this.sdmMacOffset = offset;
            return this;
        }

        public Builder sdmEncOffset(int offset) {
            this.sdmEncOffset = offset;
            return this;
        }

        /**
         * 오프셋 자동 계산
         */
        public Builder calculateOffsets() {
            if (baseUrl == null || baseUrl.isEmpty()) {
                throw new IllegalStateException("Base URL must be set before calculating offsets");
            }

            String template = baseUrl;
            if (!template.contains("?")) {
                template += "?";
            } else {
                template += "&";
            }

            int currentOffset = template.length();

            if (enableUidMirroring) {
                this.picDataOffset = currentOffset + 4; // "uid="
                currentOffset += 4 + 14 + 1; // "uid=" + 14 chars + "&"
            }

            if (enableReadCounter) {
                currentOffset += 4 + 6 + 1; // "ctr=" + 6 chars + "&"
            }

            if (enableEncryption) {
                this.sdmEncOffset = currentOffset + 4; // "enc="
                currentOffset += 4 + 32 + 1; // "enc=" + 32 chars + "&"
            }

            this.sdmMacOffset = currentOffset + 5; // "cmac="

            return this;
        }

        public SdmConfiguration build() {
            return new SdmConfiguration(this);
        }
    }

    @Override
    public String toString() {
        return String.format("SdmConfiguration{baseUrl='%s', uidMirroring=%s, readCounter=%s, encryption=%s}",
            baseUrl, enableUidMirroring, enableReadCounter, enableEncryption);
    }
}