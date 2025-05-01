package com.work.matmode.loader.web;

import lombok.Getter;

@Getter
public class PresignedUrlResponse {
    private final String fileUrl;

    public PresignedUrlResponse(String url) {
        this.fileUrl = url;
    }
}
