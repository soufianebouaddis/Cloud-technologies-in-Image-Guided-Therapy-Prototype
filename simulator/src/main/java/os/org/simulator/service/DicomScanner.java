package os.org.simulator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@Service
public class DicomScanner {

    private static final Logger log = LoggerFactory.getLogger(DicomScanner.class);

    @Value("${simulator.dicom.directory}")
    private String dicomDirectory;

    public List<Path> findDicomFiles() throws IOException {
        Path root = Path.of(dicomDirectory);
        try (Stream<Path> walk = Files.walk(root)) {
            List<Path> dcmFiles = walk
                    .filter(p -> p.toString().endsWith(".dcm"))
                    .sorted()
                    .toList();
            log.info("Found {} DICOM files in {}", dcmFiles.size(), dicomDirectory);
            return dcmFiles;
        }
    }
}
