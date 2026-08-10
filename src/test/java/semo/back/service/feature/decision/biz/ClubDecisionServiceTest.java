package semo.back.service.feature.decision.biz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.Club;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.DecisionParticipant;
import semo.back.service.database.pub.entity.DecisionRecord;
import semo.back.service.database.pub.entity.ProfileUser;
import semo.back.service.database.pub.repository.ClubOperatingTermRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.DecisionParticipantRepository;
import semo.back.service.database.pub.repository.DecisionRecordRepository;
import semo.back.service.database.pub.repository.DecisionResourceLinkRepository;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.decision.vo.UpsertDecisionRecordRequest;
import semo.back.service.feature.notification.biz.ClubNotificationPublisher;
import semo.back.service.feature.position.biz.ClubPositionPermissionEvaluator;

@ExtendWith(MockitoExtension.class)
class ClubDecisionServiceTest {
    @Mock private ClubAccessResolver clubAccessResolver;
    @Mock private ClubFeatureService clubFeatureService;
    @Mock private ClubPositionPermissionEvaluator clubPositionPermissionEvaluator;
    @Mock private ClubRepository clubRepository;
    @Mock private ClubOperatingTermRepository clubOperatingTermRepository;
    @Mock private ClubProfileRepository clubProfileRepository;
    @Mock private DecisionRecordRepository decisionRecordRepository;
    @Mock private DecisionParticipantRepository decisionParticipantRepository;
    @Mock private DecisionResourceLinkRepository decisionResourceLinkRepository;
    @Mock private DecisionResourceResolver decisionResourceResolver;
    @Mock private ClubNotificationPublisher clubNotificationPublisher;

    @InjectMocks
    private ClubDecisionService clubDecisionService;

    @Test
    void createRecord_meetingMinutesWithoutMeetingAt_rejectsBeforeSave() {
        ClubAccessResolver.ClubAccess access = ownerAccess();
        when(clubAccessResolver.requireActiveMember(1L, "owner-key")).thenReturn(access);
        when(clubRepository.findForUpdate(1L)).thenReturn(Optional.of(access.club()));

        assertThatThrownBy(() -> clubDecisionService.createRecord(
                1L,
                "owner-key",
                request("MEETING_MINUTES")
        )).isInstanceOf(SemoException.ValidationException.class)
                .hasMessageContaining("회의 일시");
    }

    @Test
    void getAdminCenter_memberWithoutDelegatedPermission_rejects() {
        ClubAccessResolver.ClubAccess access = memberAccess();
        when(clubAccessResolver.requireActiveMember(1L, "member-key")).thenReturn(access);
        when(clubPositionPermissionEvaluator.hasPermission(access, ClubPositionPermissionEvaluator.PERMISSION_DECISION_VIEW))
                .thenReturn(false);
        when(clubPositionPermissionEvaluator.hasPermission(access, ClubPositionPermissionEvaluator.PERMISSION_DECISION_MANAGE))
                .thenReturn(false);

        assertThatThrownBy(() -> clubDecisionService.getAdminCenter(1L, "member-key"))
                .isInstanceOf(SemoException.ForbiddenException.class);
    }

    @Test
    void confirmRecord_replacement_marksNewAndPreviousStatesAtomically() {
        ConfirmFixture fixture = stubConfirmFixture();

        var response = clubDecisionService.confirmRecord(1L, 11L, "owner-key");

        assertThat(List.of(response.statusCode(), fixture.previous().getStatusCode()))
                .containsExactly("CONFIRMED", "SUPERSEDED");
    }

    @Test
    void confirmRecord_participant_receivesPersistentNotification() {
        stubConfirmFixture();

        clubDecisionService.confirmRecord(1L, 11L, "owner-key");

        verify(clubNotificationPublisher).notifyClubProfile(
                org.mockito.ArgumentMatchers.eq(21L),
                any(ClubNotificationPublisher.NotificationCommand.class)
        );
    }

