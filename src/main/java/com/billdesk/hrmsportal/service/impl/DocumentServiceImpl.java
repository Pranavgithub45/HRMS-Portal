package com.billdesk.hrmsportal.service.impl;

import com.billdesk.hrmsportal.dto.response.DocumentResponse;
import com.billdesk.hrmsportal.dto.response.DocumentStatusResponse;
import com.billdesk.hrmsportal.entity.Document;
import com.billdesk.hrmsportal.entity.Employee;
import com.billdesk.hrmsportal.entity.enums.DocumentType;
import com.billdesk.hrmsportal.entity.enums.Role;
import com.billdesk.hrmsportal.mapper.DocumentMapper;
import com.billdesk.hrmsportal.repository.DocumentRepository;
import com.billdesk.hrmsportal.repository.EmployeeRepository;
import com.billdesk.hrmsportal.service.DocumentService;
import com.billdesk.hrmsportal.util.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final EmployeeRepository employeeRepository;
    private final DocumentMapper documentMapper;
    private final FileStorageService fileStorageService;

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "PDF", "application/pdf",
            "JPG", "image/jpeg",
            "PNG", "image/png"
    );

    // ---------------- UPLOAD ----------------

    @Override
    @Transactional
    public DocumentResponse upload(MultipartFile file, DocumentType documentType,
                                   Long targetEmployeeId, Long callerId, Role callerRole) {

        Long ownerId = targetEmployeeId != null ? targetEmployeeId : callerId;

        // only HR may upload on someone else's behalf
        if (!ownerId.equals(callerId) && callerRole != Role.HR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied. Only HR can upload documents for other employees");
        }

        Employee owner = findEmployeeOrThrow(ownerId);
        Employee uploader = ownerId.equals(callerId) ? owner : findEmployeeOrThrow(callerId);

        // 1. write the NEW file first (unique name, so nothing is overwritten)
        String storedName = fileStorageService.store(file, ownerId, documentType.name());

        Optional<Document> existing =
                documentRepository.findByEmployee_EmployeeIdAndDocumentType(ownerId, documentType);

        Document document = existing.orElseGet(Document::new);
        String oldStoredName = existing.map(Document::getFileUrl).orElse(null);

        document.setEmployee(owner);
        document.setDocumentType(documentType);
        document.setDocumentName(fileStorageService.sanitizeDisplayName(file.getOriginalFilename()));
        document.setFileUrl(storedName);
        document.setFileFormat(storedName.substring(storedName.lastIndexOf('.') + 1).toUpperCase());
        document.setUploadedDate(java.time.LocalDate.now());
        document.setUploadedBy(uploader);

        Document saved;
        try {
            saved = documentRepository.save(document);
        } catch (RuntimeException e) {
            fileStorageService.deleteQuietly(storedName);   // no orphan on DB failure
            throw e;
        }

        // 2. delete the OLD file only after the transaction commits
        if (oldStoredName != null && !oldStoredName.equals(storedName)) {
            registerAfterCommitDelete(oldStoredName);
        }

        return documentMapper.toResponse(saved);
    }

    // ---------------- READ ----------------

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getMyDocuments(Long callerId) {
        return documentMapper.toResponseList(
                documentRepository.findByEmployee_EmployeeId(callerId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getByEmployee(Long employeeId, Long callerId, Role callerRole) {

        Employee target = findEmployeeOrThrow(employeeId);
        requireCanView(target, callerId, callerRole);

        return documentMapper.toResponseList(
                documentRepository.findByEmployee_EmployeeId(employeeId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentStatusResponse> getTeamStatus(Long callerId, Role callerRole) {

        if (callerRole == Role.EMPLOYEE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied. MANAGER or HR role required");
        }

        List<Employee> team = employeeRepository.findByManager_EmployeeId(callerId);
        return buildStatusList(team);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getAll(Role callerRole) {
        requireHr(callerRole);
        return documentMapper.toResponseList(documentRepository.findAll());
    }

    // ---------------- DOWNLOAD ----------------

    @Override
    @Transactional(readOnly = true)
    public DownloadPayload download(Long documentId, Long callerId, Role callerRole) {

        Document document = findDocumentOrThrow(documentId);
        requireCanView(document.getEmployee(), callerId, callerRole);

        return new DownloadPayload(
                fileStorageService.load(document.getFileUrl()),
                document.getDocumentName(),
                CONTENT_TYPES.getOrDefault(document.getFileFormat(), "application/octet-stream")
        );
    }

    // ---------------- DELETE ----------------

    @Override
    @Transactional
    public void delete(Long documentId, Role callerRole) {

        requireHr(callerRole);

        Document document = findDocumentOrThrow(documentId);
        String storedName = document.getFileUrl();

        documentRepository.delete(document);
        registerAfterCommitDelete(storedName);
    }

    // ---------------- HELPERS ----------------

    /** Diff team members against uploaded documents — findByEmployeeId alone can't show who's MISSING one. */
    private List<DocumentStatusResponse> buildStatusList(List<Employee> employees) {

        if (employees.isEmpty()) return List.of();

        List<Long> ids = employees.stream().map(Employee::getEmployeeId).toList();
        List<Document> documents = documentRepository.findByEmployee_EmployeeIdIn(ids);

        List<DocumentStatusResponse> result = new ArrayList<>();

        for (Employee employee : employees) {
            Optional<Document> doc = documents.stream()
                    .filter(d -> d.getEmployee().getEmployeeId().equals(employee.getEmployeeId()))
                    .findFirst();

            result.add(new DocumentStatusResponse(
                    employee.getEmployeeId(),
                    employee.getName(),
                    doc.isPresent(),
                    doc.map(d -> d.getDocumentType().name()).orElse(null),
                    doc.map(Document::getUploadedDate).orElse(null)
            ));
        }
        return result;
    }

    /** File deletion is not transactional — defer it until the DB commit succeeds. */
    private void registerAfterCommitDelete(String storedName) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            fileStorageService.deleteQuietly(storedName);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                fileStorageService.deleteQuietly(storedName);
            }
        });
    }

    private void requireCanView(Employee target, Long callerId, Role callerRole) {

        if (callerRole == Role.HR) return;
        if (target.getEmployeeId().equals(callerId)) return;

        boolean ownReport = callerRole == Role.MANAGER
                && target.getManager() != null
                && target.getManager().getEmployeeId().equals(callerId);

        if (!ownReport) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied. You can only view your own documents or those of your team");
        }
    }

    private Employee findEmployeeOrThrow(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Employee not found with id: " + employeeId));
    }

    private Document findDocumentOrThrow(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Document not found with id: " + documentId));
    }

    private void requireHr(Role callerRole) {
        if (callerRole != Role.HR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. HR role required");
        }
    }
}