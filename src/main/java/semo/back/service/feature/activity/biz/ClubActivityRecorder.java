package semo.back.service.feature.activity.biz;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.database.pub.entity.ClubActivityLog;
import semo.back.service.database.pub.repository.ClubActivityLogRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClubActivityRecorder {
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAIL = "FAIL";

    private final ClubActivityActorResolver clubActivityActorResolver;
    private final ClubActivityLogRepository clubActivityLogRepository;

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public void recordSuccessSafely(Long clubId, String userKey, String subject, String detail) {
        try {
            save(clubId, userKey, subject, detail, STATUS_SUCCESS, null);
        } catch (Exception exception) {
            log.error("Failed to record success activity log. clubId={}, subject={}, detail={}", clubId, subject, detail, exception);
        }
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public void recordFailureSafely(Long clubId, String userKey, String subject, String detail, String errorMessage) {
        try {
            save(clubId, userKey, subject, detail, STATUS_FAIL, errorMessage);
        } catch (Exception exception) {
            log.error(
                    "Failed to record failure activity log. clubId={}, subject={}, detail={}, errorMessage={}",
                    clubId,
                    subject,
                    detail,
                    errorMessage,
                    exception
            );
        }
    }

    private void save(
            Long clubId,
            String userKey,
            String subject,
            String detail,
            String statusCode,
            String errorMessage
    ) {
        if (clubId == null || !StringUtils.hasText(subject) || !StringUtils.hasText(detail)) {
            return;
        }

        ClubActivityActorResolver.ActorSnapshot actor = clubActivityActorResolver.resolve(clubId, userKey);
        clubActivityLogRepository.save(ClubActivityLog.builder()
                .clubId(clubId)
                .actorClubMemberId(actor.actorClubMemberId())
                .actorClubProfileId(actor.actorClubProfileId())
                .actorDisplayName(limit(actor.actorDisplayName(), 100, "알 수 없는 사용자"))
                .subject(limit(subject, 100, "활동"))
                .detailText(limit(detail, 500, "활동 내역이 기록되었습니다."))
                .statusCode(limit(statusCode, 20, STATUS_SUCCESS))
                .errorMessage(limit(errorMessage, 500, null))
                .build());
    }

    private String limit(String value, int maxLength, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        String normalized = value.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }
}
