package langchain4j.multiagent;

public record LeaderDecision(
        LeaderAction action,
        WorkerType worker,
        String taskEnvelope,
        String answer,
        String reason
) {
    public static LeaderDecision call(WorkerType worker, String envelope) {
        return new LeaderDecision(LeaderAction.CALL_WORKER, worker, envelope, "", "");
    }

    public static LeaderDecision ask(String answer) {
        return new LeaderDecision(LeaderAction.ASK_USER, WorkerType.GENERAL, "", answer, "");
    }

    public static LeaderDecision finalAnswer(String answer) {
        return new LeaderDecision(LeaderAction.FINAL, WorkerType.GENERAL, "", answer, "");
    }
}
