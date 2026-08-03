package com.edda.server.websocket;

import com.edda.server.session.SessionTokenStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class TokenHandshakeInterceptor implements HandshakeInterceptor {

    private final SessionTokenStore sessionTokenStore;

    public TokenHandshakeInterceptor(SessionTokenStore sessionTokenStore) {
        this.sessionTokenStore = sessionTokenStore;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return false;
        }

        String token = servletRequest.getServletRequest().getParameter("token");
        Optional<UUID> playerId = token != null ? sessionTokenStore.resolve(token) : Optional.empty();

        if (playerId.isEmpty()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        attributes.put("playerId", playerId.get());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Nothing to do after a successful handshake
    }
}