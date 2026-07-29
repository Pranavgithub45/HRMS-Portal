package com.billdesk.hrmsportal.repository;

import com.billdesk.hrmsportal.entity.Document;
import com.billdesk.hrmsportal.entity.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    // Employee — own documents
    List<Document> findByEmployee_EmployeeId(Long employeeId);

    // Replace-on-reupload lookup (unique key: employee_id + document_type)
    Optional<Document> findByEmployee_EmployeeIdAndDocumentType(Long employeeId, DocumentType documentType);

    boolean existsByEmployee_EmployeeId(Long employeeId);

    boolean existsByEmployee_EmployeeIdAndDocumentType(Long employeeId, DocumentType documentType);

    // Team / HR document-status diff (Phase 6 & 8)
    List<Document> findByEmployee_EmployeeIdIn(List<Long> employeeIds);

    long countByDocumentType(DocumentType documentType);
}