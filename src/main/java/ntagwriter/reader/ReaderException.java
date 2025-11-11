package ntagwriter.reader;

/**
 * NFC 리더기 관련 예외
 */
public class ReaderException extends Exception {

    public ReaderException(String message) {
        super(message);
    }

    public ReaderException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReaderException(Throwable cause) {
        super(cause);
    }
}
