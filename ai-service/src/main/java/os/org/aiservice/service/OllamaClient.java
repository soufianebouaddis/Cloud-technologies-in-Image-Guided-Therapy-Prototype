package os.org.aiservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class OllamaClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);

    @Value("${ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${ollama.model:medgemma:4b}")
    private String model;

    private final RestTemplate restTemplate;

    public OllamaClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public record OllamaFinding(String finding, double confidence) {}

    public OllamaFinding analyze(byte[] imageData) {
        String imageBase64 = Base64.getEncoder().encodeToString(imageData);

        Map<String, Object> request = Map.of(
                "model", model,
                "prompt", "Analyze this medical fluoroscopy image. Provide a brief clinical finding and your confidence level (0.0 to 1.0). Respond in format: FINDING: <finding> CONFIDENCE: <value>",
                "images", List.of(imageBase64),
                "stream", false
        );

        String url = baseUrl + "/api/generate";
        log.info("Calling Ollama {} at {}", model, url);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

        String responseText = (String) response.get("response");
        log.info("Ollama response: {}", responseText);

        return parseFinding(responseText);
    }

    private OllamaFinding parseFinding(String responseText) {
        String finding = responseText;
        double confidence = 0.5;

        try {
            if (responseText.contains("FINDING:") && responseText.contains("CONFIDENCE:")) {
                finding = responseText
                        .substring(responseText.indexOf("FINDING:") + 8, responseText.indexOf("CONFIDENCE:"))
                        .trim();
                String confStr = responseText
                        .substring(responseText.indexOf("CONFIDENCE:") + 11)
                        .trim()
                        .replaceAll("[^0-9.]", "");
                confidence = Double.parseDouble(confStr);
            }
        } catch (Exception e) {
            log.warn("Could not parse structured response, using raw text: {}", e.getMessage());
            finding = responseText.trim();
        }

        return new OllamaFinding(finding, confidence);
    }
}
