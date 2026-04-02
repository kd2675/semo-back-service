package semo.back.service.feature.memberdirectory.vo;

import java.util.List;

public record ClubMemberDirectoryResponse(
        Long clubId,
        String clubName,
        boolean admin,
        boolean featureEnabled,
        int totalMemberCount,
        MemberDirectorySettingsResponse settings,
        List<MemberDirectoryMemberResponse> members
) {
}
