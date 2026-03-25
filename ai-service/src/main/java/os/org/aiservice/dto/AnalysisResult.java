package os.org.aiservice.dto;

public record AnalysisResult(
        String frameId,
        String finding,
        double confidence,
        long inferenceMs
) {}
