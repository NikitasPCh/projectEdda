package com.edda.server.security;

import com.edda.server.session.SessionTokenStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Component
public class SessionAuthInterceptor implements HandlerInterceptor {

    private final SessionTokenStore sessionTokenStore;

    public SessionAuthInterceptor(SessionTokenStore sessionTokenStore) {
        this.sessionTokenStore = sessionTokenStore;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Cookie[] cookies = request.getCookies();

        Optional<UUID> playerId = sessionTokenStore.resolveFromCookies(cookies);

        if (playerId.isEmpty()) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }

        request.setAttribute("playerId", playerId.get());
        return true;
    }
}