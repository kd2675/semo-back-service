package semo.back.service.feature.handover.biz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.Club;
import semo.back.service.database.pub.entity.ClubFeedback;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.entity.ClubOperatingTerm;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.FinanceRequest;
import semo.back.service.database.pub.entity.ProfileUser;
import semo.back.service.database.pub.entity.ClubTermCarryoverItem;
import semo.back.service.database.pub.entity.TodoItem;
import semo.back.service.database.pub.repository.ClubFeedbackRepository;
import semo.back.service.database.pub.repository.ClubHandoverNoteRepository;
import semo.back.service.database.pub.repository.ClubJoinRequestRepository;
import semo.back.service.database.pub.repository.ClubMemberPositionRepository;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubOperatingTermRepository;
import semo.back.service.database.pub.repository.ClubPositionRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.ClubTermCarryoverItemRepository;
import semo.back.service.database.pub.repository.ClubTermExecutiveAssignmentRepository;
import semo.back.service.database.pub.repository.DecisionRecordRepository;
import semo.back.service.database.pub.repository.FinanceExpenseRepository;
import semo.back.service.database.pub.repository.FinanceObligationRepository;
import semo.back.service.database.pub.repository.FinancePaymentRepository;
import semo.back.service.database.pub.repository.FinanceRequestRepository;
import semo.back.service.database.pub.repository.TodoItemRepository;
import semo.back.service.database.pub.repository.TournamentRecordRepository;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.handover.vo.CreateOperatingTermRequest;
import semo.back.service.feature.handover.vo.UpsertHandoverNoteRequest;
import semo.back.service.feature.notification.biz.ClubNotificationPublisher;
import semo.back.service.feature.position.biz.ClubPositionPermissionEvaluator;

@ExtendWith(MockitoExtension.class)
class ClubHandoverServiceTest {
    @Mock private ClubAccessResolver clubAccessResolver;
    @Mock private ClubFeatureService clubFeatureService;
    @Mock private ClubPositionPermissionEvaluator clubPositionPermissionEvaluator;
    @Mock private ClubRepository clubRepository;
    @Mock private ClubOperatingTermRepository clubOperatingTermRepository;
    @Mock private ClubTermExecutiveAssignmentRepository clubTermExecutiveAssignmentRepository;
    @Mock private ClubTermCarryoverItemRepository clubTermCarryoverItemRepository;
    @Mock private ClubHandoverNoteRepository clubHandoverNoteRepository;
    @Mock private ClubPositionRepository clubPositionRepository;
    @Mock private ClubMemberRepository clubMemberRepository;
    @Mock private ClubProfileRepository clubProfileRepository;
    @Mock private ClubMemberPositionRepository clubMemberPositionRepository;
    @Mock private TodoItemRepository todoItemRepository;
    @Mock private FinancePaymentRepository financePaymentRepository;
    @Mock private FinanceRequestRepository financeRequestRepository;
    @Mock private FinanceObligationRepository financeObligationRepository;
    @Mock private FinanceExpenseRepository financeExpenseRepository;
    @Mock private ClubScheduleEventRepository clubScheduleEventRepository;
    @Mock private ClubFeedbackRepository clubFeedbackRepository;
    @Mock private ClubJoinRequestRepository clubJoinRequestRepository;
    @Mock private TournamentRecordRepository tournamentRecordRepository;
    @Mock private ClubNotificationPublisher clubNotificationPublisher;
    @Mock private DecisionRecordRepository decisionRecordRepository;

    @InjectMocks
    private ClubHandoverService clubHandoverService;

    @Test
    void createTerm_endBeforeStart_rejectsWithoutSave() {
        ClubAccessResolver.ClubAccess access = ownerAccess();
        when(clubAccessResolver.requireActiveMember(1L, "owner-key")).thenReturn(access);
        when(clubRepository.findForUpdate(1L)).thenReturn(Optional.of(access.club()));

        assertThatThrownBy(() -> clubHandoverService.createTerm(
                1L,
                "owner-key",
                new CreateOperatingTermRequest(
                        "2026년 2학기",
                        "SEMESTER",
                        LocalDate.of(2026, 12, 31),
                        LocalDate.of(2026, 8, 1),
                        null
                )
        )).isInstanceOf(SemoException.ValidationException.class)
                .hasMessageContaining("종료일");
        verify(clubOperatingTermRepository, never()).save(any());
    }

