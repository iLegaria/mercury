package ianlegaria.personalknowledgeengine.document;

import ianlegaria.personalknowledgeengine.AbstractIntegrationTest;
import ianlegaria.personalknowledgeengine.collection.service.CollectionService;
import ianlegaria.personalknowledgeengine.document.dto.DocumentResponse;
import ianlegaria.personalknowledgeengine.document.repository.DocumentRepository;
import ianlegaria.personalknowledgeengine.document.service.DocumentService;
import ianlegaria.personalknowledgeengine.user.dto.CreateUserRequest;
import ianlegaria.personalknowledgeengine.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired DocumentService documentService;
    @Autowired DocumentRepository documentRepository;
    @Autowired UserService userService;
    @Autowired CollectionService collectionService;
    @MockBean RabbitTemplate rabbitTemplate;

    private UUID userId;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        userId = userService.createUser(new CreateUserRequest(unique + "@test.com", "Test User")).getId();
    }

    @Test
    void uploadDocument_savesFileAndReturnsResponse() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "hello.txt", "text/plain", "Hello world".getBytes());

        DocumentResponse response = documentService.uploadDocument(userId, "My Doc", file, null);

        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(documentRepository.existsById(response.getId())).isTrue();
        String savedPath = documentRepository.findById(response.getId()).orElseThrow().getFilePath();
        assertThat(Files.exists(Path.of(savedPath))).isTrue();
    }

    @Test
    void deleteDocument_removesFromDbAndDisk() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "delete-me.txt", "text/plain", "content".getBytes());
        DocumentResponse doc = documentService.uploadDocument(userId, "To Delete", file, null);
        String filePath = documentRepository.findById(doc.getId()).orElseThrow().getFilePath();

        documentService.deleteDocument(doc.getId(), userId);

        assertThat(documentRepository.existsById(doc.getId())).isFalse();
        assertThat(Files.exists(Path.of(filePath))).isFalse();
    }

    @Test
    void getDocumentsByUser_filtersByCollection() {
        UUID col1Id = collectionService.createCollection(userId, "Col1").getId();
        UUID col2Id = collectionService.createCollection(userId, "Col2").getId();

        MockMultipartFile file = new MockMultipartFile(
                "file", "f.txt", "text/plain", "x".getBytes());

        documentService.uploadDocument(userId, "Doc A", file, col1Id);
        documentService.uploadDocument(userId, "Doc B", file, col1Id);
        documentService.uploadDocument(userId, "Doc C", file, col2Id);

        assertThat(documentService.getDocumentsByUser(userId, Optional.of(col1Id), Pageable.unpaged()).getTotalElements()).isEqualTo(2);
        assertThat(documentService.getDocumentsByUser(userId, Optional.of(col2Id), Pageable.unpaged()).getTotalElements()).isEqualTo(1);
        assertThat(documentService.getDocumentsByUser(userId, Optional.empty(), Pageable.unpaged()).getTotalElements()).isEqualTo(3);
    }
}
