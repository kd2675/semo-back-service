package semo.back.service.feature.profile.vo;

public record ProfileSummaryResponse(
        Long profileId,
        String userKey,
        String displayName,
        String tagline,
        String profileColor
) {
}
