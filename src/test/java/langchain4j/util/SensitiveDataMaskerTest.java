package langchain4j.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataMaskerTest {

    @Test
    void shouldMaskPhoneAndIdCardInText() {
        String text = "患者张三，身份证11010119900307123X，手机号13812345678";

        String masked = SensitiveDataMasker.mask(text);

        assertThat(masked)
                .contains("110101********123X")
                .contains("138****5678")
                .doesNotContain("11010119900307123X")
                .doesNotContain("13812345678");
    }

    @Test
    void shouldMaskApiKeys() {
        String text = "api-key=abc123 token:secret-value password=root";

        String masked = SensitiveDataMasker.mask(text);

        assertThat(masked)
                .contains("api-key=******")
                .contains("token:******")
                .contains("password=******")
                .doesNotContain("abc123")
                .doesNotContain("secret-value")
                .doesNotContain("root");
    }
}
