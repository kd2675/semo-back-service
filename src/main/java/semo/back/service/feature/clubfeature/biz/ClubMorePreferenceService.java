package semo.back.service.feature.clubfeature.biz;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubMorePreference;
import semo.back.service.database.pub.repository.ClubMorePreferenceRepository;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.clubfeature.vo.ClubFeatureResponse;
import semo.back.service.feature.clubfeature.vo.ClubMorePreferenceResponse;
import semo.back.service.feature.clubfeature.vo.UpdateClubMorePreferenceRequest;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubMorePreferenceService {
    private final ClubAccessResolver clubAccessResolver;
    private final ClubFeatureService clubFeatureService;
    private final ClubMorePreferenceRepository clubMorePreferenceRepository;

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public ClubMorePreferenceResponse updateFavorite(
            Long clubId,
            String featureKey,
            String userKey,
            UpdateClubMorePreferenceRequest request
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        String normalizedFeatureKey = requireEnabledFeature(clubId, userKey, featureKey);
        if (request == null || request.favorite() == null) {
            throw new SemoException.ValidationException("즐겨찾기 여부는 필수입니다.");
        }
        ClubMorePreference preference = getOrCreate(
                clubId,
                access.clubProfile().getClubProfileId(),
                normalizedFeatureKey
        );
        preference.updateFavorite(request.favorite());
        return toResponse(clubMorePreferenceRepository.save(preference));
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public ClubMorePreferenceResponse markUsed(Long clubId, String featureKey, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        String normalizedFeatureKey = requireEnabledFeature(clubId, userKey, featureKey);
        ClubMorePreference preference = getOrCreate(
                clubId,
                access.clubProfile().getClubProfileId(),
                normalizedFeatureKey
        );
        preference.markUsed(LocalDateTime.now());
        return toResponse(clubMorePreferenceRepository.save(preference));
    }

    private ClubMorePreference getOrCreate(Long clubId, Long clubProfileId, String featureKey) {
        return clubMorePreferenceRepository.findForUpdate(clubId, clubProfileId, featureKey)
                .orElseGet(() -> ClubMorePreference.builder()
                        .clubId(clubId)
                        .clubProfileId(clubProfileId)
                        .featureKey(featureKey)
                        .favorite(false)
                        .build());
    }

    private String requireEnabledFeature(Long clubId, String userKey, String featureKey) {
        String normalized = featureKey == null ? "" : featureKey.trim().toUpperCase(Locale.ROOT);
        List<ClubFeatureResponse> features = clubFeatureService.getClubFeatures(clubId, userKey);
        return features.stream()
                .filter(ClubFeatureResponse::enabled)
                .map(ClubFeatureResponse::featureKey)
                .filter(normalized::equals)
                .findFirst()
                .orElseThrow(() -> new SemoException.ValidationException("활성화된 기능만 더보기 설정에 저장할 수 있습니다."));
    }

    private ClubMorePreferenceResponse toResponse(ClubMorePreference preference) {
        return new ClubMorePreferenceResponse(
                preference.getFeatureKey(),
                preference.isFavorite(),
                preference.getLastUsedAt()
        );
    }
}
