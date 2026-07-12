package os.org.simulator.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Persistent WebSocket client to imaging-service. Forwards browser uploads into the
 * pipeline and relays results (denoised frame + AI finding) back to browser sessions.
 */
@Service
public class ImagingServiceClient extends AbstractWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ImagingServiceClient.class);
    private static final int MAX_BUFFER = 16 * 1024 * 1024;

    @Value("${simulator.imaging-service.ws-url}")
    private String wsUrl;

    private final BrowserSessionRegistry browsers;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicReference<WebSocketSession> session = new AtomicReference<>();

    public ImagingServiceClient(BrowserSessionRegistry browsers) {
        this.browsers = browsers;
    }

    @PostConstruct
    public void connect() {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.setDefaultMaxBinaryMessageBufferSize(MAX_BUFFER);
            container.setDefaultMaxTextMessageBufferSize(MAX_BUFFER);
            container.setAsyncSendTimeout(30_000);
            StandardWebSocketClient client = new StandardWebSocketClient(container);
            log.info("Connecting to imaging-service at {}", wsUrl);
            client.execute(this, wsUrl).whenComplete((s, ex) -> {
                if (ex != null) {
                    log.warn("Could not connect to imaging-service ({}); retrying in 3s", ex.getMessage());
                    scheduleReconnect();
                }
            });
        } catch (Exception e) {
            log.warn("Could not connect to imaging-service ({}); retrying in 3s", e.getMessage());
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return;
            }
            connect();
        }, "imaging-reconnect");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession s) {
        session.set(s);
        log.info("Connected to imaging-service");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession s, CloseStatus status) {
        session.set(null);
        log.warn("imaging-service connection closed ({}); reconnecting in 3s", status);
        scheduleReconnect();
    }

    @Override
    public void handleTransportError(WebSocketSession s, Throwable e) {
        log.warn("imaging-service transport error: {}", e.getMessage());
    }

    /** Forward a raw DICOM upload from the browser into the pipeline. */
    public boolean sendFrame(byte[] dcmBytes) {
        WebSocketSession s = session.get();
        if (s == null || !s.isOpen()) {
            log.warn("No active imaging-service connection; dropping frame");
            return false;
        }
        try {
            synchronized (s) {
                s.sendMessage(new BinaryMessage(ByteBuffer.wrap(dcmBytes)));
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to send frame to imaging-service: {}", e.getMessage());
            return false;
        }
    }

    /** Denoised frame (raw 16-bit grayscale pixels) -> PNG -> browsers. */
    @Override
    protected void handleBinaryMessage(WebSocketSession s, BinaryMessage message) {
        byte[] pixels = message.getPayload().array();
        try {
            String dataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(toPng(pixels));
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "denoised");
            payload.put("bytes", pixels.length);
            payload.put("image", dataUrl);
            browsers.broadcast(mapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("Failed to forward denoised frame: {}", e.getMessage());
        }
    }

    /** AI finding (JSON) from imaging-service -> browsers, tagged as a finding. */
    @Override
    @SuppressWarnings("unchecked")
    protected void handleTextMessage(WebSocketSession s, TextMessage message) {
        try {
            Map<String, Object> finding = mapper.readValue(message.getPayload(), LinkedHashMap.class);
            finding.put("type", "finding");
            browsers.broadcast(mapper.writeValueAsString(finding));
        } catch (Exception e) {
            log.error("Failed to forward AI finding: {}", e.getMessage());
        }
    }

    /**
     * Convert raw 16-bit little-endian grayscale pixels into an 8-bit grayscale PNG,
     * min/max normalized. Frames are square (e.g. 1024x1024) so dimensions are inferred.
     */
    private byte[] toPng(byte[] pixelData) throws Exception {
        int pixels = pixelData.length / 2;
        int side = (int) Math.round(Math.sqrt(pixels));
        int values[] = new int[pixels];
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < pixels; i++) {
            int lo = pixelData[i * 2] & 0xFF;
            int hi = pixelData[i * 2 + 1] & 0xFF;
            int v = (hi << 8) | lo;
            values[i] = v;
            if (v < min) min = v;
            if (v > max) max = v;
        }
        int range = Math.max(1, max - min);
        BufferedImage image = new BufferedImage(side, side, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = image.getRaster();
        for (int i = 0; i < pixels; i++) {
            int v8 = (int) ((long) (values[i] - min) * 255 / range);
            raster.setSample(i % side, i / side, 0, v8);
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        }
    }
}
