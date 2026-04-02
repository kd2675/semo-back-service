package semo.back.service.feature.memberdirectory.vo;

public record MemberDirectorySettingsResponse(
        boolean showPositions,
        boolean showTagline,
        boolean showRecentActivity
) {
}
