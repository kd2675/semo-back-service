package semo.back.service.feature.attachment.biz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import semo.back.service.common.exception.SemoException;
import semo.back.service.common.util.AttachmentFileUrlResolver;
import semo.back.service.common.util.AttachmentFinalizeClient;
import semo.back.service.common.util.AttachmentFinalizeClient.FinalizedAttachment;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.ResourceAttachment;
import semo.back.service.database.pub.repository.ResourceAttachmentRepository;
import semo.back.service.feature.attachment.vo.CreateResourceAttachmentRequest;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;

@ExtendWith(MockitoExtension.class)
class ResourceAttachmentServiceTest {
    @Mock
    private ClubAccessResolver clubAccessResolver;
    @Mock
    private ResourceAttachmentPolicy resourceAttachmentPolicy;
    @Mock
    private ResourceAttachmentRepository resourceAttachmentRepository;
    @Mock
    private AttachmentFinalizeClient attachmentFinalizeClient;
    @Mock
    private AttachmentFileUrlResolver attachmentFileUrlResolver;

    @InjectMocks
    private ResourceAttachmentService resourceAttachmentService;

    @Test
    void createAttachment_authorizedResource_persistsFinalizedServerMetadata() {
        ClubAccessResolver.ClubAccess access = access(11L);
        when(clubAccessResolver.requireActiveMember(1L, "user-key")).thenReturn(access);
        when(resourceAttachmentPolicy.requireCanAttachForUpdate(access, "FEEDBACK", 31L))
                .thenReturn("OWNER_AND_ADMIN");
        when(resourceAttachmentRepository.countByClubIdAndResourceTypeAndResourceIdAndDeletedFalse(
                1L,
                "FEEDBACK",
                31L
        )).thenReturn(0L);
        when(attachmentFinalizeClient.finalizeAttachment(
                "temp/files/2026/08/10/file.pdf",
                "semo/attachments/feedback/31"
        )).thenReturn(finalized());
        when(resourceAttachmentRepository.findByFileNameAndDeletedFalse(finalized().fileName()))
                .thenReturn(Optional.empty());
        when(resourceAttachmentRepository.save(any(ResourceAttachment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(attachmentFileUrlResolver.resolveDownloadUrl(finalized().fileName(), "운영 계획.pdf"))
                .thenReturn("http://localhost:8081/files/final.pdf?downloadName=x");

        var response = resourceAttachmentService.createAttachment(
                1L,
                "user-key",
                new CreateResourceAttachmentRequest(
                        "feedback",
                        31L,
                        "temp/files/2026/08/10/file.pdf",
                        "C:\\fakepath\\운영 계획.pdf"
                )
        );

        assertThat(response)
                .returns("운영 계획.pdf", item -> item.originalFileName())
                .returns("application/pdf", item -> item.contentType())
                .returns(128L, item -> item.sizeBytes())
                .returns("OWNER_AND_ADMIN", item -> item.visibilityScope());
    }

    @Test
    void createAttachment_resourceAtLimit_rejectsBeforeFileFinalization() {
        ClubAccessResolver.ClubAccess access = mock(ClubAccessResolver.ClubAccess.class);
        when(clubAccessResolver.requireActiveMember(1L, "user-key")).thenReturn(access);
        when(resourceAttachmentPolicy.requireCanAttachForUpdate(access, "TODO_ITEM", 21L)).thenReturn("CLUB");
        when(resourceAttachmentRepository.countByClubIdAndResourceTypeAndResourceIdAndDeletedFalse(
                1L,
                "TODO_ITEM",
                21L
        )).thenReturn(10L);

        assertThatThrownBy(() -> resourceAttachmentService.createAttachment(
                1L,
                "user-key",
                new CreateResourceAttachmentRequest(
                        "TODO_ITEM",
                        21L,
                        "temp/files/2026/08/10/file.pdf",
                        "운영 계획.pdf"
                )
        )).isInstanceOf(SemoException.ValidationException.class);
        verify(attachmentFinalizeClient, never()).finalizeAttachment(any(), any());
    }

    @Test
    void createAttachment_sameFinalFileRetryAtLimit_returnsExistingBeforeCountAndFinalize() {
        ClubAccessResolver.ClubAccess access = access(11L);
        ResourceAttachment existing = ResourceAttachment.builder()
                .resourceAttachmentId(91L)
                .clubId(1L)
                .resourceType("FEEDBACK")
                .resourceId(31L)
                .uploaderClubProfileId(11L)
                .fileName("semo/attachments/feedback/31/2026/08/10/file.pdf")
                .originalFileName("운영 계획.pdf")
                .contentType("application/pdf")
                .sizeBytes(128L)
                .visibilityScope("OWNER_AND_ADMIN")
                .deleted(false)
                .build();
        when(clubAccessResolver.requireActiveMember(1L, "user-key")).thenReturn(access);
        when(resourceAttachmentRepository.findByFileNameAndDeletedFalse(existing.getFileName()))
                .thenReturn(Optional.of(existing));
        when(attachmentFileUrlResolver.resolveDownloadUrl(existing.getFileName(), existing.getOriginalFileName()))
                .thenReturn("http://localhost:8081/files/final.pdf?downloadName=x");

        var response = resourceAttachmentService.createAttachment(
                1L,
                "user-key",
                new CreateResourceAttachmentRequest(
                        "feedback",
                        31L,
                        "temp/files/2026/08/10/file.pdf",
                        "운영 계획.pdf"
                )
        );

        assertThat(response.attachmentId()).isEqualTo(91L);
        verifyNoInteractions(resourceAttachmentPolicy, attachmentFinalizeClient);
        verify(resourceAttachmentRepository, never())
                .countByClubIdAndResourceTypeAndResourceIdAndDeletedFalse(any(), any(), any());
    }

    private ClubAccessResolver.ClubAccess access(Long clubProfileId) {
        ClubProfile clubProfile = mock(ClubProfile.class);
        ClubAccessResolver.ClubAccess access = mock(ClubAccessResolver.ClubAccess.class);
        when(access.clubProfile()).thenReturn(clubProfile);
        when(clubProfile.getClubProfileId()).thenReturn(clubProfileId);
        return access;
    }

    private FinalizedAttachment finalized() {
        return new FinalizedAttachment(
                "semo/attachments/feedback/31/2026/08/10/file.pdf",
                "file.pdf",
                "application/pdf",
                128L,
                "http://localhost:8081/files/final.pdf",
                false
        );
    }
}
