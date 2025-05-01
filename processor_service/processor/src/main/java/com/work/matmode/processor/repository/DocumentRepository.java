package com.work.matmode.processor.repository;

import com.work.matmode.processor.model.DocumentMetaData;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DocumentRepository extends MongoRepository<DocumentMetaData, String> {
    Optional<DocumentMetaData> findByFileUrl(String fileUrl);
}
