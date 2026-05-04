package ianlegaria.personalknowledgeengine.collection.repository;

import ianlegaria.personalknowledgeengine.collection.entity.CollectionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CollectionRepository extends JpaRepository<CollectionEntity, UUID> {

    Page<CollectionEntity> findByUserId(UUID userId, Pageable pageable);

    boolean existsByUserIdAndName(UUID userId, String name);

    Optional<CollectionEntity> findByIdAndUserId(UUID id, UUID userId);
}
