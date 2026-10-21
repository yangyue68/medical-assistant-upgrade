package langchain4j.multiagent;

import langchain4j.assistant.GeneralWorkerAgent;
import langchain4j.assistant.HospitalWorkerAgent;
import langchain4j.assistant.LeaderAgent;
import langchain4j.assistant.RagWorkerAgent;
import langchain4j.assistant.XiaoZhiAgent;
import langchain4j.auth.AccountContext;
import langchain4j.memory.ChatMemoryPolicy;
import langchain4j.memory.ConversationMessageResponse;
import langchain4j.store.MongoChatMemoryStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/** Supervisor workflow: deterministic route, Worker execution, Leader decision, bounded handoff. */
@Service
public class MultiAgentOrchestrator implements XiaoZhiAgent {
    private final LeaderAgent leader;
    private final HospitalWorkerAgent hospitalWorker;
    private final RagWorkerAgent ragWorker;
    private final GeneralWorkerAgent generalWorker;
    private final MongoChatMemoryStore memoryStore;
    private final ChatMemoryPolicy memoryPolicy;
    private final ObjectMapper objectMapper;
    private final FinalAnswerGuard finalAnswerGuard;
    private final ConcurrentMap<Long, Object> conversationLocks = new ConcurrentHashMap<>();

    @Autowired
    public MultiAgentOrchestrator(LeaderAgent leader, HospitalWorkerAgent hospitalWorker,
                                  RagWorkerAgent ragWorker, GeneralWorkerAgent generalWorker,
                                  MongoChatMemoryStore memoryStore, ChatMemoryPolicy memoryPolicy,
                                  ObjectMapper objectMapper, FinalAnswerGuard finalAnswerGuard) {
        this.leader = leader;
        this.hospitalWorker = hospitalWorker;
        this.ragWorker = ragWorker;
        this.generalWorker = generalWorker;
        this.memoryStore = memoryStore;
        this.memoryPolicy = memoryPolicy;
        this.objectMapper = objectMapper;
        this.finalAnswerGuard = finalAnswerGuard;
    }

    public MultiAgentOrchestrator(LeaderAgent leader, HospitalWorkerAgent hospitalWorker,
                                  RagWorkerAgent ragWorker, GeneralWorkerAgent generalWorker,
                                  MongoChatMemoryStore memoryStore, ChatMemoryPolicy memoryPolicy) {
        this(leader, hospitalWorker, ragWorker, generalWorker, memoryStore, memoryPolicy,
                new ObjectMapper(), new FinalAnswerGuard());
    }

