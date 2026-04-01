package semo.back.service.feature.dues.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.DuesCharge;
import semo.back.service.database.pub.entity.DuesInvoice;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.DuesChargeRepository;
import semo.back.service.database.pub.repository.DuesInvoiceRepository;
import semo.back.service.feature.activity.biz.ClubActivityContextHolder;
import semo.back.service.feature.activity.biz.RecordClubActivity;
import semo.back.service.feature.club.biz.ClubAccessResolver;
import semo.back.service.feature.dues.vo.ClubAdminDuesChargeResponse;
import semo.back.service.feature.dues.vo.ClubAdminDuesHomeResponse;
import semo.back.service.feature.dues.vo.ClubDuesHomeResponse;
import semo.back.service.feature.dues.vo.ClubDuesInvoiceResponse;
import semo.back.service.feature.dues.vo.ClubDuesMemberOptionResponse;
import semo.back.service.feature.dues.vo.ClubDuesUserChargeResponse;
import semo.back.service.feature.dues.vo.CreateClubDuesChargeRequest;
import semo.back.service.feature.dues.vo.CreateClubDuesChargeResponse;
import semo.back.service.feature.dues.vo.UpdateClubDuesPaymentStatusRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubDuesService {
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_WAIVED = "WAIVED";
    private static final String STATUS_OVERDUE = "OVERDUE";

    private static final String TARGET_SCOPE_ALL_ACTIVE_MEMBERS = "ALL_ACTIVE_MEMBERS";
    private static final String TARGET_SCOPE_SELECTED_MEMBERS = "SELECTED_MEMBERS";
    private static final Set<String> ALLOWED_TARGET_SCOPES = Set.of(
            TARGET_SCOPE_ALL_ACTIVE_MEMBERS,
            TARGET_SCOPE_SELECTED_MEMBERS
    );

    private static final Set<String> ALLOWED_UPDATE_STATUSES = Set.of(STATUS_PENDING, STATUS_PAID, STATUS_WAIVED);
    private static final DateTimeFormatter DATE_TIME_VALUE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_TIME_LABEL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREAN);

    private final ClubAccessResolver clubAccessResolver;
    private final DuesChargeRepository duesChargeRepository;
    private final DuesInvoiceRepository duesInvoiceRepository;
    private final ClubProfileRepository clubProfileRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubDuesPermissionService clubDuesPermissionService;

    public ClubDuesHomeResponse getDues(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireDuesFeature(clubId);

        List<DuesInvoice> invoices = duesInvoiceRepository.findByClubIdAndClubProfileIdOrderByDuesInvoiceIdDesc(
                clubId,
                access.clubProfile().getClubProfileId()
        );
        Map<Long, DuesCharge> chargeById = resolveChargeById(invoices.stream().map(DuesInvoice::getDuesChargeId).toList());
        List<ClubDuesUserChargeResponse> chargeHistory = invoices.stream()
                .map(invoice -> toUserChargeResponse(
                        chargeById.get(invoice.getDuesChargeId()),
                        invoice,
                        access.clubProfile().getDisplayName(),
                        access.membership().getRoleCode()
                ))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ClubDuesUserChargeResponse::issuedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<ClubDuesUserChargeResponse> openCharges = chargeHistory.stream()
                .filter(charge -> STATUS_PENDING.equals(charge.invoice().paymentStatus()) || STATUS_OVERDUE.equals(charge.invoice().paymentStatus()))
                .sorted(Comparator
                        .comparing(
                                (ClubDuesUserChargeResponse charge) -> parseNullableDateTime(charge.dueAt()),
                                Comparator.nullsLast(Comparator.naturalOrder())
                        )
                        .thenComparing(ClubDuesUserChargeResponse::issuedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        ClubDuesUserChargeResponse nextPayableCharge = openCharges.stream().findFirst().orElse(null);

        return new ClubDuesHomeResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                (int) chargeHistory.stream().filter(charge -> STATUS_PENDING.equals(charge.invoice().paymentStatus())).count(),
                (int) chargeHistory.stream().filter(charge -> STATUS_PAID.equals(charge.invoice().paymentStatus())).count(),
                (int) chargeHistory.stream().filter(charge -> charge.invoice().overdue()).count(),
                formatAmount(
                        chargeHistory.stream()
                                .map(ClubDuesUserChargeResponse::invoice)
                                .filter(invoice -> STATUS_PENDING.equals(invoice.paymentStatus()) || invoice.overdue())
                                .map(ClubDuesInvoiceResponse::amount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        "KRW"
                ),
                nextPayableCharge,
                openCharges,
                chargeHistory
        );
    }

    public ClubAdminDuesHomeResponse getAdminDues(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireDuesFeature(clubId);
        requireAdminDuesView(access);

        List<ClubAccessResolver.ClubMemberSnapshot> activeMemberSnapshots = clubAccessResolver.getActiveMemberSnapshots(clubId);
        List<ClubDuesMemberOptionResponse> availableMembers = activeMemberSnapshots.stream()
                .map(snapshot -> new ClubDuesMemberOptionResponse(
                        snapshot.clubProfile().getClubProfileId(),
                        snapshot.clubProfile().getDisplayName(),
                        snapshot.membership().getRoleCode()
                ))
                .toList();

        List<DuesCharge> charges = duesChargeRepository.findByClubIdOrderByDuesChargeIdDesc(clubId);
        List<DuesInvoice> invoices = duesInvoiceRepository.findByClubIdOrderByDuesInvoiceIdDesc(clubId);
        Map<Long, List<DuesInvoice>> invoicesByChargeId = invoices.stream()
                .collect(Collectors.groupingBy(DuesInvoice::getDuesChargeId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, InvoiceMemberSummary> invoiceMemberSummaryByClubProfileId = resolveInvoiceMemberSummaryByClubProfileId(invoices);
        Map<Long, String> issuerNameByClubProfileId = resolveClubProfileDisplayNameById(
                charges.stream()
                        .map(DuesCharge::getIssuedByClubProfileId)
                        .filter(id -> id != null)
                        .toList()
        );

        List<ClubAdminDuesChargeResponse> chargeResponses = charges.stream()
                .map(charge -> toAdminChargeResponse(
                        charge,
                        invoicesByChargeId.getOrDefault(charge.getDuesChargeId(), List.of()),
                        invoiceMemberSummaryByClubProfileId,
                        issuerNameByClubProfileId
                ))
                .toList();

        int totalInvoiceCount = chargeResponses.stream().mapToInt(ClubAdminDuesChargeResponse::totalInvoiceCount).sum();
        int pendingInvoiceCount = chargeResponses.stream().mapToInt(ClubAdminDuesChargeResponse::pendingInvoiceCount).sum();
        int paidInvoiceCount = chargeResponses.stream().mapToInt(ClubAdminDuesChargeResponse::paidInvoiceCount).sum();
        int waivedInvoiceCount = chargeResponses.stream().mapToInt(ClubAdminDuesChargeResponse::waivedInvoiceCount).sum();
        int overdueInvoiceCount = chargeResponses.stream().mapToInt(ClubAdminDuesChargeResponse::overdueInvoiceCount).sum();
        int collectibleInvoiceCount = totalInvoiceCount - waivedInvoiceCount;

        return new ClubAdminDuesHomeResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                clubDuesPermissionService.canIssueDues(access),
                clubDuesPermissionService.canMarkPaid(access),
                clubDuesPermissionService.canMarkWaived(access),
                activeMemberSnapshots.size(),
                chargeResponses.size(),
                totalInvoiceCount,
                pendingInvoiceCount,
                paidInvoiceCount,
                waivedInvoiceCount,
                overdueInvoiceCount,
                collectibleInvoiceCount == 0 ? 0 : (int) Math.round((paidInvoiceCount * 100.0) / collectibleInvoiceCount),
                availableMembers,
                chargeResponses
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "회비관리")
    public CreateClubDuesChargeResponse createCharge(
            Long clubId,
            String userKey,
            CreateClubDuesChargeRequest request
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireDuesFeature(clubId);
        if (!clubDuesPermissionService.canIssueDues(access)) {
            throw new SemoException.ForbiddenException("회비를 발행할 권한이 없습니다.");
        }

        String title = normalizeTitle(request.title());
        String targetScope = normalizeTargetScope(request.targetScope());
        BigDecimal amount = normalizeAmount(request.amount());
        LocalDateTime dueAt = parseDateTime(request.dueAt(), "납부 마감일 형식이 잘못되었습니다.");
        String note = trimToNull(request.note());

        List<ClubAccessResolver.ClubMemberSnapshot> activeMembers = clubAccessResolver.getActiveMemberSnapshots(clubId);
        List<ClubAccessResolver.ClubMemberSnapshot> targetMembers = resolveTargetMembers(
                activeMembers,
                targetScope,
                request.clubProfileIds()
        );
        if (targetMembers.isEmpty()) {
            throw new SemoException.ValidationException("회비를 발행할 활성 멤버가 없습니다.");
        }

        DuesCharge charge = duesChargeRepository.save(DuesCharge.builder()
                .clubId(clubId)
                .issuedByClubProfileId(access.clubProfile().getClubProfileId())
                .title(title)
                .targetScope(targetScope)
                .amount(amount)
                .currencyCode("KRW")
                .dueAt(dueAt)
                .note(note)
                .build());

        List<DuesInvoice> invoices = targetMembers.stream()
                .map(snapshot -> DuesInvoice.builder()
                        .duesChargeId(charge.getDuesChargeId())
                        .clubId(clubId)
                        .clubProfileId(snapshot.clubProfile().getClubProfileId())
                        .amount(amount)
                        .currencyCode("KRW")
                        .paymentStatus(STATUS_PENDING)
                        .paidAt(null)
                        .note(note)
                        .build())
                .toList();
        duesInvoiceRepository.saveAll(invoices);

        ClubActivityContextHolder.setDetails(
                title + " 회비 항목을 " + invoices.size() + "명에게 발행했습니다.",
                "회비 발행에 실패했습니다."
        );

        return new CreateClubDuesChargeResponse(
                charge.getDuesChargeId(),
                charge.getTitle(),
                charge.getTargetScope(),
                resolveTargetScopeLabel(charge.getTargetScope()),
                invoices.size()
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "회비관리")
    public void deleteCharge(Long clubId, Long chargeId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireDuesFeature(clubId);
        if (!clubDuesPermissionService.canIssueDues(access)) {
            throw new SemoException.ForbiddenException("회비 항목을 삭제할 권한이 없습니다.");
        }

        DuesCharge charge = duesChargeRepository.findByDuesChargeIdAndClubId(chargeId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("DuesCharge", "chargeId", chargeId));
        List<DuesInvoice> invoices = duesInvoiceRepository.findByDuesChargeIdOrderByDuesInvoiceIdDesc(chargeId);
        if (!canDeleteCharge(invoices)) {
            throw new SemoException.ValidationException("아직 아무도 처리하지 않은 회비 항목만 삭제할 수 있습니다.");
        }

        duesInvoiceRepository.deleteAll(invoices);
        duesChargeRepository.delete(charge);

        ClubActivityContextHolder.setDetails(
                charge.getTitle() + " 회비 항목을 삭제했습니다.",
                "회비 항목 삭제에 실패했습니다."
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "회비관리")
    public ClubDuesInvoiceResponse updatePaymentStatus(
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

        DuesInvoice invoice = duesInvoiceRepository.findByDuesInvoiceIdAndClubId(invoiceId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("DuesInvoice", "invoiceId", invoiceId));
        DuesCharge charge = duesChargeRepository.findByDuesChargeIdAndClubId(invoice.getDuesChargeId(), clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("DuesCharge", "chargeId", invoice.getDuesChargeId()));
        String note = trimToNull(request.note());
        LocalDateTime paidAt = STATUS_PAID.equals(nextStatus) ? LocalDateTime.now() : null;

        DuesInvoice updated = duesInvoiceRepository.save(DuesInvoice.builder()
                .duesInvoiceId(invoice.getDuesInvoiceId())
                .duesChargeId(invoice.getDuesChargeId())
                .clubId(invoice.getClubId())
                .clubProfileId(invoice.getClubProfileId())
                .amount(invoice.getAmount())
                .currencyCode(invoice.getCurrencyCode())
                .paymentStatus(nextStatus)
                .paidAt(paidAt)
                .note(note == null ? invoice.getNote() : note)
                .build());

        ClubActivityContextHolder.setDetails(
                charge.getTitle() + " 회비 상태를 " + resolvePaymentStatusLabel(nextStatus, false) + "로 변경했습니다.",
                "회비 상태 변경에 실패했습니다."
        );

        InvoiceMemberSummary invoiceMemberSummary = resolveInvoiceMemberSummaryByClubProfileId(List.of(updated))
                .get(updated.getClubProfileId());
        return toInvoiceResponse(updated, charge, invoiceMemberSummary);
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

    private Map<Long, DuesCharge> resolveChargeById(Collection<Long> chargeIds) {
        if (chargeIds == null || chargeIds.isEmpty()) {
            return Map.of();
        }
        return duesChargeRepository.findAllById(chargeIds).stream()
                .collect(Collectors.toMap(DuesCharge::getDuesChargeId, Function.identity()));
    }

    private List<ClubAccessResolver.ClubMemberSnapshot> resolveTargetMembers(
            List<ClubAccessResolver.ClubMemberSnapshot> activeMembers,
            String targetScope,
            Collection<Long> requestedClubProfileIds
    ) {
        if (TARGET_SCOPE_ALL_ACTIVE_MEMBERS.equals(targetScope)) {
            return activeMembers;
        }
        Set<Long> requestedSet = requestedClubProfileIds == null
                ? Set.of()
                : requestedClubProfileIds.stream().filter(id -> id != null).collect(Collectors.toSet());
        if (requestedSet.isEmpty()) {
            throw new SemoException.ValidationException("선택 멤버 발행 시 대상 멤버를 한 명 이상 선택해주세요.");
        }
        List<ClubAccessResolver.ClubMemberSnapshot> targets = activeMembers.stream()
                .filter(snapshot -> requestedSet.contains(snapshot.clubProfile().getClubProfileId()))
                .toList();
        if (targets.size() != requestedSet.size()) {
            throw new SemoException.ValidationException("현재 활성 멤버에게만 회비를 발행할 수 있습니다.");
        }
        return targets;
    }

    private ClubAdminDuesChargeResponse toAdminChargeResponse(
            DuesCharge charge,
            List<DuesInvoice> invoices,
            Map<Long, InvoiceMemberSummary> invoiceMemberSummaryByClubProfileId,
            Map<Long, String> issuerNameByClubProfileId
    ) {
        List<ClubDuesInvoiceResponse> invoiceResponses = invoices.stream()
                .map(invoice -> toInvoiceResponse(invoice, charge, invoiceMemberSummaryByClubProfileId.get(invoice.getClubProfileId())))
                .sorted(Comparator
                        .comparing((ClubDuesInvoiceResponse invoice) -> invoice.overdue() ? 0 : 1)
                        .thenComparing(ClubDuesInvoiceResponse::paymentStatus)
                        .thenComparing(ClubDuesInvoiceResponse::memberDisplayName, Comparator.nullsLast(String::compareTo)))
                .toList();

        int totalInvoiceCount = invoiceResponses.size();
        int pendingInvoiceCount = (int) invoiceResponses.stream().filter(invoice -> STATUS_PENDING.equals(invoice.paymentStatus())).count();
        int paidInvoiceCount = (int) invoiceResponses.stream().filter(invoice -> STATUS_PAID.equals(invoice.paymentStatus())).count();
        int waivedInvoiceCount = (int) invoiceResponses.stream().filter(invoice -> STATUS_WAIVED.equals(invoice.paymentStatus())).count();
        int overdueInvoiceCount = (int) invoiceResponses.stream().filter(ClubDuesInvoiceResponse::overdue).count();
        int collectibleInvoiceCount = totalInvoiceCount - waivedInvoiceCount;
        boolean canDelete = canDeleteCharge(invoices);

        return new ClubAdminDuesChargeResponse(
                charge.getDuesChargeId(),
                charge.getTitle(),
                charge.getTargetScope(),
                resolveTargetScopeLabel(charge.getTargetScope()),
                charge.getAmount(),
                formatAmount(charge.getAmount(), charge.getCurrencyCode()),
                charge.getCurrencyCode(),
                formatDateTimeValue(charge.getDueAt()),
                formatDateTimeLabel(charge.getDueAt()),
                formatDateTimeValue(charge.getCreateDate()),
                formatDateTimeLabel(charge.getCreateDate()),
                charge.getIssuedByClubProfileId() == null ? "알 수 없는 운영자" : issuerNameByClubProfileId.getOrDefault(charge.getIssuedByClubProfileId(), "알 수 없는 운영자"),
                charge.getNote(),
                canDelete,
                totalInvoiceCount,
                pendingInvoiceCount,
                paidInvoiceCount,
                waivedInvoiceCount,
                overdueInvoiceCount,
                collectibleInvoiceCount == 0 ? 0 : (int) Math.round((paidInvoiceCount * 100.0) / collectibleInvoiceCount),
                invoiceResponses
        );
    }

    private boolean canDeleteCharge(List<DuesInvoice> invoices) {
        return invoices.stream().allMatch(invoice -> STATUS_PENDING.equals(invoice.getPaymentStatus()));
    }

    private ClubDuesUserChargeResponse toUserChargeResponse(
            DuesCharge charge,
            DuesInvoice invoice,
            String memberDisplayName,
            String memberRoleCode
    ) {
        if (charge == null) {
            return null;
        }
        return new ClubDuesUserChargeResponse(
                charge.getDuesChargeId(),
                charge.getTitle(),
                charge.getAmount(),
                formatAmount(charge.getAmount(), charge.getCurrencyCode()),
                charge.getCurrencyCode(),
                formatDateTimeValue(charge.getDueAt()),
                formatDateTimeLabel(charge.getDueAt()),
                formatDateTimeValue(charge.getCreateDate()),
                formatDateTimeLabel(charge.getCreateDate()),
                charge.getNote(),
                toInvoiceResponse(invoice, charge, new InvoiceMemberSummary(memberDisplayName, memberRoleCode))
        );
    }

    private ClubDuesInvoiceResponse toInvoiceResponse(
            DuesInvoice invoice,
            DuesCharge charge,
            InvoiceMemberSummary invoiceMemberSummary
    ) {
        boolean overdue = STATUS_PENDING.equals(invoice.getPaymentStatus())
                && charge.getDueAt() != null
                && charge.getDueAt().isBefore(LocalDateTime.now());
        String responseStatus = overdue ? STATUS_OVERDUE : invoice.getPaymentStatus();
        return new ClubDuesInvoiceResponse(
                invoice.getDuesInvoiceId(),
                invoice.getClubProfileId(),
                invoiceMemberSummary == null ? "알 수 없는 멤버" : invoiceMemberSummary.memberDisplayName(),
                invoiceMemberSummary == null ? null : invoiceMemberSummary.memberRoleCode(),
                invoice.getAmount(),
                formatAmount(invoice.getAmount(), invoice.getCurrencyCode()),
                invoice.getCurrencyCode(),
                responseStatus,
                resolvePaymentStatusLabel(invoice.getPaymentStatus(), overdue),
                overdue,
                formatDateTimeValue(invoice.getPaidAt()),
                formatDateTimeLabel(invoice.getPaidAt()),
                invoice.getNote()
        );
    }

    private Map<Long, InvoiceMemberSummary> resolveInvoiceMemberSummaryByClubProfileId(List<DuesInvoice> invoices) {
        if (invoices.isEmpty()) {
            return Map.of();
        }

        Map<Long, ClubProfile> clubProfileById = clubProfileRepository.findAllById(
                        invoices.stream()
                                .map(DuesInvoice::getClubProfileId)
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

    private Map<Long, String> resolveClubProfileDisplayNameById(Collection<Long> clubProfileIds) {
        if (clubProfileIds == null || clubProfileIds.isEmpty()) {
            return Map.of();
        }
        return clubProfileRepository.findAllById(clubProfileIds).stream()
                .collect(Collectors.toMap(ClubProfile::getClubProfileId, ClubProfile::getDisplayName));
    }

    private String normalizeTitle(String title) {
        String normalized = trimToNull(title);
        if (normalized == null) {
            throw new SemoException.ValidationException("회비 항목 이름은 필수입니다.");
        }
        return normalized;
    }

    private String normalizeTargetScope(String targetScope) {
        String normalized = trimToNull(targetScope);
        if (normalized == null) {
            return TARGET_SCOPE_ALL_ACTIVE_MEMBERS;
        }
        String upperCased = normalized.toUpperCase(Locale.ROOT);
        if (!ALLOWED_TARGET_SCOPES.contains(upperCased)) {
            throw new SemoException.ValidationException("지원하지 않는 발행 대상 타입입니다.");
        }
        return upperCased;
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

    private LocalDateTime parseNullableDateTime(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        return LocalDateTime.parse(rawValue, DATE_TIME_VALUE_FORMATTER);
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

    private String formatAmount(BigDecimal amount, String currencyCode) {
        BigDecimal normalized = amount == null ? BigDecimal.ZERO : amount.stripTrailingZeros();
        String pattern = normalized.scale() > 0 ? "#,##0.##" : "#,##0";
        String formatted = new DecimalFormat(pattern).format(amount == null ? BigDecimal.ZERO : amount);
        if ("KRW".equalsIgnoreCase(currencyCode)) {
            return formatted + "원";
        }
        return (StringUtils.hasText(currencyCode) ? currencyCode.toUpperCase(Locale.ROOT) : "KRW") + " " + formatted;
    }

    private String resolveTargetScopeLabel(String targetScope) {
        return switch (targetScope) {
            case TARGET_SCOPE_SELECTED_MEMBERS -> "선택 멤버";
            default -> "활성 멤버 전체";
        };
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

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private record InvoiceMemberSummary(String memberDisplayName, String memberRoleCode) {
    }
}
