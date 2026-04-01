package semo.back.service.feature.dues.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.ClubDuesInvoice;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubDuesInvoiceRepository;
import semo.back.service.feature.activity.biz.ClubActivityContextHolder;
import semo.back.service.feature.activity.biz.RecordClubActivity;
import semo.back.service.feature.club.biz.ClubAccessResolver;
import semo.back.service.feature.dues.vo.ClubAdminDuesHomeResponse;
import semo.back.service.feature.dues.vo.ClubDuesHomeResponse;
import semo.back.service.feature.dues.vo.ClubDuesSummaryResponse;
import semo.back.service.feature.dues.vo.IssueClubDuesInvoicesRequest;
import semo.back.service.feature.dues.vo.IssueClubDuesInvoicesResponse;
import semo.back.service.feature.dues.vo.UpdateClubDuesPaymentStatusRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubDuesService {
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_WAIVED = "WAIVED";
    private static final Set<String> ALLOWED_UPDATE_STATUSES = Set.of(STATUS_PENDING, STATUS_PAID, STATUS_WAIVED);
    private static final DateTimeFormatter DATE_TIME_VALUE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_TIME_LABEL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREAN);
    private static final DateTimeFormatter BILLING_MONTH_LABEL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN);

    private final ClubAccessResolver clubAccessResolver;
    private final ClubDuesInvoiceRepository clubDuesInvoiceRepository;
    private final ClubProfileRepository clubProfileRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubDuesPermissionService clubDuesPermissionService;

    public ClubDuesHomeResponse getDues(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireDuesFeature(clubId);

        List<ClubDuesInvoice> invoices = clubDuesInvoiceRepository
                .findByClubIdAndClubProfileIdOrderByBillingYearDescBillingMonthDescClubDuesInvoiceIdDesc(
                        clubId,
                        access.clubProfile().getClubProfileId()
                );
        List<ClubDuesSummaryResponse> summaries = invoices.stream()
                .map(invoice -> toSummaryResponse(invoice, access.clubProfile().getDisplayName(), access.membership().getRoleCode()))
                .toList();

        ClubDuesSummaryResponse nextInvoice = summaries.stream()
                .filter(invoice -> STATUS_PENDING.equals(invoice.paymentStatus()) || "OVERDUE".equals(invoice.paymentStatus()))
                .findFirst()
                .orElse(null);

        return new ClubDuesHomeResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                (int) summaries.stream().filter(invoice -> STATUS_PENDING.equals(invoice.paymentStatus())).count(),
                (int) summaries.stream().filter(invoice -> STATUS_PAID.equals(invoice.paymentStatus())).count(),
                (int) summaries.stream().filter(ClubDuesSummaryResponse::overdue).count(),
                formatAmount(
                        invoices.stream()
                                .filter(invoice -> STATUS_PENDING.equals(invoice.getPaymentStatus()))
                                .map(ClubDuesInvoice::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        "KRW"
                ),
                nextInvoice,
                summaries
        );
    }

    public ClubAdminDuesHomeResponse getAdminDues(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireDuesFeature(clubId);
        requireAdminDuesView(access);

        List<ClubAccessResolver.ClubMemberSnapshot> memberSnapshots = clubAccessResolver.getActiveMemberSnapshots(clubId);
        List<ClubDuesInvoice> invoices = clubDuesInvoiceRepository.findByClubIdOrderByBillingYearDescBillingMonthDescClubDuesInvoiceIdDesc(clubId);
        Map<Long, InvoiceMemberSummary> invoiceMemberSummaryByClubProfileId = resolveInvoiceMemberSummaryByClubProfileId(invoices);
        List<ClubDuesSummaryResponse> summaries = invoices.stream()
                .map(invoice -> {
                    InvoiceMemberSummary summary = invoiceMemberSummaryByClubProfileId.get(invoice.getClubProfileId());
                    return toSummaryResponse(
                            invoice,
                            summary == null ? "알 수 없는 멤버" : summary.memberDisplayName(),
                            summary == null ? null : summary.memberRoleCode()
                    );
                })
                .toList();

        int totalInvoiceCount = summaries.size();
        int paidInvoiceCount = (int) summaries.stream().filter(invoice -> STATUS_PAID.equals(invoice.paymentStatus())).count();
        int waivedInvoiceCount = (int) summaries.stream().filter(invoice -> STATUS_WAIVED.equals(invoice.paymentStatus())).count();
        int collectibleInvoiceCount = totalInvoiceCount - waivedInvoiceCount;

        return new ClubAdminDuesHomeResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                clubDuesPermissionService.canIssueDues(access),
                clubDuesPermissionService.canMarkPaid(access),
                clubDuesPermissionService.canMarkWaived(access),
                memberSnapshots.size(),
                totalInvoiceCount,
                (int) summaries.stream().filter(invoice -> STATUS_PENDING.equals(invoice.paymentStatus())).count(),
                paidInvoiceCount,
                waivedInvoiceCount,
                (int) summaries.stream().filter(ClubDuesSummaryResponse::overdue).count(),
                collectibleInvoiceCount == 0 ? 0 : (int) Math.round((paidInvoiceCount * 100.0) / collectibleInvoiceCount),
                summaries
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "회비관리")
    public IssueClubDuesInvoicesResponse issueMonthlyInvoices(
            Long clubId,
            String userKey,
            IssueClubDuesInvoicesRequest request
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireDuesFeature(clubId);
        if (!clubDuesPermissionService.canIssueDues(access)) {
            throw new SemoException.ForbiddenException("회비를 발행할 권한이 없습니다.");
        }

        short billingYear = normalizeBillingYear(request.billingYear());
        byte billingMonth = normalizeBillingMonth(request.billingMonth());
        BigDecimal amount = normalizeAmount(request.amount());
        LocalDateTime dueAt = parseDateTime(request.dueAt(), "납부 마감일 형식이 잘못되었습니다.");
        String note = trimToNull(request.note());

        List<ClubAccessResolver.ClubMemberSnapshot> activeMembers = clubAccessResolver.getActiveMemberSnapshots(clubId);
        List<ClubAccessResolver.ClubMemberSnapshot> targetMembers = resolveTargetMembers(activeMembers, request.clubProfileIds());
        if (targetMembers.isEmpty()) {
            throw new SemoException.ValidationException("회비를 발행할 활성 멤버가 없습니다.");
        }

        List<Long> targetClubProfileIds = targetMembers.stream()
                .map(snapshot -> snapshot.clubProfile().getClubProfileId())
                .toList();
        Set<Long> existingProfileIds = clubDuesInvoiceRepository
                .findByClubIdAndBillingYearAndBillingMonthAndClubProfileIdIn(clubId, billingYear, billingMonth, targetClubProfileIds)
                .stream()
                .map(ClubDuesInvoice::getClubProfileId)
                .collect(Collectors.toSet());

        List<ClubDuesInvoice> created = targetMembers.stream()
                .filter(snapshot -> !existingProfileIds.contains(snapshot.clubProfile().getClubProfileId()))
                .map(snapshot -> ClubDuesInvoice.builder()
                        .clubId(clubId)
                        .clubProfileId(snapshot.clubProfile().getClubProfileId())
                        .billingYear(billingYear)
                        .billingMonth(billingMonth)
                        .amount(amount)
                        .currencyCode("KRW")
                        .paymentStatus(STATUS_PENDING)
                        .dueAt(dueAt)
                        .paidAt(null)
                        .note(note)
                        .build())
                .toList();

        if (created.isEmpty()) {
            throw new SemoException.ValidationException("해당 월 회비는 이미 모두 발행되어 있습니다.");
        }

        clubDuesInvoiceRepository.saveAll(created);
        ClubActivityContextHolder.setDetails(
                formatBillingMonthLabel(billingYear, billingMonth) + " 회비 " + created.size() + "건을 발행했습니다.",
                "회비 발행에 실패했습니다."
        );

        return new IssueClubDuesInvoicesResponse(
                billingYear,
                billingMonth,
                formatBillingMonthLabel(billingYear, billingMonth),
                created.size(),
                targetMembers.size() - created.size()
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "회비관리")
    public ClubDuesSummaryResponse updatePaymentStatus(
            Long clubId,
            Long invoiceId,
            String userKey,
            UpdateClubDuesPaymentStatusRequest request
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireDuesFeature(clubId);

        String nextStatus = normalizeUpdateStatus(request.paymentStatus());
        if (STATUS_PAID.equals(nextStatus) && !clubDuesPermissionService.canMarkPaid(access)) {
            throw new SemoException.ForbiddenException("회비를 납부 완료 처리할 권한이 없습니다.");
        }
        if (STATUS_WAIVED.equals(nextStatus) && !clubDuesPermissionService.canMarkWaived(access)) {
            throw new SemoException.ForbiddenException("회비를 면제 처리할 권한이 없습니다.");
        }
        if (STATUS_PENDING.equals(nextStatus)
                && !clubDuesPermissionService.canMarkPaid(access)
                && !clubDuesPermissionService.canMarkWaived(access)) {
            throw new SemoException.ForbiddenException("회비 상태를 변경할 권한이 없습니다.");
        }

        ClubDuesInvoice invoice = clubDuesInvoiceRepository.findByClubDuesInvoiceIdAndClubId(invoiceId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("ClubDuesInvoice", "invoiceId", invoiceId));
        String note = trimToNull(request.note());
        LocalDateTime paidAt = STATUS_PAID.equals(nextStatus) ? LocalDateTime.now() : null;

        ClubDuesInvoice updated = clubDuesInvoiceRepository.save(ClubDuesInvoice.builder()
                .clubDuesInvoiceId(invoice.getClubDuesInvoiceId())
                .clubId(invoice.getClubId())
                .clubProfileId(invoice.getClubProfileId())
                .billingYear(invoice.getBillingYear())
                .billingMonth(invoice.getBillingMonth())
                .amount(invoice.getAmount())
                .currencyCode(invoice.getCurrencyCode())
                .paymentStatus(nextStatus)
                .dueAt(invoice.getDueAt())
                .paidAt(paidAt)
                .note(note == null ? invoice.getNote() : note)
                .build());

        ClubActivityContextHolder.setDetails(
                formatBillingMonthLabel(invoice.getBillingYear(), invoice.getBillingMonth())
                        + " 회비 상태를 "
                        + resolvePaymentStatusLabel(nextStatus, false)
                        + "로 변경했습니다.",
                "회비 상태 변경에 실패했습니다."
        );

        ClubAccessResolver.ClubMemberSnapshot snapshot = clubAccessResolver.getActiveMemberSnapshots(clubId).stream()
                .filter(member -> member.clubProfile().getClubProfileId().equals(updated.getClubProfileId()))
                .findFirst()
                .orElse(null);
        return toSummaryResponse(
                updated,
                snapshot == null ? "알 수 없는 멤버" : snapshot.clubProfile().getDisplayName(),
                snapshot == null ? null : snapshot.membership().getRoleCode()
        );
    }

    private void requireDuesFeature(Long clubId) {
        if (!clubDuesPermissionService.isDuesEnabled(clubId)) {
            throw new SemoException.ForbiddenException("회비관리 기능이 활성화되지 않았습니다.");
        }
    }

    private void requireAdminDuesView(ClubAccessResolver.ClubAccess access) {
        if (!clubDuesPermissionService.canViewAdminDues(access)) {
            throw new SemoException.ForbiddenException("회비관리 화면을 조회할 권한이 없습니다.");
        }
    }

    private List<ClubAccessResolver.ClubMemberSnapshot> resolveTargetMembers(
            List<ClubAccessResolver.ClubMemberSnapshot> activeMembers,
            Collection<Long> requestedClubProfileIds
    ) {
        if (requestedClubProfileIds == null || requestedClubProfileIds.isEmpty()) {
            return activeMembers;
        }
        Set<Long> requestedSet = requestedClubProfileIds.stream().collect(Collectors.toSet());
        List<ClubAccessResolver.ClubMemberSnapshot> targets = activeMembers.stream()
                .filter(snapshot -> requestedSet.contains(snapshot.clubProfile().getClubProfileId()))
                .toList();
        if (targets.size() != requestedSet.size()) {
            throw new SemoException.ValidationException("다른 모임 멤버에게는 회비를 발행할 수 없습니다.");
        }
        return targets;
    }

    private Map<Long, InvoiceMemberSummary> resolveInvoiceMemberSummaryByClubProfileId(List<ClubDuesInvoice> invoices) {
        if (invoices.isEmpty()) {
            return Map.of();
        }

        Map<Long, ClubProfile> clubProfileById = clubProfileRepository.findAllById(
                        invoices.stream()
                                .map(ClubDuesInvoice::getClubProfileId)
                                .distinct()
                                .toList()
                ).stream()
                .collect(Collectors.toMap(ClubProfile::getClubProfileId, Function.identity()));

        Map<Long, ClubMember> clubMemberById = clubMemberRepository.findAllById(
                        clubProfileById.values().stream()
                                .map(ClubProfile::getClubMemberId)
                                .distinct()
                                .toList()
                ).stream()
                .collect(Collectors.toMap(ClubMember::getClubMemberId, Function.identity()));

        Map<Long, InvoiceMemberSummary> summaryByClubProfileId = new LinkedHashMap<>();
        clubProfileById.forEach((clubProfileId, clubProfile) -> {
            ClubMember clubMember = clubMemberById.get(clubProfile.getClubMemberId());
            summaryByClubProfileId.put(
                    clubProfileId,
                    new InvoiceMemberSummary(
                            clubProfile.getDisplayName(),
                            clubMember == null ? null : clubMember.getRoleCode()
                    )
            );
        });
        return summaryByClubProfileId;
    }

    private ClubDuesSummaryResponse toSummaryResponse(
            ClubDuesInvoice invoice,
            String memberDisplayName,
            String memberRoleCode
    ) {
        boolean overdue = STATUS_PENDING.equals(invoice.getPaymentStatus())
                && invoice.getDueAt() != null
                && invoice.getDueAt().isBefore(LocalDateTime.now());
        String responseStatus = overdue ? "OVERDUE" : invoice.getPaymentStatus();
        return new ClubDuesSummaryResponse(
                invoice.getClubDuesInvoiceId(),
                invoice.getClubProfileId(),
                memberDisplayName,
                memberRoleCode,
                invoice.getBillingYear(),
                invoice.getBillingMonth(),
                formatBillingMonthLabel(invoice.getBillingYear(), invoice.getBillingMonth()),
                invoice.getAmount(),
                formatAmount(invoice.getAmount(), invoice.getCurrencyCode()),
                invoice.getCurrencyCode(),
                responseStatus,
                resolvePaymentStatusLabel(invoice.getPaymentStatus(), overdue),
                overdue,
                formatDateTimeValue(invoice.getDueAt()),
                formatDateTimeLabel(invoice.getDueAt()),
                formatDateTimeValue(invoice.getPaidAt()),
                formatDateTimeLabel(invoice.getPaidAt()),
                invoice.getNote()
        );
    }

    private String resolvePaymentStatusLabel(String paymentStatus, boolean overdue) {
        if (overdue) {
            return "연체";
        }
        return switch (paymentStatus) {
            case STATUS_PAID -> "납부 완료";
            case STATUS_WAIVED -> "면제";
            default -> "미납";
        };
    }

    private short normalizeBillingYear(Integer billingYear) {
        if (billingYear == null || billingYear < 2000 || billingYear > 2100) {
            throw new SemoException.ValidationException("청구 연도를 확인해주세요.");
        }
        return billingYear.shortValue();
    }

    private byte normalizeBillingMonth(Integer billingMonth) {
        if (billingMonth == null || billingMonth < 1 || billingMonth > 12) {
            throw new SemoException.ValidationException("청구 월을 확인해주세요.");
        }
        return billingMonth.byteValue();
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new SemoException.ValidationException("회비 금액은 0보다 커야 합니다.");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeUpdateStatus(String paymentStatus) {
        String normalized = trimToNull(paymentStatus);
        if (normalized == null) {
            throw new SemoException.ValidationException("회비 상태는 필수입니다.");
        }
        String upperCased = normalized.toUpperCase(Locale.ROOT);
        if (!ALLOWED_UPDATE_STATUSES.contains(upperCased)) {
            throw new SemoException.ValidationException("지원하지 않는 회비 상태입니다.");
        }
        return upperCased;
    }

    private LocalDateTime parseDateTime(String rawValue, String errorMessage) {
        String normalized = trimToNull(rawValue);
        if (normalized == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(normalized, DATE_TIME_VALUE_FORMATTER);
        } catch (RuntimeException exception) {
            throw new SemoException.ValidationException(errorMessage);
        }
    }

    private String formatDateTimeValue(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.format(DATE_TIME_VALUE_FORMATTER);
    }

    private String formatDateTimeLabel(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.format(DATE_TIME_LABEL_FORMATTER);
    }

    private String formatBillingMonthLabel(int billingYear, int billingMonth) {
        return LocalDateTime.of(billingYear, billingMonth, 1, 0, 0).format(BILLING_MONTH_LABEL_FORMATTER);
    }

    private String formatAmount(BigDecimal amount, String currencyCode) {
        BigDecimal normalized = amount == null ? BigDecimal.ZERO : amount.stripTrailingZeros();
        String pattern = normalized.scale() > 0 ? "#,##0.##" : "#,##0";
        String formatted = new DecimalFormat(pattern).format(amount == null ? BigDecimal.ZERO : amount);
        if ("KRW".equalsIgnoreCase(currencyCode)) {
            return formatted + "원";
        }
        return (StringUtils.hasText(currencyCode) ? currencyCode.toUpperCase(Locale.ROOT) : "KRW") + " " + formatted;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private record InvoiceMemberSummary(String memberDisplayName, String memberRoleCode) {
    }
}
