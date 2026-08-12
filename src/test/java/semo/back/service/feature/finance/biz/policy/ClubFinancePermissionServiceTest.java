package semo.back.service.feature.finance.biz.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import semo.back.service.database.pub.entity.Club;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.position.biz.ClubCapability;
import semo.back.service.feature.position.biz.ClubPositionPermissionEvaluator;

@ExtendWith(MockitoExtension.class)
class ClubFinancePermissionServiceTest {
    private static final Long CLUB_ID = 1L;
    private static final Long CLUB_MEMBER_ID = 11L;

    @Mock
    private ClubFeatureService clubFeatureService;

    @Mock
    private ClubPositionPermissionEvaluator clubPositionPermissionEvaluator;

    @InjectMocks
    private ClubFinancePermissionService clubFinancePermissionService;

    private ClubAccessResolver.ClubAccess memberAccess;

    @BeforeEach
    void setUp() {
        memberAccess = new ClubAccessResolver.ClubAccess(
                Club.builder().clubId(CLUB_ID).build(),
                ClubMember.builder().clubMemberId(CLUB_MEMBER_ID).roleCode("MEMBER").build(),
                null,
                null
        );
    }

    @Test
    void canIssueFinance_legacyBroadPermission_doesNotGrantOperations() {
        assertThat(clubFinancePermissionService.canIssueFinance(memberAccess)).isFalse();
    }

    @Test
    void canManageBilling_billingPermission_grantsBillingOnly() {
        grantPermissions(ClubFinancePermissionService.PERMISSION_FINANCE_BILLING_ISSUE);

        assertThat(clubFinancePermissionService.canManageBilling(memberAccess)).isTrue();
    }

    @Test
    void canReviewRequests_reviewPermission_grantsReview() {
        grantPermissions(ClubFinancePermissionService.PERMISSION_FINANCE_REQUEST_REVIEW);

        assertThat(clubFinancePermissionService.canReviewRequests(memberAccess)).isTrue();
    }

    @Test
    void canCreateExpenses_expensePermission_grantsExpenseEntry() {
        grantPermissions(ClubFinancePermissionService.PERMISSION_FINANCE_EXPENSE_CREATE);

        assertThat(clubFinancePermissionService.canCreateExpenses(memberAccess)).isTrue();
    }

    @Test
    void canUpdatePayments_paymentPermission_grantsPaymentUpdates() {
        grantPermissions(ClubFinancePermissionService.PERMISSION_FINANCE_PAYMENT_UPDATE);

        assertThat(clubFinancePermissionService.canUpdatePayments(memberAccess)).isTrue();
    }

    private void grantPermissions(ClubCapability capability) {
        when(clubPositionPermissionEvaluator.hasAnyPermission(memberAccess, capability)).thenReturn(true);
    }
}
