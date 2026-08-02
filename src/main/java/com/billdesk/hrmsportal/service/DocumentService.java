package com.billdesk.hrmsportal.service;

import com.billdesk.hrmsportal.dto.response.DocumentResponse;
import com.billdesk.hrmsportal.dto.response.DocumentStatusResponse;
import com.billdesk.hrmsportal.entity.enums.DocumentType;
import com.billdesk.hrmsportal.entity.enums.Role;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    DocumentResponse upload(MultipartFile file, DocumentType documentType,
                            Long targetEmployeeId, Long callerId, Role callerRole);

    List<DocumentResponse> getMyDocuments(Long callerId);

    List<DocumentResponse> getByEmployee(Long employeeId, Long callerId, Role callerRole);

    List<DocumentStatusResponse> getTeamStatus(Long callerId, Role callerRole);

    List<DocumentResponse> getAll(Role callerRole);

    DownloadPayload download(Long documentId, Long callerId, Role callerRole);

    void delete(Long documentId, Role callerRole);

    record DownloadPayload(Resource resource, String filename, String contentType) {}
}