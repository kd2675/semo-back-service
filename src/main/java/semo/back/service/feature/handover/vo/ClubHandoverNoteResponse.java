package semo.back.service.feature.handover.vo;

import java.time.LocalDateTime;

public record ClubHandoverNoteResponse(
        Long clubHandoverNoteId,
        Long fromTermId,
        String fromTermName,
        Long toTermId,
        String toTermName,
        Long clubPositionId,
        String positionDisplayName,
        Long assignedClubProfileId,
        String assignedMemberDisplayName,
        String title,
        String content,
        String statusCode,
        LocalDateTime dueAt,
        Long createdByClubProfileId,
        String createdByDisplayName,
        Long acknowledgedByClubProfileId,
        LocalDateTime acknowledgedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
