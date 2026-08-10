package semo.back.service.feature.attachment.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateResourceAttachmentRequest(
        @NotBlank @Size(max = 40) String resourceType,
        @NotNull @Positive Long resourceId,
        @NotBlank @Size(max = 500) String tempFileName,
        @NotBlank @Size(max = 255) String originalFileName
) {
}
