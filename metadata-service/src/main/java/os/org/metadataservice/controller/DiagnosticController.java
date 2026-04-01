package os.org.metadataservice.controller;

import os.org.metadataservice.model.DiagnosticRecord;
import os.org.metadataservice.repository.DiagnosticRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/diagnostics")
public class DiagnosticController {

    private final DiagnosticRepository diagnosticRepository;

    public DiagnosticController(DiagnosticRepository diagnosticRepository) {
        this.diagnosticRepository = diagnosticRepository;
    }

    @GetMapping
    public Flux<DiagnosticRecord> getAllDiagnostics() {
        return diagnosticRepository.findAll();
    }
}
