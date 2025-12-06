package com.VLmb.ai_tutor_backend.service.fileparsing;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileParser {
    String parse(MultipartFile file) throws IOException;
}
