package ntagwriter.domain;

import java.time.LocalDateTime;

/**
 * NTAG424 DNA 태그 정보를 담는 도메인 모델
 */
public class Tag {
    private String uid;
    private TagStatus status;
    private LocalDateTime programmingTime;
    private String errorMessage;
    private SdmConfig sdmConfig;

    public Tag() {
        this.status = TagStatus.PENDING;
    }

    public Tag(String uid) {
        this.uid = uid;
        this.status = TagStatus.PENDING;
        this.programmingTime = LocalDateTime.now();
    }

    // Getters and Setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public TagStatus getStatus() {
        return status;
    }

    public void setStatus(TagStatus status) {
        this.status = status;
    }

    public LocalDateTime getProgrammingTime() {
        return programmingTime;
    }

    public void setProgrammingTime(LocalDateTime programmingTime) {
        this.programmingTime = programmingTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public SdmConfig getSdmConfig() {
        return sdmConfig;
    }

    public void setSdmConfig(SdmConfig sdmConfig) {
        this.sdmConfig = sdmConfig;
    }

    @Override
    public String toString() {
        return "Tag{" +
                "uid='" + uid + '\'' +
                ", status=" + status +
                ", programmingTime=" + programmingTime +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
