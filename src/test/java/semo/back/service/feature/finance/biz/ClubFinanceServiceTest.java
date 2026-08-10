package semo.back.service.feature.finance.biz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.repository.ClubFeatureRepository;
import semo.back.service.database.pub.repository.ClubMemberPositionRepository;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubNotificationRepository;
import semo.back.service.database.pub.repository.ClubPositionPermissionRepository;
import semo.back.service.database.pub.repository.ClubPositionRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.FeatureCatalogRepository;
import semo.back.service.database.pub.repository.FinanceExpenseRepository;
import semo.back.service.database.pub.repository.FinanceExpenseRevisionRepository;
import semo.back.service.database.pub.repository.FinanceAccountRepository;
import semo.back.service.database.pub.repository.FinanceBudgetRepository;
import semo.back.service.database.pub.repository.FinanceObligationRepository;
import semo.back.service.database.pub.repository.FinancePaymentRepository;
import semo.back.service.database.pub.repository.FinancePeriodRepository;
import semo.back.service.database.pub.repository.FinanceRequestRepository;
import semo.back.service.database.pub.repository.ProfileUserRepository;
import semo.back.service.feature.club.biz.ClubService;
import semo.back.service.feature.club.vo.CreateClubRequest;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.finance.vo.CreateFinanceExpenseRequest;
import semo.back.service.feature.clubfeature.vo.UpdateClubFeaturesRequest;
import semo.back.service.feature.finance.vo.CreateFinanceObligationRequest;
import semo.back.service.feature.finance.vo.CreateFinanceRequestRequest;
import semo.back.service.feature.finance.vo.CorrectFinanceExpenseRequest;
import semo.back.service.feature.finance.vo.CreateFinancePeriodRequest;
import semo.back.service.feature.finance.vo.ReviewFinanceRequestRequest;
import semo.back.service.feature.finance.vo.UpdateFinancePaymentStatusRequest;
import semo.back.service.feature.finance.vo.UpsertFinanceBudgetRequest;
import semo.back.service.feature.finance.vo.VoidFinanceExpenseRequest;
import semo.back.service.feature.profile.biz.ProfileUserService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static semo.back.service.support.TestCatalogSeeder.seedFeatureCatalogs;

@SpringBootTest
@ActiveProfiles("test")
class ClubFinanceServiceTest {

    @Autowired
    private ClubFinanceService clubFinanceService;

    @Autowired
    private ClubFinanceOperationsService clubFinanceOperationsService;

    @Autowired
    private ClubFinanceExportService clubFinanceExportService;

    @Autowired
    private ClubService clubService;

    @Autowired
    private ClubFeatureService clubFeatureService;

    @Autowired
    private ProfileUserService profileUserService;

    @Autowired
    private FinancePaymentRepository financePaymentRepository;

    @Autowired
    private FinanceObligationRepository financeObligationRepository;

    @Autowired
    private FinanceRequestRepository financeRequestRepository;

    @Autowired
    private FinanceExpenseRepository financeExpenseRepository;

    @Autowired
    private FinanceExpenseRevisionRepository financeExpenseRevisionRepository;

    @Autowired
    private FinanceBudgetRepository financeBudgetRepository;

    @Autowired
    private FinancePeriodRepository financePeriodRepository;

    @Autowired
    private FinanceAccountRepository financeAccountRepository;

    @Autowired
    private ClubNotificationRepository clubNotificationRepository;

    @Autowired
    private ClubFeatureRepository clubFeatureRepository;

    @Autowired
    private ClubProfileRepository clubProfileRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ClubMemberPositionRepository clubMemberPositionRepository;

    @Autowired
    private ClubPositionPermissionRepository clubPositionPermissionRepository;

    @Autowired
    private ClubPositionRepository clubPositionRepository;

    @Autowired
    private ProfileUserRepository profileUserRepository;

    @Autowired
    private FeatureCatalogRepository featureCatalogRepository;

