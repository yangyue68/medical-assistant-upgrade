package langchain4j.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AccountContextTest {
    @Test
    void restoresPreviousContextAfterScopedAction() {
        AccountContext.set(1L);
        Long result = AccountContext.withUser(2L, AccountContext::currentUserId);
        assertThat(result).isEqualTo(2L);
        assertThat(AccountContext.currentUserId()).isEqualTo(1L);
        AccountContext.clear();
    }
}
