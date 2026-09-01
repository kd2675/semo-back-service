package semo.back.service.feature.growth.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.database.pub.entity.ClubGrowthCore;
import semo.back.service.database.pub.repository.ClubGrowthCoreRepository;
import semo.back.service.feature.growth.vo.ClubGrowthCoreResponse;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClubGrowthCoreQueryService {
    private final ClubGrowthCoreRepository clubGrowthCoreRepository;
    private final ClubGrowthCorePolicy clubGrowthCorePolicy;

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.MANDATORY)
    public ClubGrowthCoreResponse initialize(Long clubId) {
        ClubGrowthCore core = clubGrowthCoreRepository.findById(clubId)
                .orElseGet(() -> clubGrowthCoreRepository.save(
                        ClubGrowthCore.initial(clubId, ClubGrowthCorePolicy.POLICY_VERSION)
                ));
        return toResponse(core);
    }

    @Transactional(transactionManager = "pubTransactionManager", readOnly = true)
    public ClubGrowthCoreResponse get(Long clubId) {
        return clubGrowthCoreRepository.findById(clubId)
                .map(this::toResponse)
                .orElseGet(this::initialResponse);
    }

    @Transactional(transactionManager = "pubTransactionManager", readOnly = true)
    public Map<Long, ClubGrowthCoreResponse> getByClubIds(Collection<Long> clubIds) {
        if (clubIds == null || clubIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, ClubGrowthCore> coreByClubId = new HashMap<>();
        clubGrowthCoreRepository.findByClubIdIn(clubIds)
                .forEach(core -> coreByClubId.put(core.getClubId(), core));

        Map<Long, ClubGrowthCoreResponse> result = new LinkedHashMap<>();
        clubIds.stream().distinct().forEach(clubId -> result.put(
                clubId,
                coreByClubId.containsKey(clubId) ? toResponse(coreByClubId.get(clubId)) : initialResponse()
        ));
        return result;
    }

    private ClubGrowthCoreResponse toResponse(ClubGrowthCore core) {
        ClubGrowthTier tier = ClubGrowthTier.fromLevel(core.getTierLevel());
        ClubGrowthTier nextTier = tier.level() >= ClubGrowthCorePolicy.MAX_TIER_LEVEL
                ? null
                : ClubGrowthTier.fromLevel(tier.level() + 1);
        int togetherProgress = clubGrowthCorePolicy.progress(core.getTogetherScore(), tier.level());
        int operationsProgress = clubGrowthCorePolicy.progress(core.getOperationsScore(), tier.level());
        int continuityProgress = clubGrowthCorePolicy.progress(core.getContinuityScore(), tier.level());
        int overallProgress = (togetherProgress + operationsProgress + continuityProgress) / 3;
        return new ClubGrowthCoreResponse(
                tier.name(),
                tier.label(),
                nextTier == null ? null : nextTier.name(),
                nextTier == null ? null : nextTier.label(),
                togetherProgress,
                operationsProgress,
                continuityProgress,
                overallProgress,
                clubGrowthCorePolicy.activityLevel(core.getRecentActivityCount()),
                core.getPolicyVersion(),
                format(core.getLastProjectedAt())
        );
    }

    private ClubGrowthCoreResponse initialResponse() {
        return new ClubGrowthCoreResponse(
                ClubGrowthTier.RAW.name(),
                ClubGrowthTier.RAW.label(),
                ClubGrowthTier.IRON.name(),
                ClubGrowthTier.IRON.label(),
                0,
                0,
                0,
                0,
                0,
                ClubGrowthCorePolicy.POLICY_VERSION,
                null
        );
    }

    private String format(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toString();
    }
}
