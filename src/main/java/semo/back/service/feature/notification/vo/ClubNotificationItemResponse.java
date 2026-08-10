package semo.back.service.feature.notification.vo;

public record ClubNotificationItemResponse(
        Long notificationId,
        Long clubId,
        String notificationType,
        String title,
        String message,
        String resourceType,
        Long resourceId,
        String targetPath,
        boolean read,
        String readAt,
        String createdAt,
        String createdAtLabel
) {
}
