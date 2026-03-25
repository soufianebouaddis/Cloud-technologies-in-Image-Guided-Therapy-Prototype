package os.org.imagingservice.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import os.org.imagingservice.dto.AnalysisResult;
import os.org.imagingservice.dto.FrameEvent;
import os.org.imagingservice.grpc.DenoiseClient;
import os.org.imagingservice.kafka.FrameProducer;
import os.org.imagingservice.service.DicomProcessor;
import os.org.imagingservice.service.DicomProcessor.DicomFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class DicomWebSocketHandler extends BinaryWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(DicomWebSocketHandler.class);

    private final DicomProcessor dicomProcessor;
    private final DenoiseClient denoiseClient;
    private final FrameProducer frameProducer;
    private final ObjectMapper objectMapper;

    private final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    public DicomWebSocketHandler(DicomProcessor dicomProcessor,
                                 DenoiseClient denoiseClient,
                                 FrameProducer frameProducer,
                                 ObjectMapper objectMapper) {
        this.dicomProcessor = dicomProcessor;
        this.denoiseClient = denoiseClient;
        this.frameProducer = frameProducer;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("Simulator connected: {}", session.getId());
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        byte[] dcmBytes = message.getPayload().array();
        String frameId = UUID.randomUUID().toString();
        log.info("Received DICOM frame {}: {} bytes", frameId, dcmBytes.length);

        // Parse DICOM
        DicomFrame frame = dicomProcessor.extractFrame(dcmBytes);

        // Denoise via gRPC
        byte[] denoised = denoiseClient.denoise(frame.pixelData(), frame.width(), frame.height());
        log.info("Denoised frame {}: {} bytes", frameId, denoised.length);

        // Three actions in parallel, non-blocking
        FrameEvent rawEvent = FrameEvent.of(frameId, frame.pixelData(), frame.width(), frame.height());
        FrameEvent processedEvent = FrameEvent.of(frameId, denoised, frame.width(), frame.height());

        CompletableFuture.runAsync(() -> { if (session.isOpen()) sendBinary(session, denoised); });
        CompletableFuture.runAsync(() -> frameProducer.sendRawFrame(rawEvent));
        CompletableFuture.runAsync(() -> frameProducer.sendProcessedFrame(processedEvent));
    }

    private Void sendBinary(WebSocketSession session, byte[] data) {
        try {
            session.sendMessage(new BinaryMessage(ByteBuffer.wrap(data)));
        } catch (IOException e) {
            log.error("Failed to send denoised frame: {}", e.getMessage());
        }
        return null;
    }

    public void sendAnalysisFinding(AnalysisResult result) {
        sessions.removeIf(s -> !s.isOpen());
        for (WebSocketSession session : sessions) {
            try {
                String json = objectMapper.writeValueAsString(result);
                session.sendMessage(new TextMessage(json));
                log.info("Sent AI finding to session {}: {}", session.getId(), result.frameId());
            } catch (IOException e) {
                log.error("Failed to send AI finding to session {}: {}", session.getId(), e.getMessage());
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("Simulator disconnected: {} ({})", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket error on session {}: {}", session.getId(), exception.getMessage());
    }
}
