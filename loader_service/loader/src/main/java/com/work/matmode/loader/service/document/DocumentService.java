package com.work.matmode.loader.service.document;

import com.work.matmode.loader.dto.DocumentNotification;
import com.work.matmode.loader.model.Document;
import com.work.matmode.loader.model.DocumentStatus;
import com.work.matmode.loader.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;

    public Document save(DocumentNotification notification) {
        Document document = new Document();
        document.setFileName(notification.getFileName());
        document.setFileUrl(notification.getUrl());
        document.setUploadedAt(LocalDateTime.now());
        document.setStatus(DocumentStatus.UPLOADED);
        return documentRepository.save(document);
    }


    public Optional<Document> getById(String id) {
        return documentRepository.findById(id);
    }


    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }


    public void updateStatus(String id, DocumentStatus status) {
        documentRepository.findById(id).ifPresent(document -> {
            document.setStatus(status);
            documentRepository.save(document);
        });
    }

    public void deleteById(String id) {
        documentRepository.deleteById(id);
    }

    public void deleteByName(String name) {
        documentRepository.deleteByFileName(name);
    }
}
