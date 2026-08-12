package semo.back.service.feature.finance.biz;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubOperatingTerm;
import semo.back.service.database.pub.entity.ClubScheduleEvent;
import semo.back.service.database.pub.entity.FinanceAccount;
import semo.back.service.database.pub.entity.FinanceBudget;
import semo.back.service.database.pub.entity.FinancePeriod;
import semo.back.service.database.pub.repository.ClubOperatingTermRepository;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.FinanceAccountRepository;
import semo.back.service.database.pub.repository.FinanceBudgetRepository;
import semo.back.service.database.pub.repository.FinanceExpenseRepository;
import semo.back.service.database.pub.repository.FinanceObligationRepository;
import semo.back.service.database.pub.repository.FinancePaymentRepository;
import semo.back.service.database.pub.repository.FinancePeriodRepository;
import semo.back.service.feature.activity.biz.ClubActivityContextHolder;
import semo.back.service.feature.activity.biz.RecordClubActivity;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.finance.biz.policy.ClubFinancePermissionService;
import semo.back.service.feature.finance.biz.support.ClubFinanceSupport;
import semo.back.service.feature.finance.vo.ClubFinanceOperationsResponse;
import semo.back.service.feature.finance.vo.CreateFinancePeriodRequest;
import semo.back.service.feature.finance.vo.FinanceAccountResponse;
import semo.back.service.feature.finance.vo.FinanceBudgetResponse;
import semo.back.service.feature.finance.vo.FinancePeriodResponse;
import semo.back.service.feature.finance.vo.FinanceScheduleOptionResponse;
import semo.back.service.feature.finance.vo.UpsertFinanceAccountRequest;
import semo.back.service.feature.finance.vo.UpsertFinanceBudgetRequest;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubFinanceOperationsService {
    private static final String PERIOD_STATUS_OPEN = "OPEN";
    private static final String PERIOD_STATUS_CLOSED = "CLOSED";

    private final ClubAccessResolver clubAccessResolver;
    private final ClubFeatureService clubFeatureService;
    private final ClubFinancePermissionService clubFinancePermissionService;
    private final ClubFinanceSupport clubFinanceSupport;
    private final FinanceAccountRepository financeAccountRepository;
    private final FinancePeriodRepository financePeriodRepository;
    private final FinanceBudgetRepository financeBudgetRepository;
    private final FinanceObligationRepository financeObligationRepository;
    private final FinancePaymentRepository financePaymentRepository;
    private final FinanceExpenseRepository financeExpenseRepository;
    private final ClubOperatingTermRepository clubOperatingTermRepository;
    private final ClubScheduleEventRepository clubScheduleEventRepository;

    public ClubFinanceOperationsResponse getOperations(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = requireOperationsView(clubId, userKey);
        List<FinancePeriodResponse> periods = financePeriodRepository
                .findByClubIdOrderByStartDateDescFinancePeriodIdDesc(clubId)
                .stream()
                .map(this::toPeriodResponse)
                .toList();
        List<FinanceScheduleOptionResponse> scheduleOptions = getScheduleOptions(clubId);

        return new ClubFinanceOperationsResponse(
                access.club().getClubId(),
                access.club().getName(),
                clubFinancePermissionService.canManageBilling(access),
                clubFinancePermissionService.canReviewRequests(access),
                clubFinancePermissionService.canCreateExpenses(access),
                clubFinancePermissionService.canUpdatePayments(access),
                clubFinancePermissionService.canExport(access),
                clubFinancePermissionService.canClosePeriods(access),
                financeAccountRepository.findByClubIdOrderByActiveDescDisplayNameAscFinanceAccountIdAsc(clubId)
                        .stream()
                        .map(this::toAccountResponse)
                        .toList(),
                periods,
                scheduleOptions
        );
    }

    public List<FinanceScheduleOptionResponse> getScheduleOptions(Long clubId) {
        if (!clubFeatureService.isFeatureEnabled(clubId, "SCHEDULE_MANAGE")) {
            return List.of();
        }
        return clubScheduleEventRepository.findScheduledBetween(
                        clubId,
                        LocalDateTime.now().minusYears(1),
                        LocalDateTime.now().plusYears(2)
                ).stream()
                .map(this::toScheduleOptionResponse)
                .toList();
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "재정관리")
    public FinanceAccountResponse createAccount(Long clubId, String userKey, UpsertFinanceAccountRequest request) {
        ClubAccessResolver.ClubAccess access = requireCanConfigure(clubId, userKey);
        String usageScopeCode = clubFinanceSupport.normalizeAccountUsageScope(request.usageScopeCode());
        validateDefaultScopes(usageScopeCode, request.defaultCollection(), request.defaultExpense());
        clearExistingDefaults(clubId, request.defaultCollection(), request.defaultExpense(), null);

        FinanceAccount account = financeAccountRepository.save(FinanceAccount.builder()
                .clubId(clubId)
                .createdByClubProfileId(access.clubProfile().getClubProfileId())
                .displayName(clubFinanceSupport.normalizeAccountName(request.displayName()))
                .accountTypeCode(clubFinanceSupport.normalizeAccountType(request.accountTypeCode()))
                .providerName(clubFinanceSupport.trimToNull(request.providerName()))
                .maskedIdentifier(clubFinanceSupport.trimToNull(request.maskedIdentifier()))
                .holderName(clubFinanceSupport.trimToNull(request.holderName()))
                .usageScopeCode(usageScopeCode)
                .active(true)
                .defaultCollection(request.defaultCollection())
                .defaultExpense(request.defaultExpense())
                .build());

        ClubActivityContextHolder.setDetails(
                account.getDisplayName() + " 계좌·결제수단을 등록했습니다.",
                "계좌·결제수단 등록에 실패했습니다."
        );
        return toAccountResponse(account);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "재정관리")
    public FinanceAccountResponse updateAccount(
            Long clubId,
            Long financeAccountId,
            String userKey,
            UpsertFinanceAccountRequest request
    ) {
        requireCanConfigure(clubId, userKey);
        FinanceAccount account = requireAccount(clubId, financeAccountId);
        if (!account.isActive()) {
            throw new SemoException.ValidationException("비활성 계좌·결제수단은 수정할 수 없습니다.");
        }
        String usageScopeCode = clubFinanceSupport.normalizeAccountUsageScope(request.usageScopeCode());
        validateDefaultScopes(usageScopeCode, request.defaultCollection(), request.defaultExpense());
        clearExistingDefaults(clubId, request.defaultCollection(), request.defaultExpense(), financeAccountId);
        account.update(
                clubFinanceSupport.normalizeAccountName(request.displayName()),
                clubFinanceSupport.normalizeAccountType(request.accountTypeCode()),
                clubFinanceSupport.trimToNull(request.providerName()),
                clubFinanceSupport.trimToNull(request.maskedIdentifier()),
                clubFinanceSupport.trimToNull(request.holderName()),
                usageScopeCode,
                request.defaultCollection(),
                request.defaultExpense()
        );
        financeAccountRepository.save(account);

        ClubActivityContextHolder.setDetails(
                account.getDisplayName() + " 계좌·결제수단을 수정했습니다.",
                "계좌·결제수단 수정에 실패했습니다."
        );
        return toAccountResponse(account);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "재정관리")
    public FinanceAccountResponse deactivateAccount(Long clubId, Long financeAccountId, String userKey) {
        requireCanConfigure(clubId, userKey);
        FinanceAccount account = requireAccount(clubId, financeAccountId);
        account.deactivate();
        financeAccountRepository.save(account);

        ClubActivityContextHolder.setDetails(
                account.getDisplayName() + " 계좌·결제수단을 비활성화했습니다.",
                "계좌·결제수단 비활성화에 실패했습니다."
        );
        return toAccountResponse(account);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "재정관리")
    public FinancePeriodResponse createPeriod(Long clubId, String userKey, CreateFinancePeriodRequest request) {
        ClubAccessResolver.ClubAccess access = requireCanConfigure(clubId, userKey);
        if (request.startDate().isAfter(request.endDate())) {
            throw new SemoException.ValidationException("재정 기간 종료일은 시작일보다 빠를 수 없습니다.");
        }
        if (financePeriodRepository.countOverlapping(clubId, request.startDate(), request.endDate()) > 0) {
            throw new SemoException.ValidationException("기존 재정 기간과 날짜가 겹칩니다.");
        }
        if (request.clubOperatingTermId() != null) {
            ClubOperatingTerm term = clubOperatingTermRepository
                    .findByClubOperatingTermIdAndClubId(request.clubOperatingTermId(), clubId)
                    .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                            "ClubOperatingTerm",
                            "clubOperatingTermId",
                            request.clubOperatingTermId()
                    ));
            if (request.startDate().isBefore(term.getStartDate()) || request.endDate().isAfter(term.getEndDate())) {
                throw new SemoException.ValidationException("재정 기간은 연결한 임기·시즌 범위 안에 있어야 합니다.");
            }
        }
        BigDecimal openingBalance = request.openingBalance() == null
                ? resolvePreviousClosingBalance(clubId)
                : clubFinanceSupport.normalizeNonNegativeAmount(
                        request.openingBalance(),
                        "기초 잔액은 0보다 작을 수 없습니다."
                );
        FinancePeriod period = financePeriodRepository.save(FinancePeriod.builder()
                .clubId(clubId)
                .clubOperatingTermId(request.clubOperatingTermId())
                .createdByClubProfileId(access.clubProfile().getClubProfileId())
                .title(clubFinanceSupport.normalizePeriodTitle(request.title()))
                .startDate(request.startDate())
                .endDate(request.endDate())
                .statusCode(PERIOD_STATUS_OPEN)
                .openingBalance(openingBalance)
                .note(clubFinanceSupport.trimToNull(request.note()))
                .build());

        ClubActivityContextHolder.setDetails(
                period.getTitle() + " 재정 기간을 열었습니다.",
                "재정 기간 생성에 실패했습니다."
        );
        return toPeriodResponse(period);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "재정관리")
    public FinancePeriodResponse upsertBudget(
            Long clubId,
            Long financePeriodId,
            String userKey,
            UpsertFinanceBudgetRequest request
    ) {
        ClubAccessResolver.ClubAccess access = requireCanConfigure(clubId, userKey);
        FinancePeriod period = requireOpenPeriod(clubId, financePeriodId);
        String categoryCode = clubFinanceSupport.normalizeExpenseCategory(request.categoryCode());
        BigDecimal allocatedAmount = clubFinanceSupport.normalizeNonNegativeAmount(
                request.allocatedAmount(),
                "예산은 0보다 작을 수 없습니다."
        );
        FinanceBudget budget = financeBudgetRepository
                .findByFinancePeriodIdAndCategoryCode(financePeriodId, categoryCode)
                .orElseGet(() -> FinanceBudget.builder()
                        .clubId(clubId)
                        .financePeriodId(financePeriodId)
                        .categoryCode(categoryCode)
                        .createdByClubProfileId(access.clubProfile().getClubProfileId())
                        .build());
        budget.update(allocatedAmount, clubFinanceSupport.trimToNull(request.note()));
        financeBudgetRepository.save(budget);

        ClubActivityContextHolder.setDetails(
                period.getTitle() + "의 " + clubFinanceSupport.resolveExpenseCategoryLabel(categoryCode) + " 예산을 저장했습니다.",
                "예산 저장에 실패했습니다."
        );
        return toPeriodResponse(period);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "재정관리")
    public FinancePeriodResponse closePeriod(Long clubId, Long financePeriodId, String userKey) {
        ClubAccessResolver.ClubAccess access = requireCanConfigure(clubId, userKey);
        FinancePeriod period = financePeriodRepository.findForUpdate(financePeriodId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "FinancePeriod",
                        "financePeriodId",
                        financePeriodId
                ));
        if (PERIOD_STATUS_CLOSED.equals(period.getStatusCode())) {
            return toPeriodResponse(period);
        }
        if (period.getEndDate().isAfter(LocalDate.now())) {
            throw new SemoException.ValidationException("종료일이 지나지 않은 재정 기간은 마감할 수 없습니다.");
        }
        if (financeObligationRepository.countByFinancePeriodIdAndStatusCode(financePeriodId, "OPEN") > 0) {
            throw new SemoException.ValidationException("미수납 청구가 남아 있어 재정 기간을 마감할 수 없습니다.");
        }
        BigDecimal closingBalance = calculateBalance(period);
        period.close(access.clubProfile().getClubProfileId(), LocalDateTime.now(), closingBalance);
        financePeriodRepository.save(period);

        ClubActivityContextHolder.setDetails(
                period.getTitle() + " 재정 기간을 " + clubFinanceSupport.formatAmount(closingBalance, "KRW") + "으로 마감했습니다.",
                "재정 기간 마감에 실패했습니다."
        );
        return toPeriodResponse(period);
    }

    public FinancePeriod resolveWritablePeriod(Long clubId, Long financePeriodId, LocalDate effectiveDate) {
        FinancePeriod period;
        if (financePeriodId != null) {
            period = financePeriodRepository.findByFinancePeriodIdAndClubId(financePeriodId, clubId)
                    .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                            "FinancePeriod",
                            "financePeriodId",
                            financePeriodId
                    ));
        } else {
            period = financePeriodRepository.findContainingDate(clubId, effectiveDate).stream().findFirst().orElse(null);
        }
        if (period == null) {
            return null;
        }
        if (!PERIOD_STATUS_OPEN.equals(period.getStatusCode())) {
            throw new SemoException.ValidationException("마감된 재정 기간에는 항목을 추가하거나 수정할 수 없습니다.");
        }
        if (!period.includes(effectiveDate)) {
            throw new SemoException.ValidationException("거래 날짜가 선택한 재정 기간 범위를 벗어납니다.");
        }
        return period;
    }

    public FinanceAccount resolveActiveAccount(Long clubId, Long financeAccountId, String requiredUsageScope) {
        if (financeAccountId == null) {
            return financeAccountRepository.findByClubIdAndActiveTrue(clubId).stream()
                    .filter(account -> isCompatibleUsage(account.getUsageScopeCode(), requiredUsageScope))
                    .filter(account -> "COLLECTION".equals(requiredUsageScope)
                            ? account.isDefaultCollection()
                            : account.isDefaultExpense())
                    .findFirst()
                    .orElse(null);
        }
        FinanceAccount account = requireAccount(clubId, financeAccountId);
        if (!account.isActive()) {
            throw new SemoException.ValidationException("비활성 계좌·결제수단은 사용할 수 없습니다.");
        }
        if (!isCompatibleUsage(account.getUsageScopeCode(), requiredUsageScope)) {
            throw new SemoException.ValidationException("선택한 계좌·결제수단의 사용 범위가 맞지 않습니다.");
        }
        return account;
    }

    public ClubScheduleEvent resolveScheduleEvent(Long clubId, Long linkedScheduleEventId) {
        if (linkedScheduleEventId == null) {
            return null;
        }
        if (!clubFeatureService.isFeatureEnabled(clubId, "SCHEDULE_MANAGE")) {
            throw new SemoException.ValidationException("일정 기능이 비활성화되어 재정 항목에 일정을 연결할 수 없습니다.");
        }
        return clubScheduleEventRepository.findByEventIdAndClubId(linkedScheduleEventId, clubId)
                .filter(event -> !"CANCELLED".equals(event.getEventStatus()))
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "ClubScheduleEvent",
                        "eventId",
                        linkedScheduleEventId
                ));
    }

    public FinanceAccount requireAccount(Long clubId, Long financeAccountId) {
        return financeAccountRepository.findByFinanceAccountIdAndClubId(financeAccountId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "FinanceAccount",
                        "financeAccountId",
                        financeAccountId
                ));
    }

    private ClubAccessResolver.ClubAccess requireOperationsView(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        if (!clubFinancePermissionService.isFinanceEnabled(clubId)) {
            throw new SemoException.ForbiddenException("재정관리 기능이 활성화되지 않았습니다.");
        }
        if (!clubFinancePermissionService.canViewAdminFinance(access)) {
            throw new SemoException.ForbiddenException("재정 운영 화면을 조회할 권한이 없습니다.");
        }
        return access;
    }

    private ClubAccessResolver.ClubAccess requireCanConfigure(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = requireOperationsView(clubId, userKey);
        if (!clubFinancePermissionService.canClosePeriods(access)) {
            throw new SemoException.ForbiddenException("재정 계좌·예산·기간을 관리할 권한이 없습니다.");
        }
        return access;
    }

    private FinancePeriod requireOpenPeriod(Long clubId, Long financePeriodId) {
        FinancePeriod period = financePeriodRepository.findForUpdate(financePeriodId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "FinancePeriod",
                        "financePeriodId",
                        financePeriodId
                ));
        if (!PERIOD_STATUS_OPEN.equals(period.getStatusCode())) {
            throw new SemoException.ValidationException("마감된 재정 기간의 예산은 수정할 수 없습니다.");
        }
        return period;
    }

    private void validateDefaultScopes(String usageScopeCode, boolean defaultCollection, boolean defaultExpense) {
        if (defaultCollection && !isCompatibleUsage(usageScopeCode, "COLLECTION")) {
            throw new SemoException.ValidationException("수납에 사용할 수 없는 계좌를 기본 수납 계좌로 지정할 수 없습니다.");
        }
        if (defaultExpense && !isCompatibleUsage(usageScopeCode, "EXPENSE")) {
            throw new SemoException.ValidationException("지출에 사용할 수 없는 결제수단을 기본 지출 수단으로 지정할 수 없습니다.");
        }
    }

    private boolean isCompatibleUsage(String accountUsageScope, String requiredUsageScope) {
        return "BOTH".equals(accountUsageScope) || requiredUsageScope.equals(accountUsageScope);
    }

    private void clearExistingDefaults(
            Long clubId,
            boolean clearCollection,
            boolean clearExpense,
            Long exceptFinanceAccountId
    ) {
        if (!clearCollection && !clearExpense) {
            return;
        }
        List<FinanceAccount> accounts = financeAccountRepository.findByClubIdAndActiveTrue(clubId);
        for (FinanceAccount account : accounts) {
            if (account.getFinanceAccountId().equals(exceptFinanceAccountId)) {
                continue;
            }
            if (clearCollection) {
                account.clearCollectionDefault();
            }
            if (clearExpense) {
                account.clearExpenseDefault();
            }
        }
        financeAccountRepository.saveAll(accounts);
    }

    private BigDecimal resolvePreviousClosingBalance(Long clubId) {
        return financePeriodRepository.findByClubIdOrderByStartDateDescFinancePeriodIdDesc(clubId).stream()
                .filter(period -> PERIOD_STATUS_CLOSED.equals(period.getStatusCode()))
                .map(FinancePeriod::getClosingBalance)
                .filter(value -> value != null)
                .findFirst()
                .orElse(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateBalance(FinancePeriod period) {
        return period.getOpeningBalance()
                .add(financePaymentRepository.sumPaidAmountByFinancePeriodId(period.getFinancePeriodId()))
                .subtract(financeExpenseRepository.sumPostedAmountByFinancePeriodId(period.getFinancePeriodId()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private FinanceAccountResponse toAccountResponse(FinanceAccount account) {
        return new FinanceAccountResponse(
                account.getFinanceAccountId(),
                account.getDisplayName(),
                account.getAccountTypeCode(),
                clubFinanceSupport.resolveAccountTypeLabel(account.getAccountTypeCode()),
                account.getProviderName(),
                account.getMaskedIdentifier(),
                account.getHolderName(),
                account.getUsageScopeCode(),
                clubFinanceSupport.resolveAccountUsageScopeLabel(account.getUsageScopeCode()),
                account.isActive(),
                account.isDefaultCollection(),
                account.isDefaultExpense()
        );
    }

    private FinancePeriodResponse toPeriodResponse(FinancePeriod period) {
        BigDecimal collectedAmount = financePaymentRepository.sumPaidAmountByFinancePeriodId(period.getFinancePeriodId());
        BigDecimal spentAmount = financeExpenseRepository.sumPostedAmountByFinancePeriodId(period.getFinancePeriodId());
        BigDecimal currentBalance = period.getOpeningBalance().add(collectedAmount).subtract(spentAmount);
        List<FinanceBudgetResponse> budgets = financeBudgetRepository
                .findByFinancePeriodIdOrderByCategoryCodeAscFinanceBudgetIdAsc(period.getFinancePeriodId())
                .stream()
                .map(this::toBudgetResponse)
                .toList();
        return new FinancePeriodResponse(
                period.getFinancePeriodId(),
                period.getClubOperatingTermId(),
                period.getTitle(),
                period.getStartDate(),
                period.getEndDate(),
                period.getStatusCode(),
                period.getOpeningBalance(),
                clubFinanceSupport.formatAmount(period.getOpeningBalance(), "KRW"),
                collectedAmount,
                clubFinanceSupport.formatAmount(collectedAmount, "KRW"),
                spentAmount,
                clubFinanceSupport.formatAmount(spentAmount, "KRW"),
                currentBalance,
                clubFinanceSupport.formatAmount(currentBalance, "KRW"),
                period.getClosingBalance(),
                period.getClosingBalance() == null
                        ? null
                        : clubFinanceSupport.formatAmount(period.getClosingBalance(), "KRW"),
                clubFinanceSupport.formatDateTimeValue(period.getClosedAt()),
                period.getNote(),
                budgets
        );
    }

    private FinanceBudgetResponse toBudgetResponse(FinanceBudget budget) {
        BigDecimal spentAmount = financeExpenseRepository.sumPostedAmountByFinancePeriodIdAndCategoryCode(
                budget.getFinancePeriodId(),
                budget.getCategoryCode()
        );
        BigDecimal remainingAmount = budget.getAllocatedAmount().subtract(spentAmount);
        int executionRate = budget.getAllocatedAmount().signum() == 0
                ? 0
                : spentAmount.multiply(BigDecimal.valueOf(100))
                        .divide(budget.getAllocatedAmount(), 0, RoundingMode.HALF_UP)
                        .intValue();
        return new FinanceBudgetResponse(
                budget.getFinanceBudgetId(),
                budget.getCategoryCode(),
                clubFinanceSupport.resolveExpenseCategoryLabel(budget.getCategoryCode()),
                budget.getAllocatedAmount(),
                clubFinanceSupport.formatAmount(budget.getAllocatedAmount(), "KRW"),
                spentAmount,
                clubFinanceSupport.formatAmount(spentAmount, "KRW"),
                remainingAmount,
                clubFinanceSupport.formatAmount(remainingAmount, "KRW"),
                executionRate,
                budget.getNote()
        );
    }

    private FinanceScheduleOptionResponse toScheduleOptionResponse(ClubScheduleEvent event) {
        return new FinanceScheduleOptionResponse(
                event.getEventId(),
                event.getTitle(),
                clubFinanceSupport.formatDateTimeValue(event.getStartAt()),
                clubFinanceSupport.formatDateTimeLabel(event.getStartAt())
        );
    }
}
