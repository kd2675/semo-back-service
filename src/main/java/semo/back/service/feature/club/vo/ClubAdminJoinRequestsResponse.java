package semo.back.service.feature.club.vo;

import java.util.List;

public record ClubAdminJoinRequestsResponse(
        Long clubId,
        String clubName,
        boolean admin,
        List<ClubAdminJoinRequestResponse> requests
) {
}