    @Override
    public Flux<String> chat(Long memoryId, String userMessage) {
        return Flux.defer(() -> Flux.just(process(memoryId, userMessage)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<String> chat(Long userId, Long memoryId, String userMessage) {
        return Flux.defer(() -> Flux.just(AccountContext.withRequest(userId, userMessage,
                        () -> process(userId, memoryId, userMessage))))
                .subscribeOn(Schedulers.boundedElastic());
    }

    String process(Long memoryId, String userMessage) {
        if (memoryId == null || !StringUtils.hasText(userMessage)) {
            throw new IllegalArgumentException("memoryId and userMessage must not be empty");
        }
        synchronized (conversationLocks.computeIfAbsent(memoryId, ignored -> new Object())) {
            List<ChatMessage> history = sanitizedMainHistory(memoryId);
            String answer = runWorkflow(memoryId, userMessage, history);
            appendFinalExchange(memoryId, history, userMessage, answer);
            return answer;
        }
    }

    private String process(Long userId, Long memoryId, String userMessage) {
        if (userId == null) return process(memoryId, userMessage);
        if (memoryStore == null) throw new IllegalStateException("memoryStore must not be null");
        return process(memoryId, userMessage);
    }

    private String runWorkflow(Long memoryId, String userMessage, List<ChatMessage> history) {
        String mainHistory = formatHistory(history);
        WorkerType deterministic = deterministicRoute(history, userMessage);
        String initialEnvelope = taskEnvelope(memoryId, deterministic == null ? WorkerType.GENERAL : deterministic,
                userMessage, "NONE", "", "", "NEW");
        LeaderDecision decision = deterministic == null
                ? parseLeader(leader.handle(userMessage, mainHistory, ""), WorkerType.GENERAL, initialEnvelope)
                : LeaderDecision.call(deterministic, initialEnvelope);
        Map<WorkerType, Integer> calls = new EnumMap<>(WorkerType.class);
        String lastAnswer = "";

        for (int total = 0; total < 6; total++) {
            if (decision.action() == LeaderAction.FINAL || decision.action() == LeaderAction.ASK_USER) {
                return publicAnswer(userMessage, finalAnswerGuard.select(decision.answer(), lastAnswer));
            }
            WorkerType worker = decision.worker() == null ? WorkerType.GENERAL : decision.worker();
            if (calls.getOrDefault(worker, 0) >= 2) {
                return publicAnswer(userMessage, finalAnswerGuard.select(lastAnswer, ""));
            }
            calls.merge(worker, 1, Integer::sum);
            String envelope = decision.taskEnvelope().isBlank()
                    ? taskEnvelope(memoryId, worker, userMessage, "NONE", "", "", "NEW")
                    : decision.taskEnvelope();
            String rawWorker = callWorker(worker, memoryId, userMessage, envelope);
            WorkerResult result = MultiAgentProtocolParser.worker(objectMapper, rawWorker, worker);
            lastAnswer = result.answer();
            // Every Worker result goes back through Leader. This is required for
            // multi-command messages: Leader must see completed work and decide
            // whether another Worker still has an outstanding subtask.
            LeaderDecision next = parseLeader(
                    leader.handle(userMessage, mainHistory, writeJson(result)),
                    worker,
                    taskEnvelope(memoryId, result.suggestedWorker(), userMessage, worker.name(),
                            result.failureReason(), result.answer(), "HANDOFF"));
            if (next.action() == LeaderAction.CALL_WORKER && next.worker() == worker
                    && result.status() == WorkerStatus.SUCCESS) {
                next = LeaderDecision.finalAnswer(result.answer());
            }
            decision = next;
        }
        return publicAnswer(userMessage, finalAnswerGuard.select(lastAnswer, ""));
    }

    WorkerType deterministicRoute(List<ChatMessage> history, String userMessage) {
        String current = normalize(userMessage);
        if (containsAny(current, "怎么出发去", "出发去", "怎么到", "如何到达", "怎么到达",
                "路线规划", "规划路线", "公交路线", "地铁路线", "公交怎么走", "地铁怎么走",
                "打车怎么走", "开车怎么走", "步行怎么走")) return WorkerType.GENERAL;
        if (containsAny(current, "最新", "今年", "本年度", "获奖", "获得了", "谁获得", "实时信息")) return WorkerType.GENERAL;
        if (containsAny(current, "医院地址", "怎么去医院", "去医院怎么走", "路线", "交通", "导航")) return WorkerType.GENERAL;
        if (containsAny(current, "排班", "号源", "挂号", "预约", "取消挂号", "取消预约", "预约查询", "可预约", "有号", "号吗", "出诊", "坐诊", "门诊安排", "挂什么科", "挂哪个科", "分诊", "就诊流程", "退号", "挂一个", "约一个")
                || (containsAny(current, "医生", "专家") && containsAny(current, "找", "哪天", "什么时候", "有号", "坐诊", "号"))
                || (containsAny(current, "找", "挂") && current.length() <= 18)) return WorkerType.HOSPITAL;
        if (containsAny(current, "天气", "新闻", "股价", "汇率", "实时", "搜索一下", "联网查", "你好", "您好", "谢谢", "再见")) return WorkerType.GENERAL;
        if (containsAny(current, "介绍医院", "介绍你们医院", "医院介绍", "医院概况", "介绍科室", "科室介绍", "医生团队", "医疗团队", "特色科室", "院内服务", "糖尿病症状", "糖尿病表现", "健康科普", "医院历史", "医院环境", "教学", "科研", "重点专科", "重点实验室", "专业组", "特色门诊", "科室简介", "专长", "科普")) return WorkerType.RAG;
        if (containsAny(current, "医院", "科室", "糖尿病", "神经内科", "内分泌科", "消化科", "心内科", "呼吸科", "眼科", "儿科", "妇产科")
                && containsAny(current, "介绍", "了解", "历史", "特色", "优势", "情况", "怎么样", "有哪些", "表现", "症状", "专长", "服务", "科研", "教学", "团队", "概况", "科普", "简介", "几个", "主要看")) return WorkerType.RAG;
        String historyText = formatHistory(history);
        if (isContinuation(current) && containsAny(historyText, "排班", "号源", "挂号", "预约", "医生", "就诊")) return WorkerType.HOSPITAL;
        if (isContinuation(current) && containsAny(historyText, "医院", "科室", "医生团队", "科普", "糖尿病", "介绍")) return WorkerType.RAG;
        if (containsAny(current, "继续说说这个", "这个科室怎么样", "还有哪些") && containsAny(historyText, "医院", "科室", "神经内科", "介绍")) return WorkerType.RAG;
        return null;
    }

    private LeaderDecision parseLeader(String raw, WorkerType fallbackWorker, String fallbackEnvelope) {
        return MultiAgentProtocolParser.leader(objectMapper, raw, fallbackWorker, fallbackEnvelope);
    }

    private String callWorker(WorkerType worker, Long memoryId, String currentMessage, String envelope) {
        return switch (worker) {
            case HOSPITAL -> hospitalWorker.handle(worker.memoryId(memoryId), currentMessage, envelope);
            case RAG -> ragWorker.handle(worker.memoryId(memoryId), currentMessage, envelope);
            case GENERAL -> generalWorker.handle(worker.memoryId(memoryId), currentMessage, envelope);
        };
    }

    private String taskEnvelope(Long memoryId, WorkerType worker, String request, String previousWorker,
                                String failureReason, String previousResult, String status) {
        try {
            Map<String, Object> value = new java.util.LinkedHashMap<>();
            value.put("conversationId", String.valueOf(memoryId));
            value.put("previousWorker", previousWorker);
            value.put("currentWorker", worker.name());
            value.put("userRequest", request);
            value.put("task", request);
            value.put("knownFacts", List.of());
            value.put("completedActions", List.of());
            value.put("previousResult", previousResult == null ? "" : previousResult);
            value.put("failureReason", failureReason == null ? "" : failureReason);
            value.put("constraints", List.of("只能使用当前Worker已声明的能力", "不得编造结果"));
            value.put("expectedResponse", "严格按Worker结果JSON返回");
            value.put("status", status);
            return objectMapper.writeValueAsString(value);
        } catch (Exception ignored) { return "{}"; }
    }

    private String writeJson(WorkerResult result) {
        try { return objectMapper.writeValueAsString(result); } catch (Exception ignored) { return "{}"; }
    }

    private String publicAnswer(String request, String answer) {
        // Final defense against a worker claiming success without current-turn identity data.
        if (looksLikeBookingSuccess(answer) && !containsPatientIdentity(request)) {
            return "预约前请先提供患者姓名、18位身份证号和11位手机号。为保护您的信息，我不会使用历史会话中的个人资料代替本轮输入。";
        }
        if (containsAny(normalize(request), "你是谁", "你的身份", "你叫什么", "你是什么模型", "你是哪个模型")) {
            return "我是医疗小智，智能医疗助手，可以为您提供医院业务办理、医疗知识科普和就医流程帮助。";
        }
        return StringUtils.hasText(answer) ? answer : "抱歉，我暂时无法生成可靠的答复，请稍后再试。";
    }

    private boolean looksLikeBookingSuccess(String answer) {
        if (!StringUtils.hasText(answer)) return false;
        String value = answer.toLowerCase();
        return value.contains("预约成功") || value.contains("预约已成功")
                || value.contains("棰勭害鎴愬姛") || value.contains("棰勭害宸插畬鎴");
    }

    private boolean containsPatientIdentity(String request) {
        if (!StringUtils.hasText(request)) return false;
        return request.matches("(?s).*\\d{17}[\\dXx].*")
                && request.matches("(?s).*\\d{11}.*");
    }

    private List<ChatMessage> sanitizedMainHistory(Long memoryId) {
        List<ChatMessage> result = new ArrayList<>();
        for (ChatMessage message : memoryStore.getMessages(memoryId)) {
            if (!ConversationMessageResponse.isDisplayable(message)) continue;
            ConversationMessageResponse visible = ConversationMessageResponse.from(message, result.size());
            if (message instanceof UserMessage && StringUtils.hasText(visible.getContent())) result.add(UserMessage.from(visible.getContent()));
            else if (message instanceof AiMessage ai && !ai.hasToolExecutionRequests() && StringUtils.hasText(visible.getContent())) result.add(AiMessage.from(visible.getContent()));
        }
        return keepLast(result, memoryPolicy.maxWindowMessages());
    }

    private void appendFinalExchange(Long memoryId, List<ChatMessage> history, String request, String answer) {
        List<ChatMessage> updated = new ArrayList<>(history);
        updated.add(UserMessage.from(request));
        updated.add(AiMessage.from(answer));
        memoryStore.updateMessages(memoryId, keepLast(updated, memoryPolicy.maxWindowMessages()));
    }

    private String formatHistory(List<ChatMessage> messages) {
        StringBuilder result = new StringBuilder();
        for (ChatMessage message : keepLast(messages, 12)) {
            if (message instanceof UserMessage user) result.append("用户：").append(user.singleText()).append('\n');
            else if (message instanceof AiMessage ai) result.append("助手：").append(ai.text()).append('\n');
        }
        return result.toString().trim();
    }

    private boolean isContinuation(String text) { return containsAny(text, "继续", "还是", "那个", "这个", "上面", "刚才", "详细说说", "还有哪些", "还有", "再介绍", "那", "呢", "吧"); }
    private String normalize(String text) { return text == null ? "" : text.replaceAll("\\s+", "").toLowerCase(); }
    private boolean containsAny(String text, String... values) { for (String value : values) if (text.contains(value)) return true; return false; }
    private <T> List<T> keepLast(List<T> values, int maxSize) { return values.size() <= maxSize ? new ArrayList<>(values) : new ArrayList<>(values.subList(values.size() - maxSize, values.size())); }
}
