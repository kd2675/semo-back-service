package semo.back.service.feature.growth.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.database.pub.entity.ClubGrowthCore;
import semo.back.service.database.pub.repository.ClubGrowthCoreRepository;
import semo.back.service.database.pub.repository.ClubMemberCountRow;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.feature.growth.vo.ClubGrowthCoreResponse;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClubGrowthCoreQueryService {
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final ClubGrowthCoreRepository clubGrowthCoreRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubGrowthCorePolicy clubGrowthCorePolicy;

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.MANDATORY)
    public void initialize(Long clubId) {
        clubGrowthCoreRepository.findById(clubId)
                .orElseGet(() -> clubGrowthCoreRepository.save(
                        ClubGrowthCore.initial(clubId, ClubGrowthCorePolicy.POLICY_VERSION)
                ));
    }

    @Transactional(transactionManager = "pubTransactionManager", readOnly = true)
    public ClubGrowthCoreResponse get(Long clubId) {
        int memberCount = getActiveMemberCounts(List.of(clubId)).getOrDefault(clubId, 0);
        return clubGrowthCoreRepository.findById(clubId)
                .map(core -> toResponse(core, memberCount))
                .orElseGet(() -> initialResponse(memberCount));
    }

    @Transactional(transactionManager = "pubTransactionManager", readOnly = true)
    public Map<Long, ClubGrowthCoreResponse> getByClubIds(Collection<Long> clubIds) {
        if (clubIds == null || clubIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, ClubGrowthCore> coreByClubId = new HashMap<>();
        clubGrowthCoreRepository.findByClubIdIn(clubIds)
                .forEach(core -> coreByClubId.put(core.getClubId(), core));
        Map<Long, Integer> activeMemberCountByClubId = getActiveMemberCounts(clubIds);

        Map<Long, ClubGrowthCoreResponse> result = new LinkedHashMap<>();
        clubIds.stream().distinct().forEach(clubId -> {
            int memberCount = activeMemberCountByClubId.getOrDefault(clubId, 0);
            result.put(
                    clubId,
                    coreByClubId.containsKey(clubId)
                            ? toResponse(coreByClubId.get(clubId), memberCount)
                            : initialResponse(memberCount)
            );
        });
        return result;
    }

    private ClubGrowthCoreResponse toResponse(ClubGrowthCore core, int memberCount) {
        ClubGrowthTier tier = ClubGrowthTier.fromLevel(core.getTierLevel());
        ClubGrowthTier nextTier = tier.level() >= ClubGrowthCorePolicy.MAX_TIER_LEVEL
                ? null
                : ClubGrowthTier.fromLevel(tier.level() + 1);
        int togetherProgress = clubGrowthCorePolicy.progress(core.getTogetherScore(), tier.level());
        int operationsProgress = clubGrowthCorePolicy.progress(core.getOperationsScore(), tier.level());
        int continuityProgress = clubGrowthCorePolicy.progress(core.getContinuityScore(), tier.level());
        return new ClubGrowthCoreResponse(
                tier.name(),
                tier.label(),
                nextTier == null ? null : nextTier.name(),
                nextTier == null ? null : nextTier.label(),
                togetherProgress,
                operationsProgress,
                continuityProgress,
                memberCount,
                clubGrowthCorePolicy.activityLevel(core.getRecentActivityCount()),
                core.getPolicyVersion(),
                format(core.getLastProjectedAt())
        );
    }

    private ClubGrowthCoreResponse initialResponse(int memberCount) {
        return new ClubGrowthCoreResponse(
                ClubGrowthTier.RAW.name(),
                ClubGrowthTier.RAW.label(),
                ClubGrowthTier.IRON.name(),
                ClubGrowthTier.IRON.label(),
                0,
                0,
                0,
                memberCount,
                0,
                ClubGrowthCorePolicy.POLICY_VERSION,
                null
        );
    }

    private Map<Long, Integer> getActiveMemberCounts(Collection<Long> clubIds) {
        Map<Long, Integer> result = new HashMap<>();
        for (ClubMemberCountRow row : clubMemberRepository.countMembersByClubIdInAndMembershipStatus(
                clubIds,
                STATUS_ACTIVE
        )) {
            result.put(row.getClubId(), Math.toIntExact(row.getMemberCount()));
        }
        return result;
    }

    private String format(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toString();
    }
}
