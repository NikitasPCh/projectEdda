package com.edda.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "game")
public record GameProperties(int actionIntervalSeconds) {
}
