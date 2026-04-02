package semo.back.service.feature.dues.vo;

import java.util.List;

public record ClubAdminDuesChargeFeedResponse(
        Long clubId,
        String clubName,
        List<ClubAdminDuesChargeResponse> items,
        Long nextCursorChargeId,
        boolean hasNext
) {
}
