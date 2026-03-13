package semo.back.service.feature.club.vo;

import semo.back.service.feature.profile.vo.ProfileSummaryResponse;

import java.util.List;

public record ClubProfileResponse(
        Long clubId,
        String clubName,
        boolean admin,
        ProfileSummaryResponse appProfile,
        ClubProfileDetailResponse clubProfile,
        List<ClubProfileRecordResponse> clubRecords
) {
}
