package semo.back.service.feature.memberdirectory.vo;

public record UpdateClubAdminMemberDirectorySettingsRequest(
        Boolean showPositions,
        Boolean showTagline,
        Boolean showRecentActivity
) {
}
