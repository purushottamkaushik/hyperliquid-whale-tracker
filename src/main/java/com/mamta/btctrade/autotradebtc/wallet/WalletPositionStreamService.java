package com.mamta.btctrade.autotradebtc.wallet;

import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.mamta.btctrade.autotradebtc.hyperliquid.dto.BtcPositionUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Streams real-time BTC position updates for every marked Hyperliquid wallet, over one shared
 * WebSocket connection subscribed to Hyperliquid's per-user "clearinghouseState" feed (one
 * subscription per marked wallet). Reconciles which wallets are subscribed on a timer, so
 * marking/unmarking a wallet takes effect automatically without any direct call from
 * {@link WalletService} - keeps the two features decoupled.
 */
@Service
public class WalletPositionStreamService extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(WalletPositionStreamService.class);
    private static final String BTC = "BTC";
    private static final long RECONNECT_DELAY_MS = 5000;
    private static final int MAX_TEXT_MESSAGE_BUFFER_BYTES = 1024 * 1024;

    private final String wsUrl;
    private final ObjectMapper objectMapper;
    private final WalletRepository walletRepository;
    private final StandardWebSocketClient client;
    private volatile WebSocketSession session;
    private final Set<String> subscribedAddresses = ConcurrentHashMap.newKeySet();
    private final Map<String, BtcPositionUpdate> positionsByAddress = new ConcurrentHashMap<>();
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public WalletPositionStreamService(
            @Value("${app.hyperliquid.ws-url:wss://api.hyperliquid.xyz/ws}") String wsUrl,
            ObjectMapper objectMapper,
            WalletRepository walletRepository) {
        this.wsUrl = wsUrl;
        this.objectMapper = objectMapper;
        this.walletRepository = walletRepository;
        this.client = new StandardWebSocketClient(createContainer());
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
                log.warn("Hyperliquid position stream handshake failed, retrying in {}ms: {}", RECONNECT_DELAY_MS, ex.getMessage());
                scheduleReconnect();
                return null;
            });
        } catch (Exception e) {
            log.warn("Failed to connect to Hyperliquid position stream, retrying in {}ms: {}", RECONNECT_DELAY_MS, e.getMessage());
            scheduleReconnect();
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("Connected to Hyperliquid wallet position stream");
        this.session = session;
        subscribedAddresses.clear();
        refreshSubscriptions();
    }

    /**
     * Reconciles the live subscription set against the currently marked Hyperliquid wallets -
     * subscribing to newly marked ones and unsubscribing from unmarked/deactivated/deleted ones.
     * Runs on a timer and also right after (re)connecting.
     */
    @Scheduled(fixedRateString = "${app.wallet.position-stream-refresh-ms:30000}")
    public void refreshSubscriptions() {
        WebSocketSession s = session;
        if (s == null || !s.isOpen()) {
            return;
        }

        Set<String> desired = walletRepository
                .findByChainAndMarkedTrueAndDeactivatedAtIsNullAndDeletedAtIsNull(Chain.HYPERLIQUID)
                .stream()
                .map(w -> w.getAddress().toLowerCase())
                .collect(Collectors.toSet());

        for (String address : desired) {
            if (subscribedAddresses.add(address)) {
                sendSubscription(s, "subscribe", address);
            }
        }
        for (String address : new ArrayList<>(subscribedAddresses)) {
            if (!desired.contains(address)) {
                subscribedAddresses.remove(address);
                positionsByAddress.remove(address);
                sendSubscription(s, "unsubscribe", address);
            }
        }
    }

    private void sendSubscription(WebSocketSession session, String method, String address) {
        try {
            String message = "{\"method\":\"%s\",\"subscription\":{\"type\":\"clearinghouseState\",\"user\":\"%s\"}}"
                    .formatted(method, address);
            session.sendMessage(new TextMessage(message));
        } catch (Exception e) {
            log.warn("Failed to {} clearinghouseState for {}: {}", method, address, e.getMessage());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        JsonNode root;
        try {
            root = objectMapper.readTree(message.getPayload());
        } catch (Exception e) {
            log.warn("Failed to parse Hyperliquid position stream message: {}", e.getMessage());
            return;
        }
        if (!"clearinghouseState".equals(root.path("channel").asText())) {
            return;
        }

        JsonNode data = root.path("data");
        String address = data.path("user").asText().toLowerCase();
        if (address.isBlank() || !subscribedAddresses.contains(address)) {
            return;
        }

        BtcPositionUpdate update = toUpdate(address, data.path("clearinghouseState"));
        positionsByAddress.put(address, update);
        broadcast(update);
    }

    private static BtcPositionUpdate toUpdate(String address, JsonNode clearinghouseState) {
        Instant time = Instant.now();
        for (JsonNode assetPosition : clearinghouseState.path("assetPositions")) {
            JsonNode position = assetPosition.path("position");
            if (!BTC.equals(position.path("coin").asText())) {
                continue;
            }
            BigDecimal signedSize = decimalOrNull(position, "szi");
            if (signedSize == null || signedSize.signum() == 0) {
                break;
            }
            JsonNode leverageValue = position.path("leverage").path("value");
            BigDecimal leverage = leverageValue.isMissingNode() || leverageValue.isNull()
                    ? null : BigDecimal.valueOf(leverageValue.asDouble());
            return new BtcPositionUpdate(
                    address,
                    time,
                    signedSize.signum() > 0 ? "LONG" : "SHORT",
                    signedSize.abs(),
                    decimalOrNull(position, "entryPx"),
                    decimalOrNull(position, "positionValue"),
                    decimalOrNull(position, "unrealizedPnl"),
                    leverage);
        }
        // No open BTC position right now - still emit an event so a closed position clears the UI.
        return new BtcPositionUpdate(address, time, null, null, null, null, null, null);
    }

    private static BigDecimal decimalOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        try {
            return new BigDecimal(value.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void broadcast(BtcPositionUpdate update) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("position").data(update));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.warn("Hyperliquid position stream closed ({}), reconnecting in {}ms", status, RECONNECT_DELAY_MS);
        this.session = null;
        scheduleReconnect();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("Hyperliquid position stream transport error: {}", exception.getMessage());
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
        }, "hyperliquid-position-stream-reconnect");
        reconnectThread.setDaemon(true);
        reconnectThread.start();
    }

    /** Latest known BTC position for every currently-subscribed marked wallet. */
    public List<BtcPositionUpdate> getSnapshot() {
        return new ArrayList<>(positionsByAddress.values());
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
