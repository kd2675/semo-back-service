package semo.back.service.feature.notification.vo;

public record ClubNotificationReadResponse(
        Long notificationId,
        int updatedCount,
        long unreadCount
) {
}
