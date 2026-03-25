package os.org.imagingservice.kafka;

import os.org.imagingservice.dto.AnalysisResult;
import os.org.imagingservice.handler.DicomWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AnalysisConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnalysisConsumer.class);

    private final DicomWebSocketHandler webSocketHandler;

    public AnalysisConsumer(DicomWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @KafkaListener(topics = "analysed-frames", groupId = "imaging-service")
    public void onAnalysisResult(AnalysisResult result) {
        log.info("Received AI finding for frame {}: {} (confidence: {}, inference: {}ms)",
                result.frameId(), result.finding(), result.confidence(), result.inferenceMs());
        webSocketHandler.sendAnalysisFinding(result);
    }
}
