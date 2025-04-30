package com.work.matmode.loader.repository;

import com.work.matmode.loader.model.Document;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DocumentRepository extends MongoRepository<Document, String> {
    void deleteByFileName(String fileName);
}
