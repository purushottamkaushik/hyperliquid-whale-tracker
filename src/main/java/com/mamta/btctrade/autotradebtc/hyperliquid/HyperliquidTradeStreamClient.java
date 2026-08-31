package com.mamta.btctrade.autotradebtc.hyperliquid;

import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.mamta.btctrade.autotradebtc.hyperliquid.dto.BtcTradeEvent;
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
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Maintains a persistent WebSocket connection to Hyperliquid's public "trades" feed for BTC -
 * every executed trade on the BTC perpetual market, market-wide (not scoped to tracked wallets),
 * including both parties' addresses. Keeps a bounded recent-trades buffer for the initial page
 * load and re-broadcasts each new trade to connected browser clients via Server-Sent Events.
 *
 * Reconnects automatically on disconnect - Hyperliquid's own docs say the server can drop
 * connections without notice and expects clients to reconnect themselves.
 */
@Component
public class HyperliquidTradeStreamClient extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(HyperliquidTradeStreamClient.class);
    private static final int BUFFER_CAPACITY = 300;
    private static final long RECONNECT_DELAY_MS = 5000;
    private static final String SUBSCRIBE_MESSAGE =
            "{\"method\":\"subscribe\",\"subscription\":{\"type\":\"trades\",\"coin\":\"BTC\"}}";

    /**
     * Hyperliquid batches many trades into a single WebSocket text frame during busy periods,
     * which routinely exceeds the JSR-356 default max text message buffer (8KB) - the connection
     * was otherwise killed with close code 1009 ("message too big") within seconds of connecting.
     */
    private static final int MAX_TEXT_MESSAGE_BUFFER_BYTES = 1024 * 1024;

    private final String wsUrl;
    private final ObjectMapper objectMapper;
    private final StandardWebSocketClient client = new StandardWebSocketClient(createContainer());
    private final Deque<BtcTradeEvent> recentTrades = new ConcurrentLinkedDeque<>();
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public HyperliquidTradeStreamClient(
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
                log.warn("Hyperliquid trade stream handshake failed, retrying in {}ms: {}", RECONNECT_DELAY_MS, ex.getMessage());
                scheduleReconnect();
                return null;
            });
        } catch (Exception e) {
            log.warn("Failed to connect to Hyperliquid trade stream, retrying in {}ms: {}", RECONNECT_DELAY_MS, e.getMessage());
            scheduleReconnect();
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("Connected to Hyperliquid BTC trade stream");
        session.sendMessage(new TextMessage(SUBSCRIBE_MESSAGE));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        JsonNode root;
        try {
            root = objectMapper.readTree(message.getPayload());
        } catch (Exception e) {
            log.warn("Failed to parse Hyperliquid trade stream message: {}", e.getMessage());
            return;
        }
        if (!"trades".equals(root.path("channel").asText())) {
            return;
        }

        List<BtcTradeEvent> newTrades = new ArrayList<>();
        for (JsonNode t : root.path("data")) {
            BtcTradeEvent event = toEvent(t);
            if (event != null) {
                newTrades.add(event);
            }
        }
        if (newTrades.isEmpty()) {
            return;
        }

        // newTrades arrives oldest-first within the batch; addFirst-ing in that order leaves the
        // deque newest-first overall, matching getRecentTrades()'s documented order.
        for (BtcTradeEvent event : newTrades) {
            recentTrades.addFirst(event);
        }
        while (recentTrades.size() > BUFFER_CAPACITY) {
            recentTrades.removeLast();
        }

        broadcast(newTrades);
    }

    private static BtcTradeEvent toEvent(JsonNode t) {
        try {
            BigDecimal price = new BigDecimal(t.path("px").asText());
            BigDecimal size = new BigDecimal(t.path("sz").asText());
            String side = "B".equals(t.path("side").asText()) ? "BUY" : "SELL";
            JsonNode users = t.path("users");
            String taker = users.get(0).asText();
            String maker = users.get(1).asText();
            return new BtcTradeEvent(
                    t.path("tid").asLong(),
                    Instant.ofEpochMilli(t.path("time").asLong()),
                    side,
                    price,
                    size,
                    price.multiply(size),
                    taker,
                    maker);
        } catch (Exception e) {
            log.debug("Skipping unparseable trade event: {}", t);
            return null;
        }
    }

    private void broadcast(List<BtcTradeEvent> newTrades) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("trades").data(newTrades));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.warn("Hyperliquid trade stream closed ({}), reconnecting in {}ms", status, RECONNECT_DELAY_MS);
        scheduleReconnect();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("Hyperliquid trade stream transport error: {}", exception.getMessage());
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
        }, "hyperliquid-trade-stream-reconnect");
        reconnectThread.setDaemon(true);
        reconnectThread.start();
    }

    /** Buffered trades, newest first. */
    public List<BtcTradeEvent> getRecentTrades() {
        return new ArrayList<>(recentTrades);
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
