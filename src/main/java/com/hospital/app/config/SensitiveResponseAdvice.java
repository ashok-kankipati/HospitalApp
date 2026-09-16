package com.hospital.app.config;

import com.hospital.app.controller.LoginController;
import com.hospital.app.dto.LoginResponse;
import com.hospital.app.model.Patient;
import com.hospital.app.model.Staff;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.*;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import java.util.*;

@RestControllerAdvice
public class SensitiveResponseAdvice implements ResponseBodyAdvice<Object> {
    @Override public boolean supports(MethodParameter method, Class<? extends HttpMessageConverter<?>> converter) { return true; }
    @Override public Object beforeBodyWrite(Object body, MethodParameter method, MediaType type, Class<? extends HttpMessageConverter<?>> converter, ServerHttpRequest request, ServerHttpResponse response) {
        if (!(request instanceof ServletServerHttpRequest servlet)) return body;
        var session = servlet.getServletRequest().getSession(false);
        if (session == null || !(session.getAttribute(LoginController.AUTHENTICATED_USER) instanceof LoginResponse login)) return body;
        return redact(body, login.getRole());
    }
    private Object redact(Object body, String role) {
        if (body instanceof List<?> list) return list.stream().map(item -> redact(item, role)).toList();
        if (body instanceof Patient patient && !Set.of("Admin", "Doctor", "Surgeon", "Nurse").contains(role)) {
            Map<String, Object> safe = new LinkedHashMap<>();
            safe.put("id", patient.getId()); safe.put("name", patient.getName()); safe.put("email", patient.getEmail());
            safe.put("phone", patient.getPhone()); safe.put("dateOfBirth", patient.getDateOfBirth());
            safe.put("address", patient.getAddress()); safe.put("isActive", patient.getIsActive());
            return safe;
        }
        if (body instanceof Staff staff && !"Admin".equals(role)) {
            Map<String, Object> safe = new LinkedHashMap<>();
            safe.put("id", staff.getId()); safe.put("name", staff.getName()); safe.put("position", staff.getPosition());
            safe.put("department", staff.getDepartment()); safe.put("specialization", staff.getSpecialization()); safe.put("isActive", staff.getIsActive());
            return safe;
        }
        return body;
    }
}
