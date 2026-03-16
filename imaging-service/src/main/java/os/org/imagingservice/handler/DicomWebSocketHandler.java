package os.org.imagingservice.handler;

import os.org.imagingservice.grpc.DenoiseClient;
import os.org.imagingservice.service.DicomProcessor;
import os.org.imagingservice.service.DicomProcessor.DicomFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.nio.ByteBuffer;

@Component
public class DicomWebSocketHandler extends BinaryWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(DicomWebSocketHandler.class);

    private final DicomProcessor dicomProcessor;
    private final DenoiseClient denoiseClient;

    public DicomWebSocketHandler(DicomProcessor dicomProcessor, DenoiseClient denoiseClient) {
        this.dicomProcessor = dicomProcessor;
        this.denoiseClient = denoiseClient;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("Simulator connected: {}", session.getId());
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        byte[] dcmBytes = message.getPayload().array();
        log.info("Received DICOM frame: {} bytes from session {}", dcmBytes.length, session.getId());

        // Parse DICOM
        DicomFrame frame = dicomProcessor.extractFrame(dcmBytes);
        log.info("DICOM frame: {}x{}", frame.width(), frame.height());

        // Denoise via gRPC to cpp-service
        byte[] denoised = denoiseClient.denoise(frame.pixelData(), frame.width(), frame.height());
        log.info("Denoised frame: {} bytes", denoised.length);

        // Send denoised frame back to simulator
        session.sendMessage(new BinaryMessage(ByteBuffer.wrap(denoised)));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("Simulator disconnected: {} ({})", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket error on session {}: {}", session.getId(), exception.getMessage());
    }
}
