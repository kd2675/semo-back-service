package semo.back.service.feature.club.vo;

public record ClubJoinActionResponse(
        Long clubId,
        String clubName,
        String actionType,
        String joinStatus,
        Long clubJoinRequestId,
        Long clubMemberId
) {
}
