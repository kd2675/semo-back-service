package semo.back.service.feature.profile.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ProfileUser;
import semo.back.service.database.pub.repository.ProfileUserRepository;
import semo.back.service.feature.profile.vo.ProfileSummaryResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileUserService {
    private static final String DEFAULT_TAGLINE = "새로운 모임을 시작할 준비가 된 멤버";
    private static final String[] DEFAULT_COLORS = {
            "#135BEC",
            "#0F9D58",
            "#E57C23",
            "#D64550",
            "#6C5CE7",
            "#00897B"
    };

    private final ProfileUserRepository profileUserRepository;

    public ProfileSummaryResponse getProfileSummary(String userKey) {
        ProfileUser profile = profileUserRepository.findByUserKey(userKey)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("ProfileUser", "userKey", userKey));
        return toSummary(profile);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public ProfileSummaryResponse initializeProfile(String userKey, String userName) {
        ProfileUser profile = getOrCreateProfile(userKey, userName);
        return toSummary(profile);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public Long resolveProfileId(String userKey, String userName) {
        return getOrCreateProfile(userKey, userName).getProfileId();
    }

    public Long getProfileIdByUserKey(String userKey) {
        return profileUserRepository.findByUserKey(userKey)
                .map(ProfileUser::getProfileId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("ProfileUser", "userKey", userKey));
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.MANDATORY)
    protected ProfileUser getOrCreateProfile(String userKey, String userName) {
        return profileUserRepository.findByUserKey(userKey)
                .orElseGet(() -> profileUserRepository.save(ProfileUser.builder()
                        .userKey(userKey)
                        .displayName(resolveDisplayName(userName, userKey))
                        .tagline(DEFAULT_TAGLINE)
                        .profileColor(resolveProfileColor(userKey))
                        .build()));
    }

    private ProfileSummaryResponse toSummary(ProfileUser profile) {
        return new ProfileSummaryResponse(
                profile.getProfileId(),
                profile.getUserKey(),
                profile.getDisplayName(),
                profile.getTagline(),
                profile.getProfileColor()
        );
    }

    private String resolveDisplayName(String userName, String userKey) {
        if (StringUtils.hasText(userName)) {
            return userName;
        }
        if (!StringUtils.hasText(userKey)) {
            return "SEMO User";
        }
        return "User-" + userKey.substring(Math.max(0, userKey.length() - 6));
    }

    private String resolveProfileColor(String userKey) {
        if (!StringUtils.hasText(userKey)) {
            return DEFAULT_COLORS[0];
        }
        int hash = userKey.hashCode() & Integer.MAX_VALUE;
        return DEFAULT_COLORS[hash % DEFAULT_COLORS.length];
    }
}
