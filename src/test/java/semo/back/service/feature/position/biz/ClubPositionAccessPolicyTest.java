package semo.back.service.feature.position.biz;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ClubPositionAccessPolicyTest {
    private final ClubPositionAccessPolicy accessPolicy = new ClubPositionAccessPolicy();

    @Test
    void findPolicy_currentDelegablePermissions_coversEveryPermission() {
        Set<String> supportedPermissionKeys = List.of(
                        "NOTICE",
                        "POLL",
                        "SCHEDULE_MANAGE",
                        "ATTENDANCE",
                        "TOURNAMENT_RECORD",
                        "BRACKET",
                        "FINANCE",
                        "TODO",
                        "HANDOVER",
                        "DECISION_LOG"
                ).stream()
                .map(featureKey -> accessPolicy.findPolicy(featureKey).orElseThrow())
                .flatMap(policy -> policy.supportedPermissionKeys().stream())
                .collect(java.util.stream.Collectors.toSet());

        assertThat(supportedPermissionKeys).containsExactlyInAnyOrder(
                "NOTICE_CREATE",
                "NOTICE_UPDATE_SELF",
                "NOTICE_DELETE_SELF",
                "POLL_CREATE",
                "POLL_UPDATE_SELF",
                "POLL_DELETE_SELF",
                "SCHEDULE_CREATE",
                "SCHEDULE_UPDATE_SELF",
                "SCHEDULE_DELETE_SELF",
                "ATTENDANCE_MANAGE",
                "TOURNAMENT_RECORD_CREATE",
                "TOURNAMENT_RECORD_UPDATE_SELF",
                "TOURNAMENT_RECORD_PIN",
                "TOURNAMENT_RECORD_REVIEW",
                "TOURNAMENT_RECORD_DELETE_ANY",
                "BRACKET_CREATE",
                "BRACKET_UPDATE_SELF",
                "BRACKET_REVIEW",
                "BRACKET_DELETE_ANY",
                "FINANCE_VIEW",
                "FINANCE_BILLING_ISSUE",
                "FINANCE_REQUEST_REVIEW",
                "FINANCE_EXPENSE_CREATE",
                "FINANCE_PAYMENT_UPDATE",
                "FINANCE_EXPORT",
                "FINANCE_PERIOD_CLOSE",
                "TODO_VIEW",
                "TODO_CREATE",
                "TODO_ASSIGN",
                "TODO_MANAGE_STATUS",
                "TODO_DELETE_ANY",
                "HANDOVER_VIEW",
                "HANDOVER_MANAGE",
                "DECISION_VIEW",
                "DECISION_MANAGE"
        );
    }

    @Test
    void findPolicy_finance_separatesSensitivePermissionsFromAccessLevels() {
        ClubPositionAccessPolicy.FeatureAccessPolicy financePolicy = accessPolicy.findPolicy("FINANCE").orElseThrow();
        Set<String> levelPermissionKeys = financePolicy.accessLevels().stream()
                .flatMap(level -> level.permissionKeys().stream())
                .collect(java.util.stream.Collectors.toSet());

        assertThat(levelPermissionKeys).doesNotContainAnyElementsOf(financePolicy.sensitivePermissionKeys());
    }

    @Test
    void findPolicy_destructiveCompetitionPermissions_areSensitive() {
        assertThat(accessPolicy.findPolicy("TOURNAMENT_RECORD").orElseThrow().sensitivePermissionKeys())
                .containsExactly("TOURNAMENT_RECORD_DELETE_ANY");
        assertThat(accessPolicy.findPolicy("BRACKET").orElseThrow().sensitivePermissionKeys())
                .containsExactly("BRACKET_DELETE_ANY");
    }

    @Test
    void findPolicy_eachHigherLevel_containsPreviousLevelPermissions() {
        boolean allLevelsAreMonotonic = List.of(
                        "NOTICE",
                        "POLL",
                        "SCHEDULE_MANAGE",
                        "ATTENDANCE",
                        "TOURNAMENT_RECORD",
                        "BRACKET",
                        "FINANCE",
                        "TODO",
                        "HANDOVER",
                        "DECISION_LOG"
                ).stream()
                .map(featureKey -> accessPolicy.findPolicy(featureKey).orElseThrow())
                .allMatch(this::isMonotonic);

        assertThat(allLevelsAreMonotonic).isTrue();
    }

    @Test
    void findPolicy_unknownFeature_returnsEmpty() {
        assertThat(accessPolicy.findPolicy("UNKNOWN_FEATURE")).isEmpty();
    }

    @Test
    void positionTemplates_referenceKnownNonSensitiveAccessLevels() {
        boolean valid = accessPolicy.positionTemplates().stream()
                .flatMap(template -> template.grants().stream())
                .allMatch(grant -> accessPolicy.findPolicy(grant.featureKey())
                        .map(policy -> policy.accessLevels().stream()
                                .anyMatch(level -> level.accessLevel().equals(grant.accessLevel())))
                        .orElse(false));

        assertThat(valid).isTrue();
    }

    private boolean isMonotonic(ClubPositionAccessPolicy.FeatureAccessPolicy policy) {
        Set<String> previousPermissionKeys = Set.of();
        for (ClubPositionAccessPolicy.AccessLevel accessLevel : policy.accessLevels()) {
            Set<String> currentPermissionKeys = Set.copyOf(accessLevel.permissionKeys());
            if (!currentPermissionKeys.containsAll(previousPermissionKeys)) {
                return false;
            }
            previousPermissionKeys = currentPermissionKeys;
        }
        return true;
    }
}
