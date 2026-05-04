package ianlegaria.personalknowledgeengine.snippet.repository;

import ianlegaria.personalknowledgeengine.snippet.entity.SnippetEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SnippetRepository extends JpaRepository<SnippetEntity, UUID> {

    List<SnippetEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<SnippetEntity> findByUserId(UUID userId, Pageable pageable);

    Optional<SnippetEntity> findByIdAndUserId(UUID id, UUID userId);

    List<SnippetEntity> findByIdInAndUserIdOrderByCreatedAtAsc(List<UUID> ids, UUID userId);
}
