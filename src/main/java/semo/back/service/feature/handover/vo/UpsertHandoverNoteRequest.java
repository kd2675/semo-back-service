package semo.back.service.feature.handover.vo;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertHandoverNoteRequest(
        Long fromTermId,
        Long toTermId,
        Long clubPositionId,
        Long assignedClubProfileId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 10000) String content,
        @Size(max = 20) String statusCode,
        LocalDateTime dueAt
) {
}