    private ConfirmFixture stubConfirmFixture() {
        ClubAccessResolver.ClubAccess access = ownerAccess();
        DecisionRecord previous = DecisionRecord.builder()
                .decisionRecordId(10L)
                .clubId(1L)
                .recordType("DECISION")
                .statusCode("CONFIRMED")
                .visibilityScope("MEMBERS")
                .title("기존 결정")
                .decisionContent("기존 운영안")
                .createdByClubProfileId(20L)
                .confirmedByClubProfileId(20L)
                .deleted(false)
                .build();
        DecisionRecord replacement = DecisionRecord.builder()
                .decisionRecordId(11L)
                .clubId(1L)
                .recordType("DECISION")
                .statusCode("DRAFT")
                .visibilityScope("MEMBERS")
                .title("새 결정")
                .decisionContent("변경된 운영안")
                .supersedesDecisionRecordId(10L)
                .createdByClubProfileId(20L)
                .deleted(false)
                .build();
        List<DecisionParticipant> participants = List.of(
                participant(1L, 20L, "DECIDER", "운영자"),
                participant(2L, 21L, "PARTICIPANT", "참여자")
        );
        when(clubAccessResolver.requireActiveMember(1L, "owner-key")).thenReturn(access);
        when(clubRepository.findForUpdate(1L)).thenReturn(Optional.of(access.club()));
        when(decisionRecordRepository.findForUpdate(11L, 1L)).thenReturn(Optional.of(replacement));
        when(decisionRecordRepository.findForUpdate(10L, 1L)).thenReturn(Optional.of(previous));
        when(decisionParticipantRepository.findByDecisionRecordIdInOrderByDecisionParticipantIdAsc(any()))
                .thenReturn(participants);
        when(decisionRecordRepository.findByDecisionRecordIdAndClubIdAndDeletedFalse(11L, 1L))
                .thenReturn(Optional.of(replacement));
        when(decisionResourceLinkRepository.findByDecisionRecordIdInOrderByDecisionResourceLinkIdAsc(any()))
                .thenReturn(List.of());
        when(clubProfileRepository.findAllById(any())).thenReturn(List.of(access.clubProfile()));
        when(clubOperatingTermRepository.findByClubIdOrderByStartDateDescClubOperatingTermIdDesc(1L))
                .thenReturn(List.of());
        when(decisionRecordRepository.findAllById(Set.of(10L))).thenReturn(List.of(previous));
        return new ConfirmFixture(previous);
    }

    private DecisionParticipant participant(Long id, Long profileId, String role, String displayName) {
        return DecisionParticipant.builder()
                .decisionParticipantId(id)
                .decisionRecordId(11L)
                .clubProfileId(profileId)
                .participantRole(role)
                .displayNameSnapshot(displayName)
                .build();
    }

    private UpsertDecisionRecordRequest request(String recordType) {
        return new UpsertDecisionRecordRequest(
                recordType,
                "OPERATORS",
                "정기 회의",
                "운영 방식을 변경합니다.",
                "기존 방식의 지연이 반복됐습니다.",
                "담당자 혼선을 줄이기 위해 변경합니다.",
                null,
                LocalDate.of(2026, 8, 15),
                LocalDate.of(2026, 9, 15),
                null,
                null,
                List.of(20L),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private ClubAccessResolver.ClubAccess ownerAccess() {
        return access("owner-key", "OWNER", 20L, "운영자");
    }

    private ClubAccessResolver.ClubAccess memberAccess() {
        return access("member-key", "MEMBER", 21L, "참여자");
    }

    private ClubAccessResolver.ClubAccess access(
            String userKey,
            String roleCode,
            Long clubProfileId,
            String displayName
    ) {
        Club club = Club.builder().clubId(1L).name("세모 클럽").active(true).build();
        ClubMember member = ClubMember.builder()
                .clubMemberId(clubProfileId + 100L)
                .clubId(1L)
                .profileId(clubProfileId + 200L)
                .roleCode(roleCode)
                .membershipStatus("ACTIVE")
                .build();
        ClubProfile profile = ClubProfile.builder()
                .clubProfileId(clubProfileId)
                .clubMemberId(member.getClubMemberId())
                .displayName(displayName)
                .build();
        ProfileUser user = ProfileUser.builder()
                .profileId(member.getProfileId())
                .userKey(userKey)
                .displayName(displayName)
                .build();
        return new ClubAccessResolver.ClubAccess(club, member, profile, user);
    }

    private record ConfirmFixture(DecisionRecord previous) {
    }
}
