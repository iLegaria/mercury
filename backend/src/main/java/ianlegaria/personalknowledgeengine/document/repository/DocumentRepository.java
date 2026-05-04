package ianlegaria.personalknowledgeengine.document.repository;

import ianlegaria.personalknowledgeengine.document.entity.DocumentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {

    @Query(value = "SELECT d FROM DocumentEntity d WHERE d.user.id = :userId",
           countQuery = "SELECT COUNT(d) FROM DocumentEntity d WHERE d.user.id = :userId")
    Page<DocumentEntity> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query(value = "SELECT d FROM DocumentEntity d WHERE d.user.id = :userId AND d.collection.id = :collectionId",
           countQuery = "SELECT COUNT(d) FROM DocumentEntity d WHERE d.user.id = :userId AND d.collection.id = :collectionId")
    Page<DocumentEntity> findByUserIdAndCollectionId(@Param("userId") UUID userId, @Param("collectionId") UUID collectionId, Pageable pageable);

    List<DocumentEntity> findByStatus(String status);
}
