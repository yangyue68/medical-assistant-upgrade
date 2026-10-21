package langchain4j.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AccountServiceTest {
    @Test
    void registersPasswordAsHashAndNormalizesUsername() {
        AccountUserMapper mapper = Mockito.mock(AccountUserMapper.class);
        when(mapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(mapper.insert(any(AccountUser.class))).thenAnswer(invocation -> {
            AccountUser user = invocation.getArgument(0);
            user.setId(7L);
            return 1;
        });
        AccountUser user = new AccountService(mapper).register("Test_User", "password123");
        assertThat(user.getId()).isEqualTo(7L);
        assertThat(user.getUsername()).isEqualTo("test_user");
        assertThat(user.getPasswordHash()).isNotEqualTo("password123");
        assertThat(user.getPasswordHash()).startsWith("$2");
    }

    @Test
    void rejectsWeakPasswords() {
        AccountUserMapper mapper = Mockito.mock(AccountUserMapper.class);
        assertThatThrownBy(() -> new AccountService(mapper).register("test_user", "short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8");
    }
}
