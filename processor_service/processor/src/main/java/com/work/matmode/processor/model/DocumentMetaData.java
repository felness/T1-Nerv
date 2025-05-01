package com.work.matmode.processor.model;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "documents")
public class DocumentMetaData {
    @Id
    private String id;
    private String fileName;
    private String fileUrl;
    private LocalDateTime uploadedAt;
    private DocumentStatus status;
}
