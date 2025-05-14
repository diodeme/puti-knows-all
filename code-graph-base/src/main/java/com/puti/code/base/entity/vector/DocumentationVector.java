package com.puti.code.base.entity.vector;

import com.puti.code.base.enums.DocumentType;
import lombok.Data;

@Data
public class DocumentationVector {
    private String documentId;
    private String content;
    private DocumentType documentType;
    private float[] textDense;

    private String projectId;
    private String branchName;
}
