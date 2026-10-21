package langchain4j.multiagent;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class FinalAnswerGuard {

    private static final List<String> INTERNAL_MARKERS = List.of(
            "用户询问了",
            "用户的问题",
            "针对该问题的回复",
            "以下是针对",
            "这段回答",
            "该回答",
            "回答已经很好",
            "很好地解答",
            "如果用户",
            "他们可以根据提示",
            "leader",
            "worker",
            "routing",
            "route decision",
            "quality control",
            "approvedworkeranswer",
            "mainconversationhistory",
            "internal json",
            "repairadvice",
            "思维链",
            "质检结果",
            "路由结果"
    );

    public String select(String presentedAnswer, String workerAnswer) {
        if (isUserFacing(presentedAnswer)) {
            return presentedAnswer.trim();
        }
        if (isUserFacing(workerAnswer)) {
            return workerAnswer.trim();
        }
        return "抱歉，我暂时无法生成可靠的答复，请换一种说法后再试。";
    }

    boolean isUserFacing(String answer) {
        if (!StringUtils.hasText(answer)) {
            return false;
        }
        String normalized = answer.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("{") || normalized.startsWith("[")) {
            return false;
        }
        if (normalized.contains("```")) {
            return false;
        }
        return INTERNAL_MARKERS.stream().noneMatch(normalized::contains);
    }
}
