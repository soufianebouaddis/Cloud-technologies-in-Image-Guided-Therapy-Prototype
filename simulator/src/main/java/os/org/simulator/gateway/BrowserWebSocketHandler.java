package os.org.simulator.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Server-side WebSocket the React frontend connects to (/ws/stream).
 * Browsers upload a DICOM as a binary message; results are pushed back as JSON text.
 */
@Component
public class BrowserWebSocketHandler extends AbstractWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(BrowserWebSocketHandler.class);

    private final BrowserSessionRegistry registry;
    private final ImagingServiceClient imaging;
    private final ObjectMapper mapper = new ObjectMapper();

    public BrowserWebSocketHandler(BrowserSessionRegistry registry, ImagingServiceClient imaging) {
        this.registry = registry;
        this.imaging = imaging;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        registry.add(session);
        log.info("Browser connected: {} (total {})", session.getId(), registry.size());
        session.sendMessage(new TextMessage(mapper.writeValueAsString(
                Map.of("type", "status", "connected", true))));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        registry.remove(session);
        log.info("Browser disconnected: {} ({})", session.getId(), status);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        byte[] dcm = message.getPayload().array();
        log.info("Received DICOM upload from browser {}: {} bytes", session.getId(), dcm.length);

        boolean forwarded = imaging.sendFrame(dcm);

        Map<String, Object> ack = new LinkedHashMap<>();
        ack.put("type", "accepted");
        ack.put("bytes", dcm.length);
        ack.put("forwarded", forwarded);
        session.sendMessage(new TextMessage(mapper.writeValueAsString(ack)));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("Browser transport error on {}: {}", session.getId(), exception.getMessage());
    }
}
