package semo.back.service.feature.position.vo;

public record ClubPositionHistoryItemResponse(
        Long positionHistoryId,
        Long clubMemberId,
        Long clubProfileId,
        String memberDisplayName,
        Long clubPositionId,
        String positionCode,
        String positionDisplayName,
        String startedAt,
        String startedAtLabel,
        String endedAt,
        String endedAtLabel,
        boolean active,
        boolean deleted,
        String deleteReason
) {
}
