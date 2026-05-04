package ianlegaria.personalknowledgeengine.common.exception;

import java.util.UUID;

public class DocumentNotPendingException extends RuntimeException {
    public DocumentNotPendingException(UUID documentId, String status) {
        super("Document " + documentId + " is not PENDING (status=" + status + "), skipping re-ingestion");
    }
}
