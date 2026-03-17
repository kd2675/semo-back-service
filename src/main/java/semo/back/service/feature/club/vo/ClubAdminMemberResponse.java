package semo.back.service.feature.club.vo;

public record ClubAdminMemberResponse(
        Long clubMemberId,
        Long clubProfileId,
        Long profileId,
        String displayName,
        String tagline,
        String avatarImageUrl,
        String joinedAtLabel,
        String lastActivityAtLabel,
        String roleCode,
        String membershipStatus,
        boolean canManage,
        boolean canApprove,
        boolean self
) {
}
