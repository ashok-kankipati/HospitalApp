package com.hospital.app.validation;

import jakarta.validation.*;
import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EntityReference.Validator.class)
public @interface EntityReference {
    String message() default "Select a valid record.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    class Validator implements ConstraintValidator<EntityReference, Object> {
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            if (value == null) return false;
            try { Object id = value.getClass().getMethod("getId").invoke(value); return id instanceof Number && ((Number) id).longValue() > 0; }
            catch (ReflectiveOperationException ex) { return false; }
        }
    }
}
