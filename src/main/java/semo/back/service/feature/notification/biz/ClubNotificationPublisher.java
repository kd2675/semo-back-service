package semo.back.service.feature.notification.biz;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.entity.ClubNotification;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubNotificationRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;

@Component
@RequiredArgsConstructor
public class ClubNotificationPublisher {
    private final ClubNotificationRepository clubNotificationRepository;
    private final ClubProfileRepository clubProfileRepository;
    private final ClubMemberRepository clubMemberRepository;

    public void notifyClubProfile(Long recipientClubProfileId, NotificationCommand command) {
        ClubProfile clubProfile = clubProfileRepository.findById(recipientClubProfileId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "ClubProfile",
                        "clubProfileId",
                        recipientClubProfileId
                ));
        ClubMember clubMember = clubMemberRepository.findById(clubProfile.getClubMemberId())
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "ClubMember",
                        "clubMemberId",
                        clubProfile.getClubMemberId()
                ));
        notifyProfile(clubMember.getProfileId(), command);
    }

    public void notifyProfile(Long recipientProfileId, NotificationCommand command) {
        if (recipientProfileId == null || recipientProfileId < 1) {
            throw new SemoException.ValidationException("알림 수신자 정보가 올바르지 않습니다.");
        }
        if (command == null) {
            throw new SemoException.ValidationException("알림 명령이 누락되었습니다.");
        }
        String eventKey = normalizeEventKey(recipientProfileId, command.eventKey());
        if (clubNotificationRepository.existsByEventKey(eventKey)) {
            return;
        }
        String targetPath = normalizeTargetPath(command.targetPath());
        clubNotificationRepository.save(ClubNotification.builder()
                .profileId(recipientProfileId)
                .clubId(command.clubId())
                .notificationType(limit(command.notificationType(), 50))
                .title(limit(command.title(), 150))
                .message(limit(command.message(), 500))
                .resourceType(limitNullable(command.resourceType(), 50))
                .resourceId(command.resourceId())
                .targetPath(targetPath)
                .eventKey(eventKey)
                .build());
    }

    private String normalizeEventKey(Long recipientProfileId, String eventKey) {
        if (eventKey == null || eventKey.isBlank()) {
            throw new SemoException.ValidationException("알림 필수값이 누락되었습니다.");
        }
        String rawEventKey = eventKey.trim();
        String scopedEventKey = recipientProfileId + ":" + rawEventKey;
        if (scopedEventKey.length() <= 190) {
            return scopedEventKey;
        }
        String fingerprint = UUID.nameUUIDFromBytes(
                scopedEventKey.getBytes(StandardCharsets.UTF_8)
        ).toString();
        return scopedEventKey.substring(0, 153) + ":" + fingerprint;
    }

    private String normalizeTargetPath(String targetPath) {
        if (targetPath == null || targetPath.isBlank()) {
            return null;
        }
        String normalized = targetPath.trim();
        if (!normalized.startsWith("/") || normalized.startsWith("//")) {
            throw new SemoException.ValidationException("알림 이동 경로는 앱 내부 경로여야 합니다.");
        }
        return limit(normalized, 500);
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new SemoException.ValidationException("알림 필수값이 누락되었습니다.");
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private String limitNullable(String value, int maxLength) {
        return value == null || value.isBlank() ? null : limit(value, maxLength);
    }

    public record NotificationCommand(
            Long clubId,
            String notificationType,
            String title,
            String message,
            String resourceType,
            Long resourceId,
            String targetPath,
            String eventKey
    ) {
    }
}
