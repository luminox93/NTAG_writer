package ntagwriter;

import ntagwriter.reader.IdentivReader;
import ntagwriter.reader.ReaderException;
import ntagwriter.util.HexUtils;

public class NtagWriterApplication {
    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println(" NTAG Writer - NFC 리더기 테스트");
        System.out.println("=".repeat(60));
        System.out.println();

        IdentivReader reader = new IdentivReader();

        try {
            // 리더기 연결
            System.out.println("[1] 리더기 연결 시도 중...");
            reader.connect();
            System.out.println("    ✓ 연결 성공: " + reader.getReaderName());
            System.out.println();

            // 태그 UID 읽기
            System.out.println("[2] 태그 UID 읽기 중...");
            byte[] uid = reader.readUid();
            System.out.println("    ✓ UID: " + HexUtils.bytesToHex(uid));
            System.out.println();

            System.out.println("테스트 완료!");

        } catch (ReaderException e) {
            System.err.println();
            System.err.println("✗ 오류 발생: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("  원인: " + e.getCause().getMessage());
            }
        } finally {
            reader.disconnect();
        }
    }
}
