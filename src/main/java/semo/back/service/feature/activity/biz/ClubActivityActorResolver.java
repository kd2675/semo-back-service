package semo.back.service.feature.activity.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.ProfileUser;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ProfileUserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubActivityActorResolver {
    private static final String UNKNOWN_ACTOR = "알 수 없는 사용자";

    private final ProfileUserRepository profileUserRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubProfileRepository clubProfileRepository;

    public ActorSnapshot resolve(Long clubId, String userKey) {
        if (clubId == null || !StringUtils.hasText(userKey)) {
            return new ActorSnapshot(null, null, UNKNOWN_ACTOR);
        }

        ProfileUser profileUser = profileUserRepository.findByUserKey(userKey).orElse(null);
        if (profileUser == null) {
            return new ActorSnapshot(null, null, UNKNOWN_ACTOR);
        }

        ClubMember membership = clubMemberRepository.findByClubIdAndProfileId(clubId, profileUser.getProfileId()).orElse(null);
        ClubProfile clubProfile = membership == null
                ? null
                : clubProfileRepository.findByClubMemberId(membership.getClubMemberId()).orElse(null);

        String displayName = clubProfile != null && StringUtils.hasText(clubProfile.getDisplayName())
                ? clubProfile.getDisplayName().trim()
                : StringUtils.hasText(profileUser.getDisplayName())
                    ? profileUser.getDisplayName().trim()
                    : UNKNOWN_ACTOR;

        return new ActorSnapshot(
                membership == null ? null : membership.getClubMemberId(),
                clubProfile == null ? null : clubProfile.getClubProfileId(),
                displayName
        );
    }

    public record ActorSnapshot(Long actorClubMemberId, Long actorClubProfileId, String actorDisplayName) {
    }
}
