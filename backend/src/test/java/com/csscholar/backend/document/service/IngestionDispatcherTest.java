package com.csscholar.backend.document.service;

import com.csscholar.backend.document.entity.Document;
import com.csscholar.backend.document.entity.IngestionOutbox;
import com.csscholar.backend.document.enums.FileType;
import com.csscholar.backend.document.event.DocumentUploadedEvent;
import com.csscholar.backend.document.repository.IngestionOutboxRepository;
import com.csscholar.backend.user.Role;
import com.csscholar.backend.user.User;
import com.csscholar.backend.workspace.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestionDispatcherTest {

    @Mock
    private IngestionOutboxRepository outboxRepository;

    @Mock
    private IngestionClient ingestionClient;

    private IngestionDispatcher dispatcher;
    private Document testDocument;
    private IngestionOutbox outbox;
    private UUID outboxId;

    @BeforeEach
    void setUp() {
        dispatcher = new IngestionDispatcher(outboxRepository, ingestionClient);

        User user = new User("Test User", "test@example.com", "hash", Role.STUDENT);
        user.setId(UUID.randomUUID());

        Workspace workspace = new Workspace("Test WS", "desc", user);
        workspace.setId(UUID.randomUUID());

        testDocument = new Document(user, workspace, "Title", "doc.pdf", "workspaces/1/doc.pdf",
                FileType.PDF, "application/pdf", 1024L, null, null);
        testDocument.setId(UUID.randomUUID());

        outboxId = UUID.randomUUID();
        outbox = new IngestionOutbox(testDocument, "INGESTION_REQUESTED", "{\"document_id\":\"" + testDocument.getId() + "\"}");
        outbox.setId(outboxId);
    }

    @Test
    @DisplayName("Should mark outbox as DISPATCHED when IngestionClient succeeds")
    void dispatchOutboxEntry_Success() {
        when(outboxRepository.findById(outboxId)).thenReturn(Optional.of(outbox));
        when(ingestionClient.triggerIngestion(outbox.getPayload())).thenReturn(true);

        dispatcher.dispatchOutboxEntry(outboxId);

        assertThat(outbox.getStatus()).isEqualTo("DISPATCHED");
        assertThat(outbox.getDispatchedAt()).isNotNull();
        verify(outboxRepository).save(outbox);
    }

    @Test
    @DisplayName("Should increment retryCount and keep status PENDING when IngestionClient fails and retries < MAX")
    void dispatchOutboxEntry_Retry() {
        when(outboxRepository.findById(outboxId)).thenReturn(Optional.of(outbox));
        when(ingestionClient.triggerIngestion(outbox.getPayload())).thenReturn(false);

        dispatcher.dispatchOutboxEntry(outboxId);

        assertThat(outbox.getStatus()).isEqualTo("PENDING");
        assertThat(outbox.getRetryCount()).isEqualTo(1);
        assertThat(outbox.getLastError()).isNotNull();
        verify(outboxRepository).save(outbox);
    }

    @Test
    @DisplayName("Should mark outbox as FAILED when retryCount reaches maximum")
    void dispatchOutboxEntry_MaxRetriesReached_MarksFailed() {
        outbox.setRetryCount(4); // next failure makes it 5 (MAX_RETRIES)
        when(outboxRepository.findById(outboxId)).thenReturn(Optional.of(outbox));
        when(ingestionClient.triggerIngestion(outbox.getPayload())).thenReturn(false);

        dispatcher.dispatchOutboxEntry(outboxId);

        assertThat(outbox.getStatus()).isEqualTo("FAILED");
        assertThat(outbox.getRetryCount()).isEqualTo(5);
        verify(outboxRepository).save(outbox);
    }

    @Test
    @DisplayName("Should dispatch on DocumentUploadedEvent")
    void onDocumentUploaded_DispatchesEvent() {
        when(outboxRepository.findById(outboxId)).thenReturn(Optional.of(outbox));
        when(ingestionClient.triggerIngestion(any())).thenReturn(true);

        dispatcher.onDocumentUploaded(new DocumentUploadedEvent(testDocument.getId(), outboxId));

        assertThat(outbox.getStatus()).isEqualTo("DISPATCHED");
    }
}
