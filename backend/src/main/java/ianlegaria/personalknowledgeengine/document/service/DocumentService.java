package ianlegaria.personalknowledgeengine.document.service;

import ianlegaria.personalknowledgeengine.document.dto.DocumentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

public interface DocumentService {

    DocumentResponse uploadDocument(UUID userId, String title, MultipartFile file, UUID collectionId);

    Page<DocumentResponse> getDocumentsByUser(UUID userId, Optional<UUID> collectionId, Pageable pageable);

    DocumentResponse getDocumentById(UUID id);

    DocumentResponse assignCollection(UUID documentId, UUID userId, UUID collectionId);

    DocumentResponse createFromText(UUID userId, String title, String content, UUID collectionId);

    DocumentResponse updateContent(UUID documentId, UUID userId, String content);

    DocumentResponse updateTitle(UUID documentId, UUID userId, String title);

    void deleteDocument(UUID documentId, UUID userId);
}