    @BeforeEach
    void setUp() {
        clubNotificationRepository.deleteAll();
        financeExpenseRevisionRepository.deleteAll();
        financeExpenseRepository.deleteAll();
        financeRequestRepository.deleteAll();
        financePaymentRepository.deleteAll();
        financeObligationRepository.findAll().stream()
                .sorted(java.util.Comparator.comparingLong(
                        obligation -> -obligation.getFinanceObligationId()
                ))
                .forEach(financeObligationRepository::delete);
        financeBudgetRepository.deleteAll();
        financePeriodRepository.deleteAll();
        financeAccountRepository.deleteAll();
        clubMemberPositionRepository.deleteAll();
        clubPositionPermissionRepository.deleteAll();
        clubPositionRepository.deleteAll();
        clubFeatureRepository.deleteAll();
        clubProfileRepository.deleteAll();
        clubMemberRepository.deleteAll();
        clubRepository.deleteAll();
        profileUserRepository.deleteAll();
        seedFeatureCatalogs(featureCatalogRepository);
    }

    @Test
    void memberCanSeeIssuedObligationAndAdminCanMarkItPaid() {
        Long clubId = createEnabledClub("finance-owner-001", "Finance Owner", "Finance Club");
        addActiveMember(clubId, "finance-member-001", "Finance Member");

        var created = clubFinanceService.createObligation(
                clubId,
                "finance-owner-001",
                new CreateFinanceObligationRequest(
                        "봄 대회 참가비",
                        new BigDecimal("10000"),
                        "2026-04-30T23:59",
                        "현장 집결 전 납부",
                        "ALL_ACTIVE_MEMBERS",
                        null
                )
        );

        assertThat(created.createdCount()).isEqualTo(2);
        assertThat(created.title()).isEqualTo("봄 대회 참가비");
        assertThat(created.obligationTypeCode()).isEqualTo("FEE");
        assertThat(clubNotificationRepository.findAll())
                .hasSize(2)
                .allSatisfy(notification -> {
                    assertThat(notification.getNotificationType()).isEqualTo("FINANCE_OBLIGATION_CREATED");
                    assertThat(notification.getResourceId()).isEqualTo(created.obligationId());
                    assertThat(notification.getTargetPath()).isEqualTo("/clubs/" + clubId + "/more/finance");
                });

        var memberFinance = clubFinanceService.getFinance(clubId, "finance-member-001");
        assertThat(memberFinance.openObligations()).hasSize(1);
        assertThat(memberFinance.nextPayableObligation()).isNotNull();
        assertThat(memberFinance.nextPayableObligation().title()).isEqualTo("봄 대회 참가비");
        assertThat(memberFinance.actionRequiredCount()).isEqualTo(1);
        assertThat(memberFinance.totalPendingAmountLabel()).isEqualTo("10,000원");
        assertThat(memberFinance.totalPaidAmountLabel()).isEqualTo("0원");
        assertThat(memberFinance.recentPayments()).isEmpty();

        Long paymentId = memberFinance.nextPayableObligation().payment().paymentId();
        var updated = clubFinanceService.updatePaymentStatus(
                clubId,
                paymentId,
                "finance-owner-001",
                new UpdateFinancePaymentStatusRequest("PAID", "현장 수납")
        );

        assertThat(updated.paymentStatusCode()).isEqualTo("PAID");
        assertThat(financePaymentRepository.findByFinancePaymentIdAndClubId(paymentId, clubId))
                .get()
                .extracting(payment -> payment.getPaymentStatusCode())
                .isEqualTo("PAID");
    }

