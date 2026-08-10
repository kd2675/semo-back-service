package semo.back.service.feature.attachment.biz;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import semo.back.service.common.util.AttachmentFinalizeClient;
import semo.back.service.database.pub.repository.ResourceAttachmentRepository;

@Component
@RequiredArgsConstructor
public class ResourceAttachmentOrphanReconciler {
    private static final Logger logger = LoggerFactory.getLogger(ResourceAttachmentOrphanReconciler.class);

    private final ResourceAttachmentRepository resourceAttachmentRepository;
    private final AttachmentFinalizeClient attachmentFinalizeClient;

    @Scheduled(
            initialDelayString = "${integration.image.orphan-reconcile-initial-delay-ms:120000}",
            fixedDelayString = "${integration.image.orphan-reconcile-delay-ms:900000}"
    )
    public void reconcilePendingFinalizedAttachments() {
        final var pendingAttachments = getPendingAttachmentsSafely();
        int confirmedCount = 0;
        int deletedCount = 0;
        for (var pending : pendingAttachments) {
            try {
                if (resourceAttachmentRepository.existsByFileName(pending.fileName())) {
                    attachmentFinalizeClient.confirmFinalizedAttachment(pending.fileName());
                    confirmedCount++;
                } else {
                    attachmentFinalizeClient.deleteFinalizedAttachment(pending.fileName());
                    deletedCount++;
                }
            } catch (RuntimeException exception) {
                logger.warn("Failed to reconcile pending attachment: {}", pending.fileName(), exception);
            }
        }
        if (!pendingAttachments.isEmpty()) {
            logger.info(
                    "Reconciled pending attachments. total={}, confirmed={}, deleted={}",
                    pendingAttachments.size(),
                    confirmedCount,
                    deletedCount
            );
        }
    }

    private List<AttachmentFinalizeClient.PendingFinalizedAttachment> getPendingAttachmentsSafely() {
        try {
            return attachmentFinalizeClient.getPendingFinalizedAttachments();
        } catch (RuntimeException exception) {
            logger.warn("Failed to load pending attachments for reconciliation", exception);
            return List.of();
        }
    }
}
