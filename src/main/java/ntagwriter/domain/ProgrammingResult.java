package ntagwriter.domain;

import java.time.LocalDateTime;

/**
 * 태그 프로그래밍 결과를 담는 도메인 모델
 */
public class ProgrammingResult {
    private boolean success;
    private String uid;
    private String message;
    private LocalDateTime timestamp;
    private Exception exception;

    public ProgrammingResult(boolean success, String uid, String message) {
        this.success = success;
        this.uid = uid;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public static ProgrammingResult success(String uid) {
        return new ProgrammingResult(true, uid, "프로그래밍 성공");
    }

    public static ProgrammingResult success(String uid, String message) {
        return new ProgrammingResult(true, uid, message);
    }

    public static ProgrammingResult failure(String uid, String message) {
        return new ProgrammingResult(false, uid, message);
    }

    public static ProgrammingResult failure(String uid, Exception exception) {
        ProgrammingResult result = new ProgrammingResult(false, uid, exception.getMessage());
        result.exception = exception;
        return result;
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public String getUid() {
        return uid;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public String toString() {
        return "ProgrammingResult{" +
                "success=" + success +
                ", uid='" + uid + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
