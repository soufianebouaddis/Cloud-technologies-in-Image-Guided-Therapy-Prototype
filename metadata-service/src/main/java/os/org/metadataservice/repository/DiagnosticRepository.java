package os.org.metadataservice.repository;

import os.org.metadataservice.model.DiagnosticRecord;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface DiagnosticRepository extends ReactiveMongoRepository<DiagnosticRecord, String> {
}
