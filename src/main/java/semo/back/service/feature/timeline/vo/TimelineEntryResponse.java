package semo.back.service.feature.timeline.vo;

public record TimelineEntryResponse(
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
