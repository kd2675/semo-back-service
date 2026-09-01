package semo.back.service.feature.growth.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.database.pub.entity.ClubGrowthCore;
import semo.back.service.database.pub.repository.ClubGrowthCoreRepository;
import semo.back.service.database.pub.repository.ClubGrowthEvidenceRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ClubGrowthCoreProjectionService {
    private static final int RECENT_ACTIVITY_DAYS = 14;

    private final ClubGrowthCoreRepository clubGrowthCoreRepository;
    private final ClubGrowthEvidenceRepository clubGrowthEvidenceRepository;
    private final ClubGrowthCorePolicy clubGrowthCorePolicy;

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public void refresh(Long clubId) {
        if (clubId == null) {
            return;
        }
        LocalDateTime projectedAt = LocalDateTime.now();
        clubGrowthEvidenceRepository.findEvidence(clubId, projectedAt.minusDays(RECENT_ACTIVITY_DAYS))
                .ifPresent(evidence -> {
                    ClubGrowthCore core = clubGrowthCoreRepository.findForUpdate(clubId)
                            .orElseGet(() -> clubGrowthCoreRepository.save(
                                    ClubGrowthCore.initial(clubId, ClubGrowthCorePolicy.POLICY_VERSION)
                            ));
                    ClubGrowthCorePolicy.Projection projection = clubGrowthCorePolicy.evaluate(
                            evidence,
                            core.getTierLevel()
                    );
                    core.applyProjection(
                            projection.tierLevel(),
                            projection.togetherScore(),
                            projection.operationsScore(),
                            projection.continuityScore(),
                            projection.recentActivityCount(),
                            projection.policyVersion(),
                            projectedAt
                    );
                });
    }
}
