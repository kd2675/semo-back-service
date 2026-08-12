package semo.back.service.feature.memberdirectory.vo;

import semo.back.service.feature.position.vo.ClubPositionSummaryResponse;

import java.util.List;

public record MemberDirectoryMemberResponse(
        Long clubMemberId,
        Long clubProfileId,
        String displayName,
        String avatarImageUrl,
        String roleCode,
        String roleLabel,
        List<ClubPositionSummaryResponse> positions,
        String tagline,
        MemberDirectoryRecentActivityResponse recentActivity
) {
}
