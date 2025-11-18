package ntagwriter.reader;

import ntagwriter.util.ConsoleHelper;
import ntagwriter.util.HexUtils;

import javax.smartcardio.*;
import java.util.List;

/**
 * identiv uTrust 3700 F CL Reader 구현
 * PC/SC (javax.smartcardio) API를 사용한 identiv 리더기 제어
 * NTAG424 DNA 완벽 지원
 */
public class IdentivReader implements NfcReaderStrategy {

    private static final String READER_NAME_PATTERN = "uTrust 3700";

    private CardTerminal terminal;
    private Card card;
    private CardChannel channel;

    @Override
    public void connect() throws ReaderException {
        try {
            // TerminalFactory 가져오기
            TerminalFactory factory = TerminalFactory.getDefault();
            List<CardTerminal> terminals = factory.terminals().list();

            if (terminals.isEmpty()) {
                throw new ReaderException("NFC 리더기를 찾을 수 없습니다.");
            }

            // identiv uTrust 3700 F 리더기 찾기
            terminal = null;
            for (CardTerminal t : terminals) {
                if (t.getName().contains(READER_NAME_PATTERN)) {
                    terminal = t;
                    break;
                }
            }

            // identiv를 못 찾으면 첫 번째 리더기 사용
            if (terminal == null) {
                terminal = terminals.get(0);
                ConsoleHelper.printWarning(
                        "identiv uTrust 3700 F를 찾을 수 없어 '" +
                        terminal.getName() + "' 리더기를 사용합니다.");
            }

            // 태그가 올라올 때까지 대기
            if (!terminal.isCardPresent()) {
                ConsoleHelper.printInfo("태그를 리더기에 올려주세요...");
                terminal.waitForCardPresent(0); // 무한 대기
            }

            // 카드 연결
            card = terminal.connect("*");
            channel = card.getBasicChannel();

        } catch (CardException e) {
            throw new ReaderException("리더기 연결 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] readUid() throws ReaderException {
        try {
            // GET DATA 명령으로 UID 읽기
            byte[] getUidCommand = {(byte) 0xFF, (byte) 0xCA, 0x00, 0x00, 0x00};
            ResponseAPDU response = channel.transmit(new CommandAPDU(getUidCommand));

            if (response.getSW() != 0x9000) {
                throw new ReaderException("UID 읽기 실패: " + String.format("%04X", response.getSW()));
            }

            return response.getData();

        } catch (CardException e) {
            throw new ReaderException("UID 읽기 중 오류 발생: " + e.getMessage(), e);
        }
    }

    @Override
    public ResponseAPDU sendCommand(byte[] apdu) throws ReaderException {
        if (channel == null) {
            throw new ReaderException("리더기가 연결되지 않았습니다.");
        }

        try {
            CommandAPDU command = new CommandAPDU(apdu);
            ResponseAPDU response = channel.transmit(command);

            // 디버그 로그
            System.out.println(">>> " + HexUtils.bytesToHexSpaced(apdu));
            System.out.println("<<< " + HexUtils.bytesToHexSpaced(response.getBytes()));

            return response;

        } catch (CardException e) {
            throw new ReaderException("명령 전송 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public void disconnect() {
        try {
            if (card != null) {
                card.disconnect(false);
                card = null;
            }
            channel = null;
            terminal = null;
        } catch (CardException e) {
            System.err.println("리더기 연결 해제 중 오류: " + e.getMessage());
        }
    }

    @Override
    public boolean isConnected() {
        return card != null && channel != null;
    }

    @Override
    public String getReaderName() {
        return terminal != null ? terminal.getName() : "Unknown";
    }

    /**
     * 태그가 리더기에 올라와 있는지 확인
     *
     * @return 태그 존재 여부
     */
    public boolean isCardPresent() {
        try {
            return terminal != null && terminal.isCardPresent();
        } catch (CardException e) {
            return false;
        }
    }

    /**
     * 태그가 제거될 때까지 대기
     *
     * @param timeout 타임아웃 (밀리초, 0이면 무한 대기)
     * @return 태그 제거 여부
     * @throws ReaderException 대기 중 오류 발생 시
     */
    public boolean waitForCardAbsent(long timeout) throws ReaderException {
        try {
            return terminal.waitForCardAbsent(timeout);
        } catch (CardException e) {
            throw new ReaderException("태그 제거 대기 중 오류: " + e.getMessage(), e);
        }
    }
}
