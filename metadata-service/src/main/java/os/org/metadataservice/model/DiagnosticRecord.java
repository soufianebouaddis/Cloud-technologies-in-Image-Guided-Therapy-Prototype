package os.org.metadataservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "diagnostics")
public class DiagnosticRecord {

    @Id
    private String id;
    private String frameId;
    private String finding;
    private double confidence;
    private long inferenceMs;
    private Instant receivedAt;

    public DiagnosticRecord() {}

    public DiagnosticRecord(String frameId, String finding, double confidence, long inferenceMs) {
        this.frameId = frameId;
        this.finding = finding;
        this.confidence = confidence;
        this.inferenceMs = inferenceMs;
        this.receivedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFrameId() { return frameId; }
    public void setFrameId(String frameId) { this.frameId = frameId; }

    public String getFinding() { return finding; }
    public void setFinding(String finding) { this.finding = finding; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public long getInferenceMs() { return inferenceMs; }
    public void setInferenceMs(long inferenceMs) { this.inferenceMs = inferenceMs; }

    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
}
