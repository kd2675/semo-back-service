package semo.back.service.feature.clubfeature.vo;

import jakarta.validation.constraints.Size;

public record ApplyClubOperationTemplateRequest(
        @Size(max = 150) String titleOverride,
        String dueAt
) {
}
