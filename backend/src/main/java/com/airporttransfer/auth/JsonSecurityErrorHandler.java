package com.airporttransfer.auth;

import com.airporttransfer.common.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Answers 401 (missing or invalid token) and 403 (wrong role) in the standard {@link ApiError} format. */
@Component
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper mapper;
    private final MessageSource messages;

    public JsonSecurityErrorHandler(ObjectMapper mapper, MessageSource messages) {
        this.mapper = mapper;
        this.messages = messages;
    }

    /** Runs before Spring MVC resolves the locale: French unless the client asked for another language. */
    private String text(HttpServletRequest request, String code, String fallback) {
        Locale locale = request.getHeader("Accept-Language") == null ? Locale.FRENCH : request.getLocale();
        return messages.getMessage("error." + code, null, fallback, locale);
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
        throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, ApiError.of("UNAUTHORIZED", text(request, "UNAUTHORIZED", "Authentication is required")));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
        throws IOException {
        write(response, HttpStatus.FORBIDDEN, ApiError.of("FORBIDDEN", text(request, "FORBIDDEN", "You are not allowed to do this")));
    }

    private void write(HttpServletResponse response, HttpStatus status, ApiError body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getWriter(), body);
    }
}
