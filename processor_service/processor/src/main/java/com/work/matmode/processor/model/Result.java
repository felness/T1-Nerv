package com.work.matmode.processor.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "result")
public class Result {
    @Id
    private String id;
    private String fileUrl;
    private List<ResultItem> resultData;
    private String status; // SUCCESS или FAILED
    private LocalDateTime createdAt;

    public Result(String fileUrl, List<ResultItem> resultData, String status) {
        this.fileUrl = fileUrl;
        this.resultData = resultData;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    @Data
    public static class ResultItem {
        private String text;
        private List<Integer> bbox;
        private String label;
        private Double confidence;
    }
}