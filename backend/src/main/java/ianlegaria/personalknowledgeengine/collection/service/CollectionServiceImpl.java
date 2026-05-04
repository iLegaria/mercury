package ianlegaria.personalknowledgeengine.collection.service;

import ianlegaria.personalknowledgeengine.collection.dto.CollectionResponse;
import ianlegaria.personalknowledgeengine.collection.entity.CollectionEntity;
import ianlegaria.personalknowledgeengine.collection.repository.CollectionRepository;
import ianlegaria.personalknowledgeengine.common.exception.DuplicateResourceException;
import ianlegaria.personalknowledgeengine.common.exception.ResourceNotFoundException;
import ianlegaria.personalknowledgeengine.user.entity.UserEntity;
import ianlegaria.personalknowledgeengine.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionServiceImpl implements CollectionService {

    private final CollectionRepository collectionRepository;
    private final UserRepository userRepository;

    @Transactional
    public CollectionResponse createCollection(UUID userId, String name) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        String trimmedName = name.trim();
        if (collectionRepository.existsByUserIdAndName(userId, trimmedName)) {
            throw new DuplicateResourceException("Collection '" + trimmedName + "' already exists for this user");
        }

        CollectionEntity collection = CollectionEntity.builder()
                .user(user)
                .name(trimmedName)
                .build();

        CollectionEntity saved = collectionRepository.save(collection);
        log.info("Created collection '{}' for user {}", trimmedName, userId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<CollectionResponse> getCollectionsByUser(UUID userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return collectionRepository.findByUserId(userId, pageable).map(this::toResponse);
    }

    @Transactional
    public void deleteCollection(UUID userId, UUID collectionId) {
        CollectionEntity collection = collectionRepository.findByIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + collectionId));

        collectionRepository.delete(collection);
        log.info("Deleted collection {} for user {}", collectionId, userId);
    }

    private CollectionResponse toResponse(CollectionEntity entity) {
        return CollectionResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .name(entity.getName())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
