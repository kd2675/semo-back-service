package semo.back.service.feature.club.biz.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.ProfileUser;
import semo.back.service.database.pub.repository.ClubProfileRepository;

@Service
@RequiredArgsConstructor
public class ClubProfileProvisioner {
    private static final String DEFAULT_DISPLAY_NAME = "SEMO Member";

    private final ClubProfileRepository clubProfileRepository;

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.MANDATORY)
    public ClubProfile ensureProfile(ClubMember membership, ProfileUser profileUser) {
        return ensureProfile(
                membership,
                profileUser.getDisplayName(),
                profileUser.getTagline(),
                null
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.MANDATORY)
    public ClubProfile ensureProfile(
            ClubMember membership,
            String fallbackDisplayName,
            String fallbackTagline,
            String fallbackIntroText
    ) {
        return clubProfileRepository.findByClubMemberId(membership.getClubMemberId())
                .orElseGet(() -> clubProfileRepository.save(ClubProfile.builder()
                        .clubMemberId(membership.getClubMemberId())
                        .displayName(normalizeDisplayName(fallbackDisplayName))
                        .tagline(trimToNull(fallbackTagline))
                        .introText(trimToNull(fallbackIntroText))
                        .avatarFileName(null)
                        .build()));
    }

    private String normalizeDisplayName(String displayName) {
        return StringUtils.hasText(displayName) ? displayName.trim() : DEFAULT_DISPLAY_NAME;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
