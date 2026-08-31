package semo.back.service.feature.finance.biz.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.ClubScheduleEvent;
import semo.back.service.database.pub.entity.FinanceAccount;
import semo.back.service.database.pub.entity.FinanceExpense;
import semo.back.service.database.pub.entity.FinanceExpenseRevision;
import semo.back.service.database.pub.entity.FinanceObligation;
import semo.back.service.database.pub.entity.FinancePayment;
import semo.back.service.database.pub.entity.FinancePeriod;
import semo.back.service.database.pub.entity.FinanceRequest;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.FinanceAccountRepository;
import semo.back.service.database.pub.repository.FinanceObligationRepository;
import semo.back.service.database.pub.repository.FinancePaymentRepository;
import semo.back.service.database.pub.repository.FinancePeriodRepository;
import semo.back.service.feature.finance.biz.policy.ClubFinancePaymentPolicy;
import semo.back.service.feature.finance.vo.ClubAdminFinanceObligationResponse;
import semo.back.service.feature.finance.vo.ClubFinanceExpenseResponse;
import semo.back.service.feature.finance.vo.ClubFinancePaymentResponse;
import semo.back.service.feature.finance.vo.ClubFinanceRequestResponse;
import semo.back.service.feature.finance.vo.ClubFinanceUserObligationResponse;
import semo.back.service.feature.finance.vo.FinanceExpenseRevisionResponse;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class ClubFinanceResponseAssembler {
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_WAIVED = "WAIVED";
    private static final String STATUS_OVERDUE = "OVERDUE";

    private final FinanceObligationRepository financeObligationRepository;
    private final FinancePaymentRepository financePaymentRepository;
    private final FinanceAccountRepository financeAccountRepository;
    private final FinancePeriodRepository financePeriodRepository;
    private final ClubScheduleEventRepository clubScheduleEventRepository;
    private final ClubProfileRepository clubProfileRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubFinanceSupport clubFinanceSupport;
    private final ClubFinancePaymentPolicy clubFinancePaymentPolicy;

    public List<ClubFinanceUserObligationResponse> toUserObligationResponses(
            List<FinancePayment> payments,
            String memberDisplayName,
            String memberRoleCode
    ) {
        Map<Long, FinanceObligation> obligationById = loadObligations(
                payments.stream().map(FinancePayment::getFinanceObligationId).toList()
        );
        FinanceReferenceLabels referenceLabels = resolveReferenceLabels(
                obligationById.values().stream().map(FinanceObligation::getFinancePeriodId).toList(),
                Stream.concat(
                        obligationById.values().stream().map(FinanceObligation::getFinanceAccountId),
                        payments.stream().map(FinancePayment::getFinanceAccountId)
                ).toList(),
                obligationById.values().stream().map(FinanceObligation::getLinkedScheduleEventId).toList()
        );
        return payments.stream()
                .map(payment -> toUserObligationResponse(
                        obligationById.get(payment.getFinanceObligationId()),
                        payment,
                        memberDisplayName,
                        memberRoleCode,
                        referenceLabels
                ))
                .filter(Objects::nonNull)
                .toList();
    }

    public List<ClubAdminFinanceObligationResponse> toAdminObligationResponses(
            List<FinanceObligation> obligations
    ) {
        if (obligations.isEmpty()) {
            return List.of();
        }
        Map<Long, List<FinancePayment>> paymentsByObligationId = financePaymentRepository.findByFinanceObligationIdIn(
                        obligations.stream().map(FinanceObligation::getFinanceObligationId).toList()
                ).stream()
                .collect(Collectors.groupingBy(
                        FinancePayment::getFinanceObligationId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        Map<Long, String> issuerNameByClubProfileId = resolveClubProfileDisplayNameById(
                obligations.stream()
                        .map(FinanceObligation::getCreatedByClubProfileId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList()
        );
        FinanceReferenceLabels referenceLabels = resolveReferenceLabels(
                obligations.stream().map(FinanceObligation::getFinancePeriodId).toList(),
                obligations.stream().map(FinanceObligation::getFinanceAccountId).toList(),
                obligations.stream().map(FinanceObligation::getLinkedScheduleEventId).toList()
        );
        return obligations.stream()
                .map(obligation -> toAdminObligationResponse(
                        obligation,
                        paymentsByObligationId.getOrDefault(obligation.getFinanceObligationId(), List.of()),
                        issuerNameByClubProfileId,
                        referenceLabels
                ))
                .toList();
    }

    public ClubAdminFinanceObligationResponse toAdminObligationResponse(
            FinanceObligation obligation,
            List<FinancePayment> payments
    ) {
        Map<Long, String> issuerNames = resolveClubProfileDisplayNameById(
                java.util.Collections.singletonList(obligation.getCreatedByClubProfileId())
        );
        FinanceReferenceLabels referenceLabels = resolveReferenceLabels(
                java.util.Collections.singletonList(obligation.getFinancePeriodId()),
                java.util.Collections.singletonList(obligation.getFinanceAccountId()),
                java.util.Collections.singletonList(obligation.getLinkedScheduleEventId())
        );
        return toAdminObligationResponse(obligation, payments, issuerNames, referenceLabels);
    }

    public List<ClubFinancePaymentResponse> toPaymentResponses(
            FinanceObligation obligation,
            List<FinancePayment> payments
    ) {
        Map<Long, PaymentMemberSummary> memberSummaries = resolvePaymentMemberSummaryByClubProfileId(payments);
        FinanceReferenceLabels referenceLabels = resolveReferenceLabels(
                java.util.Collections.singletonList(obligation.getFinancePeriodId()),
                Stream.concat(
                        Stream.of(obligation.getFinanceAccountId()),
                        payments.stream().map(FinancePayment::getFinanceAccountId)
                ).toList(),
                java.util.Collections.singletonList(obligation.getLinkedScheduleEventId())
        );
        return payments.stream()
                .map(payment -> toPaymentResponse(
                        payment,
                        obligation,
                        memberSummaries.get(payment.getClubProfileId()),
                        referenceLabels
                ))
                .sorted(Comparator
                        .comparing((ClubFinancePaymentResponse payment) -> payment.overdue() ? 0 : 1)
                        .thenComparing(ClubFinancePaymentResponse::paymentStatusCode)
                        .thenComparing(
                                ClubFinancePaymentResponse::memberDisplayName,
                                Comparator.nullsLast(String::compareTo)
                        ))
                .toList();
    }

    public ClubFinancePaymentResponse toPaymentResponse(
            FinancePayment payment,
            FinanceObligation obligation
    ) {
        PaymentMemberSummary memberSummary = resolvePaymentMemberSummaryByClubProfileId(List.of(payment))
                .get(payment.getClubProfileId());
        FinanceReferenceLabels referenceLabels = resolveReferenceLabels(
                List.of(),
                java.util.Collections.singletonList(payment.getFinanceAccountId()),
                List.of()
        );
        return toPaymentResponse(payment, obligation, memberSummary, referenceLabels);
    }

    public List<ClubFinanceRequestResponse> toFinanceRequestResponses(List<FinanceRequest> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }
        Map<Long, String> displayNameByClubProfileId = resolveClubProfileDisplayNameById(
                requests.stream()
                        .flatMap(request -> Stream.of(
                                request.getRequesterClubProfileId(),
                                request.getReviewedByClubProfileId()
                        ))
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList()
        );
        FinanceReferenceLabels referenceLabels = resolveReferenceLabels(
                List.of(),
                List.of(),
                requests.stream().map(FinanceRequest::getLinkedScheduleEventId).toList()
        );
        return requests.stream()
                .map(request -> toFinanceRequestResponse(request, displayNameByClubProfileId, referenceLabels))
                .toList();
    }

    public ClubFinanceRequestResponse toFinanceRequestResponse(FinanceRequest request) {
        Map<Long, String> displayNames = resolveClubProfileDisplayNameById(
                Stream.of(request.getRequesterClubProfileId(), request.getReviewedByClubProfileId())
                        .filter(Objects::nonNull)
                        .toList()
        );
        FinanceReferenceLabels referenceLabels = resolveReferenceLabels(
                List.of(),
                List.of(),
                java.util.Collections.singletonList(request.getLinkedScheduleEventId())
        );
        return toFinanceRequestResponse(request, displayNames, referenceLabels);
    }

    public List<ClubFinanceExpenseResponse> toFinanceExpenseResponses(List<FinanceExpense> expenses) {
        if (expenses.isEmpty()) {
            return List.of();
        }
        Map<Long, String> displayNameByClubProfileId = resolveClubProfileDisplayNameById(
                expenses.stream().map(FinanceExpense::getEnteredByClubProfileId).distinct().toList()
        );
        FinanceReferenceLabels referenceLabels = resolveReferenceLabels(
                expenses.stream().map(FinanceExpense::getFinancePeriodId).toList(),
                expenses.stream().map(FinanceExpense::getFinanceAccountId).toList(),
                expenses.stream().map(FinanceExpense::getLinkedScheduleEventId).toList()
        );
        return expenses.stream()
                .map(expense -> toFinanceExpenseResponse(expense, displayNameByClubProfileId, referenceLabels))
                .toList();
    }

    public ClubFinanceExpenseResponse toFinanceExpenseResponse(FinanceExpense expense) {
        Map<Long, String> displayNames = resolveClubProfileDisplayNameById(
                java.util.Collections.singletonList(expense.getEnteredByClubProfileId())
        );
        FinanceReferenceLabels referenceLabels = resolveReferenceLabels(
                java.util.Collections.singletonList(expense.getFinancePeriodId()),
                java.util.Collections.singletonList(expense.getFinanceAccountId()),
                java.util.Collections.singletonList(expense.getLinkedScheduleEventId())
        );
        return toFinanceExpenseResponse(expense, displayNames, referenceLabels);
    }

    public List<FinanceExpenseRevisionResponse> toExpenseRevisionResponses(
            List<FinanceExpenseRevision> revisions
    ) {
        Map<Long, String> displayNames = resolveClubProfileDisplayNameById(
                revisions.stream().map(FinanceExpenseRevision::getRevisedByClubProfileId).distinct().toList()
        );
        return revisions.stream()
                .map(revision -> toExpenseRevisionResponse(revision, displayNames))
                .toList();
    }

    private Map<Long, FinanceObligation> loadObligations(Collection<Long> obligationIds) {
        if (obligationIds == null || obligationIds.isEmpty()) {
            return Map.of();
        }
        return financeObligationRepository.findAllById(obligationIds).stream()
                .collect(Collectors.toMap(FinanceObligation::getFinanceObligationId, Function.identity()));
    }

    private ClubFinanceRequestResponse toFinanceRequestResponse(
            FinanceRequest request,
            Map<Long, String> displayNameByClubProfileId,
            FinanceReferenceLabels referenceLabels
    ) {
        return new ClubFinanceRequestResponse(
                request.getFinanceRequestId(),
                request.getRequestTypeCode(),
                clubFinanceSupport.resolveRequestTypeLabel(request.getRequestTypeCode()),
                displayNameByClubProfileId.getOrDefault(request.getRequesterClubProfileId(), "알 수 없는 멤버"),
                request.getAmount(),
                clubFinanceSupport.formatAmount(request.getAmount(), request.getCurrencyCode()),
                request.getCurrencyCode(),
                request.getTitle(),
                request.getLinkedScheduleEventId(),
                referenceLabels.eventTitleById().get(request.getLinkedScheduleEventId()),
                request.getRelatedEventName(),
                request.getNote(),
                request.getStatusCode(),
                clubFinanceSupport.resolveRequestStatusLabel(request.getStatusCode()),
                clubFinanceSupport.formatDateTimeValue(request.getCreateDate()),
                clubFinanceSupport.formatDateTimeLabel(request.getCreateDate()),
                clubFinanceSupport.formatDateTimeValue(request.getReviewedAt()),
                clubFinanceSupport.formatDateTimeLabel(request.getReviewedAt()),
                request.getReviewNote()
        );
    }

    private ClubFinanceExpenseResponse toFinanceExpenseResponse(
            FinanceExpense expense,
            Map<Long, String> displayNameByClubProfileId,
            FinanceReferenceLabels referenceLabels
    ) {
        return new ClubFinanceExpenseResponse(
                expense.getFinanceExpenseId(),
                expense.getSourceFinanceRequestId(),
                expense.getFinancePeriodId(),
                referenceLabels.periodTitleById().get(expense.getFinancePeriodId()),
                expense.getFinanceAccountId(),
                referenceLabels.accountNameById().get(expense.getFinanceAccountId()),
                expense.getLinkedScheduleEventId(),
                referenceLabels.eventTitleById().get(expense.getLinkedScheduleEventId()),
                expense.getExpenseTypeCode(),
                clubFinanceSupport.resolveExpenseTypeLabel(expense.getExpenseTypeCode()),
                expense.getCategoryCode(),
                clubFinanceSupport.resolveExpenseCategoryLabel(expense.getCategoryCode()),
                displayNameByClubProfileId.getOrDefault(expense.getEnteredByClubProfileId(), "알 수 없는 운영자"),
                expense.getAmount(),
                clubFinanceSupport.formatAmount(expense.getAmount(), expense.getCurrencyCode()),
                expense.getCurrencyCode(),
                expense.getTitle(),
                expense.getRelatedEventName(),
                expense.getNote(),
                clubFinanceSupport.formatDateTimeValue(expense.getSpentAt()),
                clubFinanceSupport.formatDateTimeLabel(expense.getSpentAt()),
                expense.getStatusCode(),
                expense.getVoidReason()
        );
    }

    private ClubAdminFinanceObligationResponse toAdminObligationResponse(
            FinanceObligation obligation,
            List<FinancePayment> payments,
            Map<Long, String> issuerNameByClubProfileId,
            FinanceReferenceLabels referenceLabels
    ) {
        ObligationPaymentMetrics metrics = summarizeObligationPayments(obligation, payments);

        return new ClubAdminFinanceObligationResponse(
                obligation.getFinanceObligationId(),
                obligation.getObligationTypeCode(),
                clubFinanceSupport.resolveObligationTypeLabel(obligation.getObligationTypeCode()),
                obligation.getTitle(),
                obligation.getTargetScopeCode(),
                clubFinanceSupport.resolveTargetScopeLabel(obligation.getTargetScopeCode()),
                obligation.getAmount(),
                clubFinanceSupport.formatAmount(obligation.getAmount(), obligation.getCurrencyCode()),
                obligation.getCurrencyCode(),
                clubFinanceSupport.formatDateTimeValue(obligation.getDueAt()),
                clubFinanceSupport.formatDateTimeLabel(obligation.getDueAt()),
                clubFinanceSupport.formatDateTimeValue(obligation.getCreateDate()),
                clubFinanceSupport.formatDateTimeLabel(obligation.getCreateDate()),
                obligation.getCreatedByClubProfileId() == null
                        ? "알 수 없는 운영자"
                        : issuerNameByClubProfileId.getOrDefault(
                                obligation.getCreatedByClubProfileId(),
                                "알 수 없는 운영자"
                        ),
                obligation.getNote(),
                obligation.getFinancePeriodId(),
                referenceLabels.periodTitleById().get(obligation.getFinancePeriodId()),
                obligation.getFinanceAccountId(),
                referenceLabels.accountNameById().get(obligation.getFinanceAccountId()),
                obligation.getLinkedScheduleEventId(),
                referenceLabels.eventTitleById().get(obligation.getLinkedScheduleEventId()),
                obligation.getRecurrenceFrequency(),
                obligation.getRecurrenceInterval(),
                obligation.getRecurrenceEndDate() == null ? null : obligation.getRecurrenceEndDate().toString(),
                clubFinanceSupport.resolveRecurrenceLabel(
                        obligation.getRecurrenceFrequency(),
                        obligation.getRecurrenceInterval()
                ),
                metrics.canDelete(),
                metrics.totalPaymentCount(),
                metrics.pendingPaymentCount(),
                metrics.paidPaymentCount(),
                metrics.waivedPaymentCount(),
                metrics.overduePaymentCount(),
                metrics.collectionRate()
        );
    }

    private ObligationPaymentMetrics summarizeObligationPayments(
            FinanceObligation obligation,
            List<FinancePayment> payments
    ) {
        int totalPaymentCount = payments.size();
        int pendingPaymentCount = (int) payments.stream()
                .filter(payment -> STATUS_PENDING.equals(payment.getPaymentStatusCode()))
                .count();
        int paidPaymentCount = (int) payments.stream()
                .filter(payment -> STATUS_PAID.equals(payment.getPaymentStatusCode()))
                .count();
        int waivedPaymentCount = (int) payments.stream()
                .filter(payment -> STATUS_WAIVED.equals(payment.getPaymentStatusCode()))
                .count();
        int overduePaymentCount = (int) payments.stream()
                .filter(payment -> clubFinancePaymentPolicy.isOverdue(payment, obligation))
                .count();
        int collectiblePaymentCount = totalPaymentCount - waivedPaymentCount;
        return new ObligationPaymentMetrics(
                totalPaymentCount,
                pendingPaymentCount,
                paidPaymentCount,
                waivedPaymentCount,
                overduePaymentCount,
                collectiblePaymentCount == 0
                        ? 0
                        : (int) Math.round((paidPaymentCount * 100.0) / collectiblePaymentCount),
                clubFinancePaymentPolicy.canDeleteObligation(payments)
        );
    }

    private ClubFinanceUserObligationResponse toUserObligationResponse(
            FinanceObligation obligation,
            FinancePayment payment,
            String memberDisplayName,
            String memberRoleCode,
            FinanceReferenceLabels referenceLabels
    ) {
        if (obligation == null) {
            return null;
        }
        return new ClubFinanceUserObligationResponse(
                obligation.getFinanceObligationId(),
                obligation.getObligationTypeCode(),
                clubFinanceSupport.resolveObligationTypeLabel(obligation.getObligationTypeCode()),
                obligation.getTitle(),
                obligation.getAmount(),
                clubFinanceSupport.formatAmount(obligation.getAmount(), obligation.getCurrencyCode()),
                obligation.getCurrencyCode(),
                clubFinanceSupport.formatDateTimeValue(obligation.getDueAt()),
                clubFinanceSupport.formatDateTimeLabel(obligation.getDueAt()),
                clubFinanceSupport.formatDateTimeValue(obligation.getCreateDate()),
                clubFinanceSupport.formatDateTimeLabel(obligation.getCreateDate()),
                obligation.getNote(),
                obligation.getFinanceAccountId(),
                referenceLabels.accountNameById().get(obligation.getFinanceAccountId()),
                obligation.getLinkedScheduleEventId(),
                referenceLabels.eventTitleById().get(obligation.getLinkedScheduleEventId()),
                clubFinanceSupport.resolveRecurrenceLabel(
                        obligation.getRecurrenceFrequency(),
                        obligation.getRecurrenceInterval()
                ),
                toPaymentResponse(
                        payment,
                        obligation,
                        new PaymentMemberSummary(memberDisplayName, memberRoleCode),
                        referenceLabels
                )
        );
    }

    private ClubFinancePaymentResponse toPaymentResponse(
            FinancePayment payment,
            FinanceObligation obligation,
            PaymentMemberSummary paymentMemberSummary,
            FinanceReferenceLabels referenceLabels
    ) {
        boolean overdue = clubFinancePaymentPolicy.isOverdue(payment, obligation);
        String responseStatusCode = overdue ? STATUS_OVERDUE : payment.getPaymentStatusCode();
        return new ClubFinancePaymentResponse(
                payment.getFinancePaymentId(),
                payment.getClubProfileId(),
                paymentMemberSummary == null ? "알 수 없는 멤버" : paymentMemberSummary.memberDisplayName(),
                paymentMemberSummary == null ? null : paymentMemberSummary.memberRoleCode(),
                payment.getFinanceAccountId(),
                referenceLabels.accountNameById().get(payment.getFinanceAccountId()),
                payment.getPaymentMethodCode(),
                clubFinanceSupport.resolvePaymentMethodLabel(payment.getPaymentMethodCode()),
                payment.getAmount(),
                clubFinanceSupport.formatAmount(payment.getAmount(), payment.getCurrencyCode()),
                payment.getCurrencyCode(),
                responseStatusCode,
                clubFinanceSupport.resolvePaymentStatusLabel(payment.getPaymentStatusCode(), overdue),
                overdue,
                clubFinanceSupport.formatDateTimeValue(payment.getPaidAt()),
                clubFinanceSupport.formatDateTimeLabel(payment.getPaidAt()),
                payment.getNote()
        );
    }

    private Map<Long, PaymentMemberSummary> resolvePaymentMemberSummaryByClubProfileId(
            List<FinancePayment> payments
    ) {
        if (payments.isEmpty()) {
            return Map.of();
        }

        Map<Long, ClubProfile> clubProfileById = clubProfileRepository.findAllById(
                        payments.stream().map(FinancePayment::getClubProfileId).distinct().toList()
                ).stream()
                .collect(Collectors.toMap(ClubProfile::getClubProfileId, Function.identity()));

        Map<Long, ClubMember> clubMemberById = clubMemberRepository.findAllById(
                        clubProfileById.values().stream()
                                .map(ClubProfile::getClubMemberId)
                                .distinct()
                                .toList()
                ).stream()
                .collect(Collectors.toMap(ClubMember::getClubMemberId, Function.identity()));

        Map<Long, PaymentMemberSummary> summaryByClubProfileId = new LinkedHashMap<>();
        clubProfileById.forEach((clubProfileId, clubProfile) -> {
            ClubMember clubMember = clubMemberById.get(clubProfile.getClubMemberId());
            summaryByClubProfileId.put(
                    clubProfileId,
                    new PaymentMemberSummary(
                            clubProfile.getDisplayName(),
                            clubMember == null ? null : clubMember.getRoleCode()
                    )
            );
        });
        return summaryByClubProfileId;
    }

    private Map<Long, String> resolveClubProfileDisplayNameById(Collection<Long> clubProfileIds) {
        if (clubProfileIds == null || clubProfileIds.isEmpty()) {
            return Map.of();
        }
        List<Long> normalizedIds = clubProfileIds.stream().filter(Objects::nonNull).distinct().toList();
        if (normalizedIds.isEmpty()) {
            return Map.of();
        }
        return clubProfileRepository.findAllById(normalizedIds).stream()
                .collect(Collectors.toMap(ClubProfile::getClubProfileId, ClubProfile::getDisplayName));
    }

    private FinanceReferenceLabels resolveReferenceLabels(
            Collection<Long> financePeriodIds,
            Collection<Long> financeAccountIds,
            Collection<Long> scheduleEventIds
    ) {
        List<Long> normalizedPeriodIds = financePeriodIds == null
                ? List.of()
                : financePeriodIds.stream().filter(Objects::nonNull).distinct().toList();
        List<Long> normalizedAccountIds = financeAccountIds == null
                ? List.of()
                : financeAccountIds.stream().filter(Objects::nonNull).distinct().toList();
        List<Long> normalizedEventIds = scheduleEventIds == null
                ? List.of()
                : scheduleEventIds.stream().filter(Objects::nonNull).distinct().toList();
        Map<Long, String> periodTitleById = financePeriodRepository.findAllById(normalizedPeriodIds).stream()
                .collect(Collectors.toMap(FinancePeriod::getFinancePeriodId, FinancePeriod::getTitle));
        Map<Long, String> accountNameById = financeAccountRepository.findAllById(normalizedAccountIds).stream()
                .collect(Collectors.toMap(FinanceAccount::getFinanceAccountId, FinanceAccount::getDisplayName));
        Map<Long, String> eventTitleById = clubScheduleEventRepository.findAllById(normalizedEventIds).stream()
                .collect(Collectors.toMap(ClubScheduleEvent::getEventId, ClubScheduleEvent::getTitle));
        return new FinanceReferenceLabels(periodTitleById, accountNameById, eventTitleById);
    }

    private FinanceExpenseRevisionResponse toExpenseRevisionResponse(
            FinanceExpenseRevision revision,
            Map<Long, String> displayNameByClubProfileId
    ) {
        return new FinanceExpenseRevisionResponse(
                revision.getFinanceExpenseRevisionId(),
                revision.getRevisionTypeCode(),
                displayNameByClubProfileId.getOrDefault(revision.getRevisedByClubProfileId(), "알 수 없는 운영자"),
                revision.getPreviousAmount(),
                revision.getNextAmount(),
                revision.getPreviousTitle(),
                revision.getNextTitle(),
                revision.getPreviousCategoryCode(),
                revision.getNextCategoryCode(),
                clubFinanceSupport.formatDateTimeValue(revision.getPreviousSpentAt()),
                clubFinanceSupport.formatDateTimeValue(revision.getNextSpentAt()),
                revision.getPreviousFinancePeriodId(),
                revision.getNextFinancePeriodId(),
                revision.getPreviousFinanceAccountId(),
                revision.getNextFinanceAccountId(),
                revision.getPreviousScheduleEventId(),
                revision.getNextScheduleEventId(),
                revision.getPreviousStatusCode(),
                revision.getNextStatusCode(),
                revision.getReason(),
                clubFinanceSupport.formatDateTimeValue(revision.getCreateDate())
        );
    }

    private record FinanceReferenceLabels(
            Map<Long, String> periodTitleById,
            Map<Long, String> accountNameById,
            Map<Long, String> eventTitleById
    ) {
    }

    private record PaymentMemberSummary(String memberDisplayName, String memberRoleCode) {
    }

    private record ObligationPaymentMetrics(
            int totalPaymentCount,
            int pendingPaymentCount,
            int paidPaymentCount,
            int waivedPaymentCount,
            int overduePaymentCount,
            int collectionRate,
            boolean canDelete
    ) {
    }
}
