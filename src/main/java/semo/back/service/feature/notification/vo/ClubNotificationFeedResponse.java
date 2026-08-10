package semo.back.service.feature.notification.vo;

import java.util.List;

public record ClubNotificationFeedResponse(
        long unreadCount,
        boolean unreadOnly,
        boolean hasNext,
        Long nextCursor,
        List<ClubNotificationItemResponse> items
) {
}