    @Test
    void selectedMembersOnlyReceiveSelectedObligation() {
        Long clubId = createEnabledClub("finance-owner-002", "Finance Owner 2", "Finance Club 2");
        addActiveMember(clubId, "finance-member-002a", "Selected Member");
        addActiveMember(clubId, "finance-member-002b", "Excluded Member");

        var adminBeforeIssue = clubFinanceService.getAdminFinance(clubId, "finance-owner-002");
        Long selectedClubProfileId = adminBeforeIssue.availableMembers().stream()
                .filter(member -> "Selected Member".equals(member.memberDisplayName()))
                .findFirst()
                .orElseThrow()
                .clubProfileId();

        var created = clubFinanceService.createObligation(
                clubId,
                "finance-owner-002",
                new CreateFinanceObligationRequest(
                        "신규 유니폼비",
                        new BigDecimal("35000"),
                        null,
                        "선택 멤버만 발행",
                        "SELECTED_MEMBERS",
                        List.of(selectedClubProfileId)
                )
        );

        assertThat(created.createdCount()).isEqualTo(1);

        var adminFinance = clubFinanceService.getAdminFinance(clubId, "finance-owner-002");
        assertThat(adminFinance.totalObligationCount()).isEqualTo(1);
        assertThat(adminFinance.totalPaymentCount()).isEqualTo(1);
        assertThat(adminFinance.totalBilledAmountLabel()).isEqualTo("35,000원");
        assertThat(adminFinance.totalCollectedAmountLabel()).isEqualTo("0원");
        assertThat(adminFinance.totalOutstandingAmountLabel()).isEqualTo("35,000원");
        assertThat(getAdminObligationFeed(clubId, "finance-owner-002").items()).singleElement().satisfies(obligation -> {
            assertThat(obligation.targetScopeCode()).isEqualTo("SELECTED_MEMBERS");
            assertThat(getAdminObligationDetail(clubId, obligation.obligationId(), "finance-owner-002").payments())
                    .singleElement()
                    .satisfies(payment -> assertThat(payment.memberDisplayName()).isEqualTo("Selected Member"));
        });
    }

    @Test
    void pendingOnlyObligationCanBeDeleted() {
        Long clubId = createEnabledClub("finance-owner-003", "Finance Owner 3", "Finance Club 3");
        addActiveMember(clubId, "finance-member-003", "Finance Member 3");

        var created = clubFinanceService.createObligation(
                clubId,
                "finance-owner-003",
                new CreateFinanceObligationRequest(
                        "삭제 가능한 분담금",
                        new BigDecimal("12000"),
                        null,
                        "아직 미처리",
                        "ALL_ACTIVE_MEMBERS",
                        null
                )
        );

        assertThat(getAdminObligationFeed(clubId, "finance-owner-003").items())
                .singleElement()
                .satisfies(obligation -> assertThat(obligation.canDelete()).isTrue());

        clubFinanceService.deleteObligation(clubId, created.obligationId(), "finance-owner-003");

        var adminFinance = clubFinanceService.getAdminFinance(clubId, "finance-owner-003");
        assertThat(adminFinance.totalObligationCount()).isZero();
        assertThat(adminFinance.totalPaymentCount()).isZero();
        assertThat(adminFinance.totalBilledAmountLabel()).isEqualTo("0원");
        assertThat(financeObligationRepository.findByFinanceObligationIdAndClubId(created.obligationId(), clubId)).isEmpty();
    }

