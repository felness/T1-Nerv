package com.work.matmode.loader.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class DocumentNotification {
    @JsonProperty("fileName")
    private String fileName;

    @JsonProperty("url")
    private String url;


}