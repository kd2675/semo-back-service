package semo.back.service.feature.club.vo;

public record UpdateClubProfileRequest(
        String displayName,
        String avatarFileName,
        Boolean removeAvatar
) {
}
