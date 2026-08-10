package semo.back.service.feature.finance.biz.support;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.finance.vo.ClubFinanceUserObligationResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;

@Component
public class ClubFinanceSupport {
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_WAIVED = "WAIVED";

    private static final Set<String> ALLOWED_REQUEST_TYPES = Set.of(
            "ADVANCE",
            "REFUND_REQUEST",
            "SETTLEMENT_REQUEST"
    );
    private static final Set<String> ALLOWED_REVIEW_REQUEST_STATUSES = Set.of(
            "APPROVED",
            "REJECTED"
    );
    private static final Set<String> KNOWN_EXPENSE_CATEGORIES = Set.of(
            "MEMBERSHIP_FEE",
            "EVENT_FEE",
            "MEAL",
            "VENUE",
            "SUPPLIES",
            "TRANSPORT",
            "REFUND",
            "OTHER"
    );
    private static final Set<String> ALLOWED_TARGET_SCOPES = Set.of(
            "ALL_ACTIVE_MEMBERS",
            "SELECTED_MEMBERS"
    );
    private static final Set<String> ALLOWED_ADMIN_OBLIGATION_FILTERS = Set.of(
            "OPEN",
            "SETTLED"
    );
    private static final Set<String> ALLOWED_UPDATE_STATUSES = Set.of(
            STATUS_PENDING,
            STATUS_PAID,
            STATUS_WAIVED
    );
    private static final Set<String> ALLOWED_ACCOUNT_TYPES = Set.of("BANK", "CASH", "CARD", "OTHER");
    private static final Set<String> ALLOWED_ACCOUNT_USAGE_SCOPES = Set.of("COLLECTION", "EXPENSE", "BOTH");
    private static final Set<String> ALLOWED_PAYMENT_METHODS = Set.of("TRANSFER", "CASH", "CARD", "OTHER");
    private static final Set<String> ALLOWED_RECURRENCE_FREQUENCIES = Set.of("NONE", "MONTHLY", "YEARLY");
    private static final int DEFAULT_ADMIN_OBLIGATION_PAGE_SIZE = 10;
    private static final int MAX_ADMIN_OBLIGATION_PAGE_SIZE = 50;
    private static final DateTimeFormatter DATE_TIME_VALUE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_TIME_LABEL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREAN);

    public String normalizeTitle(String title) {
        return normalizeRequiredText(title, "재정 항목 이름은 필수입니다.");
    }

    public String normalizeRequestTitle(String title) {
        return normalizeRequiredText(title, "재정 요청 제목은 필수입니다.");
    }

    public String normalizeExpenseTitle(String title) {
        return normalizeRequiredText(title, "지출 제목은 필수입니다.");
    }

    public String normalizeRequestType(String requestTypeCode) {
        String upperCased = normalizeUpperCase(requestTypeCode, "재정 요청 타입은 필수입니다.");
        if (!ALLOWED_REQUEST_TYPES.contains(upperCased)) {
            throw new SemoException.ValidationException("지원하지 않는 재정 요청 타입입니다.");
        }
        return upperCased;
    }

    public String normalizeReviewStatus(String statusCode) {
        String upperCased = normalizeUpperCase(statusCode, "요청 검토 상태는 필수입니다.");
        if (!ALLOWED_REVIEW_REQUEST_STATUSES.contains(upperCased)) {
            throw new SemoException.ValidationException("지원하지 않는 요청 검토 상태입니다.");
        }
        return upperCased;
    }

    public String normalizeExpenseCategory(String categoryCode) {
        String normalized = trimToNull(categoryCode);
        if (normalized == null) {
            return "OTHER";
        }
        String upperCased = normalized.toUpperCase(Locale.ROOT);
        if (!KNOWN_EXPENSE_CATEGORIES.contains(upperCased)) {
            throw new SemoException.ValidationException("지원하지 않는 지출 카테고리입니다.");
        }
        return upperCased;
    }

    public String normalizeTargetScope(String targetScopeCode) {
        String normalized = trimToNull(targetScopeCode);
        if (normalized == null) {
            return "ALL_ACTIVE_MEMBERS";
        }
        String upperCased = normalized.toUpperCase(Locale.ROOT);
        if (!ALLOWED_TARGET_SCOPES.contains(upperCased)) {
            throw new SemoException.ValidationException("지원하지 않는 발행 대상 타입입니다.");
        }
        return upperCased;
    }

    public BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new SemoException.ValidationException("청구 금액은 0보다 커야 합니다.");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal normalizeNonNegativeAmount(BigDecimal amount, String message) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new SemoException.ValidationException(message);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public String normalizePeriodTitle(String title) {
        return normalizeRequiredText(title, "재정 기간 이름은 필수입니다.");
    }

    public String normalizeAccountName(String displayName) {
        return normalizeRequiredText(displayName, "계좌·결제수단 이름은 필수입니다.");
    }

    public String normalizeAccountType(String accountTypeCode) {
        String normalized = normalizeUpperCase(accountTypeCode, "계좌·결제수단 타입은 필수입니다.");
        if (!ALLOWED_ACCOUNT_TYPES.contains(normalized)) {
            throw new SemoException.ValidationException("지원하지 않는 계좌·결제수단 타입입니다.");
        }
        return normalized;
    }

    public String normalizeAccountUsageScope(String usageScopeCode) {
        String normalized = normalizeUpperCase(usageScopeCode, "사용 범위는 필수입니다.");
        if (!ALLOWED_ACCOUNT_USAGE_SCOPES.contains(normalized)) {
            throw new SemoException.ValidationException("지원하지 않는 계좌·결제수단 사용 범위입니다.");
        }
        return normalized;
    }

    public String normalizePaymentMethod(String paymentMethodCode) {
        String normalized = trimToNull(paymentMethodCode);
        if (normalized == null) {
            return null;
        }
        String upperCased = normalized.toUpperCase(Locale.ROOT);
        if (!ALLOWED_PAYMENT_METHODS.contains(upperCased)) {
            throw new SemoException.ValidationException("지원하지 않는 결제수단입니다.");
        }
        return upperCased;
    }

    public String normalizeRecurrenceFrequency(String recurrenceFrequency) {
        String normalized = trimToNull(recurrenceFrequency);
        if (normalized == null) {
            return "NONE";
        }
        String upperCased = normalized.toUpperCase(Locale.ROOT);
        if (!ALLOWED_RECURRENCE_FREQUENCIES.contains(upperCased)) {
            throw new SemoException.ValidationException("지원하지 않는 반복 회비 주기입니다.");
        }
        return upperCased;
    }

    public int normalizeRecurrenceInterval(Integer recurrenceInterval) {
        int normalized = recurrenceInterval == null ? 1 : recurrenceInterval;
        if (normalized < 1 || normalized > 12) {
            throw new SemoException.ValidationException("반복 간격은 1부터 12 사이여야 합니다.");
        }
        return normalized;
    }

    public String normalizeReason(String reason) {
        return normalizeRequiredText(reason, "정정 또는 취소 사유는 필수입니다.");
    }

    public LocalDate parseDate(String rawValue, String errorMessage) {
        String normalized = trimToNull(rawValue);
        if (normalized == null) {
            return null;
        }
        try {
            return LocalDate.parse(normalized);
        } catch (RuntimeException exception) {
            throw new SemoException.ValidationException(errorMessage);
        }
    }

    public String normalizeUpdateStatus(String paymentStatusCode) {
        String upperCased = normalizeUpperCase(paymentStatusCode, "재정 상태는 필수입니다.");
        if (!ALLOWED_UPDATE_STATUSES.contains(upperCased)) {
            throw new SemoException.ValidationException("지원하지 않는 재정 상태입니다.");
        }
        return upperCased;
    }

    public int normalizeAdminObligationPageSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_ADMIN_OBLIGATION_PAGE_SIZE;
        }
        return Math.min(size, MAX_ADMIN_OBLIGATION_PAGE_SIZE);
    }

    public String normalizeSearchQuery(String query) {
        String normalized = trimToNull(query);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    public String normalizeAdminObligationFilter(String obligationFilter) {
        String normalized = trimToNull(obligationFilter);
        if (normalized == null || "ALL".equalsIgnoreCase(normalized)) {
            return null;
        }
        String upperCased = normalized.toUpperCase(Locale.ROOT);
        if (!ALLOWED_ADMIN_OBLIGATION_FILTERS.contains(upperCased)) {
            throw new SemoException.ValidationException("지원하지 않는 재정 필터입니다.");
        }
        return upperCased;
    }

    public LocalDateTime parseDateTime(String rawValue, String errorMessage) {
        return parseDateTime(rawValue, errorMessage, null);
    }

    public LocalDateTime parseDateTime(String rawValue, String errorMessage, LocalDateTime defaultValue) {
        String normalized = trimToNull(rawValue);
        if (normalized == null) {
            return defaultValue;
        }
        try {
            return LocalDateTime.parse(normalized, DATE_TIME_VALUE_FORMATTER);
        } catch (RuntimeException exception) {
            throw new SemoException.ValidationException(errorMessage);
        }
    }

    public LocalDateTime parseNullableDateTime(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        return LocalDateTime.parse(rawValue, DATE_TIME_VALUE_FORMATTER);
    }

    public LocalDateTime resolvePaidActivityAt(ClubFinanceUserObligationResponse obligation) {
        LocalDateTime paidAt = parseNullableDateTime(obligation.payment().paidAt());
        if (paidAt != null) {
            return paidAt;
        }
        return parseNullableDateTime(obligation.issuedAt());
    }

    public String formatDateTimeValue(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_VALUE_FORMATTER);
    }

    public String formatDateTimeLabel(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_LABEL_FORMATTER);
    }

    public String formatAmount(BigDecimal amount, String currencyCode) {
        BigDecimal normalized = amount == null ? BigDecimal.ZERO : amount.stripTrailingZeros();
        String pattern = normalized.scale() > 0 ? "#,##0.##" : "#,##0";
        String formatted = new DecimalFormat(pattern).format(amount == null ? BigDecimal.ZERO : amount);
        if ("KRW".equalsIgnoreCase(currencyCode)) {
            return formatted + "원";
        }
        return (StringUtils.hasText(currencyCode) ? currencyCode.toUpperCase(Locale.ROOT) : "KRW")
                + " "
                + formatted;
    }

    public String resolveObligationTypeLabel(String obligationTypeCode) {
        return switch (obligationTypeCode) {
            case "FEE" -> "분담금";
            default -> "재정 항목";
        };
    }

    public String resolveRequestTypeLabel(String requestTypeCode) {
        return switch (requestTypeCode) {
            case "ADVANCE" -> "선지출 등록";
            case "REFUND_REQUEST" -> "환불 요청";
            case "SETTLEMENT_REQUEST" -> "정산 요청";
            default -> "재정 요청";
        };
    }

    public String resolveRequestStatusLabel(String statusCode) {
        return switch (statusCode) {
            case "APPROVED" -> "승인";
            case "REJECTED" -> "반려";
            default -> "제출 완료";
        };
    }

    public String resolveExpenseTypeLabel(String expenseTypeCode) {
        return switch (expenseTypeCode) {
            case "ADMIN_EXPENSE" -> "운영 지출";
            case "APPROVED_REQUEST" -> "승인 요청 연계";
            default -> "지출";
        };
    }

    public String resolveExpenseCategoryLabel(String categoryCode) {
        return switch (categoryCode) {
            case "MEMBERSHIP_FEE" -> "회비";
            case "EVENT_FEE" -> "행사비";
            case "MEAL" -> "식비";
            case "VENUE" -> "대관비";
            case "SUPPLIES" -> "물품비";
            case "TRANSPORT" -> "교통비";
            case "REFUND" -> "환불";
            default -> "기타";
        };
    }

    public String resolveTargetScopeLabel(String targetScopeCode) {
        return switch (targetScopeCode) {
            case "SELECTED_MEMBERS" -> "선택 멤버";
            default -> "활성 멤버 전체";
        };
    }

    public String resolvePaymentStatusLabel(String paymentStatusCode, boolean overdue) {
        if (overdue) {
            return "연체";
        }
        return switch (paymentStatusCode) {
            case STATUS_PAID -> "납부 완료";
            case STATUS_WAIVED -> "면제";
            default -> "미납";
        };
    }

    public String resolveAccountTypeLabel(String accountTypeCode) {
        return switch (accountTypeCode) {
            case "BANK" -> "은행 계좌";
            case "CASH" -> "현금";
            case "CARD" -> "카드";
            default -> "기타";
        };
    }

    public String resolveAccountUsageScopeLabel(String usageScopeCode) {
        return switch (usageScopeCode) {
            case "COLLECTION" -> "수납 전용";
            case "EXPENSE" -> "지출 전용";
            default -> "수납·지출 공용";
        };
    }

    public String resolvePaymentMethodLabel(String paymentMethodCode) {
        if (paymentMethodCode == null) {
            return null;
        }
        return switch (paymentMethodCode) {
            case "TRANSFER" -> "계좌이체";
            case "CASH" -> "현금";
            case "CARD" -> "카드";
            default -> "기타";
        };
    }

    public String resolveRecurrenceLabel(String frequency, int interval) {
        return switch (frequency) {
            case "MONTHLY" -> interval == 1 ? "매월" : interval + "개월마다";
            case "YEARLY" -> interval == 1 ? "매년" : interval + "년마다";
            default -> "반복 없음";
        };
    }

    public String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeRequiredText(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new SemoException.ValidationException(message);
        }
        return normalized;
    }

    private String normalizeUpperCase(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new SemoException.ValidationException(message);
        }
        return normalized.toUpperCase(Locale.ROOT);
    }
}
