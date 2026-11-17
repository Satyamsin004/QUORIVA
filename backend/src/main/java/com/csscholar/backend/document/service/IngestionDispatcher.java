package com.csscholar.backend.document.service;

import com.csscholar.backend.document.entity.IngestionOutbox;
import com.csscholar.backend.document.event.DocumentUploadedEvent;
import com.csscholar.backend.document.repository.IngestionOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.List;

@Service
public class IngestionDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(IngestionDispatcher.class);
    private static final int MAX_RETRIES = 5;

    private final IngestionOutboxRepository outboxRepository;
    private final IngestionClient ingestionClient;

    public IngestionDispatcher(IngestionOutboxRepository outboxRepository, IngestionClient ingestionClient) {
        this.outboxRepository = outboxRepository;
        this.ingestionClient = ingestionClient;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDocumentUploaded(DocumentUploadedEvent event) {
        logger.info("Received DocumentUploadedEvent for document: {}, outbox: {}",
                event.documentId(), event.outboxId());
        dispatchOutboxEntry(event.outboxId());
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    @Transactional
    public void pollPendingOutboxEvents() {
        List<IngestionOutbox> pendingList = outboxRepository.findPendingEvents("PENDING", PageRequest.of(0, 10));
        for (IngestionOutbox outbox : pendingList) {
            try {
                dispatchOutboxEntry(outbox.getId());
            } catch (Exception e) {
                logger.error("Error during scheduled dispatch for outbox entry {}: {}", outbox.getId(), e.getMessage());
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatchOutboxEntry(java.util.UUID outboxId) {
        int claimed = outboxRepository.claimOutboxForDispatch(outboxId);
        if (claimed == 0) {
            logger.debug("Outbox record {} was already claimed or is not pending, skipping dispatch.", outboxId);
            return;
        }

        outboxRepository.findById(outboxId).ifPresent(outbox -> {
            boolean success = ingestionClient.triggerIngestion(outbox.getPayload());
            if (success) {
                outbox.setStatus("DISPATCHED");
                outbox.setDispatchedAt(Instant.now());
                outbox.setLastError(null);
                outboxRepository.save(outbox);
                logger.info("Successfully dispatched outbox record: {}", outboxId);
            } else {
                int retries = outbox.getRetryCount() + 1;
                outbox.setRetryCount(retries);
                outbox.setLastError("Failed to dispatch to AI Service at " + Instant.now());
                if (retries >= MAX_RETRIES) {
                    outbox.setStatus("FAILED");
                    logger.warn("Outbox record {} reached max retries ({}), marked as FAILED.", outboxId, MAX_RETRIES);
                } else {
                    outbox.setStatus("PENDING");
                }
                outboxRepository.save(outbox);
            }
        });
    }
}

