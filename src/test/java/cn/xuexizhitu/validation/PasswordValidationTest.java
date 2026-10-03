package cn.xuexizhitu.validation;

import cn.xuexizhitu.identity.api.RegisterRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
class PasswordValidationTest {
    @Test void bcryptLimitCountsUtf8Bytes() {
        try (var factory=Validation.buildDefaultValidatorFactory()) {
            var validator=factory.getValidator();
            assertThat(validator.validate(new RegisterRequest("owner","owner@example.com","汉".repeat(24)))).isEmpty();
            assertThat(validator.validate(new RegisterRequest("owner","owner@example.com","汉".repeat(25)))).isNotEmpty();
            assertThat(validator.validate(new RegisterRequest("owner","owner@example.com","短密码"))).isNotEmpty();
            assertThat(validator.validate(new RegisterRequest("has space","owner@example.com","12345678"))).isNotEmpty();
        }
    }
}
