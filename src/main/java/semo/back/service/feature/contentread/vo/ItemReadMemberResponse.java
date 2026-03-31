package semo.back.service.feature.contentread.vo;

public record ItemReadMemberResponse(
        Long clubProfileId,
        String displayName,
        String avatarImageUrl,
        String avatarThumbnailUrl,
        String roleCode,
        String lastReadAtLabel
) {
}
