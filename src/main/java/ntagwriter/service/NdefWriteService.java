package ntagwriter.service;

import ntagwriter.reader.ReaderException;
import ntagwriter.util.*;

import javax.smartcardio.ResponseAPDU;

/**
 * NDEF 메시지 작성 서비스
 * NTAG424 DNA에 NDEF 메시지를 작성하는 기능을 담당
 */
public class NdefWriteService {

    private static final int NDEF_FILE_NUMBER = 0x02;

    private final NfcReaderService readerService;

    public NdefWriteService(NfcReaderService readerService) {
        this.readerService = readerService;
    }

    /**
     * NDEF 메시지 작성
     */
    public void writeNdefMessage(String baseUrl) throws ReaderException {
        ConsoleHelper.printInfo("→ NDEF 메시지 작성 중...");

        // NDEF Record 생성
        byte[] ndefMessage = createNdefMessage(baseUrl);

        // Write Binary APDU 생성
        byte[] apdu = buildWriteBinaryApdu(ndefMessage);

        DebugLogger.logHex("NDEF Message", ndefMessage);
        DebugLogger.logHex("Write APDU", apdu);

        ResponseAPDU response = readerService.sendCommand(apdu);
        ApduResponseValidator.validateSuccess(response, "NDEF 작성");

        ConsoleHelper.printSuccess("✓ NDEF 메시지 작성 완료!");
        ConsoleHelper.printInfo("  작성된 URL: " + baseUrl);
    }

    /**
     * Write Binary APDU 생성
     */
    private byte[] buildWriteBinaryApdu(byte[] ndefMessage) {
        // 90 D2 00 02 [Lc] [Offset:3bytes] [Data]
        byte[] apdu = new byte[8 + ndefMessage.length];
        apdu[0] = (byte) 0x90; // CLA
        apdu[1] = (byte) 0xD2; // INS (WriteBinary)
        apdu[2] = 0x00; // P1
        apdu[3] = NDEF_FILE_NUMBER; // P2 (File number)
        apdu[4] = (byte) (3 + ndefMessage.length); // Lc
        apdu[5] = 0x00; // Offset (3 bytes, little endian)
        apdu[6] = 0x00;
        apdu[7] = 0x00;
        System.arraycopy(ndefMessage, 0, apdu, 8, ndefMessage.length);

        return apdu;
    }

    /**
     * NDEF Message 생성
     *
     * NDEF Structure:
     * - NDEF Message Length: 2 bytes
     * - NDEF Record:
     *   - Record Header: 1 byte (0xD1 = TNF Well-known, SR, ME, MB)
     *   - Type Length: 1 byte (0x01 for URI)
     *   - Payload Length: 1 byte
     *   - Type: 'U' (0x55 for URI)
     *   - Payload: URI identifier code + URI
     */
    private byte[] createNdefMessage(String url) {
        // URI identifier code: 0x04 = "https://"
        // Remove "https://" from URL if present
        String urlContent = url.startsWith("https://") ?
            url.substring(8) : url;

        // Payload = URI identifier(1 byte) + URL
        byte[] payload = new byte[1 + urlContent.length()];
        payload[0] = 0x04; // https:// prefix
        System.arraycopy(urlContent.getBytes(), 0, payload, 1, urlContent.length());

        // NDEF Record
        int recordLength = 1 + 1 + 1 + 1 + payload.length; // Header + TypeLen + PayloadLen + Type + Payload
        byte[] record = new byte[recordLength];
        record[0] = (byte) 0xD1; // TNF=1, SR=1, ME=1, MB=1
        record[1] = 0x01; // Type Length (U)
        record[2] = (byte) payload.length; // Payload Length
        record[3] = 0x55; // Type: 'U' for URI
        System.arraycopy(payload, 0, record, 4, payload.length);

        // Complete NDEF Message with length prefix
        byte[] ndefMessage = new byte[2 + record.length];
        ndefMessage[0] = 0x00; // Length MSB
        ndefMessage[1] = (byte) record.length; // Length LSB
        System.arraycopy(record, 0, ndefMessage, 2, record.length);

        return ndefMessage;
    }
}