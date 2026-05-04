package ianlegaria.personalknowledgeengine.document;

import ianlegaria.personalknowledgeengine.chunk.repository.ChunkRepository;
import ianlegaria.personalknowledgeengine.chunk.service.ChunkingService;
import ianlegaria.personalknowledgeengine.chunk.service.EmbeddingService;
import ianlegaria.personalknowledgeengine.document.repository.DocumentContentRepository;
import ianlegaria.personalknowledgeengine.document.repository.DocumentRepository;
import ianlegaria.personalknowledgeengine.common.config.ChunkingProperties;
import ianlegaria.personalknowledgeengine.document.service.IngestionPersistenceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class IngestionPersistenceServiceTest {

    @Mock DocumentRepository documentRepository;
    @Mock DocumentContentRepository documentContentRepository;
    @Mock ChunkRepository chunkRepository;
    @Mock EmbeddingService embeddingService;
    @Mock RabbitTemplate rabbitTemplate;
    @Mock ChunkingService chunkingService;

    private IngestionPersistenceServiceImpl persistenceService;

    @BeforeEach
    void setUp() {
        persistenceService = new IngestionPersistenceServiceImpl(
                documentRepository, documentContentRepository, chunkRepository,
                embeddingService, rabbitTemplate, chunkingService, new ChunkingProperties());
    }
}
