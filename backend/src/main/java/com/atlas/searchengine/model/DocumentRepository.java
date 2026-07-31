package com.atlas.searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {
    List<Document> findByCollection(String collection);
    org.springframework.data.domain.Page<Document> findByCollection(String collection, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = "DELETE FROM documents WHERE id IN (SELECT id FROM documents ORDER BY crawled_at ASC LIMIT :limit)", nativeQuery = true)
    void deleteOldestDocuments(@org.springframework.data.repository.query.Param("limit") int limit);

    @org.springframework.data.jpa.repository.Query("SELECT d FROM Document d WHERE " +
            "LOWER(d.title) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
            "LOWER(d.text) LIKE LOWER(CONCAT('%', :term, '%'))")
    org.springframework.data.domain.Page<Document> searchDatabaseFallback(@org.springframework.data.repository.query.Param("term") String term, org.springframework.data.domain.Pageable pageable);
}
