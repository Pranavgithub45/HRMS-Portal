package com.billdesk.hrmsportal.controller;

import com.billdesk.hrmsportal.dto.response.DocumentResponse;
import com.billdesk.hrmsportal.dto.response.DocumentStatusResponse;
import com.billdesk.hrmsportal.entity.enums.DocumentType;
import com.billdesk.hrmsportal.entity.enums.Role;
import com.billdesk.hrmsportal.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /** Employee uploads/replaces their own document. */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<DocumentResponse> uploadOwn(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") DocumentType documentType,
            @RequestAttribute("employeeId") Long callerId,
            @RequestAttribute("role") Role callerRole) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.upload(file, documentType, null, callerId, callerRole));
    }

    /** HR uploads on behalf of any employee. */
    @PostMapping(value = "/{employeeId}", consumes = "multipart/form-data")
    public ResponseEntity<DocumentResponse> uploadFor(
            @PathVariable Long employeeId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") DocumentType documentType,
            @RequestAttribute("employeeId") Long callerId,
            @RequestAttribute("role") Role callerRole) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.upload(file, documentType, employeeId, callerId, callerRole));
    }

    @GetMapping("/me")
    public List<DocumentResponse> myDocuments(@RequestAttribute("employeeId") Long callerId) {
        return documentService.getMyDocuments(callerId);
    }

    @GetMapping("/team")
    public List<DocumentStatusResponse> teamStatus(@RequestAttribute("employeeId") Long callerId,
                                                   @RequestAttribute("role") Role callerRole) {
        return documentService.getTeamStatus(callerId, callerRole);
    }

    @GetMapping
    public List<DocumentResponse> getAll(@RequestAttribute("role") Role callerRole) {
        return documentService.getAll(callerRole);
    }

    @GetMapping("/download/{documentId}")
    public ResponseEntity<org.springframework.core.io.Resource> download(
            @PathVariable Long documentId,
            @RequestAttribute("employeeId") Long callerId,
            @RequestAttribute("role") Role callerRole) {

        DocumentService.DownloadPayload payload =
                documentService.download(documentId, callerId, callerRole);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, payload.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + payload.filename() + "\"")
                .body(payload.resource());
    }

    @GetMapping("/{employeeId}")
    public List<DocumentResponse> getByEmployee(@PathVariable Long employeeId,
                                                @RequestAttribute("employeeId") Long callerId,
                                                @RequestAttribute("role") Role callerRole) {
        return documentService.getByEmployee(employeeId, callerId, callerRole);
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long documentId,
                                                      @RequestAttribute("role") Role callerRole) {
        documentService.delete(documentId, callerRole);
        return ResponseEntity.ok(Map.of("message", "Document deleted successfully"));
    }
}