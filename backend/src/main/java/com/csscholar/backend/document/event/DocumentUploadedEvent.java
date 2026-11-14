package com.csscholar.backend.document.event;

import java.util.UUID;

public record DocumentUploadedEvent(UUID documentId, UUID outboxId) {
}

