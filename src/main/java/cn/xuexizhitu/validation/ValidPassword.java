package cn.xuexizhitu.validation;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy=PasswordValidator.class)
public @interface ValidPassword {
    String message() default "密码不能为空，且UTF-8长度不得超过72字节";
    int minLength() default 1;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
