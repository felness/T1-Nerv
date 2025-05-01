package com.work.matmode.loader.web;

import com.work.matmode.loader.dto.DocumentMessage;
import com.work.matmode.loader.dto.DocumentNotification;
import com.work.matmode.loader.model.*;
import com.work.matmode.loader.repository.DocumentRepository;
import com.work.matmode.loader.service.document.DocumentService;
import com.work.matmode.loader.service.kafka.KafkaProducerService;
import com.work.matmode.loader.service.minio.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class DocumentController {

    private final DocumentService documentService;
    private final KafkaProducerService kafkaProducerService;
    private final MinioService minioService;
    private final DocumentRepository documentRepository;

    @PostMapping("/notify")
    public ResponseEntity<?> notifyDocument(@RequestBody DocumentNotification notification) {
        log.info("Received document notification: {}", notification.getFileName());

        Document saved = documentService.save(notification);

        DocumentMessage message = new DocumentMessage(saved.getFileName(), saved.getFileUrl());

        kafkaProducerService.sendMessage(message);

        return ResponseEntity.ok(saved);
    }


    @GetMapping
    public ResponseEntity<List<Document>> getAll() {
        return ResponseEntity.ok(documentService.getAllDocuments());
    }


    @GetMapping("/{id}")
    public ResponseEntity<Document> getById(@PathVariable String id) {
        return documentService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }



    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteById(@PathVariable String id) {
        documentService.deleteById(id);
        return ResponseEntity.ok("Deleted document with id: " + id);
    }

    @DeleteMapping("/name/{name}")
    public ResponseEntity<?> delete(@PathVariable String name) {
        documentService.deleteByName(name);
        return ResponseEntity.ok("Deleted document with name: " + name);
    }


    @PostMapping("/upload")
    public ResponseEntity<String> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            // Загрузка файла в MinIO
            String fileName = minioService.uploadFile(file);

            // Создание метаданных документа
            Document metadata = new Document();
            metadata.setFileName(file.getOriginalFilename());
            metadata.setFileUrl(fileName); // Используем имя файла как fileUrl
            metadata.setUploadedAt(LocalDateTime.now());
            metadata.setStatus(DocumentStatus.UPLOADED);

            // Сохранение в MongoDB
            metadata = documentRepository.save(metadata);

            // Отправка сообщения в Kafka
            DocumentMessage message = new DocumentMessage(file.getOriginalFilename(), fileName);
            kafkaProducerService.sendMessage(message);


            return ResponseEntity.ok("File uploaded successfully: " + fileName);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error uploading file: " + e.getMessage());
        }
    }

    @GetMapping("/presigned-url")
    public ResponseEntity<?> getPresignedUrl(@RequestParam String fileName) {
        try {
            if (fileName == null || fileName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("File name is required");
            }
            String presignedUrl = minioService.getFileUrl(fileName);
            return ResponseEntity.ok(new PresignedUrlResponse(presignedUrl));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to generate presigned URL: " + e.getMessage());
        }
    }

    @GetMapping("/download")
    public ResponseEntity<?> downloadFile(
            @RequestParam String fileName,
            @RequestParam(required = false, defaultValue = "false") boolean inline
    ) {
        try {
            if (fileName == null || fileName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("File name is required"));
            }
            InputStream inputStream = minioService.downloadFile(fileName);
            InputStreamResource resource = new InputStreamResource(inputStream);

            String contentType = "application/octet-stream";
            if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else if (fileName.toLowerCase().endsWith(".png")) {
                contentType = "image/png";
            } else if (fileName.toLowerCase().endsWith(".pdf")) {
                contentType = "application/pdf";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.set(HttpHeaders.CONTENT_DISPOSITION, inline ? "inline;filename=" + fileName : "attachment;filename=" + fileName);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to download file: " + e.getMessage()));
        }
    }

    static class ErrorResponse {
        private final String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }
    }


}
