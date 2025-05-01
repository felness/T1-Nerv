package com.work.matmode.loader.model;


import lombok.Data;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Data
@org.springframework.data.mongodb.core.mapping.Document(collection = "documents")
public class Document {
    @Id
    private String id;

    private String fileName;
    private String fileUrl;

    private LocalDateTime uploadedAt;

    private DocumentStatus status;





}
