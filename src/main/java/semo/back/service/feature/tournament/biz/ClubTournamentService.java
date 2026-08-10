package semo.back.service.feature.tournament.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.entity.FinancePayment;
import semo.back.service.database.pub.entity.TournamentApplication;
import semo.back.service.database.pub.entity.TournamentRecord;
import semo.back.service.database.pub.entity.TournamentRosterMember;
import semo.back.service.database.pub.entity.TournamentScheduleSlot;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.TournamentApplicationRepository;
import semo.back.service.database.pub.repository.TournamentRecordRepository;
import semo.back.service.database.pub.repository.TournamentRosterMemberRepository;
import semo.back.service.database.pub.repository.TournamentScheduleSlotRepository;
import semo.back.service.feature.activity.biz.ClubActivityContextHolder;
import semo.back.service.feature.activity.biz.RecordClubActivity;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.notification.biz.ClubNotificationPublisher;
import semo.back.service.feature.notification.biz.ClubNotificationPublisher.NotificationCommand;
import semo.back.service.feature.share.biz.ClubContentShareService;
import semo.back.service.feature.tournament.biz.policy.ClubTournamentPermissionService;
import semo.back.service.feature.tournament.biz.support.ClubTournamentSupport;
import semo.back.service.feature.tournament.vo.CancelTournamentRequest;
import semo.back.service.feature.tournament.vo.ClubAdminTournamentHomeResponse;
import semo.back.service.feature.tournament.vo.ClubTournamentHomeResponse;
import semo.back.service.feature.tournament.vo.ReviewTournamentApplicationRequest;
import semo.back.service.feature.tournament.vo.ReviewTournamentRecordRequest;
import semo.back.service.feature.tournament.vo.SubmitTournamentApplicationRequest;
import semo.back.service.feature.tournament.vo.TournamentApplicationSummaryResponse;
import semo.back.service.feature.tournament.vo.TournamentDetailResponse;
import semo.back.service.feature.tournament.vo.TournamentParticipantSummaryResponse;
import semo.back.service.feature.tournament.vo.TournamentRosterMemberResponse;
import semo.back.service.feature.tournament.vo.TournamentRosterOptionResponse;
import semo.back.service.feature.tournament.vo.TournamentScheduleSlotResponse;
import semo.back.service.feature.tournament.vo.TournamentSummaryResponse;
import semo.back.service.feature.tournament.vo.TournamentUpsertResponse;
import semo.back.service.feature.tournament.vo.UpdateTournamentApplicationOperationsRequest;
import semo.back.service.feature.tournament.vo.UpsertTournamentRequest;
import semo.back.service.feature.tournament.vo.UpsertTournamentScheduleSlotRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubTournamentService {
    public static final String CONTENT_TOURNAMENT = "TOURNAMENT";

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_APPLICATION_OPEN = "APPLICATION_OPEN";
    private static final String STATUS_ENTRY_CONFIRMED = "ENTRY_CONFIRMED";
    private static final String STATUS_ONGOING = "ONGOING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private static final String APPROVAL_PENDING = "PENDING";
    private static final String APPROVAL_APPROVED = "APPROVED";
    private static final String APPROVAL_REJECTED = "REJECTED";

    private static final String APPLICATION_APPLIED = "APPLIED";
    private static final String APPLICATION_APPROVED = "APPROVED";
    private static final String APPLICATION_REJECTED = "REJECTED";
    private static final String APPLICATION_CANCELLED = "CANCELLED";
    private static final String APPLICATION_WAITLISTED = "WAITLISTED";

    private final TournamentRecordRepository tournamentRecordRepository;
    private final TournamentApplicationRepository tournamentApplicationRepository;
    private final TournamentRosterMemberRepository tournamentRosterMemberRepository;
    private final TournamentScheduleSlotRepository tournamentScheduleSlotRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubProfileRepository clubProfileRepository;
    private final ClubAccessResolver clubAccessResolver;
    private final ClubTournamentPermissionService clubTournamentPermissionService;
    private final ClubContentShareService clubContentShareService;
    private final ClubTournamentSupport clubTournamentSupport;
    private final ClubNotificationPublisher clubNotificationPublisher;
    private final TournamentFinanceLinkService tournamentFinanceLinkService;

    public ClubTournamentHomeResponse getTournamentHome(Long clubId, String userKey) {
        requireTournamentFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        List<TournamentRecord> tournaments = tournamentRecordRepository
                .findByClubIdAndDeletedFalseOrderByPinnedDescStartDateAscTournamentRecordIdDesc(clubId);
        TournamentContext context = buildTournamentContext(access, tournaments);

        List<TournamentSummaryResponse> approvedVisible = context.summaries().stream()
                .filter(summary -> APPROVAL_APPROVED.equals(summary.approvalStatus()))
                .toList();
        List<TournamentSummaryResponse> archived = approvedVisible.stream()
                .filter(summary -> isArchived(summary.tournamentStatus()))
                .toList();
        List<TournamentSummaryResponse> visible = approvedVisible.stream()
                .filter(summary -> !isArchived(summary.tournamentStatus()))
                .toList();
        List<TournamentSummaryResponse> myTournaments = context.summaries().stream()
                .filter(summary -> summary.participating() || summary.mine())
                .toList();
        TournamentSummaryResponse featured = visible.stream()
                .filter(TournamentSummaryResponse::pinned)
                .findFirst()
                .orElseGet(() -> visible.stream().findFirst().orElse(null));

        return new ClubTournamentHomeResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                clubTournamentPermissionService.canCreateTournament(access),
                visible.size(),
                (int) visible.stream().filter(summary -> STATUS_APPLICATION_OPEN.equals(summary.tournamentStatus())).count(),
                (int) visible.stream().filter(summary -> STATUS_ONGOING.equals(summary.tournamentStatus())).count(),
                myTournaments.size(),
                featured,
                visible,
                myTournaments,
                archived
        );
    }

    public ClubAdminTournamentHomeResponse getAdminTournamentHome(Long clubId, String userKey) {
        requireTournamentFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        boolean canReview = clubTournamentPermissionService.canReviewTournament(access);
        boolean canDelete = clubTournamentPermissionService.canDeleteTournament(access);
        requireTournamentAdminToolAccess(canReview, canDelete);
        List<TournamentRecord> tournaments = tournamentRecordRepository
                .findByClubIdAndDeletedFalseOrderByPinnedDescStartDateAscTournamentRecordIdDesc(clubId);
        TournamentContext context = buildTournamentContext(access, tournaments);

        return new ClubAdminTournamentHomeResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                canReview,
                canDelete,
                context.summaries().size(),
                (int) context.summaries().stream().filter(summary -> APPROVAL_PENDING.equals(summary.approvalStatus())).count(),
                (int) context.summaries().stream().filter(summary -> APPROVAL_APPROVED.equals(summary.approvalStatus())).count(),
                (int) context.summaries().stream().filter(summary -> APPROVAL_REJECTED.equals(summary.approvalStatus())).count(),
                context.summaries()
        );
    }

    public TournamentDetailResponse getTournamentDetail(Long clubId, Long tournamentRecordId, String userKey) {
        requireTournamentFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        TournamentRecord tournament = getTournament(clubId, tournamentRecordId);
        validateTournamentVisible(access, tournament);
        return buildTournamentDetail(access, tournament);
    }

    public List<TournamentSummaryResponse> getTournamentSummariesForDisplay(
            ClubAccessResolver.ClubAccess access,
            List<TournamentRecord> tournaments
    ) {
        return buildTournamentContext(access, tournaments).summaries();
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "대회관리")
    public TournamentUpsertResponse createTournament(Long clubId, String userKey, UpsertTournamentRequest request) {
        requireTournamentFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        if (!clubTournamentPermissionService.canCreateTournament(access)) {
            throw new SemoException.ForbiddenException("대회를 생성할 권한이 없습니다.");
        }
        ClubTournamentSupport.TournamentDraft draft = clubTournamentSupport.toTournamentDraft(
                request,
                access
        );
        ClubActivityContextHolder.setDetails(
                "대회 '" + draft.title() + "'을 생성했습니다.",
                "대회 '" + draft.title() + "' 생성에 실패했습니다."
        );

        TournamentRecord saved = tournamentRecordRepository.save(TournamentRecord.builder()
                .clubId(clubId)
                .authorClubProfileId(access.clubProfile().getClubProfileId())
                .title(draft.title())
                .summaryText(draft.summaryText())
                .detailText(draft.detailText())
                .tournamentStatus(resolveTournamentStatus(draft.applicationStartAt(), draft.applicationEndAt(), draft.startDate(), draft.endDate(), null))
                .approvalStatus(APPROVAL_PENDING)
                .reviewedByClubProfileId(null)
                .reviewedAt(null)
                .rejectionReason(null)
                .applicationStartAt(draft.applicationStartAt())
                .applicationEndAt(draft.applicationEndAt())
                .startDate(draft.startDate())
                .endDate(draft.endDate())
                .locationLabel(draft.locationLabel())
                .matchFormat(draft.matchFormat())
                .teamMemberLimit(draft.teamMemberLimit())
                .participantLimit(draft.participantLimit())
                .feeRequired(draft.feeRequired())
                .feeAmount(draft.feeAmount())
                .feeCurrencyCode(draft.feeCurrencyCode())
                .sharedToBoard(draft.postToBoard())
                .sharedToCalendar(draft.postToCalendar())
                .pinned(draft.pinned())
                .cancelledAt(null)
                .cancelReason(null)
                .deleted(false)
                .build());
        syncTournamentShares(saved);
        return toUpsertResponse(saved);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "대회관리")
    public TournamentUpsertResponse updateTournament(
            Long clubId,
            Long tournamentRecordId,
            String userKey,
            UpsertTournamentRequest request
    ) {
        requireTournamentFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        TournamentRecord current = getTournamentForUpdate(clubId, tournamentRecordId);
        if (!APPROVAL_PENDING.equals(current.getApprovalStatus())) {
            throw new SemoException.ValidationException("승인 대기 중인 대회만 검토할 수 있습니다.");
        }
        ClubTournamentPermissionService.TournamentActionPermission permission =
                clubTournamentPermissionService.getActionPermission(access, current.getAuthorClubProfileId());
        if (!permission.canEdit()) {
            throw new SemoException.ForbiddenException("대회를 수정할 권한이 없습니다.");
        }
        ClubTournamentSupport.TournamentDraft draft = clubTournamentSupport.toTournamentDraft(
                request,
                access
        );
        ClubActivityContextHolder.setDetails(
                "대회 '" + current.getTitle() + "'을 수정했습니다.",
                "대회 '" + current.getTitle() + "' 수정에 실패했습니다."
        );
        boolean resubmitRequired = APPROVAL_REJECTED.equals(current.getApprovalStatus())
                || (APPROVAL_APPROVED.equals(current.getApprovalStatus()) && hasMaterialChanges(current, draft));

        TournamentRecord saved = tournamentRecordRepository.save(TournamentRecord.builder()
                .tournamentRecordId(current.getTournamentRecordId())
                .clubId(current.getClubId())
                .authorClubProfileId(current.getAuthorClubProfileId())
                .title(draft.title())
                .summaryText(draft.summaryText())
                .detailText(draft.detailText())
                .tournamentStatus(resolveTournamentStatus(draft.applicationStartAt(), draft.applicationEndAt(), draft.startDate(), draft.endDate(), current.getCancelledAt()))
                .approvalStatus(resubmitRequired ? APPROVAL_PENDING : current.getApprovalStatus())
                .reviewedByClubProfileId(resubmitRequired ? null : current.getReviewedByClubProfileId())
                .reviewedAt(resubmitRequired ? null : current.getReviewedAt())
                .rejectionReason(resubmitRequired ? null : current.getRejectionReason())
                .applicationStartAt(draft.applicationStartAt())
                .applicationEndAt(draft.applicationEndAt())
                .startDate(draft.startDate())
                .endDate(draft.endDate())
                .locationLabel(draft.locationLabel())
                .matchFormat(draft.matchFormat())
                .teamMemberLimit(draft.teamMemberLimit())
                .participantLimit(draft.participantLimit())
                .feeRequired(draft.feeRequired())
                .feeAmount(draft.feeAmount())
                .feeCurrencyCode(draft.feeCurrencyCode())
                .sharedToBoard(draft.postToBoard())
                .sharedToCalendar(draft.postToCalendar())
                .pinned(draft.pinned())
                .cancelledAt(current.getCancelledAt())
                .cancelReason(current.getCancelReason())
                .deleted(false)
                .build());
        syncTournamentShares(saved);
        return toUpsertResponse(saved);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "대회관리")
    public TournamentDetailResponse reviewTournament(
            Long clubId,
            Long tournamentRecordId,
            String userKey,
            ReviewTournamentRecordRequest request
    ) {
        requireTournamentFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        if (!clubTournamentPermissionService.canReviewTournament(access)) {
            throw new SemoException.ForbiddenException("대회 승인 검토 권한이 없습니다.");
        }
        TournamentRecord current = getTournament(clubId, tournamentRecordId);
        String approvalStatus = clubTournamentSupport.normalizeTournamentApprovalStatus(
                request.approvalStatus()
        );
        String rejectionReason = clubTournamentSupport.trimToNull(request.rejectionReason());
        if (APPROVAL_REJECTED.equals(approvalStatus) && rejectionReason == null) {
            throw new SemoException.ValidationException("대회 거절 사유를 입력해야 합니다.");
        }
        if (APPROVAL_APPROVED.equals(current.getApprovalStatus()) && APPROVAL_REJECTED.equals(approvalStatus)) {
            throw new SemoException.ValidationException("이미 승인된 대회는 거절 상태로 되돌릴 수 없습니다.");
        }
        ClubActivityContextHolder.setDetails(
                "대회 '" + current.getTitle() + "'을 " + approvalStatus + " 처리했습니다.",
                "대회 '" + current.getTitle() + "' 승인 검토에 실패했습니다."
        );

        TournamentRecord saved = tournamentRecordRepository.save(TournamentRecord.builder()
                .tournamentRecordId(current.getTournamentRecordId())
                .clubId(current.getClubId())
                .authorClubProfileId(current.getAuthorClubProfileId())
                .title(current.getTitle())
                .summaryText(current.getSummaryText())
                .detailText(current.getDetailText())
                .tournamentStatus(current.getTournamentStatus())
                .approvalStatus(approvalStatus)
                .reviewedByClubProfileId(access.clubProfile().getClubProfileId())
                .reviewedAt(LocalDateTime.now())
                .rejectionReason(APPROVAL_REJECTED.equals(approvalStatus) ? rejectionReason : null)
                .applicationStartAt(current.getApplicationStartAt())
                .applicationEndAt(current.getApplicationEndAt())
                .startDate(current.getStartDate())
                .endDate(current.getEndDate())
                .locationLabel(current.getLocationLabel())
                .matchFormat(current.getMatchFormat())
                .teamMemberLimit(current.getTeamMemberLimit())
                .participantLimit(current.getParticipantLimit())
                .feeRequired(current.isFeeRequired())
                .feeAmount(current.getFeeAmount())
                .feeCurrencyCode(current.getFeeCurrencyCode())
                .sharedToBoard(current.isSharedToBoard())
                .sharedToCalendar(current.isSharedToCalendar())
                .pinned(current.isPinned())
                .cancelledAt(current.getCancelledAt())
                .cancelReason(current.getCancelReason())
                .deleted(current.isDeleted())
                .build());
        syncTournamentShares(saved);
        boolean approved = APPROVAL_APPROVED.equals(approvalStatus);
        String notificationMessage = "'" + saved.getTitle() + "' 대회가 " + (approved ? "승인" : "반려") + "되었습니다.";
        if (!approved && rejectionReason != null) {
            notificationMessage += " · 사유: " + rejectionReason;
        }
        clubNotificationPublisher.notifyClubProfile(
                saved.getAuthorClubProfileId(),
                new NotificationCommand(
                        clubId,
                        "TOURNAMENT_REVIEW",
                        "대회 승인 검토가 완료되었습니다",
                        notificationMessage,
                        "TOURNAMENT",
                        saved.getTournamentRecordId(),
                        "/clubs/" + clubId + "/more/tournaments/" + saved.getTournamentRecordId(),
                        "tournament:" + saved.getTournamentRecordId() + ":" + approvalStatus
                )
        );
        return buildTournamentDetail(access, saved);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "대회관리")
    public TournamentDetailResponse cancelTournament(
            Long clubId,
            Long tournamentRecordId,
            String userKey,
            CancelTournamentRequest request
    ) {
        requireTournamentFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        TournamentRecord current = getTournament(clubId, tournamentRecordId);
        ClubTournamentPermissionService.TournamentActionPermission permission =
                clubTournamentPermissionService.getActionPermission(access, current.getAuthorClubProfileId());
        if (!permission.canCancel()) {
            throw new SemoException.ForbiddenException("대회를 취소할 권한이 없습니다.");
        }
        ClubActivityContextHolder.setDetails(
                "대회 '" + current.getTitle() + "'을 조기 취소했습니다.",
                "대회 '" + current.getTitle() + "' 취소에 실패했습니다."
        );
        TournamentRecord saved = tournamentRecordRepository.save(TournamentRecord.builder()
                .tournamentRecordId(current.getTournamentRecordId())
                .clubId(current.getClubId())
                .authorClubProfileId(current.getAuthorClubProfileId())
                .title(current.getTitle())
                .summaryText(current.getSummaryText())
                .detailText(current.getDetailText())
                .tournamentStatus(STATUS_CANCELLED)
                .approvalStatus(current.getApprovalStatus())
                .reviewedByClubProfileId(current.getReviewedByClubProfileId())
                .reviewedAt(current.getReviewedAt())
                .rejectionReason(current.getRejectionReason())
                .applicationStartAt(current.getApplicationStartAt())
                .applicationEndAt(current.getApplicationEndAt())
                .startDate(current.getStartDate())
                .endDate(current.getEndDate())
                .locationLabel(current.getLocationLabel())
                .matchFormat(current.getMatchFormat())
                .teamMemberLimit(current.getTeamMemberLimit())
                .participantLimit(current.getParticipantLimit())
                .feeRequired(current.isFeeRequired())
                .feeAmount(current.getFeeAmount())
                .feeCurrencyCode(current.getFeeCurrencyCode())
                .sharedToBoard(current.isSharedToBoard())
                .sharedToCalendar(current.isSharedToCalendar())
                .pinned(current.isPinned())
                .cancelledAt(LocalDateTime.now())
                .cancelReason(clubTournamentSupport.trimToNull(request == null ? null : request.cancelReason()))
                .deleted(false)
                .build());
        syncTournamentShares(saved);
        tournamentApplicationRepository
                .findByTournamentRecordIdOrderByCreateDateAscTournamentApplicationIdAsc(tournamentRecordId)
                .forEach(tournamentFinanceLinkService::waivePendingTournamentFee);
        return buildTournamentDetail(access, saved);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "대회관리")
    public void deleteTournament(Long clubId, Long tournamentRecordId, String userKey) {
        requireTournamentFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        if (!clubTournamentPermissionService.canDeleteTournament(access)) {
            throw new SemoException.ForbiddenException("대회를 삭제할 권한이 없습니다.");
        }
        TournamentRecord current = getTournament(clubId, tournamentRecordId);
        ClubActivityContextHolder.setDetails(
                "대회 '" + current.getTitle() + "'을 삭제했습니다.",
                "대회 '" + current.getTitle() + "' 삭제에 실패했습니다."
        );
        clubContentShareService.removeAllShares(clubId, CONTENT_TOURNAMENT, tournamentRecordId);
        tournamentRecordRepository.save(TournamentRecord.builder()
                .tournamentRecordId(current.getTournamentRecordId())
                .clubId(current.getClubId())
                .authorClubProfileId(current.getAuthorClubProfileId())
                .title(current.getTitle())
                .summaryText(current.getSummaryText())
                .detailText(current.getDetailText())
                .tournamentStatus(current.getTournamentStatus())
                .approvalStatus(current.getApprovalStatus())
                .reviewedByClubProfileId(current.getReviewedByClubProfileId())
                .reviewedAt(current.getReviewedAt())
                .rejectionReason(current.getRejectionReason())
                .applicationStartAt(current.getApplicationStartAt())
                .applicationEndAt(current.getApplicationEndAt())
                .startDate(current.getStartDate())
                .endDate(current.getEndDate())
                .locationLabel(current.getLocationLabel())
                .matchFormat(current.getMatchFormat())
                .teamMemberLimit(current.getTeamMemberLimit())
                .participantLimit(current.getParticipantLimit())
                .feeRequired(current.isFeeRequired())
                .feeAmount(current.getFeeAmount())
                .feeCurrencyCode(current.getFeeCurrencyCode())
                .sharedToBoard(false)
                .sharedToCalendar(false)
                .pinned(false)
                .cancelledAt(current.getCancelledAt())
                .cancelReason(current.getCancelReason())
                .deleted(true)
                .build());
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "대회관리")
    public TournamentDetailResponse applyToTournament(
            Long clubId,
            Long tournamentRecordId,
            String userKey,
            SubmitTournamentApplicationRequest request
    ) {
        requireTournamentFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        TournamentRecord tournament = getTournamentForUpdate(clubId, tournamentRecordId);
        validateTournamentVisible(access, tournament);
        if (!isTournamentApproved(tournament)) {
            throw new SemoException.ValidationException("아직 승인되지 않은 대회입니다.");
        }
        if (!isApplicationOpen(tournament)) {
            throw new SemoException.ValidationException("현재 참가 신청을 받을 수 없습니다.");
        }
        List<TournamentApplication> applications = tournamentApplicationRepository
                .findByTournamentRecordIdOrderByCreateDateAscTournamentApplicationIdAsc(tournamentRecordId);
        TournamentApplication current = tournamentApplicationRepository
                .findByTournamentRecordIdAndClubProfileId(tournamentRecordId, access.clubProfile().getClubProfileId())
                .orElse(null);
        if (current != null && isActiveApplicationStatus(current.getApplicationStatus())) {
            throw new SemoException.ValidationException("이미 참가 신청한 대회입니다.");
        }

        RosterDraft rosterDraft = validateRosterDraft(access, tournament, current, applications, request);
        boolean capacityFull = isCapacityFull(tournament, applications, current);
        String applicationStatus = capacityFull ? APPLICATION_WAITLISTED : APPLICATION_APPLIED;
        Integer waitlistPosition = capacityFull ? nextWaitlistPosition(applications) : null;
        String activityLabel = capacityFull ? "대기 명단에 등록했습니다." : "참가 신청했습니다.";
        ClubActivityContextHolder.setDetails(
                "대회 '" + tournament.getTitle() + "' " + activityLabel,
                "대회 '" + tournament.getTitle() + "' 참가 신청에 실패했습니다."
        );

        String applicationNote = clubTournamentSupport.trimToNull(request == null ? null : request.applicationNote());
        TournamentApplication saved;
        if (current == null) {
            saved = tournamentApplicationRepository.save(TournamentApplication.builder()
                    .tournamentRecordId(tournamentRecordId)
                    .clubProfileId(access.clubProfile().getClubProfileId())
                    .applicationStatus(applicationStatus)
                    .applicationNote(applicationNote)
                    .teamName(rosterDraft.teamName())
                    .waitlistPosition(waitlistPosition)
                    .reviewedByClubProfileId(null)
                    .reviewedAt(null)
                    .build());
        } else {
            current.markApplied(applicationStatus, applicationNote, rosterDraft.teamName(), waitlistPosition);
            saved = tournamentApplicationRepository.save(current);
        }
        replaceRoster(saved, rosterDraft.clubProfileIds());
        return buildTournamentDetail(access, tournament);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "대회관리")
    public TournamentDetailResponse cancelMyApplication(Long clubId, Long tournamentRecordId, String userKey) {
        requireTournamentFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        TournamentRecord tournament = getTournamentForUpdate(clubId, tournamentRecordId);
        validateTournamentVisible(access, tournament);
        TournamentApplication current = tournamentApplicationRepository
                .findByTournamentRecordIdAndClubProfileId(tournamentRecordId, access.clubProfile().getClubProfileId())
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("TournamentApplication", "tournamentRecordId", tournamentRecordId));
        if (!isActiveApplicationStatus(current.getApplicationStatus())) {
            throw new SemoException.ValidationException("취소할 수 있는 참가 신청이 없습니다.");
        }
        ClubActivityContextHolder.setDetails(
                "대회 '" + tournament.getTitle() + "' 참가 신청을 취소했습니다.",
                "대회 '" + tournament.getTitle() + "' 참가 신청 취소에 실패했습니다."
        );
        current.review(APPLICATION_CANCELLED, access.clubProfile().getClubProfileId(), LocalDateTime.now());
        tournamentApplicationRepository.save(current);
        tournamentFinanceLinkService.waivePendingTournamentFee(current);
        promoteWaitlist(tournament, access.clubProfile().getClubProfileId());
        return buildTournamentDetail(access, tournament);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "대회관리")
    public TournamentDetailResponse reviewApplication(
            Long clubId,
            Long tournamentRecordId,
            Long tournamentApplicationId,
            String userKey,
            ReviewTournamentApplicationRequest request
    ) {
        requireTournamentFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        TournamentRecord tournament = getTournamentForUpdate(clubId, tournamentRecordId);
        validateTournamentVisible(access, tournament);
        if (!canManageApplications(access, tournament)) {
            throw new SemoException.ForbiddenException("참가 신청을 검토할 권한이 없습니다.");
        }
        if (!isTournamentApproved(tournament)) {
            throw new SemoException.ValidationException("승인된 대회만 참가 신청을 검토할 수 있습니다.");
        }
        TournamentApplication current = tournamentApplicationRepository.findForUpdate(
                        tournamentRecordId,
                        tournamentApplicationId
                )
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("TournamentApplication", "tournamentApplicationId", tournamentApplicationId));
        String nextStatus = clubTournamentSupport.normalizeApplicationReviewStatus(
                request.applicationStatus()
        );
        if (!Set.of(APPLICATION_APPLIED, APPLICATION_WAITLISTED).contains(current.getApplicationStatus())) {
            throw new SemoException.ValidationException("검토 대기 중인 참가 신청만 처리할 수 있습니다.");
        }
        if (APPLICATION_APPROVED.equals(nextStatus)) {
            List<TournamentApplication> applications = tournamentApplicationRepository
                    .findByTournamentRecordIdOrderByCreateDateAscTournamentApplicationIdAsc(tournamentRecordId);
            if (isCapacityFull(tournament, applications, current)) {
                nextStatus = APPLICATION_WAITLISTED;
            }
        }
        ClubActivityContextHolder.setDetails(
                "대회 '" + tournament.getTitle() + "' 참가 신청을 " + nextStatus + " 처리했습니다.",
                "대회 '" + tournament.getTitle() + "' 참가 신청 검토에 실패했습니다."
        );
        current.review(nextStatus, access.clubProfile().getClubProfileId(), LocalDateTime.now());
        if (APPLICATION_WAITLISTED.equals(nextStatus)) {
            current.updateWaitlistPosition(nextWaitlistPosition(
                    tournamentApplicationRepository
                            .findByTournamentRecordIdOrderByCreateDateAscTournamentApplicationIdAsc(tournamentRecordId)
            ));
        }
        tournamentApplicationRepository.save(current);
        if (APPLICATION_APPROVED.equals(nextStatus)) {
            Long financePaymentId = tournamentFinanceLinkService.ensureTournamentFeePayment(
                    tournament,
                    current,
                    access.clubProfile().getClubProfileId()
            );
            current.linkFinancePayment(financePaymentId);
            tournamentApplicationRepository.save(current);
        } else if (APPLICATION_REJECTED.equals(nextStatus)) {
            tournamentFinanceLinkService.waivePendingTournamentFee(current);
            promoteWaitlist(tournament, access.clubProfile().getClubProfileId());
        }
        String reviewNote = clubTournamentSupport.trimToNull(request.reviewNote());
        String resultLabel = switch (nextStatus) {
            case APPLICATION_APPROVED -> "승인";
            case APPLICATION_WAITLISTED -> "대기 전환";
            default -> "반려";
        };
        String notificationMessage = "'" + tournament.getTitle() + "' 참가 신청이 " + resultLabel + "되었습니다.";
        if (reviewNote != null) {
            notificationMessage += " · " + reviewNote;
        }
        clubNotificationPublisher.notifyClubProfile(
                current.getClubProfileId(),
                new NotificationCommand(
                        clubId,
                        "TOURNAMENT_APPLICATION_REVIEW",
                        "대회 참가 신청 결과가 도착했습니다",
                        notificationMessage,
                        "TOURNAMENT_APPLICATION",
                        current.getTournamentApplicationId(),
                        "/clubs/" + clubId + "/more/tournaments/" + tournamentRecordId,
                        "tournament-application:" + current.getTournamentApplicationId() + ":" + nextStatus
                )
        );
        return buildTournamentDetail(access, tournament);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "대회관리")
    public TournamentDetailResponse createScheduleSlot(
            Long clubId,
            Long tournamentRecordId,
            String userKey,
            UpsertTournamentScheduleSlotRequest request
    ) {
        requireTournamentFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        TournamentRecord tournament = getTournament(clubId, tournamentRecordId);
        requireApplicationManager(access, tournament);
        ScheduleSlotDraft draft = validateScheduleSlot(tournament, request);
        ClubActivityContextHolder.setDetails(
                "대회 '" + tournament.getTitle() + "'에 '" + draft.title() + "' 일정을 추가했습니다.",
                "대회 일정 추가에 실패했습니다."
        );
        tournamentScheduleSlotRepository.save(TournamentScheduleSlot.builder()
                .tournamentRecordId(tournamentRecordId)
                .title(draft.title())
                .courtLabel(draft.courtLabel())
                .startAt(draft.startAt())
                .endAt(draft.endAt())
                .note(draft.note())
                .createdByClubProfileId(access.clubProfile().getClubProfileId())
                .build());
        return buildTournamentDetail(access, tournament);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "대회관리")
    public TournamentDetailResponse updateScheduleSlot(
            Long clubId,
            Long tournamentRecordId,
            Long scheduleSlotId,
            String userKey,
            UpsertTournamentScheduleSlotRequest request
    ) {
        requireTournamentFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        TournamentRecord tournament = getTournament(clubId, tournamentRecordId);
        requireApplicationManager(access, tournament);
        TournamentScheduleSlot current = tournamentScheduleSlotRepository.findForUpdate(tournamentRecordId, scheduleSlotId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "TournamentScheduleSlot",
                        "tournamentScheduleSlotId",
                        scheduleSlotId
                ));
        ScheduleSlotDraft draft = validateScheduleSlot(tournament, request);
        ClubActivityContextHolder.setDetails(
                "대회 '" + tournament.getTitle() + "'의 '" + current.getTitle() + "' 일정을 수정했습니다.",
                "대회 일정 수정에 실패했습니다."
        );
        current.update(draft.title(), draft.courtLabel(), draft.startAt(), draft.endAt(), draft.note());
        tournamentScheduleSlotRepository.save(current);
        return buildTournamentDetail(access, tournament);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "대회관리")
    public TournamentDetailResponse deleteScheduleSlot(
            Long clubId,
            Long tournamentRecordId,
            Long scheduleSlotId,
            String userKey
    ) {
        requireTournamentFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        TournamentRecord tournament = getTournament(clubId, tournamentRecordId);
        requireApplicationManager(access, tournament);
        TournamentScheduleSlot current = tournamentScheduleSlotRepository.findForUpdate(tournamentRecordId, scheduleSlotId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "TournamentScheduleSlot",
                        "tournamentScheduleSlotId",
                        scheduleSlotId
                ));
        ClubActivityContextHolder.setDetails(
                "대회 '" + tournament.getTitle() + "'의 '" + current.getTitle() + "' 일정을 삭제했습니다.",
                "대회 일정 삭제에 실패했습니다."
        );
        tournamentScheduleSlotRepository.delete(current);
        return buildTournamentDetail(access, tournament);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "대회관리")
    public TournamentDetailResponse updateApplicationOperations(
            Long clubId,
            Long tournamentRecordId,
            Long tournamentApplicationId,
            String userKey,
            UpdateTournamentApplicationOperationsRequest request
    ) {
        requireTournamentFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        TournamentRecord tournament = getTournament(clubId, tournamentRecordId);
        requireApplicationManager(access, tournament);
        TournamentApplication current = tournamentApplicationRepository
                .findForUpdate(tournamentRecordId, tournamentApplicationId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "TournamentApplication",
                        "tournamentApplicationId",
                        tournamentApplicationId
                ));
        if (!APPLICATION_APPROVED.equals(current.getApplicationStatus())) {
            throw new SemoException.ValidationException("승인된 참가자만 체크인과 결과를 기록할 수 있습니다.");
        }
        if (request == null) {
            throw new SemoException.ValidationException("참가자 운영 정보가 비어 있습니다.");
        }
        LocalDateTime checkedInAt = current.getCheckedInAt();
        Long checkedInByClubProfileId = current.getCheckedInByClubProfileId();
        if (Boolean.TRUE.equals(request.checkedIn()) && checkedInAt == null) {
            checkedInAt = LocalDateTime.now();
            checkedInByClubProfileId = access.clubProfile().getClubProfileId();
        } else if (Boolean.FALSE.equals(request.checkedIn())) {
            checkedInAt = null;
            checkedInByClubProfileId = null;
        }
        ClubActivityContextHolder.setDetails(
                "대회 '" + tournament.getTitle() + "' 참가자의 체크인·결과를 저장했습니다.",
                "대회 참가자 운영 정보 저장에 실패했습니다."
        );
        current.updateOperations(
                checkedInAt,
                checkedInByClubProfileId,
                request.placement(),
                clubTournamentSupport.trimToNull(request.resultNote())
        );
        tournamentApplicationRepository.save(current);
        return buildTournamentDetail(access, tournament);
    }

    private TournamentDetailResponse buildTournamentDetail(ClubAccessResolver.ClubAccess access, TournamentRecord tournament) {
        List<TournamentApplication> applications = tournamentApplicationRepository
                .findByTournamentRecordIdOrderByCreateDateAscTournamentApplicationIdAsc(tournament.getTournamentRecordId());
        List<Long> applicationIds = applications.stream()
                .map(TournamentApplication::getTournamentApplicationId)
                .toList();
        List<TournamentRosterMember> rosterMembers = applicationIds.isEmpty()
                ? List.of()
                : tournamentRosterMemberRepository
                        .findByTournamentApplicationIdInOrderBySortOrderAscTournamentRosterMemberIdAsc(applicationIds);
        Map<Long, List<TournamentRosterMember>> rosterByApplicationId = rosterMembers.stream()
                .collect(Collectors.groupingBy(
                        TournamentRosterMember::getTournamentApplicationId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        Set<Long> clubProfileIds = new HashSet<>();
        clubProfileIds.add(tournament.getAuthorClubProfileId());
        if (tournament.getReviewedByClubProfileId() != null) {
            clubProfileIds.add(tournament.getReviewedByClubProfileId());
        }
        applications.forEach(application -> {
            clubProfileIds.add(application.getClubProfileId());
            if (application.getReviewedByClubProfileId() != null) {
                clubProfileIds.add(application.getReviewedByClubProfileId());
            }
        });
        rosterMembers.forEach(member -> clubProfileIds.add(member.getClubProfileId()));
        Map<Long, ClubProfile> profileById = loadClubProfiles(clubProfileIds);

        ClubTournamentPermissionService.TournamentActionPermission actionPermission =
                clubTournamentPermissionService.getActionPermission(access, tournament.getAuthorClubProfileId());
        TournamentViewerState viewerState = resolveViewerState(access, tournament, applications);
        boolean canReviewTournament = clubTournamentPermissionService.canReviewTournament(access);
        boolean canManageApplications = canManageApplications(access, tournament);
        List<TournamentApplication> approvedApplications = applications.stream()
                .filter(application -> APPLICATION_APPROVED.equals(application.getApplicationStatus()))
                .toList();
        List<TournamentScheduleSlotResponse> scheduleSlots = tournamentScheduleSlotRepository
                .findByTournamentRecordIdOrderByStartAtAscTournamentScheduleSlotIdAsc(tournament.getTournamentRecordId())
                .stream()
                .map(this::toScheduleSlotResponse)
                .toList();
        List<TournamentRosterOptionResponse> availableRosterMembers = loadAvailableRosterMembers(access.club().getClubId());
        int activeApplicantCount = (int) applications.stream()
                .filter(application -> isActiveApplicationStatus(application.getApplicationStatus()))
                .count();

        return new TournamentDetailResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                access.clubProfile().getClubProfileId(),
                tournament.getTournamentRecordId(),
                tournament.getTitle(),
                tournament.getSummaryText(),
                tournament.getDetailText(),
                tournament.getApprovalStatus(),
                resolveTournamentStatus(tournament),
                clubTournamentSupport.resolveDisplayName(profileById.get(tournament.getAuthorClubProfileId())),
                clubTournamentSupport.resolveAvatarImageUrl(profileById.get(tournament.getAuthorClubProfileId())),
                clubTournamentSupport.resolveAvatarThumbnailUrl(profileById.get(tournament.getAuthorClubProfileId())),
                clubTournamentSupport.resolveDisplayName(profileById.get(tournament.getReviewedByClubProfileId())),
                clubTournamentSupport.formatDateTimeLabel(tournament.getReviewedAt()),
                tournament.getRejectionReason(),
                clubTournamentSupport.formatDateTime(tournament.getApplicationStartAt()),
                clubTournamentSupport.formatDateTime(tournament.getApplicationEndAt()),
                clubTournamentSupport.formatApplicationWindowLabel(tournament),
                clubTournamentSupport.formatDate(tournament.getStartDate()),
                clubTournamentSupport.formatDate(tournament.getEndDate()),
                clubTournamentSupport.formatTournamentPeriodLabel(tournament),
                tournament.getLocationLabel(),
                tournament.getMatchFormat(),
                tournament.getTeamMemberLimit(),
                tournament.getParticipantLimit(),
                tournament.isFeeRequired(),
                tournament.getFeeAmount(),
                tournament.getFeeCurrencyCode(),
                tournament.isSharedToBoard(),
                tournament.isSharedToCalendar(),
                tournament.isPinned(),
                clubTournamentSupport.formatDateTimeLabel(tournament.getCancelledAt()),
                tournament.getCancelReason(),
                activeApplicantCount,
                approvedApplications.size(),
                approvedApplications.size(),
                isApplicationOpen(tournament),
                !viewerState.applied() && isApplicationOpen(tournament),
                viewerState.applied(),
                viewerState.applicationStatus(),
                viewerState.participating(),
                canReviewTournament,
                actionPermission.canEdit(),
                actionPermission.canCancel(),
                clubTournamentPermissionService.canDeleteTournament(access),
                canManageApplications,
                (canManageApplications ? applications : List.<TournamentApplication>of()).stream()
                        .map(application -> toApplicationSummary(
                                access,
                                application,
                                profileById.get(application.getClubProfileId()),
                                canManageApplications,
                                toRosterResponses(rosterByApplicationId.getOrDefault(
                                        application.getTournamentApplicationId(),
                                        List.of()
                                ), profileById)
                        ))
                        .toList(),
                approvedApplications.stream()
                        .map(application -> toParticipantSummary(
                                application,
                                profileById.get(application.getClubProfileId()),
                                toRosterResponses(rosterByApplicationId.getOrDefault(
                                        application.getTournamentApplicationId(),
                                        List.of()
                                ), profileById)
                        ))
                        .toList(),
                scheduleSlots,
                availableRosterMembers
        );
    }

    private TournamentContext buildTournamentContext(ClubAccessResolver.ClubAccess access, List<TournamentRecord> tournaments) {
        List<TournamentRecord> visibleTournaments = tournaments.stream()
                .filter(tournament -> canViewTournament(access, tournament))
                .toList();
        List<TournamentApplication> applications = visibleTournaments.isEmpty()
                ? List.of()
                : tournamentApplicationRepository.findByTournamentRecordIdIn(visibleTournaments.stream().map(TournamentRecord::getTournamentRecordId).toList());
        Map<Long, List<TournamentApplication>> applicationsByTournamentId = applications.stream()
                .collect(Collectors.groupingBy(TournamentApplication::getTournamentRecordId));
        Set<Long> profileIds = new HashSet<>();
        visibleTournaments.forEach(tournament -> {
            profileIds.add(tournament.getAuthorClubProfileId());
            if (tournament.getReviewedByClubProfileId() != null) {
                profileIds.add(tournament.getReviewedByClubProfileId());
            }
        });
        applications.forEach(application -> profileIds.add(application.getClubProfileId()));
        Map<Long, ClubProfile> profileById = loadClubProfiles(profileIds);

        List<TournamentSummaryResponse> summaries = visibleTournaments.stream()
                .map(tournament -> {
                    List<TournamentApplication> tournamentApplications = applicationsByTournamentId.getOrDefault(tournament.getTournamentRecordId(), List.of());
                    ClubTournamentPermissionService.TournamentActionPermission permission =
                            clubTournamentPermissionService.getActionPermission(access, tournament.getAuthorClubProfileId());
                    TournamentViewerState viewerState = resolveViewerState(access, tournament, tournamentApplications);
                    int approvedApplicationCount = (int) tournamentApplications.stream()
                            .filter(application -> APPLICATION_APPROVED.equals(application.getApplicationStatus()))
                            .count();
                    return new TournamentSummaryResponse(
                            tournament.getTournamentRecordId(),
                            tournament.getTitle(),
                            tournament.getSummaryText(),
                            tournament.getApprovalStatus(),
                            resolveTournamentStatus(tournament),
                            clubTournamentSupport.resolveDisplayName(profileById.get(tournament.getAuthorClubProfileId())),
                            clubTournamentSupport.resolveAvatarImageUrl(profileById.get(tournament.getAuthorClubProfileId())),
                            clubTournamentSupport.resolveAvatarThumbnailUrl(profileById.get(tournament.getAuthorClubProfileId())),
                            clubTournamentSupport.formatApplicationWindowLabel(tournament),
                            clubTournamentSupport.formatTournamentPeriodLabel(tournament),
                            clubTournamentSupport.formatDate(tournament.getStartDate()),
                            clubTournamentSupport.formatDate(tournament.getEndDate()),
                            tournament.getLocationLabel(),
                            tournament.getMatchFormat(),
                            tournament.getTeamMemberLimit(),
                            tournament.getParticipantLimit(),
                            approvedApplicationCount,
                            approvedApplicationCount,
                            tournament.isFeeRequired(),
                            tournament.getFeeAmount(),
                            tournament.getFeeCurrencyCode(),
                            tournament.isSharedToBoard(),
                            tournament.isSharedToCalendar(),
                            tournament.isPinned(),
                            viewerState.mine(),
                            viewerState.participating(),
                            permission.canEdit(),
                            permission.canCancel(),
                            clubTournamentPermissionService.canDeleteTournament(access)
                    );
                })
                .toList();

        return new TournamentContext(summaries);
    }

    private TournamentApplicationSummaryResponse toApplicationSummary(
            ClubAccessResolver.ClubAccess access,
            TournamentApplication application,
            ClubProfile profile,
            boolean canManageApplications,
            List<TournamentRosterMemberResponse> rosterMembers
    ) {
        boolean mine = access.clubProfile().getClubProfileId().equals(application.getClubProfileId());
        FinancePayment payment = tournamentFinanceLinkService.findPayment(application.getFinancePaymentId());
        return new TournamentApplicationSummaryResponse(
                application.getTournamentApplicationId(),
                application.getClubProfileId(),
                clubTournamentSupport.resolveDisplayName(profile),
                clubTournamentSupport.resolveAvatarImageUrl(profile),
                clubTournamentSupport.resolveAvatarThumbnailUrl(profile),
                application.getApplicationStatus(),
                application.getApplicationNote(),
                application.getTeamName(),
                application.getWaitlistPosition(),
                application.getFinancePaymentId(),
                payment == null ? null : payment.getPaymentStatusCode(),
                resolvePaymentStatusLabel(payment),
                clubTournamentSupport.formatDateTimeLabel(application.getCheckedInAt()),
                application.getPlacement(),
                application.getResultNote(),
                rosterMembers,
                clubTournamentSupport.formatDateTimeLabel(application.getCreateDate()),
                mine,
                canManageApplications,
                mine && isActiveApplicationStatus(application.getApplicationStatus())
        );
    }

    private TournamentParticipantSummaryResponse toParticipantSummary(
            TournamentApplication application,
            ClubProfile profile,
            List<TournamentRosterMemberResponse> rosterMembers
    ) {
        FinancePayment payment = tournamentFinanceLinkService.findPayment(application.getFinancePaymentId());
        return new TournamentParticipantSummaryResponse(
                application.getClubProfileId(),
                clubTournamentSupport.resolveDisplayName(profile),
                clubTournamentSupport.resolveAvatarImageUrl(profile),
                clubTournamentSupport.resolveAvatarThumbnailUrl(profile),
                clubTournamentSupport.formatDateTimeLabel(application.getReviewedAt()),
                application.getTeamName(),
                application.getFinancePaymentId(),
                payment == null ? null : payment.getPaymentStatusCode(),
                resolvePaymentStatusLabel(payment),
                clubTournamentSupport.formatDateTimeLabel(application.getCheckedInAt()),
                application.getPlacement(),
                application.getResultNote(),
                rosterMembers
        );
    }

    private TournamentViewerState resolveViewerState(
            ClubAccessResolver.ClubAccess access,
            TournamentRecord tournament,
            List<TournamentApplication> applications
    ) {
        Long viewerClubProfileId = access.clubProfile().getClubProfileId();
        TournamentApplication myApplication = applications.stream()
                .filter(application -> application.getClubProfileId().equals(viewerClubProfileId))
                .findFirst()
                .orElse(null);
        boolean participating = myApplication != null
                && APPLICATION_APPROVED.equals(myApplication.getApplicationStatus());
        boolean applied = myApplication != null
                && isActiveApplicationStatus(myApplication.getApplicationStatus());
        return new TournamentViewerState(
                access.clubProfile().getClubProfileId().equals(tournament.getAuthorClubProfileId()),
                applied,
                myApplication == null ? null : myApplication.getApplicationStatus(),
                participating
        );
    }

    private RosterDraft validateRosterDraft(
            ClubAccessResolver.ClubAccess access,
            TournamentRecord tournament,
            TournamentApplication current,
            List<TournamentApplication> applications,
            SubmitTournamentApplicationRequest request
    ) {
        Long applicantClubProfileId = access.clubProfile().getClubProfileId();
        List<Long> requestedIds = request == null || request.rosterClubProfileIds() == null
                ? new ArrayList<>()
                : new ArrayList<>(request.rosterClubProfileIds());
        requestedIds.removeIf(Objects::isNull);
        requestedIds.add(0, applicantClubProfileId);
        List<Long> clubProfileIds = requestedIds.stream().distinct().toList();
        String teamName = clubTournamentSupport.trimToNull(request == null ? null : request.teamName());

        switch (tournament.getMatchFormat()) {
            case "SINGLE" -> {
                if (clubProfileIds.size() != 1) {
                    throw new SemoException.ValidationException("개인전에는 신청자 본인만 등록할 수 있습니다.");
                }
                teamName = null;
            }
            case "DOUBLE" -> {
                if (clubProfileIds.size() != 2) {
                    throw new SemoException.ValidationException("복식은 신청자를 포함해 정확히 2명을 선택해야 합니다.");
                }
                if (teamName == null) {
                    throw new SemoException.ValidationException("복식 팀 이름을 입력해야 합니다.");
                }
            }
            case "TEAM" -> {
                int limit = tournament.getTeamMemberLimit() == null ? 3 : tournament.getTeamMemberLimit();
                if (clubProfileIds.size() < 3 || clubProfileIds.size() > limit) {
                    throw new SemoException.ValidationException("단체전은 신청자를 포함해 3명 이상 " + limit + "명 이하로 구성해야 합니다.");
                }
                if (teamName == null) {
                    throw new SemoException.ValidationException("단체전 팀 이름을 입력해야 합니다.");
                }
            }
            default -> throw new SemoException.ValidationException("지원하지 않는 경기 형식입니다.");
        }

        List<ClubProfile> selectedProfiles = clubProfileRepository.findAllById(clubProfileIds);
        Set<Long> activeClubMemberIds = clubMemberRepository
                .findByClubIdAndMembershipStatusOrderByJoinedAtAscClubMemberIdAsc(access.club().getClubId(), "ACTIVE")
                .stream()
                .map(ClubMember::getClubMemberId)
                .collect(Collectors.toSet());
        boolean invalidMember = selectedProfiles.size() != clubProfileIds.size()
                || selectedProfiles.stream().anyMatch(profile -> !activeClubMemberIds.contains(profile.getClubMemberId()));
        if (invalidMember) {
            throw new SemoException.ValidationException("현재 클럽의 활성 멤버만 로스터에 등록할 수 있습니다.");
        }

        Set<Long> conflictingProfileIds = findConflictingRosterProfileIds(current, applications, clubProfileIds);
        if (!conflictingProfileIds.isEmpty()) {
            String names = selectedProfiles.stream()
                    .filter(profile -> conflictingProfileIds.contains(profile.getClubProfileId()))
                    .map(ClubProfile::getDisplayName)
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new SemoException.ValidationException("이미 다른 참가 신청에 포함된 멤버가 있습니다: " + names);
        }
        return new RosterDraft(teamName, clubProfileIds);
    }

    private Set<Long> findConflictingRosterProfileIds(
            TournamentApplication current,
            List<TournamentApplication> applications,
            List<Long> requestedProfileIds
    ) {
        List<TournamentApplication> otherActiveApplications = applications.stream()
                .filter(application -> current == null
                        || !application.getTournamentApplicationId().equals(current.getTournamentApplicationId()))
                .filter(application -> isActiveApplicationStatus(application.getApplicationStatus()))
                .toList();
        Set<Long> occupiedProfileIds = otherActiveApplications.stream()
                .map(TournamentApplication::getClubProfileId)
                .collect(Collectors.toSet());
        List<Long> otherApplicationIds = otherActiveApplications.stream()
                .map(TournamentApplication::getTournamentApplicationId)
                .toList();
        if (!otherApplicationIds.isEmpty()) {
            tournamentRosterMemberRepository
                    .findByTournamentApplicationIdInOrderBySortOrderAscTournamentRosterMemberIdAsc(otherApplicationIds)
                    .forEach(member -> occupiedProfileIds.add(member.getClubProfileId()));
        }
        return requestedProfileIds.stream()
                .filter(occupiedProfileIds::contains)
                .collect(Collectors.toSet());
    }

    private void replaceRoster(TournamentApplication application, List<Long> clubProfileIds) {
        tournamentRosterMemberRepository.deleteByTournamentApplicationId(application.getTournamentApplicationId());
        List<TournamentRosterMember> rosterMembers = new ArrayList<>();
        for (int index = 0; index < clubProfileIds.size(); index++) {
            rosterMembers.add(TournamentRosterMember.builder()
                    .tournamentApplicationId(application.getTournamentApplicationId())
                    .clubProfileId(clubProfileIds.get(index))
                    .rosterRoleCode(index == 0 ? "CAPTAIN" : "MEMBER")
                    .sortOrder(index)
                    .build());
        }
        tournamentRosterMemberRepository.saveAll(rosterMembers);
    }

    private boolean isCapacityFull(
            TournamentRecord tournament,
            List<TournamentApplication> applications,
            TournamentApplication excluded
    ) {
        if (tournament.getParticipantLimit() == null) {
            return false;
        }
        long occupiedCount = applications.stream()
                .filter(application -> excluded == null
                        || !application.getTournamentApplicationId().equals(excluded.getTournamentApplicationId()))
                .filter(application -> Set.of(APPLICATION_APPLIED, APPLICATION_APPROVED)
                        .contains(application.getApplicationStatus()))
                .count();
        return occupiedCount >= tournament.getParticipantLimit();
    }

    private int nextWaitlistPosition(List<TournamentApplication> applications) {
        return applications.stream()
                .filter(application -> APPLICATION_WAITLISTED.equals(application.getApplicationStatus()))
                .map(TournamentApplication::getWaitlistPosition)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private void promoteWaitlist(TournamentRecord tournament, Long actorClubProfileId) {
        List<TournamentApplication> applications = tournamentApplicationRepository
                .findByTournamentRecordIdOrderByCreateDateAscTournamentApplicationIdAsc(tournament.getTournamentRecordId());
        long occupiedCount = applications.stream()
                .filter(application -> Set.of(APPLICATION_APPLIED, APPLICATION_APPROVED)
                        .contains(application.getApplicationStatus()))
                .count();
        long availableCount = tournament.getParticipantLimit() == null
                ? Long.MAX_VALUE
                : Math.max(0, tournament.getParticipantLimit() - occupiedCount);
        List<TournamentApplication> waitlisted = applications.stream()
                .filter(application -> APPLICATION_WAITLISTED.equals(application.getApplicationStatus()))
                .sorted((left, right) -> {
                    int leftPosition = left.getWaitlistPosition() == null ? Integer.MAX_VALUE : left.getWaitlistPosition();
                    int rightPosition = right.getWaitlistPosition() == null ? Integer.MAX_VALUE : right.getWaitlistPosition();
                    int positionComparison = Integer.compare(leftPosition, rightPosition);
                    if (positionComparison != 0) {
                        return positionComparison;
                    }
                    return Long.compare(left.getTournamentApplicationId(), right.getTournamentApplicationId());
                })
                .toList();
        int nextPosition = 1;
        for (TournamentApplication application : waitlisted) {
            if (availableCount > 0) {
                application.markApplied(
                        APPLICATION_APPLIED,
                        application.getApplicationNote(),
                        application.getTeamName(),
                        null
                );
                tournamentApplicationRepository.save(application);
                clubNotificationPublisher.notifyClubProfile(
                        application.getClubProfileId(),
                        new NotificationCommand(
                                tournament.getClubId(),
                                "TOURNAMENT_WAITLIST_PROMOTED",
                                "대회 참가 신청이 접수되었습니다",
                                "'" + tournament.getTitle() + "' 대기 순번이 해제되어 참가 신청 검토 대상으로 전환되었습니다.",
                                "TOURNAMENT_APPLICATION",
                                application.getTournamentApplicationId(),
                                "/clubs/" + tournament.getClubId() + "/more/tournaments/" + tournament.getTournamentRecordId(),
                                "tournament-waitlist-promoted:" + application.getTournamentApplicationId() + ":" + actorClubProfileId
                        )
                );
                availableCount--;
            } else {
                application.updateWaitlistPosition(nextPosition++);
                tournamentApplicationRepository.save(application);
            }
        }
    }

    private boolean isActiveApplicationStatus(String applicationStatus) {
        return Set.of(APPLICATION_APPLIED, APPLICATION_WAITLISTED, APPLICATION_APPROVED)
                .contains(applicationStatus);
    }

    private void requireApplicationManager(ClubAccessResolver.ClubAccess access, TournamentRecord tournament) {
        validateTournamentVisible(access, tournament);
        if (!canManageApplications(access, tournament)) {
            throw new SemoException.ForbiddenException("대회 참가자와 일정을 운영할 권한이 없습니다.");
        }
    }

    private ScheduleSlotDraft validateScheduleSlot(
            TournamentRecord tournament,
            UpsertTournamentScheduleSlotRequest request
    ) {
        if (request == null) {
            throw new SemoException.ValidationException("대회 일정 요청이 비어 있습니다.");
        }
        String title = clubTournamentSupport.trimToNull(request.title());
        if (title == null) {
            throw new SemoException.ValidationException("대회 일정 제목을 입력해야 합니다.");
        }
        LocalDateTime startAt = clubTournamentSupport.parseDateTime(request.startAt());
        LocalDateTime endAt = clubTournamentSupport.parseDateTime(request.endAt());
        if (!endAt.isAfter(startAt)) {
            throw new SemoException.ValidationException("대회 일정 종료 시각은 시작 시각 이후여야 합니다.");
        }
        LocalDateTime tournamentStartAt = tournament.getStartDate().atStartOfDay();
        LocalDateTime tournamentEndAt = tournament.getEndDate().plusDays(1).atStartOfDay();
        if (startAt.isBefore(tournamentStartAt) || endAt.isAfter(tournamentEndAt)) {
            throw new SemoException.ValidationException("대회 일정은 대회 기간 안에서만 등록할 수 있습니다.");
        }
        return new ScheduleSlotDraft(
                title,
                clubTournamentSupport.trimToNull(request.courtLabel()),
                startAt,
                endAt,
                clubTournamentSupport.trimToNull(request.note())
        );
    }

    private List<TournamentRosterMemberResponse> toRosterResponses(
            List<TournamentRosterMember> rosterMembers,
            Map<Long, ClubProfile> profileById
    ) {
        return rosterMembers.stream()
                .map(member -> {
                    ClubProfile profile = profileById.get(member.getClubProfileId());
                    return new TournamentRosterMemberResponse(
                            member.getClubProfileId(),
                            clubTournamentSupport.resolveDisplayName(profile),
                            clubTournamentSupport.resolveAvatarImageUrl(profile),
                            clubTournamentSupport.resolveAvatarThumbnailUrl(profile),
                            member.getRosterRoleCode()
                    );
                })
                .toList();
    }

    private List<TournamentRosterOptionResponse> loadAvailableRosterMembers(Long clubId) {
        return clubAccessResolver.getActiveMemberSnapshots(clubId).stream()
                .map(snapshot -> new TournamentRosterOptionResponse(
                        snapshot.clubProfile().getClubProfileId(),
                        snapshot.clubProfile().getDisplayName(),
                        clubTournamentSupport.resolveAvatarImageUrl(snapshot.clubProfile()),
                        clubTournamentSupport.resolveAvatarThumbnailUrl(snapshot.clubProfile())
                ))
                .toList();
    }

    private TournamentScheduleSlotResponse toScheduleSlotResponse(TournamentScheduleSlot slot) {
        return new TournamentScheduleSlotResponse(
                slot.getTournamentScheduleSlotId(),
                slot.getTitle(),
                slot.getCourtLabel(),
                clubTournamentSupport.formatDateTime(slot.getStartAt()),
                clubTournamentSupport.formatDateTimeLabel(slot.getStartAt()),
                clubTournamentSupport.formatDateTime(slot.getEndAt()),
                clubTournamentSupport.formatDateTimeLabel(slot.getEndAt()),
                slot.getNote()
        );
    }

    private String resolvePaymentStatusLabel(FinancePayment payment) {
        if (payment == null) {
            return null;
        }
        return switch (payment.getPaymentStatusCode()) {
            case "PENDING" -> "납부 대기";
            case "PAID" -> "납부 완료";
            case "WAIVED" -> "면제";
            default -> payment.getPaymentStatusCode();
        };
    }

    private TournamentUpsertResponse toUpsertResponse(TournamentRecord tournament) {
        return new TournamentUpsertResponse(
                tournament.getTournamentRecordId(),
                tournament.getTitle(),
                clubTournamentSupport.formatDate(tournament.getStartDate()),
                clubTournamentSupport.formatDate(tournament.getEndDate()),
                tournament.getApprovalStatus(),
                resolveTournamentStatus(tournament)
        );
    }

    private void syncTournamentShares(TournamentRecord tournament) {
        boolean visible = isTournamentApproved(tournament) && !tournament.isDeleted();
        clubContentShareService.syncBoardShare(
                tournament.getClubId(),
                CONTENT_TOURNAMENT,
                tournament.getTournamentRecordId(),
                visible && tournament.isSharedToBoard()
        );
        clubContentShareService.syncCalendarShare(
                tournament.getClubId(),
                CONTENT_TOURNAMENT,
                tournament.getTournamentRecordId(),
                visible && tournament.isSharedToCalendar()
        );
    }

    private TournamentRecord getTournament(Long clubId, Long tournamentRecordId) {
        return tournamentRecordRepository.findByTournamentRecordIdAndClubIdAndDeletedFalse(tournamentRecordId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("TournamentRecord", "tournamentRecordId", tournamentRecordId));
    }

    private TournamentRecord getTournamentForUpdate(Long clubId, Long tournamentRecordId) {
        return tournamentRecordRepository.findForUpdate(tournamentRecordId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("TournamentRecord", "tournamentRecordId", tournamentRecordId));
    }

    private boolean hasMaterialChanges(
            TournamentRecord current,
            ClubTournamentSupport.TournamentDraft draft
    ) {
        return !Objects.equals(current.getTitle(), draft.title())
                || !Objects.equals(current.getSummaryText(), draft.summaryText())
                || !Objects.equals(current.getDetailText(), draft.detailText())
                || !Objects.equals(current.getApplicationStartAt(), draft.applicationStartAt())
                || !Objects.equals(current.getApplicationEndAt(), draft.applicationEndAt())
                || !Objects.equals(current.getStartDate(), draft.startDate())
                || !Objects.equals(current.getEndDate(), draft.endDate())
                || !Objects.equals(current.getLocationLabel(), draft.locationLabel())
                || !Objects.equals(current.getMatchFormat(), draft.matchFormat())
                || !Objects.equals(current.getTeamMemberLimit(), draft.teamMemberLimit())
                || !Objects.equals(current.getParticipantLimit(), draft.participantLimit())
                || current.isFeeRequired() != draft.feeRequired()
                || !Objects.equals(current.getFeeAmount(), draft.feeAmount())
                || !Objects.equals(current.getFeeCurrencyCode(), draft.feeCurrencyCode());
    }

    private Map<Long, ClubProfile> loadClubProfiles(Collection<Long> clubProfileIds) {
        if (clubProfileIds == null || clubProfileIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, ClubProfile> result = new HashMap<>();
        clubProfileRepository.findAllById(clubProfileIds).forEach(profile -> result.put(profile.getClubProfileId(), profile));
        return result;
    }

    private void requireTournamentFeature(Long clubId) {
        if (!clubTournamentPermissionService.isTournamentEnabled(clubId)) {
            throw new SemoException.ValidationException("대회 기능이 활성화되지 않았습니다.");
        }
    }

    private boolean isApplicationOpen(TournamentRecord tournament) {
        if (!isTournamentApproved(tournament)) {
            return false;
        }
        String status = resolveTournamentStatus(tournament);
        return STATUS_APPLICATION_OPEN.equals(status);
    }

    private boolean isArchived(String status) {
        return STATUS_COMPLETED.equals(status) || STATUS_CANCELLED.equals(status);
    }

    private String resolveTournamentStatus(TournamentRecord tournament) {
        if (!isTournamentApproved(tournament) && tournament.getCancelledAt() == null) {
            return STATUS_DRAFT;
        }
        return resolveTournamentStatus(
                tournament.getApplicationStartAt(),
                tournament.getApplicationEndAt(),
                tournament.getStartDate(),
                tournament.getEndDate(),
                tournament.getCancelledAt()
        );
    }

    private String resolveTournamentStatus(
            LocalDateTime applicationStartAt,
            LocalDateTime applicationEndAt,
            LocalDate startDate,
            LocalDate endDate,
            LocalDateTime cancelledAt
    ) {
        if (cancelledAt != null) {
            return STATUS_CANCELLED;
        }
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        if (today.isAfter(endDate)) {
            return STATUS_COMPLETED;
        }
        if (!today.isBefore(startDate) && !today.isAfter(endDate)) {
            return STATUS_ONGOING;
        }
        if (!now.isBefore(applicationStartAt) && !now.isAfter(applicationEndAt)) {
            return STATUS_APPLICATION_OPEN;
        }
        if (today.isBefore(startDate)) {
            return STATUS_ENTRY_CONFIRMED;
        }
        return STATUS_DRAFT;
    }

    private boolean isTournamentApproved(TournamentRecord tournament) {
        return tournament != null && APPROVAL_APPROVED.equals(tournament.getApprovalStatus());
    }

    private boolean canViewTournament(ClubAccessResolver.ClubAccess access, TournamentRecord tournament) {
        return isTournamentApproved(tournament)
                || access.isAdmin()
                || clubTournamentPermissionService.canReviewTournament(access)
                || clubTournamentPermissionService.canDeleteTournament(access)
                || access.clubProfile().getClubProfileId().equals(tournament.getAuthorClubProfileId());
    }

    private void requireTournamentAdminToolAccess(boolean canReview, boolean canDelete) {
        if (!canReview && !canDelete) {
            throw new SemoException.ForbiddenException("대회 운영 도구에 접근할 권한이 없습니다.");
        }
    }

    private void validateTournamentVisible(ClubAccessResolver.ClubAccess access, TournamentRecord tournament) {
        if (!canViewTournament(access, tournament)) {
            throw new SemoException.ForbiddenException("해당 대회를 조회할 수 없습니다.");
        }
    }

    private boolean canManageApplications(ClubAccessResolver.ClubAccess access, TournamentRecord tournament) {
        return access.isAdmin()
                || access.clubProfile().getClubProfileId().equals(tournament.getAuthorClubProfileId());
    }

    private record TournamentViewerState(
            boolean mine,
            boolean applied,
            String applicationStatus,
            boolean participating
    ) {
    }

    private record TournamentContext(
            List<TournamentSummaryResponse> summaries
    ) {
    }

    private record RosterDraft(
            String teamName,
            List<Long> clubProfileIds
    ) {
    }

    private record ScheduleSlotDraft(
            String title,
            String courtLabel,
            LocalDateTime startAt,
            LocalDateTime endAt,
            String note
    ) {
    }
}
