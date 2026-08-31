package semo.back.service.feature.finance.biz.policy;

import org.springframework.stereotype.Component;
import semo.back.service.database.pub.entity.FinanceObligation;
import semo.back.service.database.pub.entity.FinancePayment;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ClubFinancePaymentPolicy {
    private static final String STATUS_PENDING = "PENDING";

    public boolean canDeleteObligation(List<FinancePayment> payments) {
        return payments.stream().allMatch(payment -> STATUS_PENDING.equals(payment.getPaymentStatusCode()));
    }

    public boolean isOverdue(FinancePayment payment, FinanceObligation obligation) {
        return STATUS_PENDING.equals(payment.getPaymentStatusCode())
                && obligation.getDueAt() != null
                && obligation.getDueAt().isBefore(LocalDateTime.now());
    }
}
