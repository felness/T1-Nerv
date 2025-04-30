package com.work.matmode.processor.service.kafka;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.work.matmode.processor.model.DocumentMessage;
import com.work.matmode.processor.model.DocumentMetaData;
import com.work.matmode.processor.model.DocumentStatus;
import com.work.matmode.processor.repository.DocumentRepository;
import com.work.matmode.processor.service.minio.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaListenerService {
    private final DocumentRepository documentRepository;

    private final MinioService minioService;

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "document-uploaded", groupId = "processor-group")
    public void listen(String messageJson) {
        try {
            // Десериализация сообщения Kafka
            DocumentMessage message = objectMapper.readValue(messageJson, DocumentMessage.class);
            String fileUrl = message.getFileUrl();

            DocumentMetaData metadata = documentRepository.findByFileUrl(fileUrl)
                    .orElseThrow(() -> new RuntimeException("Document not found for fileUrl: " + fileUrl));

            if (metadata.getStatus() != DocumentStatus.UPLOADED) {
                System.out.println("Document with fileUrl " + fileUrl + " is not in UPLOADED status, skipping.");
                return;
            }

            InputStream fileStream = minioService.downloadFile(fileUrl);


            metadata.setStatus(DocumentStatus.PREPROCESSED);
            documentRepository.save(metadata);

            // Логирование результата
            log.info("Корректно получили файл по url "+ fileUrl);


        } catch (Exception e) {
            try {
                DocumentMessage message = objectMapper.readValue(messageJson, DocumentMessage.class);
                DocumentMetaData metadata = documentRepository.findByFileUrl(message.getFileUrl()).orElse(null);
                if (metadata != null) {
                    metadata.setStatus(DocumentStatus.FAILED);
                    documentRepository.save(metadata);
                }
            } catch (Exception ex) {
                System.err.println("Error updating status to FAILED: " + ex.getMessage());
            }
            System.err.println("Error processing document: " + e.getMessage());
        }
    }

}
