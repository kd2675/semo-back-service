package semo.back.service.feature.attachment.biz;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.common.exception.SemoException;
import semo.back.service.common.util.AttachmentFileUrlResolver;
import semo.back.service.common.util.AttachmentFinalizeClient;
import semo.back.service.common.util.AttachmentFinalizeClient.FinalizedAttachment;
import semo.back.service.database.pub.entity.ResourceAttachment;
import semo.back.service.database.pub.repository.ResourceAttachmentRepository;
import semo.back.service.feature.attachment.vo.CreateResourceAttachmentRequest;
import semo.back.service.feature.attachment.vo.DeleteResourceAttachmentResponse;
import semo.back.service.feature.attachment.vo.ResourceAttachmentResponse;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResourceAttachmentService {
    private static final int MAX_ATTACHMENTS_PER_RESOURCE = 10;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ClubAccessResolver clubAccessResolver;
    private final ResourceAttachmentPolicy resourceAttachmentPolicy;
    private final ResourceAttachmentRepository resourceAttachmentRepository;
    private final AttachmentFinalizeClient attachmentFinalizeClient;
    private final AttachmentFileUrlResolver attachmentFileUrlResolver;

    public List<ResourceAttachmentResponse> getAttachments(
            Long clubId,
            String userKey,
            String requestedResourceType,
            Long resourceId
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        String resourceType = normalizeResourceType(requestedResourceType);
        requireResourceId(resourceId);
        resourceAttachmentPolicy.requireCanView(access, resourceType, resourceId);
        return resourceAttachmentRepository
                .findByClubIdAndResourceTypeAndResourceIdAndDeletedFalseOrderByResourceAttachmentIdAsc(
                        clubId,
                        resourceType,
                        resourceId
                ).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(transactionManager = "pubTransactionManager")
    public ResourceAttachmentResponse createAttachment(
            Long clubId,
            String userKey,
            CreateResourceAttachmentRequest request
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        String resourceType = normalizeResourceType(request.resourceType());
        Long resourceId = requireResourceId(request.resourceId());
        String originalFileName = normalizeOriginalFileName(request.originalFileName());
        String targetDir = "semo/attachments/"
                + resourceType.toLowerCase(Locale.ROOT).replace('_', '-')
                + "/"
                + resourceId;
        String predictedFinalFileName = predictFinalFileName(request.tempFileName(), targetDir);
        ResourceAttachment retriedAttachment = resourceAttachmentRepository
                .findByFileNameAndDeletedFalse(predictedFinalFileName)
                .orElse(null);
        if (retriedAttachment != null) {
            return requireSameAttachmentTarget(retriedAttachment, clubId, resourceType, resourceId, access);
        }

        String visibilityScope = resourceAttachmentPolicy.requireCanAttachForUpdate(access, resourceType, resourceId);
        long attachmentCount = resourceAttachmentRepository
                .countByClubIdAndResourceTypeAndResourceIdAndDeletedFalse(clubId, resourceType, resourceId);
        if (attachmentCount >= MAX_ATTACHMENTS_PER_RESOURCE) {
            throw new SemoException.ValidationException("한 항목에는 첨부파일을 최대 10개까지 등록할 수 있습니다.");
        }
        FinalizedAttachment finalized = attachmentFinalizeClient.finalizeAttachment(
                request.tempFileName(),
                targetDir
        );
        ResourceAttachment existing = resourceAttachmentRepository
                .findByFileNameAndDeletedFalse(finalized.fileName())
                .orElse(null);
        if (existing != null) {
            return requireSameAttachmentTarget(existing, clubId, resourceType, resourceId, access);
        }

        ResourceAttachment saved = resourceAttachmentRepository.save(ResourceAttachment.builder()
                .clubId(clubId)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .uploaderClubProfileId(access.clubProfile().getClubProfileId())
                .fileName(finalized.fileName())
                .originalFileName(originalFileName)
                .contentType(finalized.contentType())
                .sizeBytes(finalized.sizeBytes())
                .visibilityScope(visibilityScope)
                .deleted(false)
                .deletedByClubProfileId(null)
                .deletedAt(null)
                .build());
        return toResponse(saved);
    }

    @Transactional(transactionManager = "pubTransactionManager")
    public DeleteResourceAttachmentResponse deleteAttachment(
            Long clubId,
            Long attachmentId,
            String userKey
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        ResourceAttachment attachment = resourceAttachmentRepository
                .findByResourceAttachmentIdAndClubIdAndDeletedFalse(attachmentId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "ResourceAttachment",
                        "attachmentId",
                        attachmentId
                ));
        resourceAttachmentPolicy.requireCanDelete(access, attachment);
        attachment.markDeleted(access.clubProfile().getClubProfileId(), LocalDateTime.now());
        resourceAttachmentRepository.save(attachment);
        return new DeleteResourceAttachmentResponse(attachmentId, true);
    }

    private String normalizeResourceType(String resourceType) {
        if (resourceType == null || resourceType.isBlank()) {
            throw new SemoException.ValidationException("첨부 대상 유형은 필수입니다.");
        }
        return resourceType.trim().toUpperCase(Locale.ROOT);
    }

    private Long requireResourceId(Long resourceId) {
        if (resourceId == null || resourceId < 1) {
            throw new SemoException.ValidationException("첨부 대상 ID가 올바르지 않습니다.");
        }
        return resourceId;
    }

    private String normalizeOriginalFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new SemoException.ValidationException("원본 파일명은 필수입니다.");
        }
        String normalized = originalFileName.trim().replace('\\', '/');
        int slashIndex = normalized.lastIndexOf('/');
        String baseName = slashIndex >= 0 ? normalized.substring(slashIndex + 1) : normalized;
        if (baseName.isBlank()
                || baseName.length() > 255
                || baseName.contains("\r")
                || baseName.contains("\n")) {
            throw new SemoException.ValidationException("원본 파일명이 올바르지 않습니다.");
        }
        return baseName;
    }

    private String predictFinalFileName(String tempFileName, String targetDir) {
        if (tempFileName == null || tempFileName.isBlank()) {
            throw new SemoException.ValidationException("임시 파일 경로는 필수입니다.");
        }
        String normalized = tempFileName.trim().replace('\\', '/');
        String prefix = "temp/files/";
        if (!normalized.startsWith(prefix)
                || normalized.contains("../")
                || normalized.endsWith("/")) {
            throw new SemoException.ValidationException("첨부파일 임시 경로가 올바르지 않습니다.");
        }
        return targetDir + "/" + normalized.substring(prefix.length());
    }

    private ResourceAttachmentResponse requireSameAttachmentTarget(
            ResourceAttachment attachment,
            Long clubId,
            String resourceType,
            Long resourceId,
            ClubAccessResolver.ClubAccess access
    ) {
        if (attachment.getClubId().equals(clubId)
                && attachment.getResourceType().equals(resourceType)
                && attachment.getResourceId().equals(resourceId)
                && attachment.getUploaderClubProfileId().equals(access.clubProfile().getClubProfileId())) {
            return toResponse(attachment);
        }
        throw new SemoException.ConflictException("이미 다른 항목에 등록된 첨부파일입니다.");
    }

    private ResourceAttachmentResponse toResponse(ResourceAttachment attachment) {
        return new ResourceAttachmentResponse(
                attachment.getResourceAttachmentId(),
                attachment.getClubId(),
                attachment.getResourceType(),
                attachment.getResourceId(),
                attachment.getUploaderClubProfileId(),
                attachment.getOriginalFileName(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getVisibilityScope(),
                attachmentFileUrlResolver.resolveDownloadUrl(
                        attachment.getFileName(),
                        attachment.getOriginalFileName()
                ),
                attachment.getCreateDate() == null
                        ? null
                        : DATE_TIME_FORMATTER.format(attachment.getCreateDate())
        );
    }
}
