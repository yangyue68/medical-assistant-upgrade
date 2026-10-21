package langchain4j.multiagent;

public enum WorkerType {
    HOSPITAL("hospital"),
    RAG("rag"),
    GENERAL("general");

    private final String memorySuffix;

    WorkerType(String memorySuffix) {
        this.memorySuffix = memorySuffix;
    }

    public String memoryId(Long conversationId) {
        return "conversation:" + conversationId + ":worker:" + memorySuffix;
    }

    public static WorkerType from(String value) {
        if (value == null) {
            return GENERAL;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return GENERAL;
        }
    }
}
