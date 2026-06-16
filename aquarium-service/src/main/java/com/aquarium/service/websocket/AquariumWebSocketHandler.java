package com.aquarium.service.websocket;

import com.aquarium.common.dto.SensorData;
import com.aquarium.common.dto.WaterQualityAlert;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AquariumWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private static final ConcurrentHashMap<String, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        SESSIONS.put(session.getId(), session);
        log.info("WebSocket connected: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        SESSIONS.remove(session.getId());
        log.info("WebSocket disconnected: {}, status: {}", session.getId(), status);
    }

    public void broadcastSensorData(SensorData data) {
        try {
            String json = objectMapper.writeValueAsString(
                    new WebSocketMessage("SENSOR_DATA", data));
            broadcast(json);
        } catch (Exception e) {
            log.error("Failed to broadcast sensor data", e);
        }
    }

    public void broadcastAlert(WaterQualityAlert alert) {
        try {
            String json = objectMapper.writeValueAsString(
                    new WebSocketMessage("ALERT", alert));
            broadcast(json);
        } catch (Exception e) {
            log.error("Failed to broadcast alert", e);
        }
    }

    public void broadcastMessage(String type, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(
                    new WebSocketMessage(type, payload));
            broadcast(json);
        } catch (Exception e) {
            log.error("Failed to broadcast message: type={}", type, e);
        }
    }

    private void broadcast(String message) {
        SESSIONS.forEach((id, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    log.error("Failed to send WebSocket message to session {}", id, e);
                }
            }
        });
    }

    public record WebSocketMessage(String type, Object payload) {}
}
