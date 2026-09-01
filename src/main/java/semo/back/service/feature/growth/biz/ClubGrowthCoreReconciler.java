package semo.back.service.feature.growth.biz;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import semo.back.service.database.pub.repository.ClubGrowthCoreRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "semo.growth-core.reconcile.enabled",
        havingValue = "true",
        matchIfMissing = true
)
@Slf4j
public class ClubGrowthCoreReconciler {
    private static final int BATCH_SIZE = 100;
    private static final int STALE_MINUTES = 10;

    private final ClubGrowthCoreRepository clubGrowthCoreRepository;
    private final ClubGrowthCoreRefreshCoordinator clubGrowthCoreRefreshCoordinator;

    @Scheduled(
            initialDelayString = "${semo.growth-core.reconcile.initial-delay-ms:30000}",
            fixedDelayString = "${semo.growth-core.reconcile.fixed-delay-ms:900000}"
    )
    public void reconcileStaleCores() {
        List<Long> clubIds = clubGrowthCoreRepository.findStaleClubIds(
                LocalDateTime.now().minusMinutes(STALE_MINUTES),
                PageRequest.of(0, BATCH_SIZE)
        );
        clubIds.forEach(clubGrowthCoreRefreshCoordinator::refreshSafely);
        if (!clubIds.isEmpty()) {
            log.info("Reconciled club growth cores. count={}", clubIds.size());
        }
    }
}
