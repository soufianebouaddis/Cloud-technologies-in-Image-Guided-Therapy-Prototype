package os.org.metadataservice.dto;

public record AnalysisResult(
        String frameId,
        String finding,
        double confidence,
        long inferenceMs
) {}
