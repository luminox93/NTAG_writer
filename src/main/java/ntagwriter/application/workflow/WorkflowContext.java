package ntagwriter.application.workflow;

import ntagwriter.domain.tag.Tag;
import ntagwriter.domain.config.SdmConfiguration;
import java.util.HashMap;
import java.util.Map;

/**
 * 워크플로우 실행 컨텍스트
 * 워크플로우 실행 중 필요한 모든 데이터를 보관
 */
public final class WorkflowContext {

    // Immutable fields
    public final WorkflowMode mode;
    public final SdmConfiguration sdmConfig;

    // Mutable state
    private Tag currentTag;
    private WorkflowState state;
    private final Map<String, Object> attributes;
    private String lastError;

    public WorkflowContext(WorkflowMode mode, SdmConfiguration sdmConfig) {
        this.mode = mode;
        this.sdmConfig = sdmConfig;
        this.state = WorkflowState.INITIALIZED;
        this.attributes = new HashMap<>();
    }

    // Tag management
    public void updateTag(Tag tag) {
        this.currentTag = tag;
    }

    public Tag currentTag() {
        return currentTag;
    }

    // State management
    public void transitionTo(WorkflowState newState) {
        this.state = newState;
    }

    public WorkflowState currentState() {
        return state;
    }

    // Attribute management
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, Class<T> type) {
        Object value = attributes.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

    // Error management
    public void recordError(String error) {
        this.lastError = error;
        this.state = WorkflowState.ERROR;
    }

    public String lastError() {
        return lastError;
    }

    public boolean hasError() {
        return lastError != null;
    }

    public void clearError() {
        this.lastError = null;
    }

    // Validation
    public boolean isValid() {
        return currentTag != null &&
               sdmConfig != null &&
               sdmConfig.isValid() &&
               state != WorkflowState.ERROR;
    }

    // Common attribute keys
    public static final String ATTR_SESSION_KEYS = "sessionKeys";
    public static final String ATTR_TRANSACTION_ID = "transactionId";
    public static final String ATTR_COMMAND_COUNTER = "commandCounter";
    public static final String ATTR_AUTH_KEY = "authKey";
    public static final String ATTR_RND_A = "rndA";
    public static final String ATTR_RND_B = "rndB";
}