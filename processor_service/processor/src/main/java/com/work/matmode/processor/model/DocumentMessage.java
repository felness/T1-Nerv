package com.work.matmode.processor.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocumentMessage {
    private String fileName;
    private String fileUrl;
}

