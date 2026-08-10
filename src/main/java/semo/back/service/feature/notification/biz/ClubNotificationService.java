package semo.back.service.feature.notification.biz;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubNotification;
import semo.back.service.database.pub.entity.ProfileUser;
import semo.back.service.database.pub.repository.ClubNotificationRepository;
import semo.back.service.database.pub.repository.ProfileUserRepository;
import semo.back.service.feature.notification.vo.ClubNotificationFeedResponse;
import semo.back.service.feature.notification.vo.ClubNotificationItemResponse;
import semo.back.service.feature.notification.vo.ClubNotificationReadResponse;
import semo.back.service.feature.notification.vo.ClubNotificationSummaryResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubNotificationService {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final ProfileUserRepository profileUserRepository;
    private final ClubNotificationRepository clubNotificationRepository;

    public ClubNotificationSummaryResponse getSummary(String userKey) {
        ProfileUser profileUser = requireProfileUser(userKey);
        return new ClubNotificationSummaryResponse(
                clubNotificationRepository.countByProfileIdAndReadAtIsNull(profileUser.getProfileId())
        );
    }

    public ClubNotificationFeedResponse getNotifications(
            String userKey,
            boolean unreadOnly,
            Long beforeId,
            Integer requestedSize
    ) {
        ProfileUser profileUser = requireProfileUser(userKey);
        int size = normalizeSize(requestedSize);
        List<ClubNotification> fetched = clubNotificationRepository.findFeed(
                profileUser.getProfileId(),
                unreadOnly,
                beforeId,
                PageRequest.of(0, size + 1)
        );
        boolean hasNext = fetched.size() > size;
        List<ClubNotification> page = hasNext ? fetched.subList(0, size) : fetched;
        Long nextCursor = hasNext && !page.isEmpty()
                ? page.get(page.size() - 1).getClubNotificationId()
                : null;
        return new ClubNotificationFeedResponse(
                clubNotificationRepository.countByProfileIdAndReadAtIsNull(profileUser.getProfileId()),
                unreadOnly,
                hasNext,
                nextCursor,
                page.stream().map(this::toResponse).toList()
        );
    }

    @Transactional(transactionManager = "pubTransactionManager")
    public ClubNotificationReadResponse markRead(String userKey, Long notificationId) {
        ProfileUser profileUser = requireProfileUser(userKey);
        ClubNotification notification = clubNotificationRepository
                .findByClubNotificationIdAndProfileId(notificationId, profileUser.getProfileId())
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "ClubNotification",
                        "notificationId",
                        notificationId
                ));
        boolean wasUnread = notification.getReadAt() == null;
        notification.markRead(LocalDateTime.now());
        clubNotificationRepository.save(notification);
        return new ClubNotificationReadResponse(
                notificationId,
                wasUnread ? 1 : 0,
                clubNotificationRepository.countByProfileIdAndReadAtIsNull(profileUser.getProfileId())
        );
    }

    @Transactional(transactionManager = "pubTransactionManager")
    public ClubNotificationReadResponse markAllRead(String userKey) {
        ProfileUser profileUser = requireProfileUser(userKey);
        int updatedCount = clubNotificationRepository.markAllRead(profileUser.getProfileId(), LocalDateTime.now());
        return new ClubNotificationReadResponse(null, updatedCount, 0);
    }

    private ProfileUser requireProfileUser(String userKey) {
        if (userKey == null || userKey.isBlank()) {
            throw new SemoException.UnauthorizedException("Login required");
        }
        return profileUserRepository.findByUserKey(userKey)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("ProfileUser", "userKey", userKey));
    }

    private int normalizeSize(Integer requestedSize) {
        int size = requestedSize == null ? DEFAULT_PAGE_SIZE : requestedSize;
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new SemoException.ValidationException("알림 조회 크기는 1에서 50 사이여야 합니다.");
        }
        return size;
    }

    private ClubNotificationItemResponse toResponse(ClubNotification notification) {
        LocalDateTime createdAt = notification.getCreateDate();
        LocalDateTime readAt = notification.getReadAt();
        return new ClubNotificationItemResponse(
                notification.getClubNotificationId(),
                notification.getClubId(),
                notification.getNotificationType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getResourceType(),
                notification.getResourceId(),
                notification.getTargetPath(),
                readAt != null,
                readAt == null ? null : DATE_TIME_FORMATTER.format(readAt),
                createdAt == null ? null : DATE_TIME_FORMATTER.format(createdAt),
                createdAt == null ? "" : LABEL_FORMATTER.format(createdAt)
        );
    }
}
