package semo.back.service.feature.bracket.vo;

import java.util.List;

public record BracketRoundResponse(
        int roundNumber,
        String title,
        List<BracketMatchResponse> matches
) {
}
