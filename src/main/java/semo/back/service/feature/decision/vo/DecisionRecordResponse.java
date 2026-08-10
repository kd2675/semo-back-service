package semo.back.service.feature.decision.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DecisionRecordResponse(
        Long decisionRecordId,
        Long clubOperatingTermId,
        String operatingTermName,
        String recordType,
        String statusCode,
        String visibilityScope,
        String title,
        String decisionContent,
        String backgroundContext,
        String rationale,
        LocalDateTime meetingAt,
        LocalDate effectiveDate,
        LocalDate reviewDate,
        Long supersedesDecisionRecordId,
        String supersedesDecisionTitle,
        Long createdByClubProfileId,
        String createdByDisplayName,
        Long confirmedByClubProfileId,
        String confirmedByDisplayName,
        LocalDateTime confirmedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<DecisionParticipantResponse> participants,
        List<DecisionResourceLinkResponse> resourceLinks
) {
}
