package semo.back.service.feature.handover.vo;

public record ClubExecutiveAssignmentResponse(
        Long clubTermExecutiveAssignmentId,
        Long clubOperatingTermId,
        Long clubMemberId,
        Long clubProfileId,
        String memberDisplayName,
        String avatarFileName,
        Long clubPositionId,
        String positionDisplayName,
        String positionIconName,
        String positionColorHex,
        String responsibility,
        int sortOrder
) {
}
