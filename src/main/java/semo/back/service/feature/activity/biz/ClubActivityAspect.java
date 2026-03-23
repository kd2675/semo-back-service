package semo.back.service.feature.activity.biz;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Aspect
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
public class ClubActivityAspect {
    private final ClubActivityRecorder clubActivityRecorder;

    @Around("@annotation(recordClubActivity)")
    public Object recordActivity(ProceedingJoinPoint joinPoint, RecordClubActivity recordClubActivity) throws Throwable {
        Long clubId = resolveClubId(joinPoint.getArgs());
        String userKey = resolveUserKey(joinPoint.getArgs());
        ClubActivityContextHolder.push();
        try {
            Object result = joinPoint.proceed();
            ClubActivityContextHolder.Snapshot snapshot = ClubActivityContextHolder.currentSnapshot();
            String detail = resolveDetail(
                    snapshot.successDetail(),
                    recordClubActivity.successDetail(),
                    recordClubActivity.subject() + " 작업을 완료했습니다."
            );
            Runnable recordTask = () -> clubActivityRecorder.recordSuccessSafely(clubId, userKey, recordClubActivity.subject(), detail);
            if (TransactionSynchronizationManager.isActualTransactionActive()
                    && TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        recordTask.run();
                    }
                });
            } else {
                recordTask.run();
            }
            return result;
        } catch (Throwable throwable) {
            ClubActivityContextHolder.Snapshot snapshot = ClubActivityContextHolder.currentSnapshot();
            String detail = resolveDetail(
                    snapshot.failureDetail(),
                    recordClubActivity.failureDetail(),
                    recordClubActivity.subject() + " 작업에 실패했습니다."
            );
            clubActivityRecorder.recordFailureSafely(
                    clubId,
                    userKey,
                    recordClubActivity.subject(),
                    detail,
                    resolveErrorMessage(throwable)
            );
            throw throwable;
        } finally {
            ClubActivityContextHolder.pop();
        }
    }

    private Long resolveClubId(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Long value) {
                return value;
            }
        }
        return null;
    }

    private String resolveUserKey(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof String value && StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String resolveDetail(String preferred, String fallback, String defaultValue) {
        if (StringUtils.hasText(preferred)) {
            return preferred.trim();
        }
        if (StringUtils.hasText(fallback)) {
            return fallback.trim();
        }
        return defaultValue;
    }

    private String resolveErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        if (StringUtils.hasText(throwable.getMessage())) {
            return throwable.getMessage().trim();
        }
        return throwable.getClass().getSimpleName();
    }
}
