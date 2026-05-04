package ianlegaria.personalknowledgeengine.document.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
public class TextExtractionServiceImpl implements TextExtractionService {

    private final Tika tika = new Tika();

    public String extractFromFile(MultipartFile file) {
        try (InputStream stream = file.getInputStream()) {
            String text = tika.parseToString(stream);
            log.info("Extracted {} characters from file: {}", text.length(), file.getOriginalFilename());
            return text;
        } catch (IOException | TikaException e) {
            log.error("Failed to extract text from file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to extract text from file: " + e.getMessage(), e);
        }
    }

    public String extractFromPath(Path filePath) {
        try (InputStream stream = Files.newInputStream(filePath)) {
            String text = tika.parseToString(stream);
            log.info("Extracted {} characters from path: {}", text.length(), filePath);
            return text;
        } catch (IOException | TikaException e) {
            log.error("Failed to extract text from path: {}", filePath, e);
            throw new RuntimeException("Failed to extract text from path: " + e.getMessage(), e);
        }
    }
}
