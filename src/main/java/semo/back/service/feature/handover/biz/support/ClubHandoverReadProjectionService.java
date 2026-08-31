package semo.back.service.feature.handover.biz.support;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.database.pub.entity.ClubFeedback;
import semo.back.service.database.pub.entity.ClubOperatingTerm;
import semo.back.service.database.pub.entity.ClubScheduleEvent;
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
import semo.back.service.feature.finance.vo.ClubAdminFinanceSummaryAggregate;
import semo.back.service.feature.handover.vo.ClubTermMetricsResponse;
import semo.back.service.feature.handover.vo.HandoverQueueItemResponse;
import semo.back.service.feature.handover.vo.HandoverQueueSummaryResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubHandoverReadProjectionService {
    private static final Set<String> OPEN_TODO_STATUSES = Set.of("OPEN", "IN_PROGRESS");
    private static final Set<String> OPEN_FEEDBACK_STATUSES = Set.of("RECEIVED", "IN_REVIEW");
    private static final String REQUEST_STATUS_SUBMITTED = "SUBMITTED";

    private final ClubFeatureService clubFeatureService;
    private final TodoItemRepository todoItemRepository;
    private final FinancePaymentRepository financePaymentRepository;
    private final FinanceRequestRepository financeRequestRepository;
    private final FinanceObligationRepository financeObligationRepository;
    private final FinanceExpenseRepository financeExpenseRepository;
    private final ClubScheduleEventRepository clubScheduleEventRepository;
    private final ClubFeedbackRepository clubFeedbackRepository;
    private final ClubJoinRequestRepository clubJoinRequestRepository;
    private final TournamentRecordRepository tournamentRecordRepository;
    private final ClubHandoverNoteRepository clubHandoverNoteRepository;
    private final ClubTermCarryoverItemRepository clubTermCarryoverItemRepository;

    public QueueSnapshot loadQueue(Long clubId) {
        LocalDateTime now = LocalDateTime.now();
        boolean todoEnabled = clubFeatureService.isFeatureEnabled(clubId, "TODO");
        boolean financeEnabled = clubFeatureService.isFeatureEnabled(clubId, "FINANCE");
        boolean scheduleEnabled = clubFeatureService.isFeatureEnabled(clubId, "SCHEDULE_MANAGE");
        boolean feedbackEnabled = clubFeatureService.isFeatureEnabled(clubId, "FEEDBACK");
        boolean joinRequestEnabled = clubFeatureService.isFeatureEnabled(clubId, "JOIN_REQUEST");
        List<TodoItem> openTodos = todoEnabled
                ? todoItemRepository.findByStatusCodes(clubId, OPEN_TODO_STATUSES, PageRequest.of(0, 5))
                : List.of();
        List<FinanceRequest> pendingFinanceRequests = financeEnabled ? financeRequestRepository
                .findByClubIdAndStatusCodeOrderByFinanceRequestIdDesc(
                        clubId,
                        REQUEST_STATUS_SUBMITTED,
                        PageRequest.of(0, 3)
                ) : List.of();
        List<ClubScheduleEvent> upcomingSchedules = scheduleEnabled
                ? clubScheduleEventRepository.findUpcomingActiveEvents(clubId, now, PageRequest.of(0, 3))
                : List.of();
        List<ClubFeedback> openFeedback = feedbackEnabled
                ? clubFeedbackRepository.findOpenFeedback(clubId, OPEN_FEEDBACK_STATUSES, PageRequest.of(0, 2))
                : List.of();
        int openTodoCount = todoEnabled
                ? safeCount(todoItemRepository.countByClubIdAndStatusCodeIn(clubId, OPEN_TODO_STATUSES))
                : 0;
        int overdueTodoCount = todoEnabled ? safeCount(todoItemRepository.countOverdueForAdmin(clubId, now)) : 0;
        int pendingFinanceRequestCount = financeEnabled ? safeCount(
                financeRequestRepository.countByClubIdAndStatusCode(clubId, REQUEST_STATUS_SUBMITTED)
        ) : 0;
        int upcomingScheduleCount = scheduleEnabled
                ? safeCount(clubScheduleEventRepository.countUpcomingActiveEvents(clubId, now))
                : 0;
        int openFeedbackCount = feedbackEnabled ? safeCount(
                clubFeedbackRepository.countByClubIdAndDeletedFalseAndStatusCodeIn(clubId, OPEN_FEEDBACK_STATUSES)
        ) : 0;
        int pendingJoinRequestCount = joinRequestEnabled ? safeCount(
                clubJoinRequestRepository.countByClubIdAndRequestStatus(clubId, "PENDING")
        ) : 0;
        ClubAdminFinanceSummaryAggregate financeSummary = financeEnabled
                ? financePaymentRepository.summarizeAdminFinance(clubId, now)
                : null;
        int unpaidPaymentCount = financeSummary == null ? 0 : safeCount(financeSummary.pendingPaymentCount());
        int openNoteCount = safeCount(clubHandoverNoteRepository.countByClubIdAndDeletedFalseAndStatusCodeIn(
                clubId,
                List.of("DRAFT", "READY")
        ));
        int openCarryoverCount = safeCount(clubTermCarryoverItemRepository.countByClubIdAndStatusCode(clubId, "OPEN"));

        List<HandoverQueueItemResponse> items = new ArrayList<>();
        openTodos.stream()
                .sorted(Comparator
                        .comparing(TodoItem::getDueAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(TodoItem::getTodoItemId))
                .limit(5)
                .map(item -> new HandoverQueueItemResponse(
                        "TODO_ITEM",
                        item.getTodoItemId(),
                        item.getTitle(),
                        "IN_PROGRESS".equals(item.getStatusCode()) ? "진행 중" : "대기",
                        item.getDueAt(),
                        "/clubs/%d/admin/more/todos".formatted(clubId),
                        item.getDueAt() != null && item.getDueAt().isBefore(now)
                ))
                .forEach(items::add);
        pendingFinanceRequests.stream().limit(3)
                .map(item -> new HandoverQueueItemResponse(
                        "FINANCE_REQUEST",
                        item.getFinanceRequestId(),
                        item.getTitle(),
                        "정산 검토 대기",
                        null,
                        "/clubs/%d/admin/more/finance?tab=settlements".formatted(clubId),
                        false
                ))
                .forEach(items::add);
        upcomingSchedules.stream().limit(3)
                .map(item -> new HandoverQueueItemResponse(
                        "SCHEDULE_EVENT",
                        item.getEventId(),
                        item.getTitle(),
                        "예정 일정",
                        item.getStartAt(),
                        "/clubs/%d/schedule/%d".formatted(clubId, item.getEventId()),
                        false
                ))
                .forEach(items::add);
        openFeedback.stream().limit(2)
                .map(item -> new HandoverQueueItemResponse(
                        "FEEDBACK",
                        item.getFeedbackId(),
                        item.getTitle(),
                        "IN_REVIEW".equals(item.getStatusCode()) ? "검토 중" : "접수",
                        null,
                        "/clubs/%d/admin/more/feedback".formatted(clubId),
                        false
                ))
                .forEach(items::add);
        if (pendingJoinRequestCount > 0) {
            items.add(new HandoverQueueItemResponse(
                    "JOIN_REQUEST",
                    null,
                    "가입 신청 " + pendingJoinRequestCount + "건",
                    "승인 대기",
                    null,
                    "/clubs/%d/admin/more/join-requests".formatted(clubId),
                    false
            ));
        }

        return new QueueSnapshot(
                new HandoverQueueSummaryResponse(
                        openTodoCount,
                        overdueTodoCount,
                        unpaidPaymentCount,
                        pendingFinanceRequestCount,
                        upcomingScheduleCount,
                        openFeedbackCount,
                        pendingJoinRequestCount,
                        openNoteCount,
                        openCarryoverCount
                ),
                items.stream()
                        .sorted(Comparator
                                .comparing(HandoverQueueItemResponse::urgent).reversed()
                                .thenComparing(
                                        HandoverQueueItemResponse::dueAt,
                                        Comparator.nullsLast(Comparator.naturalOrder())
                                ))
                        .limit(12)
                        .toList()
        );
    }

    public List<HandoverQueueItemResponse> loadCarryoverCandidates(Long clubId) {
        LocalDateTime now = LocalDateTime.now();
        List<HandoverQueueItemResponse> items = new ArrayList<>();
        if (clubFeatureService.isFeatureEnabled(clubId, "TODO")) {
            todoItemRepository.findAllByStatusCodes(clubId, OPEN_TODO_STATUSES).stream()
                    .map(item -> new HandoverQueueItemResponse(
                            "TODO_ITEM",
                            item.getTodoItemId(),
                            item.getTitle(),
                            "IN_PROGRESS".equals(item.getStatusCode()) ? "진행 중" : "대기",
                            item.getDueAt(),
                            "/clubs/%d/admin/more/todos".formatted(clubId),
                            item.getDueAt() != null && item.getDueAt().isBefore(now)
                    ))
                    .forEach(items::add);
        }
        if (clubFeatureService.isFeatureEnabled(clubId, "FINANCE")) {
            financeRequestRepository.findByClubIdAndStatusCodeOrderByFinanceRequestIdDesc(
                            clubId,
                            REQUEST_STATUS_SUBMITTED
                    ).stream()
                    .map(item -> new HandoverQueueItemResponse(
                            "FINANCE_REQUEST",
                            item.getFinanceRequestId(),
                            item.getTitle(),
                            "정산 검토 대기",
                            null,
                            "/clubs/%d/admin/more/finance?tab=settlements".formatted(clubId),
                            false
                    ))
                    .forEach(items::add);
        }
        if (clubFeatureService.isFeatureEnabled(clubId, "FEEDBACK")) {
            clubFeedbackRepository.findAllOpenFeedback(clubId, OPEN_FEEDBACK_STATUSES).stream()
                    .map(item -> new HandoverQueueItemResponse(
                            "FEEDBACK",
                            item.getFeedbackId(),
                            item.getTitle(),
                            "IN_REVIEW".equals(item.getStatusCode()) ? "검토 중" : "접수",
                            null,
                            "/clubs/%d/admin/more/feedback".formatted(clubId),
                            false
                    ))
                    .forEach(items::add);
        }
        return List.copyOf(items);
    }

    public ClubTermMetricsResponse loadTermMetrics(Long clubId, ClubOperatingTerm term) {
        LocalDateTime from = term.getStartDate().atStartOfDay();
        LocalDateTime toExclusive = term.getEndDate().plusDays(1).atStartOfDay();
        int todoCount = safeCount(todoItemRepository.countWithinTerm(clubId, from, toExclusive));
        int scheduleCount = safeCount(clubScheduleEventRepository.countActiveEventsWithinTerm(
                clubId,
                from,
                toExclusive
        ));
        int tournamentCount = safeCount(tournamentRecordRepository
                .countByClubIdAndDeletedFalseAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        clubId,
                        term.getEndDate(),
                        term.getStartDate()
                ));
        int obligationCount = safeCount(financeObligationRepository.countWithinTerm(clubId, from, toExclusive));
        int financeRequestCount = safeCount(financeRequestRepository
                .countByClubIdAndCreateDateGreaterThanEqualAndCreateDateLessThan(clubId, from, toExclusive));
        BigDecimal expenseAmount = financeExpenseRepository.sumAmountWithinTerm(clubId, from, toExclusive);
        return new ClubTermMetricsResponse(
                todoCount,
                scheduleCount,
                tournamentCount,
                obligationCount,
                financeRequestCount,
                expenseAmount,
                "KRW"
        );
    }

    private int safeCount(long value) {
        if (value <= 0) {
            return 0;
        }
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    public record QueueSnapshot(
            HandoverQueueSummaryResponse summary,
            List<HandoverQueueItemResponse> items
    ) {
    }
}
