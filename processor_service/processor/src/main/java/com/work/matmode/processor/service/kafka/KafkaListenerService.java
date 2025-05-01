package com.work.matmode.processor.service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.work.matmode.processor.model.DocumentMessage;
import com.work.matmode.processor.model.DocumentMetaData;
import com.work.matmode.processor.model.DocumentStatus;
import com.work.matmode.processor.model.Result;
import com.work.matmode.processor.repository.DocumentRepository;
import com.work.matmode.processor.repository.ResultRepository;
import com.work.matmode.processor.service.minio.MinioService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaListenerService {
    private final DocumentRepository documentRepository;
    private final ResultRepository resultRepository;
    private final MinioService minioService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @KafkaListener(topics = "document-uploaded", groupId = "processor-group")
    public void listen(String messageJson) {
        try {
            // Десериализация сообщения Kafka
            DocumentMessage message = objectMapper.readValue(messageJson, DocumentMessage.class);
            String fileUrl = message.getFileUrl();

            DocumentMetaData metadata = documentRepository.findByFileUrl(fileUrl)
                    .orElseThrow(() -> new RuntimeException("Document not found for fileUrl: " + fileUrl));

            if (metadata.getStatus() != DocumentStatus.UPLOADED) {
                log.info("Document with fileUrl {} is not in UPLOADED status, skipping.", fileUrl);
                return;
            }

            // Скачиваем файл из MinIO
            InputStream fileStream = minioService.downloadFile(fileUrl);

            // Обновляем статус на PREPROCESSED
            metadata.setStatus(DocumentStatus.PREPROCESSED);
            documentRepository.save(metadata);

            // Отправляем асинхронный POST-запрос в FastAPI
            sendToFastApiAsync(fileStream, metadata, fileUrl);

            log.info("Successfully downloaded file from MinIO: {}", fileUrl);

        } catch (Exception e) {
            handleError(messageJson, e);
        }
    }

    @Async("taskExecutor")
    public void sendToFastApiAsync(InputStream fileStream, DocumentMetaData metadata, String fileUrl) {
        try {
            // Читаем InputStream в байты
            byte[] fileContent = fileStream.readAllBytes();

            // Формируем multipart/form-data
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(fileContent) {
                @Override
                public String getFilename() {
                    return metadata.getFileName();
                }
            });

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Отправляем запрос
            FastApiResponse response = restTemplate.postForObject(
                    "http://host.docker.internal:8000/extract_text/",
                    requestEntity,
                    FastApiResponse.class
            );

            // Сохраняем результат в коллекцию result
            Result resultDoc = new Result(fileUrl, response.getResults(), "SUCCESS");
            resultRepository.save(resultDoc);
            log.info("Successfully processed and saved result for fileUrl: {}", fileUrl);

        } catch (Exception e) {
            log.error("Error processing fileUrl {} in FastAPI: {}", fileUrl, e.getMessage());
            DocumentMetaData metadataUpdated = documentRepository.findByFileUrl(fileUrl).orElse(null);
            if (metadataUpdated != null) {
                metadataUpdated.setStatus(DocumentStatus.FAILED);
                documentRepository.save(metadataUpdated);
            }
            Result resultDoc = new Result(fileUrl, null, "FAILED");
            resultRepository.save(resultDoc);
        }
    }

    private void handleError(String messageJson, Exception e) {
        try {
            DocumentMessage message = objectMapper.readValue(messageJson, DocumentMessage.class);
            DocumentMetaData metadata = documentRepository.findByFileUrl(message.getFileUrl()).orElse(null);
            if (metadata != null) {
                metadata.setStatus(DocumentStatus.FAILED);
                documentRepository.save(metadata);
                Result resultDoc = new Result(message.getFileUrl(), null, "FAILED");
                resultRepository.save(resultDoc);
            }
            log.error("Error processing document: {}", e.getMessage(), e);
        } catch (Exception ex) {
            log.error("Error updating status to FAILED: {}", ex.getMessage(), ex);
        }
    }

    // Класс для десериализации ответа FastAPI
    @Data
    private static class FastApiResponse {
        private List<Result.ResultItem> results;
    }
}