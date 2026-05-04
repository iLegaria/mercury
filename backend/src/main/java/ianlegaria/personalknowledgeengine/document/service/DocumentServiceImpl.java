package ianlegaria.personalknowledgeengine.document.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ianlegaria.personalknowledgeengine.chunk.repository.ChunkRepository;
import ianlegaria.personalknowledgeengine.collection.entity.CollectionEntity;
import ianlegaria.personalknowledgeengine.collection.repository.CollectionRepository;
import ianlegaria.personalknowledgeengine.common.config.RabbitMQConfig;
import ianlegaria.personalknowledgeengine.common.config.StorageConfig;
import ianlegaria.personalknowledgeengine.common.exception.ResourceNotFoundException;
import ianlegaria.personalknowledgeengine.common.outbox.OutboxEvent;
import ianlegaria.personalknowledgeengine.common.outbox.OutboxEventRepository;
import ianlegaria.personalknowledgeengine.common.outbox.OutboxEventSavedEvent;
import ianlegaria.personalknowledgeengine.document.dto.DocumentIngestionEvent;
import ianlegaria.personalknowledgeengine.document.dto.DocumentResponse;
import ianlegaria.personalknowledgeengine.document.entity.DocumentContentEntity;
import ianlegaria.personalknowledgeengine.document.entity.DocumentEntity;
import ianlegaria.personalknowledgeengine.document.repository.DocumentContentRepository;
import ianlegaria.personalknowledgeengine.document.repository.DocumentRepository;
import ianlegaria.personalknowledgeengine.chunk.service.SemanticSearchService;
import ianlegaria.personalknowledgeengine.user.entity.UserEntity;
import ianlegaria.personalknowledgeengine.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentContentRepository documentContentRepository;
    private final UserRepository userRepository;
    private final CollectionRepository collectionRepository;
    private final ChunkRepository chunkRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final SemanticSearchService semanticSearchService;
    private final ObjectMapper objectMapper;
    private final StorageConfig storageConfig;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public DocumentResponse uploadDocument(UUID userId, String title, MultipartFile file, UUID collectionId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        String filePath = storeFile(file, userId);

        String sourceType = detectSourceType(file.getContentType());

        DocumentEntity.DocumentEntityBuilder builder = DocumentEntity.builder()
                .user(user)
                .title(title)
                .sourceType(sourceType)
                .filePath(filePath);

        if (collectionId != null) {
            CollectionEntity collection = collectionRepository.findByIdAndUserId(collectionId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + collectionId));
            builder.collection(collection);
        }

        DocumentEntity saved = documentRepository.saveAndFlush(builder.build());

        publishIngestionEvent(saved);

        log.info("Document uploaded: {} -> {}", file.getOriginalFilename(), saved.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> getDocumentsByUser(UUID userId, Optional<UUID> collectionId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        Page<DocumentEntity> docs = collectionId.isPresent()
                ? documentRepository.findByUserIdAndCollectionId(userId, collectionId.get(), pageable)
                : documentRepository.findByUserId(userId, pageable);
        return docs.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(UUID id) {
        DocumentEntity document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));
        String content = documentContentRepository.findByDocumentId(id)
                .map(DocumentContentEntity::getContent)
                .orElse(null);
        return toResponse(document, content);
    }

    @Transactional
    public DocumentResponse assignCollection(UUID documentId, UUID userId, UUID collectionId) {
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        if (!document.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Document not found with id: " + documentId);
        }

        if (collectionId == null) {
            document.setCollection(null);
        } else {
            CollectionEntity collection = collectionRepository.findByIdAndUserId(collectionId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + collectionId));
            document.setCollection(collection);
        }

        return toResponse(documentRepository.save(document));
    }

    @Transactional
    public DocumentResponse createFromText(UUID userId, String title, String content, UUID collectionId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        DocumentEntity.DocumentEntityBuilder builder = DocumentEntity.builder()
                .user(user)
                .title(title)
                .sourceType("TXT");

        if (collectionId != null) {
            CollectionEntity collection = collectionRepository.findByIdAndUserId(collectionId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + collectionId));
            builder.collection(collection);
        }

        DocumentEntity saved = documentRepository.saveAndFlush(builder.build());

        documentContentRepository.save(DocumentContentEntity.builder()
                .document(saved)
                .content(content)
                .build());

        saveToOutbox(DocumentIngestionEvent.builder()
                .documentId(saved.getId())
                .userId(saved.getUser().getId())
                .title(saved.getTitle())
                .sourceType(saved.getSourceType())
                .textContent(content)
                .build());

        log.info("Created document from text: {} -> {}", title, saved.getId());
        return toResponse(saved, content);
    }

    @Transactional
    public DocumentResponse updateContent(UUID documentId, UUID userId, String content) {
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        if (!document.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Document not found with id: " + documentId);
        }

        document.setStatus("PENDING");
        documentRepository.save(document);

        DocumentContentEntity docContent = documentContentRepository.findByDocumentId(documentId)
                .orElseGet(() -> DocumentContentEntity.builder().document(document).build());
        docContent.setContent(content);
        documentContentRepository.save(docContent);

        chunkRepository.deleteByDocumentId(documentId);
        semanticSearchService.evictUserSearchCache(userId);

        saveToOutbox(DocumentIngestionEvent.builder()
                .documentId(document.getId())
                .userId(document.getUser().getId())
                .title(document.getTitle())
                .sourceType(document.getSourceType())
                .textContent(content)
                .build());

        log.info("Document {} content updated, re-queued for ingestion", documentId);
        return toResponse(document, content);
    }

    @Transactional
    public DocumentResponse updateTitle(UUID documentId, UUID userId, String title) {
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        if (!document.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Document not found with id: " + documentId);
        }

        document.setTitle(title);
        return toResponse(documentRepository.save(document));
    }

    @Transactional
    public void deleteDocument(UUID documentId, UUID userId) {
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        if (!document.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Document not found with id: " + documentId);
        }

        documentRepository.delete(document);
        semanticSearchService.evictUserSearchCache(userId);

        if (document.getFilePath() != null) {
            try {
                Files.deleteIfExists(Path.of(document.getFilePath()));
                log.info("Deleted file: {}", document.getFilePath());
            } catch (IOException e) {
                log.warn("Could not delete file {}: {}", document.getFilePath(), e.getMessage());
            }
        }

        log.info("Deleted document {} for user {}", documentId, userId);
    }

    private String storeFile(MultipartFile file, UUID userId) {
        try {
            Path userDir = Paths.get(storageConfig.getUploadDir(), userId.toString());
            Files.createDirectories(userDir);

            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = userDir.resolve(filename);

            Files.copy(file.getInputStream(), filePath);

            log.info("File stored at: {}", filePath);
            return filePath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }

    private String detectSourceType(String contentType) {
        if (contentType == null) return "UNKNOWN";
        return switch (contentType) {
            case "application/pdf" -> "PDF";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "DOCX";
            case "text/plain" -> "TXT";
            case "text/html" -> "HTML";
            case "text/markdown" -> "MARKDOWN";
            default -> "OTHER";
        };
    }

    private void publishIngestionEvent(DocumentEntity document) {
        saveToOutbox(DocumentIngestionEvent.builder()
                .documentId(document.getId())
                .userId(document.getUser().getId())
                .title(document.getTitle())
                .sourceType(document.getSourceType())
                .filePath(document.getFilePath())
                .build());

        log.info("Queued outbox event for document: {}", document.getId());
    }

    private void saveToOutbox(DocumentIngestionEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            outboxEventRepository.save(OutboxEvent.builder()
                    .aggregateType("Document")
                    .aggregateId(event.getDocumentId())
                    .eventType(DocumentIngestionEvent.class.getName())
                    .exchange(RabbitMQConfig.EXCHANGE)
                    .routingKey(RabbitMQConfig.INGESTION_ROUTING_KEY)
                    .payload(payload)
                    .build());
            eventPublisher.publishEvent(new OutboxEventSavedEvent(this));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize ingestion event for document: " + event.getDocumentId(), e);
        }
    }

    private DocumentResponse toResponse(DocumentEntity entity) {
        return toResponse(entity, null);
    }

    private DocumentResponse toResponse(DocumentEntity entity, String extractedText) {
        CollectionEntity col = entity.getCollection();
        return DocumentResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .title(entity.getTitle())
                .sourceType(entity.getSourceType())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .collectionId(col != null ? col.getId() : null)
                .collectionName(col != null ? col.getName() : null)
                .extractedText(extractedText)
                .build();
    }
}
