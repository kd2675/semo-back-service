package semo.back.service.feature.activity.vo;

public record ClubMemberActivityEntryResponse(
        Long activityId,
        String actorDisplayName,
        String actorAvatarLabel,
        String subject,
        String detail,
        String status,
        String createdAt,
        String createdAtLabel
) {
}