    @Test
    void processedObligationCannotBeDeleted() {
        Long clubId = createEnabledClub("finance-owner-004", "Finance Owner 4", "Finance Club 4");
        addActiveMember(clubId, "finance-member-004", "Finance Member 4");

        var created = clubFinanceService.createObligation(
                clubId,
                "finance-owner-004",
                new CreateFinanceObligationRequest(
                        "삭제 불가 분담금",
                        new BigDecimal("15000"),
                        null,
                        "처리됨",
                        "ALL_ACTIVE_MEMBERS",
                        null
                )
        );

        Long obligationId = getAdminObligationFeed(clubId, "finance-owner-004").items().getFirst().obligationId();
        Long paymentId = getAdminObligationDetail(clubId, obligationId, "finance-owner-004").payments().getFirst().paymentId();
        clubFinanceService.updatePaymentStatus(
                clubId,
                paymentId,
                "finance-owner-004",
                new UpdateFinancePaymentStatusRequest("PAID", "납부 완료")
        );

        var updatedObligation = getAdminObligationFeed(clubId, "finance-owner-004").items().getFirst();
        assertThat(updatedObligation.canDelete()).isFalse();
        var adminFinance = clubFinanceService.getAdminFinance(clubId, "finance-owner-004");
        assertThat(adminFinance.totalCollectedAmountLabel()).isEqualTo("15,000원");
        assertThat(adminFinance.totalOutstandingAmountLabel()).isEqualTo("15,000원");

        assertThatThrownBy(() -> clubFinanceService.deleteObligation(clubId, created.obligationId(), "finance-owner-004"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("아직 아무도 처리하지 않은 재정 항목만 삭제할 수 있습니다.");
    }

    @Test
    void adminObligationFeedUsesCursorAndCanFilterSettledItems() {
        Long clubId = createEnabledClub("finance-owner-005", "Finance Owner 5", "Finance Club 5");
        addActiveMember(clubId, "finance-member-005a", "Alpha Member");
        addActiveMember(clubId, "finance-member-005b", "Beta Member");

        for (int index = 1; index <= 12; index += 1) {
            clubFinanceService.createObligation(
                    clubId,
                    "finance-owner-005",
                    new CreateFinanceObligationRequest(
                            "페이지 분담금 " + index,
                            new BigDecimal("1000"),
                            null,
                            "커서 테스트",
                            "ALL_ACTIVE_MEMBERS",
                            null
                    )
            );
        }

        var firstPage = clubFinanceService.getAdminFinanceObligations(clubId, "finance-owner-005", null, null, null, 10);
        assertThat(firstPage.items()).hasSize(10);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.nextCursorObligationId()).isNotNull();
        assertThat(firstPage.items().getFirst().title()).isEqualTo("페이지 분담금 12");

        var secondPage = clubFinanceService.getAdminFinanceObligations(
                clubId,
                "finance-owner-005",
                null,
                null,
                firstPage.nextCursorObligationId(),
                10
        );
        assertThat(secondPage.items()).hasSize(2);
        assertThat(secondPage.hasNext()).isFalse();

        var obligation = firstPage.items().getFirst();
        var detail = clubFinanceService.getAdminFinanceObligationDetail(clubId, obligation.obligationId(), "finance-owner-005");
        detail.payments().forEach(payment ->
                clubFinanceService.updatePaymentStatus(
                        clubId,
                        payment.paymentId(),
                        "finance-owner-005",
                        new UpdateFinancePaymentStatusRequest("PAID", "일괄 완료")
                )
        );

        var settledOnly = clubFinanceService.getAdminFinanceObligations(
                clubId,
                "finance-owner-005",
                null,
                "SETTLED",
                null,
                10
        );
        assertThat(settledOnly.items())
                .extracting(item -> item.obligationId())
                .contains(obligation.obligationId());
    }

    @Test
    void memberRecentPaymentsOnlyIncludePaidItemsAndAreLimited() {
        Long clubId = createEnabledClub("finance-owner-006", "Finance Owner 6", "Finance Club 6");
        addActiveMember(clubId, "finance-member-006", "Finance Member 6");

        IntStream.rangeClosed(1, 6).forEach(index ->
                clubFinanceService.createObligation(
                        clubId,
                        "finance-owner-006",
                        new CreateFinanceObligationRequest(
                                "최근 납부 테스트 " + index,
                                new BigDecimal("1000"),
                                null,
                                "recent payment",
                                "ALL_ACTIVE_MEMBERS",
                                null
                        )
                )
        );

        var feed = getAdminObligationFeed(clubId, "finance-owner-006");
        feed.items().stream()
                .limit(6)
                .forEach(obligation -> {
                    Long paymentId = getAdminObligationDetail(clubId, obligation.obligationId(), "finance-owner-006")
                            .payments()
                            .stream()
                            .filter(payment -> "Finance Member 6".equals(payment.memberDisplayName()))
                            .findFirst()
                            .orElseThrow()
                            .paymentId();
                    clubFinanceService.updatePaymentStatus(
                            clubId,
                            paymentId,
                            "finance-owner-006",
                            new UpdateFinancePaymentStatusRequest("PAID", "recent paid")
                    );
                });

        var memberFinance = clubFinanceService.getFinance(clubId, "finance-member-006");
        assertThat(memberFinance.recentPayments()).hasSize(5);
        assertThat(memberFinance.recentPayments())
                .allSatisfy(item -> assertThat(item.payment().paymentStatusCode()).isEqualTo("PAID"));
    }

    @Test
    void memberCanSubmitFinanceRequestAndAdminCanReviewIt() {
        Long clubId = createEnabledClub("finance-owner-007", "Finance Owner 7", "Finance Club 7");
        addActiveMember(clubId, "finance-member-007", "Finance Member 7");

        var created = clubFinanceService.createFinanceRequest(
                clubId,
                "finance-member-007",
                new CreateFinanceRequestRequest(
                        "ADVANCE",
                        "행사 간식 선지출",
                        new BigDecimal("24800"),
                        "봄 친선전",
                        "영수증 첨부 예정"
                )
        );

        assertThat(created.requestTypeCode()).isEqualTo("ADVANCE");
        assertThat(created.statusCode()).isEqualTo("SUBMITTED");
        assertThat(created.amountLabel()).isEqualTo("24,800원");

        var memberRequests = clubFinanceService.getMyFinanceRequests(clubId, "finance-member-007");
        assertThat(memberRequests.items()).singleElement().satisfies(item -> {
            assertThat(item.title()).isEqualTo("행사 간식 선지출");
            assertThat(item.requestTypeLabel()).isEqualTo("선지출 등록");
        });

        var reviewed = clubFinanceService.reviewFinanceRequest(
                clubId,
                created.requestId(),
                "finance-owner-007",
                new ReviewFinanceRequestRequest("APPROVED", "확인 후 상환 예정")
        );

        assertThat(reviewed.statusCode()).isEqualTo("APPROVED");
        assertThat(reviewed.statusLabel()).isEqualTo("승인");
        assertThat(clubFinanceService.getAdminFinanceRequests(clubId, "finance-owner-007").items())
                .singleElement()
                .satisfies(item -> assertThat(item.reviewNote()).isEqualTo("확인 후 상환 예정"));
        assertThat(clubFinanceService.getAdminFinanceExpenses(clubId, "finance-owner-007").items())
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.sourceRequestId()).isEqualTo(created.requestId());
                    assertThat(item.expenseTypeCode()).isEqualTo("APPROVED_REQUEST");
                });
    }

    @Test
    void adminCanCreateExpenseEntry() {
        Long clubId = createEnabledClub("finance-owner-008", "Finance Owner 8", "Finance Club 8");
        addActiveMember(clubId, "finance-member-008", "Finance Member 8");

        var created = clubFinanceService.createFinanceExpense(
                clubId,
                "finance-owner-008",
                new CreateFinanceExpenseRequest(
                        "연습장 대관비",
                        "VENUE",
                        new BigDecimal("56000"),
                        "2026-04-05T19:30:00",
                        "봄 시즌 훈련",
                        "현장 카드 결제"
                )
        );

        assertThat(created.expenseTypeCode()).isEqualTo("ADMIN_EXPENSE");
        assertThat(created.categoryLabel()).isEqualTo("대관비");
        assertThat(created.amountLabel()).isEqualTo("56,000원");

        var expenses = clubFinanceService.getAdminFinanceExpenses(clubId, "finance-owner-008");
        assertThat(expenses.items()).singleElement().satisfies(item -> {
            assertThat(item.title()).isEqualTo("연습장 대관비");
            assertThat(item.enteredByDisplayName()).isEqualTo("Finance Owner 8");
        });
    }

    @Test
    void reviewedRequestCannotBeReviewedTwice() {
        Long clubId = createEnabledClub("finance-owner-009", "Finance Owner 9", "Finance Club 9");
        addActiveMember(clubId, "finance-member-009", "Finance Member 9");

        var created = clubFinanceService.createFinanceRequest(
                clubId,
                "finance-member-009",
                new CreateFinanceRequestRequest(
                        "SETTLEMENT_REQUEST",
                        "정산 재검토 요청",
                        new BigDecimal("13000"),
                        "봄 시즌전",
                        "첫 요청"
                )
        );

        clubFinanceService.reviewFinanceRequest(
                clubId,
                created.requestId(),
                "finance-owner-009",
                new ReviewFinanceRequestRequest("APPROVED", "1차 승인")
        );

        assertThatThrownBy(() -> clubFinanceService.reviewFinanceRequest(
                clubId,
                created.requestId(),
                "finance-owner-009",
                new ReviewFinanceRequestRequest("REJECTED", "재검토")
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이미 검토가 완료된 재정 요청입니다.");
    }

    @Test
    void completedRecurringObligationCreatesExactlyOneNextCycle() {
        Long clubId = createEnabledClub("finance-owner-010", "Finance Owner 10", "Finance Club 10");
        addActiveMember(clubId, "finance-member-010", "Finance Member 10");

        var created = clubFinanceService.createObligation(
                clubId,
                "finance-owner-010",
                new CreateFinanceObligationRequest(
                        "월 회비",
                        new BigDecimal("20000"),
                        "2026-08-31T23:59:00",
                        "매월 말일까지 납부",
                        "ALL_ACTIVE_MEMBERS",
                        null,
                        null,
                        null,
                        null,
                        "MONTHLY",
                        1,
                        LocalDate.of(2026, 12, 31)
                )
        );

        getAdminObligationDetail(clubId, created.obligationId(), "finance-owner-010")
                .payments()
                .forEach(payment -> clubFinanceService.updatePaymentStatus(
                        clubId,
                        payment.paymentId(),
                        "finance-owner-010",
                        new UpdateFinancePaymentStatusRequest("PAID", "납부 확인")
                ));

        var next = financeObligationRepository
                .findByRecurrenceSourceFinanceObligationId(created.obligationId())
                .orElseThrow();
        assertThat(next.getDueAt()).isEqualTo(LocalDateTime.of(2026, 9, 30, 23, 59));
        assertThat(financePaymentRepository.findByFinanceObligationIdOrderByFinancePaymentIdDesc(
                next.getFinanceObligationId()
        )).hasSize(2);

        Long alreadyPaidId = getAdminObligationDetail(clubId, created.obligationId(), "finance-owner-010")
                .payments()
                .getFirst()
                .paymentId();
        clubFinanceService.updatePaymentStatus(
                clubId,
                alreadyPaidId,
                "finance-owner-010",
                new UpdateFinancePaymentStatusRequest("PAID", "중복 호출")
        );
        assertThat(financeObligationRepository.findByClubIdOrderByFinanceObligationIdAsc(clubId)).hasSize(2);
    }

    @Test
    void periodBudgetUsesPostedExpensesAndClosedPeriodRejectsCorrection() {
        Long clubId = createEnabledClub("finance-owner-011", "Finance Owner 11", "Finance Club 11");
        var period = clubFinanceOperationsService.createPeriod(
                clubId,
                "finance-owner-011",
                new CreateFinancePeriodRequest(
                        "2026년 7월",
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31),
                        null,
                        new BigDecimal("100000"),
                        "월간 마감"
                )
        );
        var expense = clubFinanceService.createFinanceExpense(
                clubId,
                "finance-owner-011",
                new CreateFinanceExpenseRequest(
                        "7월 대관비",
                        "VENUE",
                        new BigDecimal("30000"),
                        "2026-07-15T19:00:00",
                        null,
                        "카드 결제",
                        period.financePeriodId(),
                        null,
                        null
                )
        );

        var budgeted = clubFinanceOperationsService.upsertBudget(
                clubId,
                period.financePeriodId(),
                "finance-owner-011",
                new UpsertFinanceBudgetRequest("VENUE", new BigDecimal("50000"), "대관 예산")
        );
        assertThat(budgeted.budgets()).singleElement().satisfies(budget -> {
            assertThat(budget.spentAmount()).isEqualByComparingTo("30000.00");
            assertThat(budget.remainingAmount()).isEqualByComparingTo("20000.00");
            assertThat(budget.executionRate()).isEqualTo(60);
        });

        var closed = clubFinanceOperationsService.closePeriod(
                clubId,
                period.financePeriodId(),
                "finance-owner-011"
        );
        assertThat(closed.statusCode()).isEqualTo("CLOSED");
        assertThat(closed.closingBalance()).isEqualByComparingTo("70000.00");
        assertThatThrownBy(() -> clubFinanceService.correctFinanceExpense(
                clubId,
                expense.expenseId(),
                "finance-owner-011",
                new CorrectFinanceExpenseRequest(
                        "7월 대관비 정정",
                        "VENUE",
                        new BigDecimal("25000"),
                        "2026-07-15T19:00:00",
                        period.financePeriodId(),
                        null,
                        null,
                        null,
                        "금액 정정",
                        "영수증 금액 재확인"
                )
        )).hasMessageContaining("마감된 재정 기간");
    }

    @Test
    void expenseCorrectionAndVoidKeepRevisionHistoryAndExcludeVoidedAmount() {
        Long clubId = createEnabledClub("finance-owner-012", "Finance Owner 12", "Finance Club 12");
        var expense = clubFinanceService.createFinanceExpense(
                clubId,
                "finance-owner-012",
                new CreateFinanceExpenseRequest(
                        "간식비",
                        "MEAL",
                        new BigDecimal("12000"),
                        "2026-08-05T18:30:00",
                        null,
                        "초기 입력"
                )
        );
        var corrected = clubFinanceService.correctFinanceExpense(
                clubId,
                expense.expenseId(),
                "finance-owner-012",
                new CorrectFinanceExpenseRequest(
                        "행사 간식비",
                        "MEAL",
                        new BigDecimal("15000"),
                        "2026-08-05T18:30:00",
                        null,
                        null,
                        null,
                        null,
                        "영수증 기준",
                        "누락 품목 반영"
                )
        );
        assertThat(corrected.amount()).isEqualByComparingTo("15000.00");

        var voided = clubFinanceService.voidFinanceExpense(
                clubId,
                expense.expenseId(),
                "finance-owner-012",
                new VoidFinanceExpenseRequest("중복 입력 확인")
        );
        assertThat(voided.statusCode()).isEqualTo("VOIDED");
        assertThat(financeExpenseRepository.sumAmountByClubId(clubId)).isEqualByComparingTo("0.00");
        assertThat(clubFinanceService.getExpenseRevisions(
                clubId,
                expense.expenseId(),
                "finance-owner-012"
        )).extracting(revision -> revision.revisionTypeCode())
                .containsExactly("VOID", "CORRECTION");
    }

    @Test
    void csvExportPrefixesFormulaLikeCells() {
        Long clubId = createEnabledClub("finance-owner-013", "Finance Owner 13", "Finance Club 13");
        clubFinanceService.createFinanceExpense(
                clubId,
                "finance-owner-013",
                new CreateFinanceExpenseRequest(
                        "=HYPERLINK(\"https://example.com\")",
                        "OTHER",
                        new BigDecimal("1000"),
                        "2026-08-05T18:30:00",
                        null,
                        "CSV 검사"
                )
        );

        String csv = new String(clubFinanceExportService.exportCsv(
                clubId,
                null,
                "finance-owner-013"
        ).content(), java.nio.charset.StandardCharsets.UTF_8);

        assertThat(csv).contains("\"'=HYPERLINK(\"\"https://example.com\"\")\"");
        assertThat(csv).doesNotContain("\"=HYPERLINK");
    }

    private Long createEnabledClub(String ownerUserKey, String ownerDisplayName, String clubName) {
        Long clubId = clubService.createClub(
                ownerUserKey,
                ownerDisplayName,
                new CreateClubRequest(
                        clubName,
                        "재정 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        clubFeatureService.updateClubFeatures(
                clubId,
                ownerUserKey,
                new UpdateClubFeaturesRequest(List.of("FINANCE"))
        );
        return clubId;
    }

    private Long addActiveMember(Long clubId, String userKey, String displayName) {
        Long profileId = profileUserService.resolveProfileId(userKey, displayName);
        clubMemberRepository.save(ClubMember.builder()
                .clubId(clubId)
                .profileId(profileId)
                .roleCode("MEMBER")
                .membershipStatus("ACTIVE")
                .joinedAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .build());
        return profileId;
    }

    private semo.back.service.feature.finance.vo.ClubAdminFinanceObligationFeedResponse getAdminObligationFeed(Long clubId, String userKey) {
        return clubFinanceService.getAdminFinanceObligations(clubId, userKey, null, null, null, 10);
    }

    private semo.back.service.feature.finance.vo.ClubAdminFinanceObligationDetailResponse getAdminObligationDetail(
            Long clubId,
            Long obligationId,
            String userKey
    ) {
        return clubFinanceService.getAdminFinanceObligationDetail(clubId, obligationId, userKey);
    }
}
