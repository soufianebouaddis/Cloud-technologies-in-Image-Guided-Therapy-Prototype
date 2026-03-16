package os.org.imagingservice.service;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
public class DicomProcessor {

    private static final Logger log = LoggerFactory.getLogger(DicomProcessor.class);

    public record DicomFrame(byte[] pixelData, int width, int height) {}

    public DicomFrame extractFrame(byte[] dcmBytes) throws IOException {
        try (DicomInputStream dis = new DicomInputStream(new ByteArrayInputStream(dcmBytes))) {
            Attributes attrs = dis.readDataset();
            int width = attrs.getInt(Tag.Columns, 0);
            int height = attrs.getInt(Tag.Rows, 0);
            byte[] pixelData = attrs.getBytes(Tag.PixelData);

            if (pixelData == null || width == 0 || height == 0) {
                throw new IOException("Invalid DICOM: missing pixel data or dimensions");
            }

            log.debug("Extracted DICOM frame: {}x{}, {} bytes", width, height, pixelData.length);
            return new DicomFrame(pixelData, width, height);
        }
    }
}
