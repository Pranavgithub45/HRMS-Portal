package com.billdesk.hrmsportal.dto.response;

import com.billdesk.hrmsportal.entity.enums.DocumentType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor
public class DocumentResponse {

    private Long documentId;
    private DocumentType documentType;
    private String documentName;
    private String fileFormat;
    private LocalDate uploadedDate;
    private String downloadUrl;      // API path, never a filesystem path

    private Long employeeId;
    private String employeeName;

    private Long uploadedById;
    private String uploadedByName;

    // fileUrl deliberately absent — exposing the server path is an information leak
}