package semo.back.service.feature.activity.vo;

import java.util.List;

public record ClubAdminActivityItemResponse(
        Long activityId,
        String actorDisplayName,
        String actorAvatarLabel,
        List<ClubAdminActivityPositionResponse> actorPositions,
        String subject,
        String detail,
        String status,
        String errorMessage,
        String createdAt,
        String createdAtLabel
) {
}
