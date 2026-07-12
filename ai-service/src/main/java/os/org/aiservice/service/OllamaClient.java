package os.org.aiservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
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

    public OllamaFinding analyze(byte[] pixelData, int width, int height) {
        // Ollama expects a real encoded image (PNG/JPEG); raw DICOM pixel bytes are rejected
        // with "image: unknown format". Wrap the 16-bit grayscale frame into a PNG first.
        String imageBase64 = Base64.getEncoder().encodeToString(toPng(pixelData, width, height));

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

    /**
     * Converts raw DICOM grayscale pixel data into an 8-bit grayscale PNG.
     * Supports 8-bit (1 byte/pixel) and 16-bit little-endian (2 bytes/pixel) frames,
     * min/max normalized to 0-255 so the anatomy is visible to the vision model.
     */
    private byte[] toPng(byte[] pixelData, int width, int height) {
        int pixels = width * height;
        int bytesPerPixel = pixels > 0 ? pixelData.length / pixels : 1;

        int[] values = new int[pixels];
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < pixels; i++) {
            int v;
            if (bytesPerPixel >= 2) {
                int lo = pixelData[i * 2] & 0xFF;
                int hi = pixelData[i * 2 + 1] & 0xFF;
                v = (hi << 8) | lo;
            } else {
                v = pixelData[i] & 0xFF;
            }
            values[i] = v;
            if (v < min) min = v;
            if (v > max) max = v;
        }

        int range = Math.max(1, max - min);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = image.getRaster();
        for (int i = 0; i < pixels; i++) {
            int v8 = (int) ((long) (values[i] - min) * 255 / range);
            raster.setSample(i % width, i / width, 0, v8);
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to encode frame as PNG", e);
        }
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
