package os.org.simulator.runner;

import os.org.simulator.client.SimulatorWebSocketHandler;
import os.org.simulator.service.DicomScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class StreamingRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StreamingRunner.class);

    @Value("${simulator.imaging-service.ws-url}")
    private String wsUrl;

    @Value("${simulator.frame-interval-ms:1000}")
    private long frameIntervalMs;

    private final DicomScanner dicomScanner;

    public StreamingRunner(DicomScanner dicomScanner) {
        this.dicomScanner = dicomScanner;
    }

    @Override
    public void run(String... args) throws Exception {
        List<Path> dcmFiles = dicomScanner.findDicomFiles();
        if (dcmFiles.isEmpty()) {
            log.warn("No DICOM files found. Exiting.");
            return;
        }

        SimulatorWebSocketHandler handler = new SimulatorWebSocketHandler();

        // Denoised frames returned by imaging-service are multi-megabyte; raise the
        // client buffers above the 8 KB default so they aren't rejected with code 1009.
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.setDefaultMaxBinaryMessageBufferSize(16 * 1024 * 1024);
        container.setDefaultMaxTextMessageBufferSize(1 * 1024 * 1024);
        container.setAsyncSendTimeout(30_000);
        StandardWebSocketClient client = new StandardWebSocketClient(container);

        log.info("Connecting to imaging-service at {}", wsUrl);
        client.execute(handler, wsUrl);

        if (!handler.awaitConnection(5000)) {
            log.error("Failed to connect to imaging-service within 5s");
            return;
        }

        log.info("Streaming {} DICOM frames at {}ms interval", dcmFiles.size(), frameIntervalMs);
        for (Path dcmFile : dcmFiles) {
            byte[] dcmBytes = Files.readAllBytes(dcmFile);
            log.info("Sending: {} ({} bytes)", dcmFile.getFileName(), dcmBytes.length);
            handler.getSession().sendMessage(new BinaryMessage(ByteBuffer.wrap(dcmBytes)));
            Thread.sleep(frameIntervalMs);
        }

        log.info("All frames sent. Waiting for remaining responses...");
        Thread.sleep(3000);
        handler.getSession().close();
    }
}
