package ntagwriter.domain.tag;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * NFC 태그 도메인 엔티티
 * Immutable object - 상태 변경 시 새로운 인스턴스 생성
 */
public final class Tag {
    public final byte[] uid;
    public final NtagType type;
    public final LocalDateTime detectedAt;
    public final TagStatus status;
    public final byte[] atr;  // Answer To Reset
    public final int memorySize;
    public final boolean isFormatted;
    public final boolean isLocked;

    public Tag(byte[] uid) {
        this(uid, NtagType.identifyByUid(uid), TagStatus.DETECTED, null, 0, false, false);
    }

    public Tag(byte[] uid, NtagType type) {
        this(uid, type, TagStatus.DETECTED, null, 0, false, false);
    }

    private Tag(byte[] uid, NtagType type, TagStatus status, byte[] atr,
                int memorySize, boolean isFormatted, boolean isLocked) {
        if (uid == null || uid.length == 0) {
            throw new IllegalArgumentException("UID cannot be null or empty");
        }
        this.uid = Arrays.copyOf(uid, uid.length);
        this.type = type;
        this.detectedAt = LocalDateTime.now();
        this.status = status;
        this.atr = atr != null ? Arrays.copyOf(atr, atr.length) : null;
        this.memorySize = memorySize;
        this.isFormatted = isFormatted;
        this.isLocked = isLocked;
    }

    // 상태 변경 메서드들 - 새로운 인스턴스 반환
    public Tag withStatus(TagStatus newStatus) {
        return new Tag(uid, type, newStatus, atr, memorySize, isFormatted, isLocked);
    }

    public Tag withAtr(byte[] newAtr) {
        return new Tag(uid, type, status, newAtr, memorySize, isFormatted, isLocked);
    }

    public Tag withMemorySize(int newMemorySize) {
        return new Tag(uid, type, status, atr, newMemorySize, isFormatted, isLocked);
    }

    public Tag markAsFormatted() {
        return new Tag(uid, type, status, atr, memorySize, true, isLocked);
    }

    public Tag markAsLocked() {
        return new Tag(uid, type, status, atr, memorySize, isFormatted, true);
    }

    // Business methods
    public String uidAsHex() {
        StringBuilder sb = new StringBuilder();
        for (byte b : uid) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    public boolean isNtag424() {
        return type == NtagType.NTAG_424_DNA || type == NtagType.NTAG_424_DNA_TT;
    }

    public boolean supportsSdm() {
        return type.supportsSdm();
    }

    public boolean canProceed() {
        return status.canProceed();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tag tag = (Tag) o;
        return Arrays.equals(uid, tag.uid);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(uid);
    }

    @Override
    public String toString() {
        return String.format("Tag{uid=%s, type=%s, status=%s}",
            uidAsHex(), type.displayName, status.description);
    }
}