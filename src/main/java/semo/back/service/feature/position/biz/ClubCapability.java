package semo.back.service.feature.position.biz;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ClubCapability {
    NOTICE_CREATE("NOTICE"),
    NOTICE_UPDATE_SELF("NOTICE"),
    NOTICE_DELETE_SELF("NOTICE"),
    POLL_CREATE("POLL"),
    POLL_UPDATE_SELF("POLL"),
    POLL_DELETE_SELF("POLL"),
    SCHEDULE_CREATE("SCHEDULE_MANAGE"),
    SCHEDULE_UPDATE_SELF("SCHEDULE_MANAGE"),
    SCHEDULE_DELETE_SELF("SCHEDULE_MANAGE"),
    ATTENDANCE_MANAGE("ATTENDANCE"),
    TOURNAMENT_RECORD_CREATE("TOURNAMENT_RECORD"),
    TOURNAMENT_RECORD_UPDATE_SELF("TOURNAMENT_RECORD"),
    TOURNAMENT_RECORD_PIN("TOURNAMENT_RECORD"),
    TOURNAMENT_RECORD_REVIEW("TOURNAMENT_RECORD"),
    TOURNAMENT_RECORD_DELETE_ANY("TOURNAMENT_RECORD"),
    BRACKET_CREATE("BRACKET"),
    BRACKET_UPDATE_SELF("BRACKET"),
    BRACKET_REVIEW("BRACKET"),
    BRACKET_DELETE_ANY("BRACKET"),
    FINANCE_VIEW("FINANCE"),
    FINANCE_BILLING_ISSUE("FINANCE"),
    FINANCE_REQUEST_REVIEW("FINANCE"),
    FINANCE_EXPENSE_CREATE("FINANCE"),
    FINANCE_PAYMENT_UPDATE("FINANCE"),
    FINANCE_EXPORT("FINANCE"),
    FINANCE_PERIOD_CLOSE("FINANCE"),
    TODO_VIEW("TODO"),
    TODO_CREATE("TODO"),
    TODO_ASSIGN("TODO"),
    TODO_MANAGE_STATUS("TODO"),
    TODO_DELETE_ANY("TODO"),
    HANDOVER_VIEW("HANDOVER"),
    HANDOVER_MANAGE("HANDOVER"),
    DECISION_VIEW("DECISION_LOG"),
    DECISION_MANAGE("DECISION_LOG");

    private static final Map<String, ClubCapability> BY_KEY = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(ClubCapability::permissionKey, Function.identity()));

    private final String featureKey;

    ClubCapability(String featureKey) {
        this.featureKey = featureKey;
    }

    public String permissionKey() {
        return name();
    }

    public String featureKey() {
        return featureKey;
    }

    public static Optional<ClubCapability> fromPermissionKey(String permissionKey) {
        return Optional.ofNullable(BY_KEY.get(permissionKey));
    }
}
