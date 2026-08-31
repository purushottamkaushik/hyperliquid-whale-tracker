package com.mamta.btctrade.autotradebtc.hyperliquid;

import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.mamta.btctrade.autotradebtc.hyperliquid.dto.OrderBookLevel;
import com.mamta.btctrade.autotradebtc.hyperliquid.dto.OrderBookSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Maintains a persistent WebSocket connection to Hyperliquid's public "l2Book" feed for BTC -
 * live order book depth (aggregated resting-order size at each price level), market-wide. Each
 * push is a full replacement snapshot (not a delta), so this just keeps the latest one and
 * re-broadcasts it to connected browser clients via Server-Sent Events.
 */
@Component
public class HyperliquidOrderBookStreamClient extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(HyperliquidOrderBookStreamClient.class);
    private static final long RECONNECT_DELAY_MS = 5000;
    private static final int MAX_TEXT_MESSAGE_BUFFER_BYTES = 1024 * 1024;
    private static final String SUBSCRIBE_MESSAGE =
            "{\"method\":\"subscribe\",\"subscription\":{\"type\":\"l2Book\",\"coin\":\"BTC\"}}";

    private final String wsUrl;
    private final ObjectMapper objectMapper;
    private final StandardWebSocketClient client = new StandardWebSocketClient(createContainer());
    private final AtomicReference<OrderBookSnapshot> latest = new AtomicReference<>();
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public HyperliquidOrderBookStreamClient(
            @Value("${app.hyperliquid.ws-url:wss://api.hyperliquid.xyz/ws}") String wsUrl,
            ObjectMapper objectMapper) {
        this.wsUrl = wsUrl;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        connect();
    }

    private static WebSocketContainer createContainer() {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.setDefaultMaxTextMessageBufferSize(MAX_TEXT_MESSAGE_BUFFER_BYTES);
        return container;
    }

    private void connect() {
        try {
            client.execute(this, wsUrl).exceptionally(ex -> {
                log.warn("Hyperliquid order book stream handshake failed, retrying in {}ms: {}", RECONNECT_DELAY_MS, ex.getMessage());
                scheduleReconnect();
                return null;
            });
        } catch (Exception e) {
            log.warn("Failed to connect to Hyperliquid order book stream, retrying in {}ms: {}", RECONNECT_DELAY_MS, e.getMessage());
            scheduleReconnect();
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("Connected to Hyperliquid BTC order book stream");
        session.sendMessage(new TextMessage(SUBSCRIBE_MESSAGE));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        JsonNode root;
        try {
            root = objectMapper.readTree(message.getPayload());
        } catch (Exception e) {
            log.warn("Failed to parse Hyperliquid order book stream message: {}", e.getMessage());
            return;
        }
        if (!"l2Book".equals(root.path("channel").asText())) {
            return;
        }

        OrderBookSnapshot snapshot = toSnapshot(root.path("data"));
        if (snapshot == null) {
            return;
        }
        latest.set(snapshot);
        broadcast(snapshot);
    }

    private static OrderBookSnapshot toSnapshot(JsonNode data) {
        try {
            JsonNode levels = data.path("levels");
            List<OrderBookLevel> bids = toLevels(levels.get(0));
            List<OrderBookLevel> asks = toLevels(levels.get(1));
            return new OrderBookSnapshot(
                    data.path("coin").asText(),
                    Instant.ofEpochMilli(data.path("time").asLong()),
                    bids,
                    asks);
        } catch (Exception e) {
            log.debug("Skipping unparseable order book snapshot: {}", e.getMessage());
            return null;
        }
    }

    private static List<OrderBookLevel> toLevels(JsonNode side) {
        List<OrderBookLevel> result = new ArrayList<>();
        if (side == null) {
            return result;
        }
        for (JsonNode level : side) {
            result.add(new OrderBookLevel(
                    new BigDecimal(level.path("px").asText()),
                    new BigDecimal(level.path("sz").asText()),
                    level.path("n").asInt()));
        }
        return result;
    }

    private void broadcast(OrderBookSnapshot snapshot) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("book").data(snapshot));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.warn("Hyperliquid order book stream closed ({}), reconnecting in {}ms", status, RECONNECT_DELAY_MS);
        scheduleReconnect();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("Hyperliquid order book stream transport error: {}", exception.getMessage());
    }

    private void scheduleReconnect() {
        Thread reconnectThread = new Thread(() -> {
            try {
                Thread.sleep(RECONNECT_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            connect();
        }, "hyperliquid-orderbook-stream-reconnect");
        reconnectThread.setDaemon(true);
        reconnectThread.start();
    }

    /** Latest order book snapshot, or null if none has arrived yet. */
    public OrderBookSnapshot getLatest() {
        return latest.get();
    }

    /** Registers a new SSE client; unregistered automatically on completion, timeout, or error. */
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }
}
