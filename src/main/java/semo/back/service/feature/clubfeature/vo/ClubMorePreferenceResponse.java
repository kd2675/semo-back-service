package semo.back.service.feature.clubfeature.vo;

import java.time.LocalDateTime;

public record ClubMorePreferenceResponse(
        String featureKey,
        boolean favorite,
        LocalDateTime lastUsedAt
) {
}
