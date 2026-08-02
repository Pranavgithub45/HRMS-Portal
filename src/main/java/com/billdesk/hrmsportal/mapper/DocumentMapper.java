package com.billdesk.hrmsportal.mapper;

import com.billdesk.hrmsportal.dto.response.DocumentResponse;
import com.billdesk.hrmsportal.entity.Document;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(target = "employeeId",     source = "employee.employeeId")
    @Mapping(target = "employeeName",   source = "employee.name")
    @Mapping(target = "uploadedById",   source = "uploadedBy.employeeId")
    @Mapping(target = "uploadedByName", source = "uploadedBy.name")
    @Mapping(target = "downloadUrl",    ignore = true)
    DocumentResponse toResponse(Document document);

    List<DocumentResponse> toResponseList(List<Document> documents);

    @AfterMapping
    default void addDownloadUrl(Document document, @MappingTarget DocumentResponse response) {
        response.setDownloadUrl("/api/documents/download/" + document.getDocumentId());
    }
}