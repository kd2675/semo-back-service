package semo.back.service.feature.schedule.vo;

public record ScheduleEventParticipantSummaryResponse(
        Long clubProfileId,
        String displayName,
        String avatarImageUrl,
        String avatarThumbnailUrl
) {
}
