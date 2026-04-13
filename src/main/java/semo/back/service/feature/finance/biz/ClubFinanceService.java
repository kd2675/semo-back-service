package semo.back.service.feature.finance.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.FinanceExpense;
import semo.back.service.database.pub.entity.FinanceObligation;
import semo.back.service.database.pub.entity.FinancePayment;
import semo.back.service.database.pub.entity.FinanceRequest;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.FinanceExpenseRepository;
import semo.back.service.database.pub.repository.FinanceObligationRepository;
import semo.back.service.database.pub.repository.FinancePaymentRepository;
import semo.back.service.database.pub.repository.FinanceRequestRepository;
import semo.back.service.feature.activity.biz.ClubActivityContextHolder;
import semo.back.service.feature.activity.biz.RecordClubActivity;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.finance.biz.policy.ClubFinancePermissionService;
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
import semo.back.service.feature.finance.vo.ReviewFinanceRequestRequest;
import semo.back.service.feature.finance.vo.UpdateFinancePaymentStatusRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
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
    private static final String TARGET_SCOPE_ALL_ACTIVE_MEMBERS = "ALL_ACTIVE_MEMBERS";
    private static final String TARGET_SCOPE_SELECTED_MEMBERS = "SELECTED_MEMBERS";

    private final ClubAccessResolver clubAccessResolver;
    private final FinanceObligationRepository financeObligationRepository;
    private final FinancePaymentRepository financePaymentRepository;
    private final FinanceRequestRepository financeRequestRepository;
    private final FinanceExpenseRepository financeExpenseRepository;
    private final ClubProfileRepository clubProfileRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubFinancePermissionService clubFinancePermissionService;
    private final ClubFinanceSupport clubFinanceSupport;

    public ClubFinanceHomeResponse getFinance(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireFinanceFeature(clubId);

        List<FinancePayment> payments = financePaymentRepository.findByClubIdAndClubProfileIdOrderByFinancePaymentIdDesc(
                clubId,
                access.clubProfile().getClubProfileId()
        );
        Map<Long, FinanceObligation> obligationById = resolveObligationById(
                payments.stream().map(FinancePayment::getFinanceObligationId).toList()
        );
        List<ClubFinanceUserObligationResponse> paymentHistory = payments.stream()
                .map(payment -> toUserObligationResponse(
                        obligationById.get(payment.getFinanceObligationId()),
                        payment,
                        access.clubProfile().getDisplayName(),
                        access.membership().getRoleCode()
                ))
                .filter(Objects::nonNull)
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
                paymentHistory
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
                toFinanceRequestResponses(requests)
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
                toFinanceRequestResponses(requests)
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
                toFinanceExpenseResponses(expenses)
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
        List<ClubAdminFinanceObligationResponse> items = toAdminObligationResponses(pageObligations);
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
        Map<Long, PaymentMemberSummary> paymentMemberSummaryByClubProfileId = resolvePaymentMemberSummaryByClubProfileId(payments);
        Map<Long, String> issuerNameByClubProfileId = obligation.getCreatedByClubProfileId() == null
                ? Map.of()
                : resolveClubProfileDisplayNameById(List.of(obligation.getCreatedByClubProfileId()));
        List<ClubFinancePaymentResponse> paymentResponses = toPaymentResponses(obligation, payments, paymentMemberSummaryByClubProfileId);

        return new ClubAdminFinanceObligationDetailResponse(
                toAdminObligationResponse(obligation, payments, issuerNameByClubProfileId),
                paymentResponses
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
        if (!clubFinancePermissionService.canIssueFinance(access)) {
            throw new SemoException.ForbiddenException("재정 항목을 발행할 권한이 없습니다.");
        }

        String title = clubFinanceSupport.normalizeTitle(request.title());
        String targetScopeCode = clubFinanceSupport.normalizeTargetScope(request.targetScopeCode());
        BigDecimal amount = clubFinanceSupport.normalizeAmount(request.amount());
        LocalDateTime dueAt = clubFinanceSupport.parseDateTime(request.dueAt(), "납부 마감일 형식이 잘못되었습니다.");
        String note = clubFinanceSupport.trimToNull(request.note());

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
                .obligationTypeCode(OBLIGATION_TYPE_FEE)
                .title(title)
                .targetScopeCode(targetScopeCode)
                .amount(amount)
                .currencyCode("KRW")
                .dueAt(dueAt)
                .statusCode(OBLIGATION_STATUS_OPEN)
                .note(note)
                .build());

        List<FinancePayment> payments = targetMembers.stream()
                .map(snapshot -> FinancePayment.builder()
                        .financeObligationId(obligation.getFinanceObligationId())
                        .clubId(clubId)
                        .clubProfileId(snapshot.clubProfile().getClubProfileId())
                        .amount(amount)
                        .currencyCode("KRW")
                        .paymentStatusCode(STATUS_PENDING)
                        .paidAt(null)
                        .note(note)
                        .build())
                .toList();
        financePaymentRepository.saveAll(payments);

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

        FinanceRequest saved = financeRequestRepository.save(FinanceRequest.builder()
                .clubId(clubId)
                .requesterClubProfileId(access.clubProfile().getClubProfileId())
                .requestTypeCode(clubFinanceSupport.normalizeRequestType(request.requestTypeCode()))
                .title(clubFinanceSupport.normalizeRequestTitle(request.title()))
                .amount(clubFinanceSupport.normalizeAmount(request.amount()))
                .currencyCode("KRW")
                .relatedEventName(clubFinanceSupport.trimToNull(request.relatedEventName()))
                .note(clubFinanceSupport.trimToNull(request.note()))
                .statusCode(REQUEST_STATUS_SUBMITTED)
                .build());

        ClubActivityContextHolder.setDetails(
                saved.getTitle() + " 재정 요청을 제출했습니다.",
                "재정 요청 제출에 실패했습니다."
        );
        return toFinanceRequestResponse(saved, resolveClubProfileDisplayNameById(List.of(saved.getRequesterClubProfileId())));
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
        if (!clubFinancePermissionService.canIssueFinance(access)) {
            throw new SemoException.ForbiddenException("재정 요청을 검토할 권한이 없습니다.");
        }

        FinanceRequest financeRequest = financeRequestRepository.findByFinanceRequestIdAndClubId(requestId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("FinanceRequest", "requestId", requestId));
        if (!REQUEST_STATUS_SUBMITTED.equals(financeRequest.getStatusCode())) {
            throw new SemoException.ValidationException("이미 검토가 완료된 재정 요청입니다.");
        }
        FinanceRequest updated = financeRequestRepository.save(FinanceRequest.builder()
                .financeRequestId(financeRequest.getFinanceRequestId())
                .clubId(financeRequest.getClubId())
                .requesterClubProfileId(financeRequest.getRequesterClubProfileId())
                .requestTypeCode(financeRequest.getRequestTypeCode())
                .title(financeRequest.getTitle())
                .amount(financeRequest.getAmount())
                .currencyCode(financeRequest.getCurrencyCode())
                .relatedEventName(financeRequest.getRelatedEventName())
                .note(financeRequest.getNote())
                .statusCode(clubFinanceSupport.normalizeReviewStatus(request.statusCode()))
                .reviewedByClubProfileId(access.clubProfile().getClubProfileId())
                .reviewedAt(LocalDateTime.now())
                .reviewNote(clubFinanceSupport.trimToNull(request.reviewNote()))
                .build());

        ClubActivityContextHolder.setDetails(
                updated.getTitle() + " 요청을 " + clubFinanceSupport.resolveRequestStatusLabel(updated.getStatusCode()) + " 처리했습니다.",
                "재정 요청 검토에 실패했습니다."
        );
        return toFinanceRequestResponse(
                updated,
                resolveClubProfileDisplayNameById(List.of(updated.getRequesterClubProfileId(), access.clubProfile().getClubProfileId()))
        );
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
        if (!clubFinancePermissionService.canIssueFinance(access)) {
            throw new SemoException.ForbiddenException("지출을 입력할 권한이 없습니다.");
        }

        FinanceExpense saved = financeExpenseRepository.save(FinanceExpense.builder()
                .clubId(clubId)
                .enteredByClubProfileId(access.clubProfile().getClubProfileId())
                .expenseTypeCode(EXPENSE_TYPE_ADMIN)
                .categoryCode(clubFinanceSupport.normalizeExpenseCategory(request.categoryCode()))
                .title(clubFinanceSupport.normalizeExpenseTitle(request.title()))
                .amount(clubFinanceSupport.normalizeAmount(request.amount()))
                .currencyCode("KRW")
                .spentAt(clubFinanceSupport.parseDateTime(request.spentAt(), "지출 일시 형식이 잘못되었습니다.", LocalDateTime.now()))
                .relatedEventName(clubFinanceSupport.trimToNull(request.relatedEventName()))
                .note(clubFinanceSupport.trimToNull(request.note()))
                .build());

        ClubActivityContextHolder.setDetails(
                saved.getTitle() + " 지출을 입력했습니다.",
                "지출 입력에 실패했습니다."
        );
        return toFinanceExpenseResponse(saved, resolveClubProfileDisplayNameById(List.of(saved.getEnteredByClubProfileId())));
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "재정관리")
    public void deleteObligation(Long clubId, Long obligationId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireFinanceFeature(clubId);
        if (!clubFinancePermissionService.canIssueFinance(access)) {
            throw new SemoException.ForbiddenException("재정 항목을 삭제할 권한이 없습니다.");
        }

        FinanceObligation obligation = financeObligationRepository.findByFinanceObligationIdAndClubId(obligationId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("FinanceObligation", "obligationId", obligationId));
        List<FinancePayment> payments = financePaymentRepository.findByFinanceObligationIdOrderByFinancePaymentIdDesc(obligationId);
        if (!canDeleteObligation(payments)) {
            throw new SemoException.ValidationException("아직 아무도 처리하지 않은 재정 항목만 삭제할 수 있습니다.");
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

        FinancePayment payment = financePaymentRepository.findByFinancePaymentIdAndClubId(paymentId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("FinancePayment", "paymentId", paymentId));
        FinanceObligation obligation = financeObligationRepository.findByFinanceObligationIdAndClubId(payment.getFinanceObligationId(), clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("FinanceObligation", "obligationId", payment.getFinanceObligationId()));
        String note = clubFinanceSupport.trimToNull(request.note());
        LocalDateTime paidAt = STATUS_PAID.equals(nextStatus) ? LocalDateTime.now() : null;

        FinancePayment updated = financePaymentRepository.save(FinancePayment.builder()
                .financePaymentId(payment.getFinancePaymentId())
                .financeObligationId(payment.getFinanceObligationId())
                .clubId(payment.getClubId())
                .clubProfileId(payment.getClubProfileId())
                .amount(payment.getAmount())
                .currencyCode(payment.getCurrencyCode())
                .paymentStatusCode(nextStatus)
                .paidAt(paidAt)
                .note(note == null ? payment.getNote() : note)
                .build());

        syncObligationStatus(obligation);

        ClubActivityContextHolder.setDetails(
                obligation.getTitle() + " 재정 상태를 " + clubFinanceSupport.resolvePaymentStatusLabel(nextStatus, false) + "로 변경했습니다.",
                "재정 상태 변경에 실패했습니다."
        );

        PaymentMemberSummary paymentMemberSummary = resolvePaymentMemberSummaryByClubProfileId(List.of(updated))
                .get(updated.getClubProfileId());
        return toPaymentResponse(updated, obligation, paymentMemberSummary);
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

    private Map<Long, FinanceObligation> resolveObligationById(Collection<Long> obligationIds) {
        if (obligationIds == null || obligationIds.isEmpty()) {
            return Map.of();
        }
        return financeObligationRepository.findAllById(obligationIds).stream()
                .collect(Collectors.toMap(FinanceObligation::getFinanceObligationId, Function.identity()));
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

    private List<ClubAdminFinanceObligationResponse> toAdminObligationResponses(List<FinanceObligation> obligations) {
        if (obligations.isEmpty()) {
            return List.of();
        }
        Map<Long, List<FinancePayment>> paymentsByObligationId = financePaymentRepository.findByFinanceObligationIdIn(
                        obligations.stream().map(FinanceObligation::getFinanceObligationId).toList()
                ).stream()
                .collect(Collectors.groupingBy(FinancePayment::getFinanceObligationId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, String> issuerNameByClubProfileId = resolveClubProfileDisplayNameById(
                obligations.stream()
                        .map(FinanceObligation::getCreatedByClubProfileId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList()
        );
        return obligations.stream()
                .map(obligation -> toAdminObligationResponse(
                        obligation,
                        paymentsByObligationId.getOrDefault(obligation.getFinanceObligationId(), List.of()),
                        issuerNameByClubProfileId
                ))
                .toList();
    }

    private List<ClubFinanceRequestResponse> toFinanceRequestResponses(List<FinanceRequest> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }
        Map<Long, String> displayNameByClubProfileId = resolveClubProfileDisplayNameById(
                requests.stream()
                        .flatMap(request -> java.util.stream.Stream.of(request.getRequesterClubProfileId(), request.getReviewedByClubProfileId()))
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList()
        );
        return requests.stream()
                .map(request -> toFinanceRequestResponse(request, displayNameByClubProfileId))
                .toList();
    }

    private ClubFinanceRequestResponse toFinanceRequestResponse(
            FinanceRequest request,
            Map<Long, String> displayNameByClubProfileId
    ) {
        return new ClubFinanceRequestResponse(
                request.getFinanceRequestId(),
                request.getRequestTypeCode(),
                clubFinanceSupport.resolveRequestTypeLabel(request.getRequestTypeCode()),
                displayNameByClubProfileId.getOrDefault(request.getRequesterClubProfileId(), "알 수 없는 멤버"),
                request.getAmount(),
                clubFinanceSupport.formatAmount(request.getAmount(), request.getCurrencyCode()),
                request.getCurrencyCode(),
                request.getTitle(),
                request.getRelatedEventName(),
                request.getNote(),
                request.getStatusCode(),
                clubFinanceSupport.resolveRequestStatusLabel(request.getStatusCode()),
                clubFinanceSupport.formatDateTimeValue(request.getCreateDate()),
                clubFinanceSupport.formatDateTimeLabel(request.getCreateDate()),
                clubFinanceSupport.formatDateTimeValue(request.getReviewedAt()),
                clubFinanceSupport.formatDateTimeLabel(request.getReviewedAt()),
                request.getReviewNote()
        );
    }

    private List<ClubFinanceExpenseResponse> toFinanceExpenseResponses(List<FinanceExpense> expenses) {
        if (expenses.isEmpty()) {
            return List.of();
        }
        Map<Long, String> displayNameByClubProfileId = resolveClubProfileDisplayNameById(
                expenses.stream().map(FinanceExpense::getEnteredByClubProfileId).distinct().toList()
        );
        return expenses.stream()
                .map(expense -> toFinanceExpenseResponse(expense, displayNameByClubProfileId))
                .toList();
    }

    private ClubFinanceExpenseResponse toFinanceExpenseResponse(
            FinanceExpense expense,
            Map<Long, String> displayNameByClubProfileId
    ) {
        return new ClubFinanceExpenseResponse(
                expense.getFinanceExpenseId(),
                expense.getExpenseTypeCode(),
                clubFinanceSupport.resolveExpenseTypeLabel(expense.getExpenseTypeCode()),
                expense.getCategoryCode(),
                clubFinanceSupport.resolveExpenseCategoryLabel(expense.getCategoryCode()),
                displayNameByClubProfileId.getOrDefault(expense.getEnteredByClubProfileId(), "알 수 없는 운영자"),
                expense.getAmount(),
                clubFinanceSupport.formatAmount(expense.getAmount(), expense.getCurrencyCode()),
                expense.getCurrencyCode(),
                expense.getTitle(),
                expense.getRelatedEventName(),
                expense.getNote(),
                clubFinanceSupport.formatDateTimeValue(expense.getSpentAt()),
                clubFinanceSupport.formatDateTimeLabel(expense.getSpentAt())
        );
    }

    private ClubAdminFinanceObligationResponse toAdminObligationResponse(
            FinanceObligation obligation,
            List<FinancePayment> payments,
            Map<Long, String> issuerNameByClubProfileId
    ) {
        ObligationPaymentMetrics metrics = summarizeObligationPayments(obligation, payments);

        return new ClubAdminFinanceObligationResponse(
                obligation.getFinanceObligationId(),
                obligation.getObligationTypeCode(),
                clubFinanceSupport.resolveObligationTypeLabel(obligation.getObligationTypeCode()),
                obligation.getTitle(),
                obligation.getTargetScopeCode(),
                clubFinanceSupport.resolveTargetScopeLabel(obligation.getTargetScopeCode()),
                obligation.getAmount(),
                clubFinanceSupport.formatAmount(obligation.getAmount(), obligation.getCurrencyCode()),
                obligation.getCurrencyCode(),
                clubFinanceSupport.formatDateTimeValue(obligation.getDueAt()),
                clubFinanceSupport.formatDateTimeLabel(obligation.getDueAt()),
                clubFinanceSupport.formatDateTimeValue(obligation.getCreateDate()),
                clubFinanceSupport.formatDateTimeLabel(obligation.getCreateDate()),
                obligation.getCreatedByClubProfileId() == null ? "알 수 없는 운영자" : issuerNameByClubProfileId.getOrDefault(obligation.getCreatedByClubProfileId(), "알 수 없는 운영자"),
                obligation.getNote(),
                metrics.canDelete(),
                metrics.totalPaymentCount(),
                metrics.pendingPaymentCount(),
                metrics.paidPaymentCount(),
                metrics.waivedPaymentCount(),
                metrics.overduePaymentCount(),
                metrics.collectionRate()
        );
    }

    private ObligationPaymentMetrics summarizeObligationPayments(FinanceObligation obligation, List<FinancePayment> payments) {
        int totalPaymentCount = payments.size();
        int pendingPaymentCount = (int) payments.stream()
                .filter(payment -> STATUS_PENDING.equals(payment.getPaymentStatusCode()))
                .count();
        int paidPaymentCount = (int) payments.stream()
                .filter(payment -> STATUS_PAID.equals(payment.getPaymentStatusCode()))
                .count();
        int waivedPaymentCount = (int) payments.stream()
                .filter(payment -> STATUS_WAIVED.equals(payment.getPaymentStatusCode()))
                .count();
        int overduePaymentCount = (int) payments.stream()
                .filter(payment -> isOverdue(payment, obligation))
                .count();
        int collectiblePaymentCount = totalPaymentCount - waivedPaymentCount;
        return new ObligationPaymentMetrics(
                totalPaymentCount,
                pendingPaymentCount,
                paidPaymentCount,
                waivedPaymentCount,
                overduePaymentCount,
                collectiblePaymentCount == 0 ? 0 : (int) Math.round((paidPaymentCount * 100.0) / collectiblePaymentCount),
                canDeleteObligation(payments)
        );
    }

    private List<ClubFinancePaymentResponse> toPaymentResponses(
            FinanceObligation obligation,
            List<FinancePayment> payments,
            Map<Long, PaymentMemberSummary> paymentMemberSummaryByClubProfileId
    ) {
        return payments.stream()
                .map(payment -> toPaymentResponse(payment, obligation, paymentMemberSummaryByClubProfileId.get(payment.getClubProfileId())))
                .sorted(Comparator
                        .comparing((ClubFinancePaymentResponse payment) -> payment.overdue() ? 0 : 1)
                        .thenComparing(ClubFinancePaymentResponse::paymentStatusCode)
                        .thenComparing(ClubFinancePaymentResponse::memberDisplayName, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private boolean canDeleteObligation(List<FinancePayment> payments) {
        return payments.stream().allMatch(payment -> STATUS_PENDING.equals(payment.getPaymentStatusCode()));
    }

    private ClubFinanceUserObligationResponse toUserObligationResponse(
            FinanceObligation obligation,
            FinancePayment payment,
            String memberDisplayName,
            String memberRoleCode
    ) {
        if (obligation == null) {
            return null;
        }
        return new ClubFinanceUserObligationResponse(
                obligation.getFinanceObligationId(),
                obligation.getObligationTypeCode(),
                clubFinanceSupport.resolveObligationTypeLabel(obligation.getObligationTypeCode()),
                obligation.getTitle(),
                obligation.getAmount(),
                clubFinanceSupport.formatAmount(obligation.getAmount(), obligation.getCurrencyCode()),
                obligation.getCurrencyCode(),
                clubFinanceSupport.formatDateTimeValue(obligation.getDueAt()),
                clubFinanceSupport.formatDateTimeLabel(obligation.getDueAt()),
                clubFinanceSupport.formatDateTimeValue(obligation.getCreateDate()),
                clubFinanceSupport.formatDateTimeLabel(obligation.getCreateDate()),
                obligation.getNote(),
                toPaymentResponse(payment, obligation, new PaymentMemberSummary(memberDisplayName, memberRoleCode))
        );
    }

    private ClubFinancePaymentResponse toPaymentResponse(
            FinancePayment payment,
            FinanceObligation obligation,
            PaymentMemberSummary paymentMemberSummary
    ) {
        boolean overdue = isOverdue(payment, obligation);
        String responseStatusCode = overdue ? STATUS_OVERDUE : payment.getPaymentStatusCode();
        return new ClubFinancePaymentResponse(
                payment.getFinancePaymentId(),
                payment.getClubProfileId(),
                paymentMemberSummary == null ? "알 수 없는 멤버" : paymentMemberSummary.memberDisplayName(),
                paymentMemberSummary == null ? null : paymentMemberSummary.memberRoleCode(),
                payment.getAmount(),
                clubFinanceSupport.formatAmount(payment.getAmount(), payment.getCurrencyCode()),
                payment.getCurrencyCode(),
                responseStatusCode,
                clubFinanceSupport.resolvePaymentStatusLabel(payment.getPaymentStatusCode(), overdue),
                overdue,
                clubFinanceSupport.formatDateTimeValue(payment.getPaidAt()),
                clubFinanceSupport.formatDateTimeLabel(payment.getPaidAt()),
                payment.getNote()
        );
    }

    private boolean isOverdue(FinancePayment payment, FinanceObligation obligation) {
        return STATUS_PENDING.equals(payment.getPaymentStatusCode())
                && obligation.getDueAt() != null
                && obligation.getDueAt().isBefore(LocalDateTime.now());
    }

    private void syncObligationStatus(FinanceObligation obligation) {
        List<FinancePayment> payments = financePaymentRepository.findByFinanceObligationIdOrderByFinancePaymentIdDesc(obligation.getFinanceObligationId());
        String nextStatusCode = payments.stream().anyMatch(payment -> STATUS_PENDING.equals(payment.getPaymentStatusCode()))
                ? OBLIGATION_STATUS_OPEN
                : OBLIGATION_STATUS_CLOSED;
        if (nextStatusCode.equals(obligation.getStatusCode())) {
            return;
        }
        financeObligationRepository.save(FinanceObligation.builder()
                .financeObligationId(obligation.getFinanceObligationId())
                .clubId(obligation.getClubId())
                .createdByClubProfileId(obligation.getCreatedByClubProfileId())
                .obligationTypeCode(obligation.getObligationTypeCode())
                .title(obligation.getTitle())
                .targetScopeCode(obligation.getTargetScopeCode())
                .amount(obligation.getAmount())
                .currencyCode(obligation.getCurrencyCode())
                .dueAt(obligation.getDueAt())
                .statusCode(nextStatusCode)
                .note(obligation.getNote())
                .build());
    }

    private Map<Long, PaymentMemberSummary> resolvePaymentMemberSummaryByClubProfileId(List<FinancePayment> payments) {
        if (payments.isEmpty()) {
            return Map.of();
        }

        Map<Long, ClubProfile> clubProfileById = clubProfileRepository.findAllById(
                        payments.stream()
                                .map(FinancePayment::getClubProfileId)
                                .distinct()
                                .toList()
                ).stream()
                .collect(Collectors.toMap(ClubProfile::getClubProfileId, Function.identity()));

        Map<Long, ClubMember> clubMemberById = clubMemberRepository.findAllById(
                        clubProfileById.values().stream()
                                .map(ClubProfile::getClubMemberId)
                                .distinct()
                                .toList()
                ).stream()
                .collect(Collectors.toMap(ClubMember::getClubMemberId, Function.identity()));

        Map<Long, PaymentMemberSummary> summaryByClubProfileId = new LinkedHashMap<>();
        clubProfileById.forEach((clubProfileId, clubProfile) -> {
            ClubMember clubMember = clubMemberById.get(clubProfile.getClubMemberId());
            summaryByClubProfileId.put(
                    clubProfileId,
                    new PaymentMemberSummary(
                            clubProfile.getDisplayName(),
                            clubMember == null ? null : clubMember.getRoleCode()
                    )
            );
        });
        return summaryByClubProfileId;
    }

    private Map<Long, String> resolveClubProfileDisplayNameById(Collection<Long> clubProfileIds) {
        if (clubProfileIds == null || clubProfileIds.isEmpty()) {
            return Map.of();
        }
        return clubProfileRepository.findAllById(clubProfileIds).stream()
                .collect(Collectors.toMap(ClubProfile::getClubProfileId, ClubProfile::getDisplayName));
    }

    private record PaymentMemberSummary(String memberDisplayName, String memberRoleCode) {
    }

    private record ObligationPaymentMetrics(
            int totalPaymentCount,
            int pendingPaymentCount,
            int paidPaymentCount,
            int waivedPaymentCount,
            int overduePaymentCount,
            int collectionRate,
            boolean canDelete
    ) {
    }
}
