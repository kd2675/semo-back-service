package semo.back.service.feature.position.vo;

import java.util.List;

public record ClubPositionHistoryResponse(
        Long clubId,
        String clubName,
        boolean admin,
        List<ClubPositionHistoryItemResponse> histories
) {
}
