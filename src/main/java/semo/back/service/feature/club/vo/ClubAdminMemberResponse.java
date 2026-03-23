package semo.back.service.feature.club.vo;

import semo.back.service.feature.position.vo.ClubPositionSummaryResponse;

import java.util.List;

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
        boolean self,
        List<ClubPositionSummaryResponse> positions
) {
}
