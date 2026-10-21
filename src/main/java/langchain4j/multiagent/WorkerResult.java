package langchain4j.multiagent;

import java.util.List;

public record WorkerResult(
        WorkerType worker,
        WorkerStatus status,
        String answer,
        List<String> knownFacts,
        List<String> pendingInformation,
        String failureReason,
        WorkerType suggestedWorker,
        List<String> toolResults
) {
    public WorkerResult {
        worker = worker == null ? WorkerType.GENERAL : worker;
        status = status == null ? WorkerStatus.ERROR : status;
        answer = answer == null ? "" : answer;
        knownFacts = knownFacts == null ? List.of() : List.copyOf(knownFacts);
        pendingInformation = pendingInformation == null ? List.of() : List.copyOf(pendingInformation);
        failureReason = failureReason == null ? "" : failureReason;
        suggestedWorker = suggestedWorker == null ? WorkerType.GENERAL : suggestedWorker;
        toolResults = toolResults == null ? List.of() : List.copyOf(toolResults);
    }

    public static WorkerResult plain(WorkerType worker, String answer) {
        return new WorkerResult(worker, WorkerStatus.SUCCESS, answer, List.of(), List.of(), "", WorkerType.GENERAL, List.of());
    }
}
