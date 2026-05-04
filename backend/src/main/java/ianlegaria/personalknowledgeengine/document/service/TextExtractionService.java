package ianlegaria.personalknowledgeengine.document.service;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface TextExtractionService {

    String extractFromFile(MultipartFile file);

    String extractFromPath(Path filePath);
}
