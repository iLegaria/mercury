package ianlegaria.personalknowledgeengine.document.repository;

import ianlegaria.personalknowledgeengine.document.entity.DocumentContentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentContentRepository extends JpaRepository<DocumentContentEntity, UUID> {

    Optional<DocumentContentEntity> findByDocumentId(UUID documentId);
}
