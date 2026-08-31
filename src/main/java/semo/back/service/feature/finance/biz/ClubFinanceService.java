package semo.back.service.feature.finance.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubScheduleEvent;
import semo.back.service.database.pub.entity.FinanceAccount;
import semo.back.service.database.pub.entity.FinanceExpense;
import semo.back.service.database.pub.entity.FinanceExpenseRevision;
import semo.back.service.database.pub.entity.FinanceObligation;
import semo.back.service.database.pub.entity.FinancePayment;
import semo.back.service.database.pub.entity.FinancePeriod;
import semo.back.service.database.pub.entity.FinanceRequest;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.FinanceExpenseRepository;
import semo.back.service.database.pub.repository.FinanceExpenseRevisionRepository;
import semo.back.service.database.pub.repository.FinanceObligationRepository;
import semo.back.service.database.pub.repository.FinancePaymentRepository;
import semo.back.service.database.pub.repository.FinanceRequestRepository;
import semo.back.service.feature.activity.biz.ClubActivityContextHolder;
import semo.back.service.feature.activity.biz.RecordClubActivity;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.finance.biz.policy.ClubFinancePermissionService;
import semo.back.service.feature.finance.biz.policy.ClubFinancePaymentPolicy;
import semo.back.service.feature.finance.biz.support.ClubFinanceRecurrenceService;
import semo.back.service.feature.finance.biz.support.ClubFinanceResponseAssembler;
import semo.back.service.feature.finance.biz.support.ClubFinanceSupport;
import semo.back.service.feature.finance.vo.ClubAdminFinanceHomeResponse;
import semo.back.service.feature.finance.vo.ClubAdminFinanceObligationDetailResponse;
import semo.back.service.feature.finance.vo.ClubAdminFinanceObligationFeedResponse;
import semo.back.service.feature.finance.vo.ClubAdminFinanceObligationResponse;
import semo.back.service.feature.finance.vo.ClubAdminFinanceSummaryAggregate;
import semo.back.service.feature.finance.vo.ClubFinanceExpenseFeedResponse;
import semo.back.service.feature.finance.vo.ClubFinanceExpenseResponse;
import semo.back.service.feature.finance.vo.ClubFinanceHomeResponse;
import semo.back.service.feature.finance.vo.ClubFinanceMemberOptionResponse;
import semo.back.service.feature.finance.vo.ClubFinancePaymentResponse;
import semo.back.service.feature.finance.vo.ClubFinanceRequestFeedResponse;
import semo.back.service.feature.finance.vo.ClubFinanceRequestResponse;
import semo.back.service.feature.finance.vo.ClubFinanceUserObligationResponse;
import semo.back.service.feature.finance.vo.CreateFinanceExpenseRequest;
import semo.back.service.feature.finance.vo.CreateFinanceObligationRequest;
import semo.back.service.feature.finance.vo.CreateFinanceObligationResponse;
import semo.back.service.feature.finance.vo.CreateFinanceRequestRequest;
import semo.back.service.feature.finance.vo.CorrectFinanceExpenseRequest;
import semo.back.service.feature.finance.vo.FinanceExpenseRevisionResponse;
import semo.back.service.feature.finance.vo.ReviewFinanceRequestRequest;
import semo.back.service.feature.finance.vo.UpdateFinancePaymentStatusRequest;
import semo.back.service.feature.finance.vo.VoidFinanceExpenseRequest;
import semo.back.service.feature.notification.biz.ClubNotificationPublisher;
import semo.back.service.feature.notification.biz.ClubNotificationPublisher.NotificationCommand;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubFinanceService {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_WAIVED = "WAIVED";
    public static final String STATUS_OVERDUE = "OVERDUE";
    private static final String OBLIGATION_STATUS_OPEN = "OPEN";
    private static final String OBLIGATION_STATUS_CLOSED = "CLOSED";
    private static final String OBLIGATION_TYPE_FEE = "FEE";
    private static final String REQUEST_STATUS_SUBMITTED = "SUBMITTED";
    private static final String REQUEST_STATUS_APPROVED = "APPROVED";
    private static final String REQUEST_STATUS_REJECTED = "REJECTED";
    private static final String EXPENSE_TYPE_ADMIN = "ADMIN_EXPENSE";
    private static final String EXPENSE_TYPE_APPROVED_REQUEST = "APPROVED_REQUEST";
    private static final String TARGET_SCOPE_ALL_ACTIVE_MEMBERS = "ALL_ACTIVE_MEMBERS";
    private static final String TARGET_SCOPE_SELECTED_MEMBERS = "SELECTED_MEMBERS";

    private final ClubAccessResolver clubAccessResolver;
    private final FinanceObligationRepository financeObligationRepository;
    private final FinancePaymentRepository financePaymentRepository;
    private final FinanceRequestRepository financeRequestRepository;
    private final FinanceExpenseRepository financeExpenseRepository;
    private final FinanceExpenseRevisionRepository financeExpenseRevisionRepository;
    private final ClubScheduleEventRepository clubScheduleEventRepository;
    private final ClubFinancePermissionService clubFinancePermissionService;
    private final ClubFinanceSupport clubFinanceSupport;
    private final ClubFinanceOperationsService clubFinanceOperationsService;
    private final ClubNotificationPublisher clubNotificationPublisher;
    private final ClubFinancePaymentPolicy clubFinancePaymentPolicy;
    private final ClubFinanceResponseAssembler clubFinanceResponseAssembler;
    private final ClubFinanceRecurrenceService clubFinanceRecurrenceService;

    public ClubFinanceHomeResponse getFinance(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireFinanceFeature(clubId);

        List<FinancePayment> payments = financePaymentRepository.findByClubIdAndClubProfileIdOrderByFinancePaymentIdDesc(
                clubId,
                access.clubProfile().getClubProfileId()
        );
        List<ClubFinanceUserObligationResponse> paymentHistory = clubFinanceResponseAssembler
                .toUserObligationResponses(
                        payments,
                        access.clubProfile().getDisplayName(),
                        access.membership().getRoleCode()
                ).stream()
                .sorted(Comparator.comparing(ClubFinanceUserObligationResponse::issuedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<ClubFinanceUserObligationResponse> openObligations = paymentHistory.stream()
                .filter(obligation -> STATUS_PENDING.equals(obligation.payment().paymentStatusCode()) || STATUS_OVERDUE.equals(obligation.payment().paymentStatusCode()))
                .sorted(Comparator
                        .comparing(
                                (ClubFinanceUserObligationResponse obligation) -> clubFinanceSupport.parseNullableDateTime(obligation.dueAt()),
                                Comparator.nullsLast(Comparator.naturalOrder())
                        )
                        .thenComparing(ClubFinanceUserObligationResponse::issuedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        List<ClubFinanceUserObligationResponse> recentPayments = paymentHistory.stream()
                .filter(obligation -> STATUS_PAID.equals(obligation.payment().paymentStatusCode()))
                .sorted(Comparator
                        .comparing(
                                (ClubFinanceUserObligationResponse obligation) -> clubFinanceSupport.resolvePaidActivityAt(obligation),
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
                        .thenComparing(ClubFinanceUserObligationResponse::issuedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .toList();

        ClubFinanceUserObligationResponse nextPayableObligation = openObligations.stream().findFirst().orElse(null);

        return new ClubFinanceHomeResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                (int) paymentHistory.stream().filter(item -> STATUS_PENDING.equals(item.payment().paymentStatusCode())).count(),
                (int) paymentHistory.stream().filter(item -> STATUS_PAID.equals(item.payment().paymentStatusCode())).count(),
                (int) paymentHistory.stream().filter(item -> item.payment().overdue()).count(),
                openObligations.size(),
                clubFinanceSupport.formatAmount(
                        paymentHistory.stream()
                                .map(ClubFinanceUserObligationResponse::payment)
                                .filter(payment -> STATUS_PENDING.equals(payment.paymentStatusCode()) || payment.overdue())
                                .map(ClubFinancePaymentResponse::amount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        "KRW"
                ),
                clubFinanceSupport.formatAmount(
                        paymentHistory.stream()
                                .map(ClubFinanceUserObligationResponse::payment)
                                .filter(payment -> STATUS_PAID.equals(payment.paymentStatusCode()))
                                .map(ClubFinancePaymentResponse::amount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        "KRW"
                ),
                recentPayments,
                nextPayableObligation,
                openObligations,
                paymentHistory,
                clubFinanceOperationsService.getScheduleOptions(clubId)
        );
    }

    public ClubAdminFinanceHomeResponse getAdminFinance(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireFinanceFeature(clubId);
        requireAdminFinanceView(access);

        List<ClubAccessResolver.ClubMemberSnapshot> activeMemberSnapshots = clubAccessResolver.getActiveMemberSnapshots(clubId);
        List<ClubFinanceMemberOptionResponse> availableMembers = activeMemberSnapshots.stream()
                .map(snapshot -> new ClubFinanceMemberOptionResponse(
                        snapshot.clubProfile().getClubProfileId(),
                        snapshot.clubProfile().getDisplayName(),
                        snapshot.membership().getRoleCode()
                ))
                .toList();

        ClubAdminFinanceSummaryAggregate summary = financePaymentRepository.summarizeAdminFinance(clubId, LocalDateTime.now());
        int totalObligationCount = Math.toIntExact(financeObligationRepository.countByClubId(clubId));
        int totalPaymentCount = Math.toIntExact(summary.totalPaymentCount());
        int pendingPaymentCount = Math.toIntExact(summary.pendingPaymentCount());
        int paidPaymentCount = Math.toIntExact(summary.paidPaymentCount());
        int waivedPaymentCount = Math.toIntExact(summary.waivedPaymentCount());
        int overduePaymentCount = Math.toIntExact(summary.overduePaymentCount());
        int collectiblePaymentCount = totalPaymentCount - waivedPaymentCount;

        return new ClubAdminFinanceHomeResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                clubFinancePermissionService.canIssueFinance(access),
                clubFinancePermissionService.canMarkPaid(access),
                clubFinancePermissionService.canMarkWaived(access),
                clubFinancePermissionService.canManageBilling(access),
                clubFinancePermissionService.canReviewRequests(access),
                clubFinancePermissionService.canCreateExpenses(access),
                clubFinancePermissionService.canUpdatePayments(access),
                clubFinancePermissionService.canExport(access),
                clubFinancePermissionService.canClosePeriods(access),
                activeMemberSnapshots.size(),
                totalObligationCount,
                totalPaymentCount,
                pendingPaymentCount,
                paidPaymentCount,
                waivedPaymentCount,
                overduePaymentCount,
                collectiblePaymentCount == 0 ? 0 : (int) Math.round((paidPaymentCount * 100.0) / collectiblePaymentCount),
                clubFinanceSupport.formatAmount(summary.totalBilledAmount(), "KRW"),
                clubFinanceSupport.formatAmount(summary.totalCollectedAmount(), "KRW"),
                clubFinanceSupport.formatAmount(summary.totalOutstandingAmount(), "KRW"),
                clubFinanceSupport.formatAmount(summary.totalWaivedAmount(), "KRW"),
                availableMembers
        );
    }

    public ClubFinanceRequestFeedResponse getMyFinanceRequests(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireFinanceFeature(clubId);

        List<FinanceRequest> requests = financeRequestRepository.findByClubIdAndRequesterClubProfileIdOrderByFinanceRequestIdDesc(
                clubId,
                access.clubProfile().getClubProfileId()
        );

        return new ClubFinanceRequestFeedResponse(
                access.club().getClubId(),
                access.club().getName(),
                clubFinanceResponseAssembler.toFinanceRequestResponses(requests)
        );
    }

    public ClubFinanceRequestFeedResponse getAdminFinanceRequests(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireFinanceFeature(clubId);
        requireAdminFinanceView(access);

        List<FinanceRequest> requests = financeRequestRepository.findByClubIdOrderByFinanceRequestIdDesc(clubId);
        return new ClubFinanceRequestFeedResponse(
                access.club().getClubId(),
                access.club().getName(),
                clubFinanceResponseAssembler.toFinanceRequestResponses(requests)
        );
    }

    public ClubFinanceExpenseFeedResponse getAdminFinanceExpenses(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireFinanceFeature(clubId);
        requireAdminFinanceView(access);

        List<FinanceExpense> expenses = financeExpenseRepository.findByClubIdOrderBySpentAtDescFinanceExpenseIdDesc(clubId);
        return new ClubFinanceExpenseFeedResponse(
                access.club().getClubId(),
                access.club().getName(),
                clubFinanceResponseAssembler.toFinanceExpenseResponses(expenses)
        );
    }

    public ClubAdminFinanceObligationFeedResponse getAdminFinanceObligations(
            Long clubId,
            String userKey,
            String query,
            String obligationFilter,
            Long cursorObligationId,
            Integer size
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireFinanceFeature(clubId);
        requireAdminFinanceView(access);

        int pageSize = clubFinanceSupport.normalizeAdminObligationPageSize(size);
        String normalizedQuery = clubFinanceSupport.normalizeSearchQuery(query);
        String normalizedObligationFilter = clubFinanceSupport.normalizeAdminObligationFilter(obligationFilter);
        List<FinanceObligation> obligations = financeObligationRepository.findAdminFeed(
                clubId,
                cursorObligationId,
                normalizedQuery,
                normalizedObligationFilter,
                PageRequest.of(0, pageSize + 1)
        );
        boolean hasNext = obligations.size() > pageSize;
        List<FinanceObligation> pageObligations = hasNext ? obligations.subList(0, pageSize) : obligations;
        List<ClubAdminFinanceObligationResponse> items = clubFinanceResponseAssembler
                .toAdminObligationResponses(pageObligations);
        FinanceObligation lastObligation = pageObligations.isEmpty() ? null : pageObligations.get(pageObligations.size() - 1);

        return new ClubAdminFinanceObligationFeedResponse(
                access.club().getClubId(),
                access.club().getName(),
                items,
                lastObligation == null ? null : lastObligation.getFinanceObligationId(),
                hasNext
        );
    }

    public ClubAdminFinanceObligationDetailResponse getAdminFinanceObligationDetail(Long clubId, Long obligationId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireFinanceFeature(clubId);
        requireAdminFinanceView(access);

        FinanceObligation obligation = financeObligationRepository.findByFinanceObligationIdAndClubId(obligationId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("FinanceObligation", "obligationId", obligationId));
        List<FinancePayment> payments = financePaymentRepository.findByFinanceObligationIdOrderByFinancePaymentIdDesc(obligationId);
        return new ClubAdminFinanceObligationDetailResponse(
                clubFinanceResponseAssembler.toAdminObligationResponse(obligation, payments),
                clubFinanceResponseAssembler.toPaymentResponses(obligation, payments)
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "재정관리")
    public CreateFinanceObligationResponse createObligation(
            Long clubId,
            String userKey,
            CreateFinanceObligationRequest request
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireFinanceFeature(clubId);
        if (!clubFinancePermissionService.canManageBilling(access)) {
            throw new SemoException.ForbiddenException("재정 항목을 발행할 권한이 없습니다.");
        }

        String title = clubFinanceSupport.normalizeTitle(request.title());
        String targetScopeCode = clubFinanceSupport.normalizeTargetScope(request.targetScopeCode());
        BigDecimal amount = clubFinanceSupport.normalizeAmount(request.amount());
        LocalDateTime dueAt = clubFinanceSupport.parseDateTime(request.dueAt(), "납부 마감일 형식이 잘못되었습니다.");
        String note = clubFinanceSupport.trimToNull(request.note());
        String recurrenceFrequency = clubFinanceSupport.normalizeRecurrenceFrequency(request.recurrenceFrequency());
        int recurrenceInterval = clubFinanceSupport.normalizeRecurrenceInterval(request.recurrenceInterval());
        LocalDate recurrenceEndDate = "NONE".equals(recurrenceFrequency) ? null : request.recurrenceEndDate();
        if (!"NONE".equals(recurrenceFrequency) && dueAt == null) {
            throw new SemoException.ValidationException("반복 회비는 첫 납부 마감일이 필요합니다.");
        }
        if (recurrenceEndDate != null && recurrenceEndDate.isBefore(dueAt.toLocalDate())) {
            throw new SemoException.ValidationException("반복 종료일은 첫 납부 마감일보다 빠를 수 없습니다.");
        }
        FinancePeriod period = clubFinanceOperationsService.resolveWritablePeriod(
                clubId,
                request.financePeriodId(),
                dueAt == null ? LocalDate.now() : dueAt.toLocalDate()
        );
        FinanceAccount account = clubFinanceOperationsService.resolveActiveAccount(
                clubId,
                request.financeAccountId(),
                "COLLECTION"
        );
        ClubScheduleEvent linkedEvent = clubFinanceOperationsService.resolveScheduleEvent(
                clubId,
                request.linkedScheduleEventId()
        );

        List<ClubAccessResolver.ClubMemberSnapshot> activeMembers = clubAccessResolver.getActiveMemberSnapshots(clubId);
        List<ClubAccessResolver.ClubMemberSnapshot> targetMembers = resolveTargetMembers(
                activeMembers,
                targetScopeCode,
                request.clubProfileIds()
        );
        if (targetMembers.isEmpty()) {
            throw new SemoException.ValidationException("재정 항목을 발행할 활성 멤버가 없습니다.");
        }

        FinanceObligation obligation = financeObligationRepository.save(FinanceObligation.builder()
                .clubId(clubId)
                .createdByClubProfileId(access.clubProfile().getClubProfileId())
                .financePeriodId(period == null ? null : period.getFinancePeriodId())
                .financeAccountId(account == null ? null : account.getFinanceAccountId())
                .linkedScheduleEventId(linkedEvent == null ? null : linkedEvent.getEventId())
                .obligationTypeCode(OBLIGATION_TYPE_FEE)
                .title(title)
                .targetScopeCode(targetScopeCode)
                .amount(amount)
                .currencyCode("KRW")
                .dueAt(dueAt)
                .statusCode(OBLIGATION_STATUS_OPEN)
                .note(note)
                .recurrenceFrequency(recurrenceFrequency)
                .recurrenceInterval(recurrenceInterval)
                .recurrenceEndDate(recurrenceEndDate)
                .recurrenceSourceFinanceObligationId(null)
                .build());

        List<FinancePayment> payments = targetMembers.stream()
                .map(snapshot -> FinancePayment.builder()
                        .financeObligationId(obligation.getFinanceObligationId())
                        .clubId(clubId)
                        .clubProfileId(snapshot.clubProfile().getClubProfileId())
                        .financeAccountId(account == null ? null : account.getFinanceAccountId())
                        .amount(amount)
                        .currencyCode("KRW")
                        .paymentStatusCode(STATUS_PENDING)
                        .paidAt(null)
                        .paymentMethodCode(null)
                        .note(note)
                        .build())
                .toList();
        financePaymentRepository.saveAll(payments);
        clubFinanceRecurrenceService.notifyObligationCreated(obligation, payments);

        ClubActivityContextHolder.setDetails(
                title + " 재정 항목을 " + payments.size() + "명에게 발행했습니다.",
                "재정 항목 발행에 실패했습니다."
        );

        return new CreateFinanceObligationResponse(
                obligation.getFinanceObligationId(),
                obligation.getObligationTypeCode(),
                obligation.getTitle(),
                obligation.getTargetScopeCode(),
                clubFinanceSupport.resolveTargetScopeLabel(obligation.getTargetScopeCode()),
                payments.size()
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "재정관리")
    public ClubFinanceRequestResponse createFinanceRequest(
            Long clubId,
            String userKey,
            CreateFinanceRequestRequest request
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireFinanceFeature(clubId);

        ClubScheduleEvent linkedEvent = clubFinanceOperationsService.resolveScheduleEvent(
                clubId,
                request.linkedScheduleEventId()
        );

        FinanceRequest saved = financeRequestRepository.save(FinanceRequest.builder()
                .clubId(clubId)
                .requesterClubProfileId(access.clubProfile().getClubProfileId())
                .requestTypeCode(clubFinanceSupport.normalizeRequestType(request.requestTypeCode()))
                .title(clubFinanceSupport.normalizeRequestTitle(request.title()))
                .amount(clubFinanceSupport.normalizeAmount(request.amount()))
                .currencyCode("KRW")
                .linkedScheduleEventId(linkedEvent == null ? null : linkedEvent.getEventId())
                .relatedEventName(resolveRelatedEventName(request.relatedEventName(), linkedEvent))
                .note(clubFinanceSupport.trimToNull(request.note()))
                .statusCode(REQUEST_STATUS_SUBMITTED)
                .build());

        ClubActivityContextHolder.setDetails(
                saved.getTitle() + " 재정 요청을 제출했습니다.",
                "재정 요청 제출에 실패했습니다."
        );
        return clubFinanceResponseAssembler.toFinanceRequestResponse(saved);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "재정관리")
    public ClubFinanceRequestResponse reviewFinanceRequest(
            Long clubId,
            Long requestId,
            String userKey,
            ReviewFinanceRequestRequest request
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireFinanceFeature(clubId);
        if (!clubFinancePermissionService.canReviewRequests(access)) {
            throw new SemoException.ForbiddenException("재정 요청을 검토할 권한이 없습니다.");
        }

        FinanceRequest financeRequest = financeRequestRepository.findForUpdate(requestId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("FinanceRequest", "requestId", requestId));
        if (financeRequest.getRequesterClubProfileId().equals(access.clubProfile().getClubProfileId())) {
            throw new SemoException.ForbiddenException("본인이 제출한 재정 요청은 직접 검토할 수 없습니다.");
        }
        if (!REQUEST_STATUS_SUBMITTED.equals(financeRequest.getStatusCode())) {
            throw new SemoException.ValidationException("이미 검토가 완료된 재정 요청입니다.");
        }
        String reviewStatus = clubFinanceSupport.normalizeReviewStatus(request.statusCode());
        LocalDateTime reviewedAt = LocalDateTime.now();
        FinanceRequest updated = financeRequestRepository.save(FinanceRequest.builder()
                .financeRequestId(financeRequest.getFinanceRequestId())
                .clubId(financeRequest.getClubId())
                .requesterClubProfileId(financeRequest.getRequesterClubProfileId())
                .requestTypeCode(financeRequest.getRequestTypeCode())
                .title(financeRequest.getTitle())
                .amount(financeRequest.getAmount())
                .currencyCode(financeRequest.getCurrencyCode())
                .linkedScheduleEventId(financeRequest.getLinkedScheduleEventId())
                .relatedEventName(financeRequest.getRelatedEventName())
                .note(financeRequest.getNote())
                .statusCode(reviewStatus)
                .reviewedByClubProfileId(access.clubProfile().getClubProfileId())
                .reviewedAt(reviewedAt)
                .reviewNote(clubFinanceSupport.trimToNull(request.reviewNote()))
                .build());

        if (REQUEST_STATUS_APPROVED.equals(reviewStatus)) {
            FinancePeriod period = clubFinanceOperationsService.resolveWritablePeriod(
                    clubId,
                    null,
                    reviewedAt.toLocalDate()
            );
            FinanceAccount account = clubFinanceOperationsService.resolveActiveAccount(clubId, null, "EXPENSE");
            financeExpenseRepository.save(FinanceExpense.builder()
                    .clubId(clubId)
                    .enteredByClubProfileId(access.clubProfile().getClubProfileId())
                    .sourceFinanceRequestId(updated.getFinanceRequestId())
                    .financePeriodId(period == null ? null : period.getFinancePeriodId())
                    .financeAccountId(account == null ? null : account.getFinanceAccountId())
                    .linkedScheduleEventId(updated.getLinkedScheduleEventId())
                    .expenseTypeCode(EXPENSE_TYPE_APPROVED_REQUEST)
                    .categoryCode("REFUND_REQUEST".equals(updated.getRequestTypeCode()) ? "REFUND" : "OTHER")
                    .title(updated.getTitle())
                    .amount(updated.getAmount())
                    .currencyCode(updated.getCurrencyCode())
                    .spentAt(reviewedAt)
                    .relatedEventName(updated.getRelatedEventName())
                    .note(updated.getNote())
                    .statusCode("POSTED")
                    .build());
        }

        ClubActivityContextHolder.setDetails(
                updated.getTitle() + " 요청을 " + clubFinanceSupport.resolveRequestStatusLabel(updated.getStatusCode()) + " 처리했습니다.",
                "재정 요청 검토에 실패했습니다."
        );
        String statusLabel = clubFinanceSupport.resolveRequestStatusLabel(updated.getStatusCode());
        String notificationMessage = "'" + updated.getTitle() + "' 요청이 " + statusLabel + " 처리되었습니다.";
        if (updated.getReviewNote() != null && !updated.getReviewNote().isBlank()) {
            notificationMessage += " · " + updated.getReviewNote();
        }
        clubNotificationPublisher.notifyClubProfile(
                updated.getRequesterClubProfileId(),
                new NotificationCommand(
                        clubId,
                        "FINANCE_REQUEST_REVIEW",
                        "재정 요청 검토가 완료되었습니다",
                        notificationMessage,
                        "FINANCE_REQUEST",
                        updated.getFinanceRequestId(),
                        "/clubs/" + clubId + "/more/finance",
                        "finance-request:" + updated.getFinanceRequestId() + ":" + updated.getStatusCode()
                )
        );
        return clubFinanceResponseAssembler.toFinanceRequestResponse(updated);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "재정관리")
    public ClubFinanceExpenseResponse createFinanceExpense(
            Long clubId,
            String userKey,
            CreateFinanceExpenseRequest request
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireFinanceFeature(clubId);
        if (!clubFinancePermissionService.canCreateExpenses(access)) {
            throw new SemoException.ForbiddenException("지출을 입력할 권한이 없습니다.");
        }

        LocalDateTime spentAt = clubFinanceSupport.parseDateTime(
                request.spentAt(),
                "지출 일시 형식이 잘못되었습니다.",
                LocalDateTime.now()
        );
        FinancePeriod period = clubFinanceOperationsService.resolveWritablePeriod(
                clubId,
                request.financePeriodId(),
                spentAt.toLocalDate()
        );
        FinanceAccount account = clubFinanceOperationsService.resolveActiveAccount(
                clubId,
                request.financeAccountId(),
                "EXPENSE"
        );
        ClubScheduleEvent linkedEvent = clubFinanceOperationsService.resolveScheduleEvent(
                clubId,
                request.linkedScheduleEventId()
        );

        FinanceExpense saved = financeExpenseRepository.save(FinanceExpense.builder()
                .clubId(clubId)
                .enteredByClubProfileId(access.clubProfile().getClubProfileId())
                .sourceFinanceRequestId(null)
                .financePeriodId(period == null ? null : period.getFinancePeriodId())
                .financeAccountId(account == null ? null : account.getFinanceAccountId())
                .linkedScheduleEventId(linkedEvent == null ? null : linkedEvent.getEventId())
                .expenseTypeCode(EXPENSE_TYPE_ADMIN)
                .categoryCode(clubFinanceSupport.normalizeExpenseCategory(request.categoryCode()))
                .title(clubFinanceSupport.normalizeExpenseTitle(request.title()))
                .amount(clubFinanceSupport.normalizeAmount(request.amount()))
                .currencyCode("KRW")
                .spentAt(spentAt)
                .relatedEventName(resolveRelatedEventName(request.relatedEventName(), linkedEvent))
                .note(clubFinanceSupport.trimToNull(request.note()))
                .statusCode("POSTED")
                .build());

        ClubActivityContextHolder.setDetails(
                saved.getTitle() + " 지출을 입력했습니다.",
                "지출 입력에 실패했습니다."
        );
        return clubFinanceResponseAssembler.toFinanceExpenseResponse(saved);
    }

    public List<FinanceExpenseRevisionResponse> getExpenseRevisions(
            Long clubId,
            Long expenseId,
            String userKey
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireFinanceFeature(clubId);
        requireAdminFinanceView(access);
        financeExpenseRepository.findByFinanceExpenseIdAndClubId(expenseId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "FinanceExpense",
                        "expenseId",
                        expenseId
                ));
        List<FinanceExpenseRevision> revisions = financeExpenseRevisionRepository
                .findByFinanceExpenseIdOrderByFinanceExpenseRevisionIdDesc(expenseId);
        return clubFinanceResponseAssembler.toExpenseRevisionResponses(revisions);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "재정관리")
    public ClubFinanceExpenseResponse correctFinanceExpense(
            Long clubId,
            Long expenseId,
            String userKey,
            CorrectFinanceExpenseRequest request
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireFinanceFeature(clubId);
        if (!clubFinancePermissionService.canCreateExpenses(access)) {
            throw new SemoException.ForbiddenException("지출을 정정할 권한이 없습니다.");
        }
        FinanceExpense expense = financeExpenseRepository.findForUpdate(expenseId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "FinanceExpense",
                        "expenseId",
                        expenseId
                ));
        requirePostedExpense(expense);
        requireExistingExpensePeriodWritable(expense);

        LocalDateTime spentAt = clubFinanceSupport.parseDateTime(
                request.spentAt(),
                "지출 일시 형식이 잘못되었습니다.",
                expense.getSpentAt()
        );
        FinancePeriod period = clubFinanceOperationsService.resolveWritablePeriod(
                clubId,
                request.financePeriodId(),
                spentAt.toLocalDate()
        );
        FinanceAccount account = clubFinanceOperationsService.resolveActiveAccount(
                clubId,
                request.financeAccountId(),
                "EXPENSE"
        );
        ClubScheduleEvent linkedEvent = clubFinanceOperationsService.resolveScheduleEvent(
                clubId,
                request.linkedScheduleEventId()
        );
        String nextTitle = clubFinanceSupport.normalizeExpenseTitle(request.title());
        String nextCategoryCode = clubFinanceSupport.normalizeExpenseCategory(request.categoryCode());
        BigDecimal nextAmount = clubFinanceSupport.normalizeAmount(request.amount());
        String nextNote = clubFinanceSupport.trimToNull(request.note());
        String reason = clubFinanceSupport.normalizeReason(request.reason());
        FinanceExpenseRevision revision = createExpenseRevision(
                expense,
                access.clubProfile().getClubProfileId(),
                "CORRECTION",
                nextTitle,
                nextCategoryCode,
                nextAmount,
                spentAt,
                period == null ? null : period.getFinancePeriodId(),
                linkedEvent == null ? null : linkedEvent.getEventId(),
                account == null ? null : account.getFinanceAccountId(),
                nextNote,
                "POSTED",
                reason
        );
        financeExpenseRevisionRepository.save(revision);
        expense.correct(
                period == null ? null : period.getFinancePeriodId(),
                account == null ? null : account.getFinanceAccountId(),
                linkedEvent == null ? null : linkedEvent.getEventId(),
                nextCategoryCode,
                nextTitle,
                nextAmount,
                spentAt,
                resolveRelatedEventName(request.relatedEventName(), linkedEvent),
                nextNote
        );
        financeExpenseRepository.save(expense);

        ClubActivityContextHolder.setDetails(
                expense.getTitle() + " 지출을 정정했습니다. 사유: " + reason,
                "지출 정정에 실패했습니다."
        );
        return clubFinanceResponseAssembler.toFinanceExpenseResponse(expense);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "재정관리")
    public ClubFinanceExpenseResponse voidFinanceExpense(
            Long clubId,
            Long expenseId,
            String userKey,
            VoidFinanceExpenseRequest request
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireFinanceFeature(clubId);
        if (!clubFinancePermissionService.canCreateExpenses(access)) {
            throw new SemoException.ForbiddenException("지출을 취소할 권한이 없습니다.");
        }
        FinanceExpense expense = financeExpenseRepository.findForUpdate(expenseId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "FinanceExpense",
                        "expenseId",
                        expenseId
                ));
        requirePostedExpense(expense);
        requireExistingExpensePeriodWritable(expense);
        String reason = clubFinanceSupport.normalizeReason(request.reason());
        financeExpenseRevisionRepository.save(createExpenseRevision(
                expense,
                access.clubProfile().getClubProfileId(),
                "VOID",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "VOIDED",
                reason
        ));
        expense.voidExpense(access.clubProfile().getClubProfileId(), LocalDateTime.now(), reason);
        financeExpenseRepository.save(expense);

        ClubActivityContextHolder.setDetails(
                expense.getTitle() + " 지출을 취소했습니다. 사유: " + reason,
                "지출 취소에 실패했습니다."
        );
        return clubFinanceResponseAssembler.toFinanceExpenseResponse(expense);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "재정관리")
    public void deleteObligation(Long clubId, Long obligationId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireFinanceFeature(clubId);
        if (!clubFinancePermissionService.canManageBilling(access)) {
            throw new SemoException.ForbiddenException("재정 항목을 삭제할 권한이 없습니다.");
        }

        FinanceObligation obligation = financeObligationRepository.findByFinanceObligationIdAndClubId(obligationId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("FinanceObligation", "obligationId", obligationId));
        List<FinancePayment> payments = financePaymentRepository.findByFinanceObligationIdOrderByFinancePaymentIdDesc(obligationId);
        if (!clubFinancePaymentPolicy.canDeleteObligation(payments)) {
            throw new SemoException.ValidationException("아직 아무도 처리하지 않은 재정 항목만 삭제할 수 있습니다.");
        }
        if (financeObligationRepository
                .findByRecurrenceSourceFinanceObligationId(obligation.getFinanceObligationId())
                .isPresent()) {
            throw new SemoException.ValidationException("다음 반복 회비가 생성된 재정 항목은 삭제할 수 없습니다.");
        }

        financePaymentRepository.deleteAll(payments);
        financeObligationRepository.delete(obligation);

        ClubActivityContextHolder.setDetails(
                obligation.getTitle() + " 재정 항목을 삭제했습니다.",
                "재정 항목 삭제에 실패했습니다."
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "재정관리")
    public ClubFinancePaymentResponse updatePaymentStatus(
            Long clubId,
            Long paymentId,
            String userKey,
            UpdateFinancePaymentStatusRequest request
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireFinanceFeature(clubId);

        String nextStatus = clubFinanceSupport.normalizeUpdateStatus(request.paymentStatusCode());
        if (STATUS_PAID.equals(nextStatus) && !clubFinancePermissionService.canMarkPaid(access)) {
            throw new SemoException.ForbiddenException("재정 항목을 납부 완료 처리할 권한이 없습니다.");
        }
        if (STATUS_WAIVED.equals(nextStatus) && !clubFinancePermissionService.canMarkWaived(access)) {
            throw new SemoException.ForbiddenException("재정 항목을 면제 처리할 권한이 없습니다.");
        }
        if (STATUS_PENDING.equals(nextStatus)
                && !clubFinancePermissionService.canMarkPaid(access)
                && !clubFinancePermissionService.canMarkWaived(access)) {
            throw new SemoException.ForbiddenException("재정 상태를 변경할 권한이 없습니다.");
        }

        FinancePayment payment = financePaymentRepository.findForUpdate(paymentId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("FinancePayment", "paymentId", paymentId));
        FinanceObligation obligation = financeObligationRepository.findForUpdate(payment.getFinanceObligationId(), clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("FinanceObligation", "obligationId", payment.getFinanceObligationId()));
        if (STATUS_PENDING.equals(nextStatus)
                && OBLIGATION_STATUS_CLOSED.equals(obligation.getStatusCode())
                && financeObligationRepository
                        .findByRecurrenceSourceFinanceObligationId(obligation.getFinanceObligationId())
                        .isPresent()) {
            throw new SemoException.ValidationException("다음 반복 회비가 이미 생성되어 이전 회비를 다시 열 수 없습니다.");
        }
        if (obligation.getFinancePeriodId() != null) {
            clubFinanceOperationsService.resolveWritablePeriod(
                    clubId,
                    obligation.getFinancePeriodId(),
                    obligation.getDueAt() == null ? LocalDate.now() : obligation.getDueAt().toLocalDate()
            );
        }
        String note = clubFinanceSupport.trimToNull(request.note());
        LocalDateTime paidAt = STATUS_PAID.equals(nextStatus) ? LocalDateTime.now() : null;
        FinanceAccount account = null;
        String paymentMethodCode = null;
        if (STATUS_PAID.equals(nextStatus)) {
            Long requestedAccountId = request.financeAccountId() != null
                    ? request.financeAccountId()
                    : payment.getFinanceAccountId();
            account = clubFinanceOperationsService.resolveActiveAccount(clubId, requestedAccountId, "COLLECTION");
            paymentMethodCode = clubFinanceSupport.normalizePaymentMethod(request.paymentMethodCode());
            if (paymentMethodCode == null) {
                paymentMethodCode = inferPaymentMethod(account);
            }
        }
        payment.updateStatus(
                nextStatus,
                paidAt,
                account == null ? payment.getFinanceAccountId() : account.getFinanceAccountId(),
                STATUS_PAID.equals(nextStatus) ? paymentMethodCode : payment.getPaymentMethodCode(),
                note == null ? payment.getNote() : note
        );
        FinancePayment updated = financePaymentRepository.save(payment);

        syncObligationStatus(obligation);

        ClubActivityContextHolder.setDetails(
                obligation.getTitle() + " 재정 상태를 " + clubFinanceSupport.resolvePaymentStatusLabel(nextStatus, false) + "로 변경했습니다.",
                "재정 상태 변경에 실패했습니다."
        );

        return clubFinanceResponseAssembler.toPaymentResponse(updated, obligation);
    }

    private void requireFinanceFeature(Long clubId) {
        if (!clubFinancePermissionService.isFinanceEnabled(clubId)) {
            throw new SemoException.ForbiddenException("재정관리 기능이 활성화되지 않았습니다.");
        }
    }

    private void requireAdminFinanceView(ClubAccessResolver.ClubAccess access) {
        if (!clubFinancePermissionService.canViewAdminFinance(access)) {
            throw new SemoException.ForbiddenException("재정관리 화면을 조회할 권한이 없습니다.");
        }
    }

    private List<ClubAccessResolver.ClubMemberSnapshot> resolveTargetMembers(
            List<ClubAccessResolver.ClubMemberSnapshot> activeMembers,
            String targetScopeCode,
            Collection<Long> requestedClubProfileIds
    ) {
        if (TARGET_SCOPE_ALL_ACTIVE_MEMBERS.equals(targetScopeCode)) {
            return activeMembers;
        }
        Set<Long> requestedSet = requestedClubProfileIds == null
                ? Set.of()
                : requestedClubProfileIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (requestedSet.isEmpty()) {
            throw new SemoException.ValidationException("선택 멤버 발행 시 대상 멤버를 한 명 이상 선택해주세요.");
        }
        List<ClubAccessResolver.ClubMemberSnapshot> targets = activeMembers.stream()
                .filter(snapshot -> requestedSet.contains(snapshot.clubProfile().getClubProfileId()))
                .toList();
        if (targets.size() != requestedSet.size()) {
            throw new SemoException.ValidationException("현재 활성 멤버에게만 재정 항목을 발행할 수 있습니다.");
        }
        return targets;
    }

    private void syncObligationStatus(FinanceObligation obligation) {
        List<FinancePayment> payments = financePaymentRepository.findByFinanceObligationIdOrderByFinancePaymentIdDesc(obligation.getFinanceObligationId());
        String nextStatusCode = payments.stream().anyMatch(payment -> STATUS_PENDING.equals(payment.getPaymentStatusCode()))
                ? OBLIGATION_STATUS_OPEN
                : OBLIGATION_STATUS_CLOSED;
        if (!nextStatusCode.equals(obligation.getStatusCode())) {
            obligation.updateStatus(nextStatusCode);
            financeObligationRepository.save(obligation);
        }
        if (OBLIGATION_STATUS_CLOSED.equals(nextStatusCode)) {
            clubFinanceRecurrenceService.createNextObligation(obligation, payments);
        }
    }

    private void requirePostedExpense(FinanceExpense expense) {
        if (!"POSTED".equals(expense.getStatusCode())) {
            throw new SemoException.ValidationException("이미 취소된 지출은 정정하거나 다시 취소할 수 없습니다.");
        }
    }

    private void requireExistingExpensePeriodWritable(FinanceExpense expense) {
        if (expense.getFinancePeriodId() == null) {
            return;
        }
        clubFinanceOperationsService.resolveWritablePeriod(
                expense.getClubId(),
                expense.getFinancePeriodId(),
                expense.getSpentAt().toLocalDate()
        );
    }

    private FinanceExpenseRevision createExpenseRevision(
            FinanceExpense expense,
            Long actorClubProfileId,
            String revisionTypeCode,
            String nextTitle,
            String nextCategoryCode,
            BigDecimal nextAmount,
            LocalDateTime nextSpentAt,
            Long nextFinancePeriodId,
            Long nextScheduleEventId,
            Long nextFinanceAccountId,
            String nextNote,
            String nextStatusCode,
            String reason
    ) {
        return FinanceExpenseRevision.builder()
                .financeExpenseId(expense.getFinanceExpenseId())
                .clubId(expense.getClubId())
                .revisedByClubProfileId(actorClubProfileId)
                .revisionTypeCode(revisionTypeCode)
                .previousTitle(expense.getTitle())
                .nextTitle(nextTitle)
                .previousCategoryCode(expense.getCategoryCode())
                .nextCategoryCode(nextCategoryCode)
                .previousAmount(expense.getAmount())
                .nextAmount(nextAmount)
                .previousSpentAt(expense.getSpentAt())
                .nextSpentAt(nextSpentAt)
                .previousFinancePeriodId(expense.getFinancePeriodId())
                .nextFinancePeriodId(nextFinancePeriodId)
                .previousScheduleEventId(expense.getLinkedScheduleEventId())
                .nextScheduleEventId(nextScheduleEventId)
                .previousFinanceAccountId(expense.getFinanceAccountId())
                .nextFinanceAccountId(nextFinanceAccountId)
                .previousNote(expense.getNote())
                .nextNote(nextNote)
                .previousStatusCode(expense.getStatusCode())
                .nextStatusCode(nextStatusCode)
                .reason(reason)
                .build();
    }

    private String resolveRelatedEventName(String requestedName, ClubScheduleEvent linkedEvent) {
        String normalizedName = clubFinanceSupport.trimToNull(requestedName);
        return normalizedName != null || linkedEvent == null ? normalizedName : linkedEvent.getTitle();
    }

    private String inferPaymentMethod(FinanceAccount account) {
        if (account == null) {
            return null;
        }
        return switch (account.getAccountTypeCode()) {
            case "BANK" -> "TRANSFER";
            case "CASH" -> "CASH";
            case "CARD" -> "CARD";
            default -> "OTHER";
        };
    }

}
