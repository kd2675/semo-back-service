package semo.back.service.feature.position.vo;

import java.util.List;

public record UpdateClubMemberPositionsRequest(
        List<Long> clubPositionIds
) {
}
