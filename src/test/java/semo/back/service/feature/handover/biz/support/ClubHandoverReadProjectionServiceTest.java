package semo.back.service.feature.handover.biz.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import semo.back.service.database.pub.entity.ClubFeedback;
import semo.back.service.database.pub.entity.FinanceRequest;
import semo.back.service.database.pub.entity.TodoItem;
import semo.back.service.database.pub.repository.ClubFeedbackRepository;
import semo.back.service.database.pub.repository.ClubHandoverNoteRepository;
import semo.back.service.database.pub.repository.ClubJoinRequestRepository;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.ClubTermCarryoverItemRepository;
import semo.back.service.database.pub.repository.FinanceExpenseRepository;
import semo.back.service.database.pub.repository.FinanceObligationRepository;
import semo.back.service.database.pub.repository.FinancePaymentRepository;
import semo.back.service.database.pub.repository.FinanceRequestRepository;
import semo.back.service.database.pub.repository.TodoItemRepository;
import semo.back.service.database.pub.repository.TournamentRecordRepository;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubHandoverReadProjectionServiceTest {
    @Mock private ClubFeatureService clubFeatureService;
    @Mock private TodoItemRepository todoItemRepository;
    @Mock private FinancePaymentRepository financePaymentRepository;
    @Mock private FinanceRequestRepository financeRequestRepository;
    @Mock private FinanceObligationRepository financeObligationRepository;
    @Mock private FinanceExpenseRepository financeExpenseRepository;
    @Mock private ClubScheduleEventRepository clubScheduleEventRepository;
    @Mock private ClubFeedbackRepository clubFeedbackRepository;
    @Mock private ClubJoinRequestRepository clubJoinRequestRepository;
    @Mock private TournamentRecordRepository tournamentRecordRepository;
    @Mock private ClubHandoverNoteRepository clubHandoverNoteRepository;
    @Mock private ClubTermCarryoverItemRepository clubTermCarryoverItemRepository;

    @InjectMocks
    private ClubHandoverReadProjectionService projectionService;

    @Test
    void loadCarryoverCandidates_enabledResources_returnsEverySupportedOpenType() {
        when(clubFeatureService.isFeatureEnabled(1L, "TODO")).thenReturn(true);
        when(clubFeatureService.isFeatureEnabled(1L, "FINANCE")).thenReturn(true);
        when(clubFeatureService.isFeatureEnabled(1L, "FEEDBACK")).thenReturn(true);
        when(todoItemRepository.findAllByStatusCodes(1L, Set.of("OPEN", "IN_PROGRESS"))).thenReturn(List.of(
                TodoItem.builder()
                        .todoItemId(11L)
                        .clubId(1L)
                        .statusCode("OPEN")
                        .title("행사 장소 확정")
                        .dueAt(LocalDateTime.of(2026, 9, 1, 18, 0))
                        .build()
        ));
        when(financeRequestRepository.findByClubIdAndStatusCodeOrderByFinanceRequestIdDesc(1L, "SUBMITTED"))
                .thenReturn(List.of(FinanceRequest.builder()
                        .financeRequestId(21L)
                        .clubId(1L)
                        .statusCode("SUBMITTED")
                        .title("교통비 정산")
                        .build()));
        when(clubFeedbackRepository.findAllOpenFeedback(1L, Set.of("RECEIVED", "IN_REVIEW")))
                .thenReturn(List.of(ClubFeedback.builder()
                        .feedbackId(31L)
                        .clubId(1L)
                        .statusCode("IN_REVIEW")
                        .title("모임 시간 변경")
                        .deleted(false)
                        .build()));

        var candidates = projectionService.loadCarryoverCandidates(1L);

        assertThat(candidates)
                .extracting(item -> item.resourceType() + ":" + item.resourceId())
                .containsExactly("TODO_ITEM:11", "FINANCE_REQUEST:21", "FEEDBACK:31");
    }
}
