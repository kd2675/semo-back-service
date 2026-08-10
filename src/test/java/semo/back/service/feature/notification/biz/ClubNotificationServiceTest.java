package semo.back.service.feature.notification.biz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubNotification;
import semo.back.service.database.pub.entity.ProfileUser;
import semo.back.service.database.pub.repository.ClubNotificationRepository;
import semo.back.service.database.pub.repository.ProfileUserRepository;
import semo.back.service.feature.notification.vo.ClubNotificationFeedResponse;
import semo.back.service.feature.notification.vo.ClubNotificationReadResponse;
import semo.back.service.feature.notification.vo.ClubNotificationSummaryResponse;

@ExtendWith(MockitoExtension.class)
class ClubNotificationServiceTest {
    @Mock
    private ProfileUserRepository profileUserRepository;
    @Mock
    private ClubNotificationRepository clubNotificationRepository;

    @InjectMocks
    private ClubNotificationService clubNotificationService;

    @Test
    void getSummary_authenticatedUser_returnsUnreadCount() {
        when(profileUserRepository.findByUserKey("user-key")).thenReturn(Optional.of(profileUser()));
        when(clubNotificationRepository.countByProfileIdAndReadAtIsNull(7L)).thenReturn(4L);

        ClubNotificationSummaryResponse response = clubNotificationService.getSummary("user-key");

        assertThat(response).isEqualTo(new ClubNotificationSummaryResponse(4L));
    }

    @Test
    void getNotifications_pageOverflow_returnsCursorWithoutOverflowItem() {
        when(profileUserRepository.findByUserKey("user-key")).thenReturn(Optional.of(profileUser()));
        when(clubNotificationRepository.findFeed(anyLong(), anyBoolean(), isNull(), any(Pageable.class)))
                .thenReturn(List.of(notification(30L, null), notification(20L, null), notification(10L, null)));
        when(clubNotificationRepository.countByProfileIdAndReadAtIsNull(7L)).thenReturn(3L);

        ClubNotificationFeedResponse response = clubNotificationService.getNotifications(
                "user-key",
                true,
                null,
                2
        );

        assertThat(response)
                .extracting(
                        ClubNotificationFeedResponse::unreadCount,
                        ClubNotificationFeedResponse::unreadOnly,
                        ClubNotificationFeedResponse::hasNext,
                        ClubNotificationFeedResponse::nextCursor,
                        feed -> feed.items().stream().map(item -> item.notificationId()).toList()
                )
                .containsExactly(3L, true, true, 20L, List.of(30L, 20L));
    }

    @Test
    void markRead_ownedUnreadNotification_returnsRemainingUnreadCount() {
        ClubNotification notification = notification(30L, null);
        when(profileUserRepository.findByUserKey("user-key")).thenReturn(Optional.of(profileUser()));
        when(clubNotificationRepository.findByClubNotificationIdAndProfileId(30L, 7L))
                .thenReturn(Optional.of(notification));
        when(clubNotificationRepository.countByProfileIdAndReadAtIsNull(7L)).thenReturn(2L);

        ClubNotificationReadResponse response = clubNotificationService.markRead("user-key", 30L);

        assertThat(response).isEqualTo(new ClubNotificationReadResponse(30L, 1, 2L));
    }

    @Test
    void markRead_otherUsersNotification_returnsNotFound() {
        when(profileUserRepository.findByUserKey("user-key")).thenReturn(Optional.of(profileUser()));
        when(clubNotificationRepository.findByClubNotificationIdAndProfileId(30L, 7L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> clubNotificationService.markRead("user-key", 30L))
                .isInstanceOf(SemoException.ResourceNotFoundException.class);
    }

    private ProfileUser profileUser() {
        return ProfileUser.builder()
                .profileId(7L)
                .userKey("user-key")
                .displayName("알림 사용자")
                .build();
    }

    private ClubNotification notification(Long notificationId, LocalDateTime readAt) {
        return ClubNotification.builder()
                .clubNotificationId(notificationId)
                .profileId(7L)
                .clubId(1L)
                .notificationType("FEEDBACK_STATUS")
                .title("알림 제목")
                .message("알림 내용")
                .resourceType("FEEDBACK")
                .resourceId(10L)
                .targetPath("/clubs/1/more/feedback")
                .eventKey("7:feedback:10:ANSWERED")
                .readAt(readAt)
                .build();
    }
}
