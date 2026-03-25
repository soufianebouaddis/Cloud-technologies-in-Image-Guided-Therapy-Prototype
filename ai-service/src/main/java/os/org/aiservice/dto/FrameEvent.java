package os.org.aiservice.dto;

import java.util.Base64;

public record FrameEvent(
        String frameId,
        String pixelDataBase64,
        int width,
        int height,
        long timestamp
) {
    public byte[] decodePixelData() {
        return Base64.getDecoder().decode(pixelDataBase64);
    }
}
