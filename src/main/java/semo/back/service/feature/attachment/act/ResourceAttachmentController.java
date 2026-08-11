package semo.back.service.feature.attachment.act;

import java.nio.charset.StandardCharsets;
import java.util.List;

import auth.common.core.context.RequirePrincipalRole;
import auth.common.core.context.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.attachment.biz.ResourceAttachmentService;
import semo.back.service.feature.attachment.vo.CreateResourceAttachmentRequest;
import semo.back.service.feature.attachment.vo.DeleteResourceAttachmentResponse;
import semo.back.service.feature.attachment.vo.ResourceAttachmentResponse;
import web.common.core.response.base.dto.ResponseDataDTO;

@RequirePrincipalRole
@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}/attachments")
@RequiredArgsConstructor
public class ResourceAttachmentController {
    private final ResourceAttachmentService resourceAttachmentService;

    @GetMapping
    public ResponseDataDTO<List<ResourceAttachmentResponse>> getAttachments(
            @PathVariable Long clubId,
            @RequestParam String resourceType,
            @RequestParam Long resourceId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                resourceAttachmentService.getAttachments(
                        clubId,
                        requireUserKey(userContext),
                        resourceType,
                        resourceId
                ),
                "첨부파일 조회 성공"
        );
    }

    @PostMapping
    public ResponseDataDTO<ResourceAttachmentResponse> createAttachment(
            @PathVariable Long clubId,
            @Valid @RequestBody CreateResourceAttachmentRequest request,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                resourceAttachmentService.createAttachment(clubId, requireUserKey(userContext), request),
                "첨부파일 등록 성공"
        );
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseDataDTO<DeleteResourceAttachmentResponse> deleteAttachment(
            @PathVariable Long clubId,
            @PathVariable Long attachmentId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                resourceAttachmentService.deleteAttachment(clubId, attachmentId, requireUserKey(userContext)),
                "첨부파일 삭제 성공"
        );
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<StreamingResponseBody> downloadAttachment(
            @PathVariable Long clubId,
            @PathVariable Long attachmentId,
            UserContext userContext
    ) {
        ResourceAttachmentService.AttachmentDownload download = resourceAttachmentService.prepareAttachmentDownload(
                clubId,
                attachmentId,
                requireUserKey(userContext)
        );
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(download.originalFileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(download.sizeBytes())
                .body(outputStream -> resourceAttachmentService.writeAttachment(download, outputStream));
    }

    private String requireUserKey(UserContext userContext) {
        if (userContext == null || !StringUtils.hasText(userContext.getUserKey())) {
            throw new SemoException.UnauthorizedException("Login required");
        }
        return userContext.getUserKey();
    }
}
