package os.org.simulator.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.util.concurrent.CountDownLatch;

public class SimulatorWebSocketHandler extends BinaryWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(SimulatorWebSocketHandler.class);

    private final CountDownLatch connectionLatch = new CountDownLatch(1);
    private WebSocketSession session;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        this.session = session;
        log.info("Connected to imaging-service: {}", session.getUri());
        connectionLatch.countDown();
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        byte[] payload = message.getPayload().array();
        log.info("Received denoised frame: {} bytes", payload.length);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("Disconnected from imaging-service: {}", status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket error: {}", exception.getMessage());
    }

    public WebSocketSession getSession() {
        return session;
    }

    public boolean awaitConnection(long timeoutMs) throws InterruptedException {
        return connectionLatch.await(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
}