    @Test
    void activateTerm_existingActive_carriesEverySupportedOpenResourceAndClosesPrevious() {
        ClubAccessResolver.ClubAccess access = ownerAccess();
        ClubOperatingTerm current = term(1L, "2026 상반기", "ACTIVE", LocalDate.of(2026, 1, 1));
        ClubOperatingTerm next = term(2L, "2026 하반기", "PLANNED", LocalDate.of(2026, 7, 1));
        when(clubAccessResolver.requireActiveMember(1L, "owner-key")).thenReturn(access);
        when(clubRepository.findForUpdate(1L)).thenReturn(Optional.of(access.club()));
        when(clubOperatingTermRepository.findByClubOperatingTermIdAndClubId(2L, 1L))
                .thenReturn(Optional.of(next));
        when(clubOperatingTermRepository
                .findFirstByClubIdAndStatusCodeOrderByStartDateDescClubOperatingTermIdDesc(1L, "ACTIVE"))
                .thenReturn(Optional.of(current));
        when(todoItemRepository.findAllByStatusCodes(1L, Set.of("OPEN", "IN_PROGRESS"))).thenReturn(List.of(
                TodoItem.builder()
                        .todoItemId(11L)
                        .clubId(1L)
                        .statusCode("OPEN")
                        .title("행사 장소 확정")
                        .dueAt(LocalDateTime.of(2026, 7, 5, 18, 0))
                        .build()
        ));
        when(financeRequestRepository.findByClubIdAndStatusCodeOrderByFinanceRequestIdDesc(1L, "SUBMITTED"))
                .thenReturn(List.of(
                FinanceRequest.builder()
                        .financeRequestId(21L)
                        .clubId(1L)
                        .statusCode("SUBMITTED")
                        .title("대회 교통비 정산")
                        .amount(BigDecimal.valueOf(30_000))
                        .build()
        ));
        when(clubFeedbackRepository.findAllOpenFeedback(1L, Set.of("RECEIVED", "IN_REVIEW"))).thenReturn(List.of(
                ClubFeedback.builder()
                        .feedbackId(31L)
                        .clubId(1L)
                        .statusCode("IN_REVIEW")
                        .title("정기 모임 시간 변경")
                        .deleted(false)
                        .build()
        ));
        when(clubTermCarryoverItemRepository.existsByToTermIdAndResourceTypeAndResourceId(any(), any(), any()))
                .thenReturn(false);
        when(clubTermCarryoverItemRepository.save(any(ClubTermCarryoverItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(clubTermExecutiveAssignmentRepository.countByClubOperatingTermId(2L)).thenReturn(0L);
        when(clubPositionRepository.findByClubIdAndActiveTrueOrderByDisplayNameAscClubPositionIdAsc(1L))
                .thenReturn(List.of());

        var response = clubHandoverService.activateTerm(1L, 2L, "owner-key");

        assertThat(response.statusCode()).isEqualTo("ACTIVE");
        assertThat(current.getStatusCode()).isEqualTo("CLOSED");
        ArgumentCaptor<ClubTermCarryoverItem> captor = ArgumentCaptor.forClass(ClubTermCarryoverItem.class);
        verify(clubTermCarryoverItemRepository, org.mockito.Mockito.times(3)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(ClubTermCarryoverItem::getResourceType)
                .containsExactlyInAnyOrder("TODO_ITEM", "FINANCE_REQUEST", "FEEDBACK");
        assertThat(captor.getAllValues())
                .allSatisfy(item -> {
                    assertThat(item.getFromTermId()).isEqualTo(1L);
                    assertThat(item.getToTermId()).isEqualTo(2L);
                    assertThat(item.getStatusCode()).isEqualTo("OPEN");
                });
    }

    @Test
    void createHandoverNote_acknowledgedStatus_rejectsDirectStateSpoofing() {
        ClubAccessResolver.ClubAccess access = ownerAccess();
        ClubOperatingTerm current = term(1L, "2026 상반기", "ACTIVE", LocalDate.of(2026, 1, 1));
        when(clubAccessResolver.requireActiveMember(1L, "owner-key")).thenReturn(access);
        when(clubRepository.findForUpdate(1L)).thenReturn(Optional.of(access.club()));
        when(clubOperatingTermRepository.findByClubOperatingTermIdAndClubId(1L, 1L))
                .thenReturn(Optional.of(current));

        assertThatThrownBy(() -> clubHandoverService.createHandoverNote(
                1L,
                "owner-key",
                new UpsertHandoverNoteRequest(
                        1L,
                        null,
                        null,
                        null,
                        "운영 문서",
                        "다음 임기 담당자가 확인해야 합니다.",
                        "ACKNOWLEDGED",
                        null
                )
        )).isInstanceOf(SemoException.ValidationException.class);
        verify(clubHandoverNoteRepository, never()).save(any());
    }

    private ClubAccessResolver.ClubAccess ownerAccess() {
        Club club = Club.builder().clubId(1L).name("세모 클럽").active(true).build();
        ClubMember member = ClubMember.builder()
                .clubMemberId(10L)
                .clubId(1L)
                .profileId(100L)
                .roleCode("OWNER")
                .membershipStatus("ACTIVE")
                .build();
        ClubProfile profile = ClubProfile.builder()
                .clubProfileId(20L)
                .clubMemberId(10L)
                .displayName("운영자")
                .build();
        ProfileUser user = ProfileUser.builder()
                .profileId(100L)
                .userKey("owner-key")
                .displayName("운영자")
                .build();
        return new ClubAccessResolver.ClubAccess(club, member, profile, user);
    }

    private ClubOperatingTerm term(Long id, String name, String status, LocalDate startDate) {
        return ClubOperatingTerm.builder()
                .clubOperatingTermId(id)
                .clubId(1L)
                .termName(name)
                .termType("SEMESTER")
                .startDate(startDate)
                .endDate(startDate.plusMonths(6).minusDays(1))
                .statusCode(status)
                .createdByClubProfileId(20L)
                .build();
    }
}
