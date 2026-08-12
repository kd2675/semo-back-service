package semo.back.service.feature.clubfeature.biz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import semo.back.service.database.pub.repository.BracketRecordRepository;
import semo.back.service.database.pub.repository.ClubEventParticipantRepository;
import semo.back.service.database.pub.repository.ClubFeedbackRepository;
import semo.back.service.database.pub.repository.ClubJoinRequestRepository;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteRepository;
import semo.back.service.database.pub.repository.FinancePaymentRepository;
import semo.back.service.database.pub.repository.FinanceRequestRepository;
import semo.back.service.database.pub.repository.TodoItemApplicationRepository;
import semo.back.service.database.pub.repository.TodoItemRepository;
import semo.back.service.database.pub.repository.TournamentApplicationRepository;
import semo.back.service.database.pub.repository.TournamentRecordRepository;
import semo.back.service.feature.clubfeature.vo.ClubFeatureResponse;
import semo.back.service.feature.finance.vo.ClubAdminFinanceSummaryAggregate;

@ExtendWith(MockitoExtension.class)
class ClubMoreWorkQueueServiceTest {
    @Mock
    private TodoItemRepository todoItemRepository;
    @Mock
    private TodoItemApplicationRepository todoItemApplicationRepository;
    @Mock
    private FinancePaymentRepository financePaymentRepository;
    @Mock
    private FinanceRequestRepository financeRequestRepository;
    @Mock
    private ClubFeedbackRepository clubFeedbackRepository;
    @Mock
    private ClubJoinRequestRepository clubJoinRequestRepository;
    @Mock
    private TournamentRecordRepository tournamentRecordRepository;
    @Mock
    private TournamentApplicationRepository tournamentApplicationRepository;
    @Mock
    private BracketRecordRepository bracketRecordRepository;
    @Mock
    private ClubScheduleEventRepository clubScheduleEventRepository;
    @Mock
    private ClubEventParticipantRepository clubEventParticipantRepository;
    @Mock
    private ClubScheduleVoteRepository clubScheduleVoteRepository;

    @InjectMocks
    private ClubMoreWorkQueueService clubMoreWorkQueueService;

    @Test
    void getQueueCounts_delegatedOperator_separatesPersonalAndAdminWork() {
        when(todoItemRepository.countActiveAssigned(1L, 11L)).thenReturn(2L);
        when(todoItemRepository.countOverdueAssigned(eq(1L), eq(11L), any())).thenReturn(1L);
        when(todoItemRepository.countActiveForAdmin(1L)).thenReturn(5L);
        when(todoItemRepository.countOverdueForAdmin(eq(1L), any())).thenReturn(2L);
        when(todoItemApplicationRepository.countPendingApplicationsForClub(1L)).thenReturn(3L);
        when(financePaymentRepository.countPendingForMember(1L, 11L)).thenReturn(1L);
        when(financePaymentRepository.countOverdueForMember(eq(1L), eq(11L), any())).thenReturn(1L);
        when(financeRequestRepository.countByClubIdAndRequesterClubProfileIdAndStatusCode(
                1L,
                11L,
                "SUBMITTED"
        )).thenReturn(2L);
        when(financeRequestRepository.countByClubIdAndStatusCode(1L, "SUBMITTED")).thenReturn(4L);
        when(financePaymentRepository.summarizeAdminFinance(eq(1L), any())).thenReturn(
                new ClubAdminFinanceSummaryAggregate(
                        10,
                        6,
                        4,
                        0,
                        2,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                )
        );
        when(clubScheduleEventRepository.countPendingParticipationResponses(any(), any(), any())).thenReturn(3L);
        when(clubEventParticipantRepository.countStartedEventAttendancePending(eq(1L), any())).thenReturn(7L);

        var result = clubMoreWorkQueueService.getQueueCounts(
                1L,
                11L,
                List.of(
                        feature("TODO"),
                        feature("FINANCE"),
                        feature("SCHEDULE_MANAGE"),
                        feature("ATTENDANCE")
                ),
                Set.of("TODO", "FINANCE", "ATTENDANCE")
        );

        assertThat(result)
                .extractingByKeys("TODO", "FINANCE", "SCHEDULE_MANAGE", "ATTENDANCE")
                .containsExactly(
                        new ClubMoreWorkQueueService.FeatureQueueCounts(2, 1, 8, 2),
                        new ClubMoreWorkQueueService.FeatureQueueCounts(3, 1, 10, 2),
                        new ClubMoreWorkQueueService.FeatureQueueCounts(3, 0, 0, 0),
                        new ClubMoreWorkQueueService.FeatureQueueCounts(0, 0, 7, 0)
                );
    }

    private ClubFeatureResponse feature(String featureKey) {
        return new ClubFeatureResponse(
                featureKey,
                featureKey,
                null,
                "apps",
                "USER_AND_ADMIN",
                10,
                true,
                "/user/" + featureKey,
                "/admin/" + featureKey,
                List.of(),
                false,
                null,
                true,
                null
        );
    }
}
