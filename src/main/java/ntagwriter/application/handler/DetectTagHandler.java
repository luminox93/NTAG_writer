package ntagwriter.application.handler;

import ntagwriter.application.workflow.WorkflowContext;
import ntagwriter.application.workflow.WorkflowState;
import ntagwriter.domain.port.NfcReaderPort;
import ntagwriter.domain.port.UserInteractionPort;
import ntagwriter.domain.tag.Tag;

/**
 * 태그 감지 단계 핸들러
 */
public class DetectTagHandler implements StepHandler {

    private final NfcReaderPort readerPort;
    private final UserInteractionPort uiPort;
    private static final long DETECTION_TIMEOUT_MS = 30000; // 30초

    public DetectTagHandler(NfcReaderPort readerPort, UserInteractionPort uiPort) {
        this.readerPort = readerPort;
        this.uiPort = uiPort;
    }

    @Override
    public boolean canHandle(WorkflowState state) {
        return state == WorkflowState.CONNECTED || state == WorkflowState.DETECTING_TAG;
    }

    @Override
    public void execute(WorkflowContext context) throws StepExecutionException {
        uiPort.beginTask("태그 감지");
        uiPort.showInfo("NFC 태그를 리더기에 올려놓으세요...");

        try {
            // 태그 감지 대기
            Tag tag = readerPort.waitForTag(DETECTION_TIMEOUT_MS);

            if (tag == null) {
                throw new StepExecutionException("태그 감지 시간 초과");
            }

            // 태그 정보 표시
            uiPort.showSuccess("태그 감지됨!");
            uiPort.showInfo("UID: " + tag.uidAsHex());
            uiPort.showInfo("타입: " + tag.type.displayName);

            // NTAG424 확인
            if (!tag.isNtag424()) {
                throw new StepExecutionException(
                    "NTAG424 DNA 태그가 아닙니다. 감지된 태그: " + tag.type.displayName
                );
            }

            // 컨텍스트에 태그 저장
            context.updateTag(tag);
            uiPort.endTask("태그 감지", true);

        } catch (NfcReaderPort.ReaderException e) {
            uiPort.showError("태그 감지 오류: " + e.getMessage());
            uiPort.endTask("태그 감지", false);
            throw new StepExecutionException("태그 감지 실패", e);
        }
    }

    @Override
    public String name() {
        return "DetectTag";
    }

    @Override
    public WorkflowState nextState() {
        return WorkflowState.TAG_DETECTED;
    }
}