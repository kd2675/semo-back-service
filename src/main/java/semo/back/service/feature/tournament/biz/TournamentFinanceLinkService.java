package semo.back.service.feature.tournament.biz;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.database.pub.entity.FinanceAccount;
import semo.back.service.database.pub.entity.FinanceObligation;
import semo.back.service.database.pub.entity.FinancePayment;
import semo.back.service.database.pub.entity.FinancePeriod;
import semo.back.service.database.pub.entity.TournamentApplication;
import semo.back.service.database.pub.entity.TournamentRecord;
import semo.back.service.database.pub.repository.FinanceAccountRepository;
import semo.back.service.database.pub.repository.FinanceObligationRepository;
import semo.back.service.database.pub.repository.FinancePaymentRepository;
import semo.back.service.database.pub.repository.FinancePeriodRepository;
import semo.back.service.feature.finance.biz.policy.ClubFinancePermissionService;
import semo.back.service.feature.notification.biz.ClubNotificationPublisher;
import semo.back.service.feature.notification.biz.ClubNotificationPublisher.NotificationCommand;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TournamentFinanceLinkService {
    private final ClubFinancePermissionService clubFinancePermissionService;
    private final FinanceObligationRepository financeObligationRepository;
    private final FinancePaymentRepository financePaymentRepository;
    private final FinancePeriodRepository financePeriodRepository;
    private final FinanceAccountRepository financeAccountRepository;
    private final ClubNotificationPublisher clubNotificationPublisher;

    @Transactional(transactionManager = "pubTransactionManager")
    public Long ensureTournamentFeePayment(
            TournamentRecord tournament,
            TournamentApplication application,
            Long actorClubProfileId
    ) {
        if (!tournament.isFeeRequired()
                || tournament.getFeeAmount() == null
                || tournament.getFeeAmount() <= 0
                || !clubFinancePermissionService.isFinanceEnabled(tournament.getClubId())) {
            return null;
        }
        if (application.getFinancePaymentId() != null) {
            FinancePayment linkedPayment = financePaymentRepository.findById(application.getFinancePaymentId()).orElse(null);
            if (linkedPayment != null) {
                reopenWaivedPayment(linkedPayment);
                return linkedPayment.getFinancePaymentId();
            }
        }
        FinanceObligation existing = financeObligationRepository
                .findBySourceTournamentApplicationId(application.getTournamentApplicationId())
                .orElse(null);
        if (existing != null) {
            FinancePayment existingPayment = financePaymentRepository
                    .findByFinanceObligationIdOrderByFinancePaymentIdDesc(existing.getFinanceObligationId())
                    .stream()
                    .filter(payment -> payment.getClubProfileId().equals(application.getClubProfileId()))
                    .findFirst()
                    .orElse(null);
            if (existingPayment == null) {
                return null;
            }
            reopenWaivedPayment(existingPayment);
            return existingPayment.getFinancePaymentId();
        }

        LocalDateTime dueAt = tournament.getApplicationEndAt().isAfter(LocalDateTime.now())
                ? tournament.getApplicationEndAt()
                : tournament.getStartDate().atTime(23, 59);
        FinancePeriod period = financePeriodRepository.findContainingDate(tournament.getClubId(), dueAt.toLocalDate())
                .stream()
                .filter(candidate -> "OPEN".equals(candidate.getStatusCode()))
                .findFirst()
                .orElse(null);
        FinanceAccount account = financeAccountRepository.findByClubIdAndActiveTrue(tournament.getClubId())
                .stream()
                .filter(candidate -> candidate.isDefaultCollection())
                .filter(candidate -> !"EXPENSE".equals(candidate.getUsageScopeCode()))
                .findFirst()
                .orElse(null);
        BigDecimal amount = BigDecimal.valueOf(tournament.getFeeAmount());
        FinanceObligation obligation = financeObligationRepository.save(FinanceObligation.builder()
                .clubId(tournament.getClubId())
                .createdByClubProfileId(actorClubProfileId)
                .financePeriodId(period == null ? null : period.getFinancePeriodId())
                .financeAccountId(account == null ? null : account.getFinanceAccountId())
                .linkedScheduleEventId(null)
                .obligationTypeCode("TOURNAMENT_FEE")
                .title(tournament.getTitle() + " 참가비")
                .targetScopeCode("SELECTED_MEMBERS")
                .amount(amount)
                .currencyCode(tournament.getFeeCurrencyCode())
                .dueAt(dueAt)
                .statusCode("OPEN")
                .note("대회 참가 승인과 연결된 참가비입니다.")
                .recurrenceFrequency("NONE")
                .recurrenceInterval(1)
                .recurrenceEndDate(null)
                .recurrenceSourceFinanceObligationId(null)
                .sourceTournamentApplicationId(application.getTournamentApplicationId())
                .build());
        FinancePayment payment = financePaymentRepository.save(FinancePayment.builder()
                .financeObligationId(obligation.getFinanceObligationId())
                .clubId(tournament.getClubId())
                .clubProfileId(application.getClubProfileId())
                .financeAccountId(account == null ? null : account.getFinanceAccountId())
                .amount(amount)
                .currencyCode(tournament.getFeeCurrencyCode())
                .paymentStatusCode("PENDING")
                .paidAt(null)
                .paymentMethodCode(null)
                .note(obligation.getNote())
                .build());
        clubNotificationPublisher.notifyClubProfile(
                application.getClubProfileId(),
                new NotificationCommand(
                        tournament.getClubId(),
                        "FINANCE_OBLIGATION_CREATED",
                        "대회 참가비가 발행되었습니다",
                        "'" + tournament.getTitle() + "' 참가비 " + tournament.getFeeAmount() + tournament.getFeeCurrencyCode(),
                        "FINANCE_OBLIGATION",
                        obligation.getFinanceObligationId(),
                        "/clubs/" + tournament.getClubId() + "/more/finance",
                        "tournament-fee:" + application.getTournamentApplicationId()
                )
        );
        return payment.getFinancePaymentId();
    }

    @Transactional(transactionManager = "pubTransactionManager")
    public void waivePendingTournamentFee(TournamentApplication application) {
        if (application.getFinancePaymentId() == null) {
            return;
        }
        FinancePayment payment = financePaymentRepository.findById(application.getFinancePaymentId()).orElse(null);
        if (payment == null || !"PENDING".equals(payment.getPaymentStatusCode())) {
            return;
        }
        payment.updateStatus("WAIVED", null, null, null, "대회 참가 취소 또는 반려");
        financePaymentRepository.save(payment);
        financeObligationRepository.findById(payment.getFinanceObligationId()).ifPresent(obligation -> {
            obligation.updateStatus("CLOSED");
            financeObligationRepository.save(obligation);
        });
    }

    public FinancePayment findPayment(Long financePaymentId) {
        return financePaymentId == null ? null : financePaymentRepository.findById(financePaymentId).orElse(null);
    }

    private void reopenWaivedPayment(FinancePayment payment) {
        if (!"WAIVED".equals(payment.getPaymentStatusCode())) {
            return;
        }
        payment.updateStatus("PENDING", null, payment.getFinanceAccountId(), null, "대회 참가 재승인");
        financePaymentRepository.save(payment);
        financeObligationRepository.findById(payment.getFinanceObligationId()).ifPresent(obligation -> {
            obligation.updateStatus("OPEN");
            financeObligationRepository.save(obligation);
        });
    }
}
