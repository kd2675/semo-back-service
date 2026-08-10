package semo.back.service.feature.notification.biz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubNotification;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubNotificationRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.feature.notification.biz.ClubNotificationPublisher.NotificationCommand;

@ExtendWith(MockitoExtension.class)
class ClubNotificationPublisherTest {
    @Mock
    private ClubNotificationRepository clubNotificationRepository;
    @Mock
    private ClubProfileRepository clubProfileRepository;
    @Mock
    private ClubMemberRepository clubMemberRepository;

    @InjectMocks
    private ClubNotificationPublisher clubNotificationPublisher;

    @Test
    void notifyProfile_duplicateEvent_doesNotPersistAgain() {
        when(clubNotificationRepository.existsByEventKey("7:feedback:10:ANSWERED")).thenReturn(true);

        clubNotificationPublisher.notifyProfile(7L, command("/clubs/1/more/feedback", "feedback:10:ANSWERED"));

        verify(clubNotificationRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void notifyProfile_externalTargetPath_throwsValidation() {
        when(clubNotificationRepository.existsByEventKey(anyString())).thenReturn(false);

        assertThatThrownBy(() -> clubNotificationPublisher.notifyProfile(
                7L,
                command("https://malicious.example", "feedback:10:ANSWERED")
        )).isInstanceOf(SemoException.ValidationException.class);
    }

    @Test
    void notifyProfile_longSimilarEventKeys_keepDistinctBoundedFingerprints() {
        when(clubNotificationRepository.existsByEventKey(anyString())).thenReturn(false);
        String sharedPrefix = "x".repeat(500);

        clubNotificationPublisher.notifyProfile(7L, command("/", sharedPrefix + "a"));
        clubNotificationPublisher.notifyProfile(7L, command("/", sharedPrefix + "b"));

        ArgumentCaptor<ClubNotification> captor = ArgumentCaptor.forClass(ClubNotification.class);
        verify(clubNotificationRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        List<String> eventKeys = captor.getAllValues().stream()
                .map(ClubNotification::getEventKey)
                .toList();
        assertThat(eventKeys)
                .hasSize(2)
                .allSatisfy(eventKey -> assertThat(eventKey).hasSize(190))
                .doesNotHaveDuplicates();
    }

    private NotificationCommand command(String targetPath, String eventKey) {
        return new NotificationCommand(
                1L,
                "FEEDBACK_STATUS",
                "알림 제목",
                "알림 내용",
                "FEEDBACK",
                10L,
                targetPath,
                eventKey
        );
    }
}
