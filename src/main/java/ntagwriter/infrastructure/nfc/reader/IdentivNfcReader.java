package ntagwriter.infrastructure.nfc.reader;

import ntagwriter.domain.port.NfcReaderPort;
import ntagwriter.domain.tag.Tag;
import ntagwriter.util.HexUtils;

import javax.smartcardio.*;
import java.util.List;

/**
 * Identiv uTrust 3700 F NFC 리더기 구현
 */
public class IdentivNfcReader implements NfcReaderPort {

    private static final String IDENTIV_READER_NAME_PREFIX = "Identiv";
    private static final String UTRUST_READER_NAME_PREFIX = "uTrust";

    // APDU 응답 코드
    private static final int SW_SUCCESS = 0x9000;
    private static final int SW_COMMAND_NOT_ALLOWED = 0x6986;
    private static final int SW_WRONG_DATA = 0x6A80;
    private static final int SW_FILE_NOT_FOUND = 0x6A82;
    private static final int SW_INCORRECT_PARAMETERS = 0x6A86;

    // 태그 감지 설정
    private static final int TAG_DETECTION_RETRY_DELAY_MS = 500;
    private static final int TAG_DETECTION_MAX_RETRIES = 60;  // 30초 = 60 * 500ms

    // ATR (Answer To Reset) 관련
    private static final int ATR_MIN_LENGTH = 2;

    private CardTerminal terminal;
    private Card card;
    private CardChannel channel;

    @Override
    public boolean connect() {
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            List<CardTerminal> terminals = factory.terminals().list();

            if (terminals.isEmpty()) {
                return false;
            }

            // Identiv 리더기 찾기
            for (CardTerminal candidateTerminal : terminals) {
                String terminalName = candidateTerminal.getName();
                if (isIdentivReader(terminalName)) {
                    this.terminal = candidateTerminal;
                    return true;
                }
            }

            // Identiv 리더기를 찾지 못한 경우 첫 번째 리더기 사용
            this.terminal = terminals.get(0);
            return true;

        } catch (CardException exception) {
            throw new ReaderException("리더기 연결 실패", exception);
        }
    }

    private boolean isIdentivReader(String terminalName) {
        String upperCaseName = terminalName.toUpperCase();
        return upperCaseName.contains(IDENTIV_READER_NAME_PREFIX.toUpperCase()) ||
               upperCaseName.contains(UTRUST_READER_NAME_PREFIX.toUpperCase());
    }

    @Override
    public void disconnect() {
        if (card != null) {
            try {
                card.disconnect(false);
            } catch (CardException ignored) {
                // 연결 해제 실패 무시
            } finally {
                card = null;
                channel = null;
            }
        }
        terminal = null;
    }

    @Override
    public boolean isConnected() {
        return terminal != null;
    }

    @Override
    public Tag waitForTag(long timeoutMs) {
        if (terminal == null) {
            throw new ReaderException("리더기가 연결되지 않았습니다");
        }

        long startTimeMs = System.currentTimeMillis();
        long endTimeMs = startTimeMs + timeoutMs;

        while (System.currentTimeMillis() < endTimeMs) {
            try {
                if (terminal.isCardPresent()) {
                    return detectTag();
                }

                Thread.sleep(TAG_DETECTION_RETRY_DELAY_MS);

            } catch (CardException cardException) {
                throw new ReaderException("태그 감지 중 오류", cardException);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new ReaderException("태그 감지가 중단되었습니다");
            }
        }

        return null;  // 타임아웃
    }

    private Tag detectTag() throws CardException {
        // 카드 연결
        card = terminal.connect("*");
        channel = card.getBasicChannel();

        // ATR 읽기
        ATR atr = card.getATR();
        byte[] atrBytes = atr.getBytes();

        // UID 읽기 (간단한 구현 - 실제로는 GET_DATA 명령 사용)
        byte[] uid = readTagUid();

        Tag detectedTag = new Tag(uid);
        return detectedTag.withAtr(atrBytes);
    }

    private byte[] readTagUid() throws CardException {
        // GET DATA 명령으로 UID 읽기
        // CLA=0x00, INS=0xCA, P1=0x00, P2=0x00
        byte[] getUidCommand = new byte[] {
            (byte) 0x00,  // CLA
            (byte) 0xCA,  // INS (GET DATA)
            (byte) 0x00,  // P1
            (byte) 0x00,  // P2
            (byte) 0x00   // Le
        };

        ResponseAPDU response = channel.transmit(new CommandAPDU(getUidCommand));

        if (response.getSW() != SW_SUCCESS) {
            // UID를 읽을 수 없는 경우 기본값 반환
            return new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        }

        return response.getData();
    }

    @Override
    public boolean isTagPresent() {
        if (terminal == null) {
            return false;
        }

        try {
            return terminal.isCardPresent();
        } catch (CardException exception) {
            return false;
        }
    }

    @Override
    public byte[] sendCommand(byte[] command) throws ReaderException {
        if (channel == null) {
            throw new ReaderException("태그가 연결되지 않았습니다");
        }

        try {
            CommandAPDU commandApdu = new CommandAPDU(command);
            logCommand("TX", command);

            ResponseAPDU response = channel.transmit(commandApdu);
            byte[] responseBytes = response.getBytes();
            logCommand("RX", responseBytes);

            int statusWord = response.getSW();
            if (statusWord != SW_SUCCESS) {
                String errorMessage = formatApduError(statusWord);
                throw new ReaderException(errorMessage);
            }

            return response.getData();

        } catch (CardException cardException) {
            throw new ReaderException("APDU 전송 실패", cardException);
        }
    }

    private String formatApduError(int statusWord) {
        String hexStatus = String.format("%04X", statusWord);

        switch (statusWord) {
            case SW_COMMAND_NOT_ALLOWED:
                return "명령이 허용되지 않습니다 (SW=" + hexStatus + ")";
            case SW_WRONG_DATA:
                return "잘못된 데이터 (SW=" + hexStatus + ")";
            case SW_FILE_NOT_FOUND:
                return "파일을 찾을 수 없습니다 (SW=" + hexStatus + ")";
            case SW_INCORRECT_PARAMETERS:
                return "잘못된 파라미터 (SW=" + hexStatus + ")";
            default:
                return "APDU 오류 (SW=" + hexStatus + ")";
        }
    }

    private void logCommand(String direction, byte[] data) {
        // 디버그 모드에서만 로깅
        if (isDebugEnabled()) {
            System.out.printf("[%s] %s%n", direction, HexUtils.bytesToHex(data));
        }
    }

    private boolean isDebugEnabled() {
        String debugProperty = System.getProperty("ntagwriter.debug");
        return "true".equalsIgnoreCase(debugProperty);
    }

    @Override
    public String getReaderName() {
        if (terminal == null) {
            return "Not connected";
        }
        return terminal.getName();
    }
}