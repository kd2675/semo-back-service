package semo.back.service.feature.finance.biz;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.ClubScheduleEvent;
import semo.back.service.database.pub.entity.FinanceAccount;
import semo.back.service.database.pub.entity.FinanceExpense;
import semo.back.service.database.pub.entity.FinanceObligation;
import semo.back.service.database.pub.entity.FinancePayment;
import semo.back.service.database.pub.entity.FinancePeriod;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.FinanceAccountRepository;
import semo.back.service.database.pub.repository.FinanceExpenseRepository;
import semo.back.service.database.pub.repository.FinanceObligationRepository;
import semo.back.service.database.pub.repository.FinancePaymentRepository;
import semo.back.service.database.pub.repository.FinancePeriodRepository;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.finance.biz.policy.ClubFinancePermissionService;
import semo.back.service.feature.finance.biz.support.ClubFinanceSupport;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubFinanceExportService {
    private static final String[] CSV_HEADER = {
            "record_type",
            "record_id",
            "period",
            "occurred_at",
            "title_or_member",
            "category",
            "status",
            "amount",
            "currency",
            "account",
            "schedule_event",
            "note"
    };

    private final ClubAccessResolver clubAccessResolver;
    private final ClubFinancePermissionService clubFinancePermissionService;
    private final ClubFinanceSupport clubFinanceSupport;
    private final FinanceObligationRepository financeObligationRepository;
    private final FinancePaymentRepository financePaymentRepository;
    private final FinanceExpenseRepository financeExpenseRepository;
    private final FinancePeriodRepository financePeriodRepository;
    private final FinanceAccountRepository financeAccountRepository;
    private final ClubScheduleEventRepository clubScheduleEventRepository;
    private final ClubProfileRepository clubProfileRepository;

    public FinanceExportFile exportCsv(Long clubId, Long financePeriodId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        if (!clubFinancePermissionService.isFinanceEnabled(clubId)) {
            throw new SemoException.ForbiddenException("재정관리 기능이 활성화되지 않았습니다.");
        }
        if (!clubFinancePermissionService.canExport(access)) {
            throw new SemoException.ForbiddenException("재정 내역을 내보낼 권한이 없습니다.");
        }

        List<FinanceObligation> allObligations = financeObligationRepository
                .findByClubIdOrderByFinanceObligationIdAsc(clubId);
        Map<Long, FinanceObligation> obligationById = allObligations.stream()
                .collect(Collectors.toMap(FinanceObligation::getFinanceObligationId, Function.identity()));
        List<FinanceObligation> obligations = allObligations.stream()
                .filter(obligation -> financePeriodId == null
                        || financePeriodId.equals(obligation.getFinancePeriodId()))
                .toList();
        List<FinancePayment> payments = financePaymentRepository.findByClubIdOrderByFinancePaymentIdAsc(clubId).stream()
                .filter(payment -> financePeriodId == null
                        || financePeriodId.equals(resolvePeriodId(payment, obligationById)))
                .toList();
        List<FinanceExpense> expenses = financeExpenseRepository
                .findByClubIdOrderBySpentAtDescFinanceExpenseIdDesc(clubId).stream()
                .filter(expense -> financePeriodId == null
                        || financePeriodId.equals(expense.getFinancePeriodId()))
                .toList();

        ReferenceMaps references = resolveReferences(obligations, payments, expenses);
        StringBuilder csv = new StringBuilder("\uFEFF");
        appendRow(csv, (Object[]) CSV_HEADER);
        for (FinanceObligation obligation : obligations) {
            appendRow(
                    csv,
                    "OBLIGATION",
                    obligation.getFinanceObligationId(),
                    references.periodTitleById().get(obligation.getFinancePeriodId()),
                    clubFinanceSupport.formatDateTimeValue(obligation.getCreateDate()),
                    obligation.getTitle(),
                    obligation.getObligationTypeCode(),
                    obligation.getStatusCode(),
                    obligation.getAmount(),
                    obligation.getCurrencyCode(),
                    references.accountNameById().get(obligation.getFinanceAccountId()),
                    references.eventTitleById().get(obligation.getLinkedScheduleEventId()),
                    obligation.getNote()
            );
        }
        for (FinancePayment payment : payments) {
            FinanceObligation obligation = obligationById.get(payment.getFinanceObligationId());
            appendRow(
                    csv,
                    "PAYMENT",
                    payment.getFinancePaymentId(),
                    obligation == null
                            ? null
                            : references.periodTitleById().get(obligation.getFinancePeriodId()),
                    clubFinanceSupport.formatDateTimeValue(payment.getPaidAt()),
                    references.profileNameById().get(payment.getClubProfileId()),
                    payment.getPaymentMethodCode(),
                    payment.getPaymentStatusCode(),
                    payment.getAmount(),
                    payment.getCurrencyCode(),
                    references.accountNameById().get(payment.getFinanceAccountId()),
                    obligation == null
                            ? null
                            : references.eventTitleById().get(obligation.getLinkedScheduleEventId()),
                    payment.getNote()
            );
        }
        for (FinanceExpense expense : expenses) {
            appendRow(
                    csv,
                    "EXPENSE",
                    expense.getFinanceExpenseId(),
                    references.periodTitleById().get(expense.getFinancePeriodId()),
                    clubFinanceSupport.formatDateTimeValue(expense.getSpentAt()),
                    expense.getTitle(),
                    expense.getCategoryCode(),
                    expense.getStatusCode(),
                    expense.getAmount(),
                    expense.getCurrencyCode(),
                    references.accountNameById().get(expense.getFinanceAccountId()),
                    references.eventTitleById().get(expense.getLinkedScheduleEventId()),
                    "VOIDED".equals(expense.getStatusCode()) ? expense.getVoidReason() : expense.getNote()
            );
        }

        String periodSuffix = financePeriodId == null ? "all" : "period-" + financePeriodId;
        return new FinanceExportFile(
                "semo-finance-" + clubId + "-" + periodSuffix + "-" + LocalDate.now() + ".csv",
                csv.toString().getBytes(StandardCharsets.UTF_8)
        );
    }

    private Long resolvePeriodId(FinancePayment payment, Map<Long, FinanceObligation> obligationById) {
        FinanceObligation obligation = obligationById.get(payment.getFinanceObligationId());
        return obligation == null ? null : obligation.getFinancePeriodId();
    }

    private ReferenceMaps resolveReferences(
            List<FinanceObligation> obligations,
            List<FinancePayment> payments,
            List<FinanceExpense> expenses
    ) {
        List<Long> periodIds = java.util.stream.Stream.concat(
                        obligations.stream().map(FinanceObligation::getFinancePeriodId),
                        expenses.stream().map(FinanceExpense::getFinancePeriodId)
                ).filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> accountIds = java.util.stream.Stream.of(
                        obligations.stream().map(FinanceObligation::getFinanceAccountId),
                        payments.stream().map(FinancePayment::getFinanceAccountId),
                        expenses.stream().map(FinanceExpense::getFinanceAccountId)
                ).flatMap(Function.identity())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> eventIds = java.util.stream.Stream.concat(
                        obligations.stream().map(FinanceObligation::getLinkedScheduleEventId),
                        expenses.stream().map(FinanceExpense::getLinkedScheduleEventId)
                ).filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> profileIds = payments.stream()
                .map(FinancePayment::getClubProfileId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return new ReferenceMaps(
                financePeriodRepository.findAllById(periodIds).stream()
                        .collect(Collectors.toMap(FinancePeriod::getFinancePeriodId, FinancePeriod::getTitle)),
                financeAccountRepository.findAllById(accountIds).stream()
                        .collect(Collectors.toMap(FinanceAccount::getFinanceAccountId, FinanceAccount::getDisplayName)),
                clubScheduleEventRepository.findAllById(eventIds).stream()
                        .collect(Collectors.toMap(ClubScheduleEvent::getEventId, ClubScheduleEvent::getTitle)),
                clubProfileRepository.findAllById(profileIds).stream()
                        .collect(Collectors.toMap(ClubProfile::getClubProfileId, ClubProfile::getDisplayName))
        );
    }

    private void appendRow(StringBuilder csv, Object... values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                csv.append(',');
            }
            csv.append(toCsvCell(values[index]));
        }
        csv.append("\r\n");
    }

    private String toCsvCell(Object value) {
        String rawValue = value == null ? "" : value instanceof BigDecimal amount
                ? amount.toPlainString()
                : String.valueOf(value);
        String protectedValue = startsWithFormulaPrefix(rawValue) ? "'" + rawValue : rawValue;
        return '"' + protectedValue.replace("\"", "\"\"") + '"';
    }

    private boolean startsWithFormulaPrefix(String value) {
        if (value.isEmpty()) {
            return false;
        }
        char first = value.charAt(0);
        return first == '=' || first == '+' || first == '-' || first == '@';
    }

    public record FinanceExportFile(String fileName, byte[] content) {
    }

    private record ReferenceMaps(
            Map<Long, String> periodTitleById,
            Map<Long, String> accountNameById,
            Map<Long, String> eventTitleById,
            Map<Long, String> profileNameById
    ) {
    }
}
