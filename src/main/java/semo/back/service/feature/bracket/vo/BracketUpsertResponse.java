package semo.back.service.feature.bracket.vo;

public record BracketUpsertResponse(
        Long bracketRecordId,
        String title,
        String approvalStatus,
        int participantCount
) {
}
