package semo.back.service.feature.club.vo;

import java.util.List;

import semo.back.service.feature.position.vo.ClubPositionSummaryResponse;

import java.util.List;

public record ClubAdminMembersResponse(
        Long clubId,
        String clubName,
        boolean admin,
        boolean roleManagementEnabled,
        List<ClubPositionSummaryResponse> availablePositions,
        List<ClubAdminMemberResponse> members
) {
}
