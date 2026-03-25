package os.org.aiservice.kafka;

import os.org.aiservice.dto.AnalysisResult;
import os.org.aiservice.dto.FrameEvent;
import os.org.aiservice.service.OllamaClient;
import os.org.aiservice.service.OllamaClient.OllamaFinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class FrameConsumer {

    private static final Logger log = LoggerFactory.getLogger(FrameConsumer.class);

    private final OllamaClient ollamaClient;
    private final KafkaTemplate<String, AnalysisResult> kafkaTemplate;

    public FrameConsumer(OllamaClient ollamaClient, KafkaTemplate<String, AnalysisResult> kafkaTemplate) {
        this.ollamaClient = ollamaClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "processed-frames", groupId = "ai-service")
    public void onProcessedFrame(FrameEvent event) {
        log.info("Received processed frame {} for AI analysis", event.frameId());

        long start = System.currentTimeMillis();
        OllamaFinding finding = ollamaClient.analyze(event.decodePixelData());
        long inferenceMs = System.currentTimeMillis() - start;

        AnalysisResult result = new AnalysisResult(
                event.frameId(),
                finding.finding(),
                finding.confidence(),
                inferenceMs
        );

        kafkaTemplate.send("analysed-frames", result.frameId(), result);
        log.info("Published analysis for frame {}: {} (confidence: {}, inference: {}ms)",
                result.frameId(), result.finding(), result.confidence(), result.inferenceMs());
    }
}
