package semo.back.service.feature.attachment.biz;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import semo.back.service.common.util.AttachmentFinalizeClient;
import semo.back.service.common.util.AttachmentFinalizeClient.PendingFinalizedAttachment;
import semo.back.service.database.pub.repository.ResourceAttachmentRepository;

@ExtendWith(MockitoExtension.class)
class ResourceAttachmentOrphanReconcilerTest {
    @Mock
    private ResourceAttachmentRepository resourceAttachmentRepository;
    @Mock
    private AttachmentFinalizeClient attachmentFinalizeClient;

    @InjectMocks
    private ResourceAttachmentOrphanReconciler resourceAttachmentOrphanReconciler;

    @Test
    void reconcilePendingFinalizedAttachments_registeredFile_confirmsClaim() {
        PendingFinalizedAttachment pending = pending("semo/attachments/feedback/31/file.pdf");
        when(attachmentFinalizeClient.getPendingFinalizedAttachments()).thenReturn(List.of(pending));
        when(resourceAttachmentRepository.existsByFileName(pending.fileName())).thenReturn(true);

        resourceAttachmentOrphanReconciler.reconcilePendingFinalizedAttachments();

        verify(attachmentFinalizeClient).confirmFinalizedAttachment(pending.fileName());
    }

    @Test
    void reconcilePendingFinalizedAttachments_unregisteredFile_deletesOrphan() {
        PendingFinalizedAttachment pending = pending("semo/attachments/feedback/31/file.pdf");
        when(attachmentFinalizeClient.getPendingFinalizedAttachments()).thenReturn(List.of(pending));
        when(resourceAttachmentRepository.existsByFileName(pending.fileName())).thenReturn(false);

        resourceAttachmentOrphanReconciler.reconcilePendingFinalizedAttachments();

        verify(attachmentFinalizeClient).deleteFinalizedAttachment(pending.fileName());
    }

    @Test
    void reconcilePendingFinalizedAttachments_imageServerUnavailable_skipsCycle() {
        when(attachmentFinalizeClient.getPendingFinalizedAttachments())
                .thenThrow(new IllegalStateException("image server unavailable"));

        assertThatCode(resourceAttachmentOrphanReconciler::reconcilePendingFinalizedAttachments)
                .doesNotThrowAnyException();
    }

    private PendingFinalizedAttachment pending(String fileName) {
        return new PendingFinalizedAttachment(fileName, LocalDateTime.now().minusHours(2));
    }
}
