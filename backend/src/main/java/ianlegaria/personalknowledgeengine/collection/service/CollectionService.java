package ianlegaria.personalknowledgeengine.collection.service;

import ianlegaria.personalknowledgeengine.collection.dto.CollectionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CollectionService {

    CollectionResponse createCollection(UUID userId, String name);

    Page<CollectionResponse> getCollectionsByUser(UUID userId, Pageable pageable);

    void deleteCollection(UUID userId, UUID collectionId);
}
