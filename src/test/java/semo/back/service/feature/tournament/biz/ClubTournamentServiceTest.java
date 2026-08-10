package semo.back.service.feature.tournament.biz;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.Club;
import semo.back.service.database.pub.repository.BracketParticipantRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.TournamentApplicationRepository;
import semo.back.service.database.pub.repository.TournamentRecordRepository;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.share.biz.ClubContentShareService;
import semo.back.service.feature.tournament.biz.policy.ClubTournamentPermissionService;
import semo.back.service.feature.tournament.biz.support.ClubTournamentSupport;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubTournamentServiceTest {
    @Mock
    private TournamentRecordRepository tournamentRecordRepository;

    @Mock
    private TournamentApplicationRepository tournamentApplicationRepository;

    @Mock
    private BracketParticipantRepository bracketParticipantRepository;

    @Mock
    private ClubProfileRepository clubProfileRepository;

    @Mock
    private ClubAccessResolver clubAccessResolver;

    @Mock
    private ClubTournamentPermissionService clubTournamentPermissionService;

    @Mock
    private ClubContentShareService clubContentShareService;

    @Mock
    private ClubTournamentSupport clubTournamentSupport;

    @InjectMocks
    private ClubTournamentService clubTournamentService;

    @Test
    void getAdminTournamentHome_delegatedReviewer_returnsCapabilityScopedHome() {
        ClubAccessResolver.ClubAccess access = accessWithClub(false);
        when(clubTournamentPermissionService.isTournamentEnabled(1L)).thenReturn(true);
        when(clubAccessResolver.requireActiveMember(1L, "delegated-reviewer")).thenReturn(access);
        when(clubTournamentPermissionService.canReviewTournament(access)).thenReturn(true);
        when(clubTournamentPermissionService.canDeleteTournament(access)).thenReturn(false);
        when(tournamentRecordRepository
                .findByClubIdAndDeletedFalseOrderByPinnedDescStartDateAscTournamentRecordIdDesc(1L))
                .thenReturn(List.of());

        var response = clubTournamentService.getAdminTournamentHome(1L, "delegated-reviewer");

        assertThat(response)
                .returns(false, item -> item.admin())
                .returns(true, item -> item.canReview())
                .returns(false, item -> item.canDelete());
        verify(clubAccessResolver, never()).requireAdmin(1L, "delegated-reviewer");
    }

    @Test
    void getAdminTournamentHome_memberWithoutOperatingPermission_throwsForbidden() {
        ClubAccessResolver.ClubAccess access = mock(ClubAccessResolver.ClubAccess.class);
        when(clubTournamentPermissionService.isTournamentEnabled(1L)).thenReturn(true);
        when(clubAccessResolver.requireActiveMember(1L, "regular-member")).thenReturn(access);
        when(clubTournamentPermissionService.canReviewTournament(access)).thenReturn(false);
        when(clubTournamentPermissionService.canDeleteTournament(access)).thenReturn(false);

        assertThatThrownBy(() -> clubTournamentService.getAdminTournamentHome(1L, "regular-member"))
                .isInstanceOf(SemoException.ForbiddenException.class)
                .hasMessage("대회 운영 도구에 접근할 권한이 없습니다.");
    }

    private ClubAccessResolver.ClubAccess accessWithClub(boolean admin) {
        Club club = mock(Club.class);
        ClubAccessResolver.ClubAccess access = mock(ClubAccessResolver.ClubAccess.class);
        when(access.club()).thenReturn(club);
        when(access.isAdmin()).thenReturn(admin);
        when(club.getClubId()).thenReturn(1L);
        when(club.getName()).thenReturn("권한 테스트 클럽");
        return access;
    }
}
