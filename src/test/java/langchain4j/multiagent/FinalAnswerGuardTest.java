package langchain4j.multiagent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FinalAnswerGuardTest {

    private final FinalAnswerGuard guard = new FinalAnswerGuard();

    @Test
    void shouldAcceptDirectUserFacingAnswer() {
        assertThat(guard.isUserFacing("头痛通常可以先挂神经内科。请问您是否伴有发热？")).isTrue();
    }

    @Test
    void shouldRejectReviewLanguageAndCodeFence() {
        assertThat(guard.isUserFacing("用户询问了头痛挂什么科，这段回答很好地解答了用户的疑问。"))
                .isFalse();
        assertThat(guard.isUserFacing("```markdown\n请挂神经内科\n```"))
                .isFalse();
    }

    @Test
    void shouldFallBackToWorkerCandidate() {
        String answer = guard.select(
                "这段回答已经很好地解答了用户的疑问。",
                "头痛通常可以先挂神经内科。"
        );

        assertThat(answer).isEqualTo("头痛通常可以先挂神经内科。");
    }
}
