package semo.back.service.feature.club.vo;

import java.util.List;

public record ClubAdminMembersResponse(
        Long clubId,
        String clubName,
        boolean admin,
        List<ClubAdminMemberResponse> members
) {
}
