package langchain4j.multiagent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Parses model protocol defensively; malformed model output never reaches the UI. */
public final class MultiAgentProtocolParser {
    private MultiAgentProtocolParser() {
    }

    public static LeaderDecision leader(ObjectMapper mapper, String raw, WorkerType fallbackWorker, String fallbackEnvelope) {
        JsonNode node = read(mapper, raw);
        if (node != null && node.isObject()) {
            LeaderAction action = enumValue(LeaderAction.class, text(node, "action"), null);
            WorkerType worker = enumValue(WorkerType.class, text(node, "worker"), fallbackWorker);
            String envelope = node.path("taskEnvelope").isObject()
                    ? node.path("taskEnvelope").toString() : text(node, "taskEnvelope");
            if (action != null) {
                return switch (action) {
                    case CALL_WORKER -> LeaderDecision.call(worker, envelope.isBlank() ? fallbackEnvelope : envelope);
                    case ASK_USER -> LeaderDecision.ask(text(node, "answer"));
                    case FINAL -> LeaderDecision.finalAnswer(text(node, "answer"));
                };
            }
        }
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (value.equals("HOSPITAL") || value.equals("RAG") || value.equals("GENERAL")) {
            return LeaderDecision.call(WorkerType.valueOf(value), fallbackEnvelope);
        }
        // A malformed Leader response is an internal protocol failure, not a user answer.
        // Keep the workflow on the known route instead of exposing model commentary.
        return LeaderDecision.call(fallbackWorker == null ? WorkerType.GENERAL : fallbackWorker,
                fallbackEnvelope == null ? "{}" : fallbackEnvelope);
    }

    public static WorkerResult worker(ObjectMapper mapper, String raw, WorkerType expectedWorker) {
        JsonNode node = read(mapper, raw);
        if (node == null || !node.isObject()) {
            return WorkerResult.plain(expectedWorker, raw == null ? "" : raw.trim());
        }
        WorkerType worker = enumValue(WorkerType.class, text(node, "worker"), expectedWorker);
        WorkerStatus status = enumValue(WorkerStatus.class, text(node, "status"), WorkerStatus.ERROR);
        WorkerType suggested = enumValue(WorkerType.class, text(node, "suggestedWorker"), WorkerType.GENERAL);
        return new WorkerResult(worker, status, text(node, "answer"), strings(node.get("knownFacts")),
                strings(node.get("pendingInformation")), text(node, "failureReason"), suggested,
                strings(node.get("toolResults")));
    }

    private static JsonNode read(ObjectMapper mapper, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if (value.startsWith("```") && value.endsWith("```")) {
            int newline = value.indexOf('\n');
            value = newline >= 0 ? value.substring(newline + 1, value.length() - 3).trim() : value;
        }
        try {
            return mapper.readTree(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }

    private static List<String> strings(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        node.forEach(value -> result.add(value.asText("")));
        return result;
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, E fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
