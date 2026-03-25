package os.org.imagingservice.dto;

import java.util.Base64;

public record FrameEvent(
        String frameId,
        String pixelDataBase64,
        int width,
        int height,
        long timestamp
) {
    public static FrameEvent of(String frameId, byte[] pixelData, int width, int height) {
        return new FrameEvent(
                frameId,
                Base64.getEncoder().encodeToString(pixelData),
                width,
                height,
                System.currentTimeMillis()
        );
    }

    public byte[] decodePixelData() {
        return Base64.getDecoder().decode(pixelDataBase64);
    }
}
