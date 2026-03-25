package os.org.imagingservice.dto;

public record AnalysisResult(
        String frameId,
        String finding,
        double confidence,
        long inferenceMs
) {}
