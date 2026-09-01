package semo.back.service.feature.growth.biz;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClubGrowthCoreRefreshCoordinator {
    private final ClubGrowthCoreProjectionService clubGrowthCoreProjectionService;

    public void refreshSafely(Long clubId) {
        if (clubId == null) {
            return;
        }
        try {
            clubGrowthCoreProjectionService.refresh(clubId);
        } catch (RuntimeException exception) {
            log.error("Failed to refresh club growth core. clubId={}", clubId, exception);
        }
    }
}
