package com.work.matmode.loader.service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.work.matmode.loader.dto.DocumentMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class KafkaProducerService {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;
    private final ObjectMapper objectMapper;

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate,
                                @Value("${spring.kafka.topic}") String topic,
                                ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.objectMapper = objectMapper;
    }

    public void sendMessage(DocumentMessage message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, messageJson);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Sent message=[{}] with offset=[{}]", messageJson, result.getRecordMetadata().offset());
                } else {
                    log.error("Unable to send message=[{}] due to : {}", messageJson, ex.getMessage());
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Error serializing message: {}", e.getMessage());
            throw new RuntimeException("Error serializing message", e);
        }
    }
}