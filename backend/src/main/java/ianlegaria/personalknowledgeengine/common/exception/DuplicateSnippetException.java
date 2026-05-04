package ianlegaria.personalknowledgeengine.common.exception;

import java.util.UUID;

public class DuplicateSnippetException extends DuplicateResourceException {

    private final UUID existingSnippetId;

    public DuplicateSnippetException(UUID existingSnippetId) {
        super("A similar snippet already exists");
        this.existingSnippetId = existingSnippetId;
    }

    public UUID getExistingSnippetId() {
        return existingSnippetId;
    }
}
