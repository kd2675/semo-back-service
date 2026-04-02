package semo.back.service.feature.club.vo;

import java.util.List;

public record ClubDiscoverResponse(
        String query,
        boolean recommended,
        String recommendationLabel,
        int totalCount,
        List<ClubDiscoverSummaryResponse> clubs
) {
}
