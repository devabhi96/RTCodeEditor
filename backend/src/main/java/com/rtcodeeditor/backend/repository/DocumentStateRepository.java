package com.rtcodeeditor.backend.repository;

import com.rtcodeeditor.backend.model.DocumentState;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentStateRepository extends JpaRepository<DocumentState, Long> {
    List<DocumentState> findAllByDocumentIdOrderByIdAsc(String documentId);

    @Modifying
    @Query(value = "INSERT INTO document_updates (document_id, state_update, created_at) VALUES (:documentId, :stateUpdate, CURRENT_TIMESTAMP)", nativeQuery = true)
    void appendUpdate(@Param("documentId") String documentId, @Param("stateUpdate") byte[] stateUpdate);
}
