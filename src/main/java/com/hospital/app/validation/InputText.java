package com.hospital.app.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = InputTextValidator.class)
public @interface InputText {
    String message() default "Enter a valid value.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String kind() default "text";
    int max() default 255;
    boolean required() default false;
}
