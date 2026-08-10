package semo.back.service.feature.decision.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpsertDecisionRecordRequest(
        @NotBlank @Size(max = 30) String recordType,
        @NotBlank @Size(max = 20) String visibilityScope,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 20000) String decisionContent,
        @Size(max = 20000) String backgroundContext,
        @Size(max = 20000) String rationale,
        LocalDateTime meetingAt,
        LocalDate effectiveDate,
        LocalDate reviewDate,
        @Positive Long clubOperatingTermId,
        @Positive Long supersedesDecisionRecordId,
        @NotNull @Size(min = 1, max = 50) List<@NotNull @Positive Long> deciderClubProfileIds,
        @NotNull @Size(max = 100) List<@NotNull @Positive Long> participantClubProfileIds,
        @NotNull @Size(max = 50) List<@Valid DecisionResourceReferenceRequest> relatedResources,
        @NotNull @Size(max = 50) List<@NotNull @Positive Long> followUpTodoItemIds
) {
}
