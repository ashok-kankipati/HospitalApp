package com.hospital.app.config;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import org.springframework.web.server.ResponseStatusException;
import java.lang.reflect.Type;

/** Create endpoints must not turn attacker-supplied primary keys into JPA updates. */
@RestControllerAdvice
public class EntityCreateGuard extends RequestBodyAdviceAdapter {
    @Override public boolean supports(MethodParameter parameter, Type type, Class<? extends HttpMessageConverter<?>> converter) {
        return parameter.hasMethodAnnotation(PostMapping.class) && parameter.getParameterType().isAnnotationPresent(jakarta.persistence.Entity.class);
    }
    @Override public Object afterBodyRead(Object body, HttpInputMessage input, MethodParameter parameter, Type type, Class<? extends HttpMessageConverter<?>> converter) {
        for (var field : body.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(jakarta.persistence.Id.class)) {
                try {
                    field.setAccessible(true);
                    if (field.get(body) != null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Do not supply a record ID when creating a record.");
                } catch (IllegalAccessException exception) { throw new IllegalStateException("Cannot validate record identifier", exception); }
            }
        }
        return body;
    }
}
