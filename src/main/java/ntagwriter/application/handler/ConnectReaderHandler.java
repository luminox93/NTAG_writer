package ntagwriter.application.handler;

import ntagwriter.application.workflow.WorkflowContext;
import ntagwriter.application.workflow.WorkflowState;
import ntagwriter.domain.port.NfcReaderPort;
import ntagwriter.domain.port.UserInteractionPort;

/**
 * 리더기 연결 단계 핸들러
 */
public class ConnectReaderHandler implements StepHandler {

    private final NfcReaderPort readerPort;
    private final UserInteractionPort uiPort;

    public ConnectReaderHandler(NfcReaderPort readerPort, UserInteractionPort uiPort) {
        this.readerPort = readerPort;
        this.uiPort = uiPort;
    }

    @Override
    public boolean canHandle(WorkflowState state) {
        return state == WorkflowState.INITIALIZED || state == WorkflowState.CONNECTED;
    }

    @Override
    public void execute(WorkflowContext context) throws StepExecutionException {
        uiPort.beginTask("리더기 연결");

        try {
            // 이미 연결되어 있는지 확인
            if (readerPort.isConnected()) {
                uiPort.showInfo("리더기가 이미 연결되어 있습니다: " + readerPort.getReaderName());
                return;
            }

            // 리더기 연결 시도
            uiPort.showInfo("NFC 리더기에 연결 중...");
            boolean connected = readerPort.connect();

            if (!connected) {
                throw new StepExecutionException("리더기 연결에 실패했습니다");
            }

            uiPort.showSuccess("리더기 연결 성공: " + readerPort.getReaderName());
            uiPort.endTask("리더기 연결", true);

        } catch (NfcReaderPort.ReaderException e) {
            uiPort.showError("리더기 연결 오류: " + e.getMessage());
            uiPort.endTask("리더기 연결", false);
            throw new StepExecutionException("리더기 연결 실패", e);
        }
    }

    @Override
    public String name() {
        return "ConnectReader";
    }

    @Override
    public WorkflowState nextState() {
        return WorkflowState.CONNECTED;
    }
}