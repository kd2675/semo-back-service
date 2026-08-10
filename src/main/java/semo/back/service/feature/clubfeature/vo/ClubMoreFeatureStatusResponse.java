package semo.back.service.feature.clubfeature.vo;

import java.time.LocalDateTime;

public record ClubMoreFeatureStatusResponse(
        String featureKey,
        boolean userAccessible,
        boolean adminAccessible,
        int userPendingCount,
        int userOverdueCount,
        int adminPendingCount,
        int adminOverdueCount,
        boolean favorite,
        LocalDateTime lastUsedAt
) {
}
