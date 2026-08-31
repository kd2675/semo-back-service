package semo.back.service.feature.finance.biz.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.database.pub.entity.FinanceAccount;
import semo.back.service.database.pub.entity.FinanceObligation;
import semo.back.service.database.pub.entity.FinancePayment;
import semo.back.service.database.pub.entity.FinancePeriod;
import semo.back.service.database.pub.repository.FinanceAccountRepository;
import semo.back.service.database.pub.repository.FinanceObligationRepository;
import semo.back.service.database.pub.repository.FinancePaymentRepository;
import semo.back.service.feature.finance.biz.ClubFinanceOperationsService;
import semo.back.service.feature.notification.biz.ClubNotificationPublisher;
import semo.back.service.feature.notification.biz.ClubNotificationPublisher.NotificationCommand;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClubFinanceRecurrenceService {
    private static final String STATUS_PENDING = "PENDING";
    private static final String OBLIGATION_STATUS_OPEN = "OPEN";

    private final FinanceObligationRepository financeObligationRepository;
    private final FinancePaymentRepository financePaymentRepository;
    private final FinanceAccountRepository financeAccountRepository;
    private final ClubFinanceOperationsService clubFinanceOperationsService;
    private final ClubFinanceSupport clubFinanceSupport;
    private final ClubNotificationPublisher clubNotificationPublisher;

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.MANDATORY)
    public void createNextObligation(FinanceObligation source, List<FinancePayment> sourcePayments) {
        if ("NONE".equals(source.getRecurrenceFrequency()) || source.getDueAt() == null) {
            return;
        }
        if (financeObligationRepository
                .findByRecurrenceSourceFinanceObligationId(source.getFinanceObligationId())
                .isPresent()) {
            return;
        }
        LocalDateTime nextDueAt = switch (source.getRecurrenceFrequency()) {
            case "MONTHLY" -> source.getDueAt().plusMonths(source.getRecurrenceInterval());
            case "YEARLY" -> source.getDueAt().plusYears(source.getRecurrenceInterval());
            default -> null;
        };
        if (nextDueAt == null
                || (source.getRecurrenceEndDate() != null
                && nextDueAt.toLocalDate().isAfter(source.getRecurrenceEndDate()))) {
            return;
        }
        FinancePeriod nextPeriod = clubFinanceOperationsService.resolveWritablePeriod(
                source.getClubId(),
                null,
                nextDueAt.toLocalDate()
        );
        FinanceAccount nextAccount = resolveCollectionAccount(source);
        FinanceObligation next = financeObligationRepository.save(FinanceObligation.builder()
                .clubId(source.getClubId())
                .createdByClubProfileId(source.getCreatedByClubProfileId())
                .financePeriodId(nextPeriod == null ? null : nextPeriod.getFinancePeriodId())
                .financeAccountId(nextAccount == null ? null : nextAccount.getFinanceAccountId())
                .linkedScheduleEventId(null)
                .obligationTypeCode(source.getObligationTypeCode())
                .title(source.getTitle())
                .targetScopeCode(source.getTargetScopeCode())
                .amount(source.getAmount())
                .currencyCode(source.getCurrencyCode())
                .dueAt(nextDueAt)
                .statusCode(OBLIGATION_STATUS_OPEN)
                .note(source.getNote())
                .recurrenceFrequency(source.getRecurrenceFrequency())
                .recurrenceInterval(source.getRecurrenceInterval())
                .recurrenceEndDate(source.getRecurrenceEndDate())
                .recurrenceSourceFinanceObligationId(source.getFinanceObligationId())
                .build());
        List<FinancePayment> nextPayments = sourcePayments.stream()
                .map(sourcePayment -> FinancePayment.builder()
                        .financeObligationId(next.getFinanceObligationId())
                        .clubId(next.getClubId())
                        .clubProfileId(sourcePayment.getClubProfileId())
                        .financeAccountId(next.getFinanceAccountId())
                        .amount(next.getAmount())
                        .currencyCode(next.getCurrencyCode())
                        .paymentStatusCode(STATUS_PENDING)
                        .paidAt(null)
                        .paymentMethodCode(null)
                        .note(next.getNote())
                        .build())
                .toList();
        financePaymentRepository.saveAll(nextPayments);
        notifyObligationCreated(next, nextPayments);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.MANDATORY)
    public void notifyObligationCreated(FinanceObligation obligation, List<FinancePayment> payments) {
        for (FinancePayment payment : payments) {
            clubNotificationPublisher.notifyClubProfile(
                    payment.getClubProfileId(),
                    new NotificationCommand(
                            obligation.getClubId(),
                            "FINANCE_OBLIGATION_CREATED",
                            "새 회비·분담금이 발행되었습니다",
                            "'" + obligation.getTitle() + "' "
                                    + clubFinanceSupport.formatAmount(
                                            obligation.getAmount(),
                                            obligation.getCurrencyCode()
                                    )
                                    + " · 마감 " + clubFinanceSupport.formatDateTimeLabel(obligation.getDueAt()),
                            "FINANCE_OBLIGATION",
                            obligation.getFinanceObligationId(),
                            "/clubs/" + obligation.getClubId() + "/more/finance",
                            "finance-obligation:" + obligation.getFinanceObligationId()
                                    + ":" + payment.getClubProfileId()
                    )
            );
        }
    }

    private FinanceAccount resolveCollectionAccount(FinanceObligation source) {
        if (source.getFinanceAccountId() != null) {
            FinanceAccount sourceAccount = financeAccountRepository
                    .findByFinanceAccountIdAndClubId(source.getFinanceAccountId(), source.getClubId())
                    .orElse(null);
            if (sourceAccount != null
                    && sourceAccount.isActive()
                    && ("COLLECTION".equals(sourceAccount.getUsageScopeCode())
                    || "BOTH".equals(sourceAccount.getUsageScopeCode()))) {
                return sourceAccount;
            }
        }
        return clubFinanceOperationsService.resolveActiveAccount(source.getClubId(), null, "COLLECTION");
    }
}
