package semo.back.service.feature.clubfeature.biz;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import semo.back.service.database.pub.entity.Club;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.repository.ClubMorePreferenceRepository;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.clubfeature.vo.ClubFeatureResponse;
import semo.back.service.feature.position.biz.ClubPositionPermissionEvaluator;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubMoreSummaryServiceTest {
    @Mock
    private ClubAccessResolver clubAccessResolver;

    @Mock
    private ClubFeatureService clubFeatureService;

    @Mock
    private ClubPositionPermissionEvaluator clubPositionPermissionEvaluator;

    @Mock
    private ClubMorePreferenceRepository clubMorePreferenceRepository;

    @Mock
    private ClubMoreWorkQueueService clubMoreWorkQueueService;

    @InjectMocks
    private ClubMoreSummaryService clubMoreSummaryService;

    @Test
    void getSummary_delegatedMemberReceivesOnlyViewableAdminTools() {
        Club club = mock(Club.class);
        ClubMember membership = mock(ClubMember.class);
        ClubProfile clubProfile = mock(ClubProfile.class);
        ClubAccessResolver.ClubAccess access = mock(ClubAccessResolver.ClubAccess.class);
        when(access.club()).thenReturn(club);
        when(access.membership()).thenReturn(membership);
        when(access.clubProfile()).thenReturn(clubProfile);
        when(access.isAdmin()).thenReturn(false);
        when(club.getClubId()).thenReturn(1L);
        when(club.getName()).thenReturn("권한 테스트 클럽");
        when(membership.getClubMemberId()).thenReturn(10L);
        when(clubProfile.getClubProfileId()).thenReturn(11L);
        when(clubAccessResolver.requireActiveMember(1L, "delegated-user")).thenReturn(access);
        when(clubFeatureService.getClubFeatures(1L, "delegated-user")).thenReturn(List.of(
                feature("FINANCE", 10),
                feature("TODO", 20),
                feature("ROLE_MANAGEMENT", 30),
                feature("TOURNAMENT_RECORD", 40),
                feature("BRACKET", 50),
                feature("ATTENDANCE", 60)
        ));
        when(clubPositionPermissionEvaluator.getPermissionKeysForMember(1L, 10L)).thenReturn(Set.of(
                ClubPositionPermissionEvaluator.PERMISSION_FINANCE_VIEW,
                ClubPositionPermissionEvaluator.PERMISSION_TODO_VIEW,
                ClubPositionPermissionEvaluator.PERMISSION_ROLE_MANAGEMENT_VIEW,
                ClubPositionPermissionEvaluator.PERMISSION_TOURNAMENT_REVIEW,
                ClubPositionPermissionEvaluator.PERMISSION_BRACKET_DELETE_ANY,
                ClubPositionPermissionEvaluator.PERMISSION_ATTENDANCE_MANAGE
        ));
        when(clubMorePreferenceRepository.findByClubIdAndClubProfileId(1L, 11L)).thenReturn(List.of());
        when(clubMoreWorkQueueService.getQueueCounts(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(11L),
                org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.anySet()
        )).thenReturn(java.util.Map.of());

        var response = clubMoreSummaryService.getSummary(1L, "delegated-user");

        assertThat(response.adminToolFeatureKeys()).containsExactly(
                "FINANCE",
                "TODO",
                "ROLE_MANAGEMENT",
                "TOURNAMENT_RECORD",
                "BRACKET",
                "ATTENDANCE"
        );
    }

    private ClubFeatureResponse feature(String featureKey, int sortOrder) {
        return new ClubFeatureResponse(
                featureKey,
                featureKey,
                null,
                "apps",
                "USER_AND_ADMIN",
                sortOrder,
                true,
                "/user/" + featureKey,
                "/admin/" + featureKey
        );
    }
}
