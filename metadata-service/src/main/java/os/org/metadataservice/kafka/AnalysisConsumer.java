package os.org.metadataservice.kafka;

import os.org.metadataservice.dto.AnalysisResult;
import os.org.metadataservice.model.DiagnosticRecord;
import os.org.metadataservice.repository.DiagnosticRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AnalysisConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnalysisConsumer.class);

    private final DiagnosticRepository diagnosticRepository;

    public AnalysisConsumer(DiagnosticRepository diagnosticRepository) {
        this.diagnosticRepository = diagnosticRepository;
    }

    @KafkaListener(topics = "analysed-frames", groupId = "metadata-service")
    public void onAnalysisResult(AnalysisResult result) {
        log.info("Received analysis for frame {}: {}", result.frameId(), result.finding());

        DiagnosticRecord record = new DiagnosticRecord(
                result.frameId(),
                result.finding(),
                result.confidence(),
                result.inferenceMs()
        );

        diagnosticRepository.save(record)
                .doOnSuccess(saved -> log.info("Persisted diagnostic record {} for frame {}",
                        saved.getId(), saved.getFrameId()))
                .doOnError(err -> log.error("Failed to persist diagnostic: {}", err.getMessage()))
                .subscribe();
    }
}
