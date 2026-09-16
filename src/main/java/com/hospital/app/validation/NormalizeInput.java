package com.hospital.app.validation;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import java.lang.reflect.*;

/** Normalize surrounding whitespace before Bean Validation; never alter passwords. */
@ControllerAdvice
public class NormalizeInput extends RequestBodyAdviceAdapter {
    public boolean supports(MethodParameter parameter, Type type, Class<? extends HttpMessageConverter<?>> converter) { return true; }
    public Object afterBodyRead(Object body, HttpInputMessage input, MethodParameter parameter, Type type, Class<? extends HttpMessageConverter<?>> converter) {
        normalize(body); return body;
    }
    private void normalize(Object body) {
        if (body == null || !body.getClass().getPackageName().startsWith("com.hospital.app") || body.getClass().isRecord()) return;
        var wrapper = new org.springframework.beans.BeanWrapperImpl(body);
        for (var property : wrapper.getPropertyDescriptors()) {
            String name = property.getName();
            if (!wrapper.isReadableProperty(name) || !wrapper.isWritableProperty(name) || name.toLowerCase().contains("password")) continue;
            Object value = wrapper.getPropertyValue(name);
            if (value instanceof String text) wrapper.setPropertyValue(name, text.strip());
            else if (value instanceof java.util.List<?> list) list.forEach(this::normalize);
        }
    }
}
