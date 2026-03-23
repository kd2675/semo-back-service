package semo.back.service.feature.activity.vo;

public record ClubAdminActivityItemResponse(
        Long activityId,
        String actorDisplayName,
        String actorAvatarLabel,
        String subject,
        String detail,
        String status,
        String errorMessage,
        String createdAt,
        String createdAtLabel
) {
}
