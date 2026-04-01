package semo.back.service.feature.tournament.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.common.util.ImageFileUrlResolver;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.TournamentApplication;
import semo.back.service.database.pub.entity.TournamentRecord;
import semo.back.service.database.pub.repository.BracketParticipantRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.TournamentApplicationRepository;
import semo.back.service.database.pub.repository.TournamentRecordRepository;
import semo.back.service.feature.activity.biz.ClubActivityContextHolder;
import semo.back.service.feature.activity.biz.RecordClubActivity;
import semo.back.service.feature.club.biz.ClubAccessResolver;
import semo.back.service.feature.share.biz.ClubContentShareService;
import semo.back.service.feature.tournament.vo.CancelTournamentRequest;
import semo.back.service.feature.tournament.vo.ClubAdminTournamentHomeResponse;
import semo.back.service.feature.tournament.vo.ClubTournamentHomeResponse;
import semo.back.service.feature.tournament.vo.ReviewTournamentApplicationRequest;
import semo.back.service.feature.tournament.vo.ReviewTournamentRecordRequest;
import semo.back.service.feature.tournament.vo.SubmitTournamentApplicationRequest;
import semo.back.service.feature.tournament.vo.TournamentApplicationSummaryResponse;
import semo.back.service.feature.tournament.vo.TournamentDetailResponse;
import semo.back.service.feature.tournament.vo.TournamentParticipantSummaryResponse;
import semo.back.service.feature.tournament.vo.TournamentSummaryResponse;
import semo.back.service.feature.tournament.vo.TournamentUpsertResponse;
import semo.back.service.feature.tournament.vo.UpsertTournamentRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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

    private static final String MATCH_FORMAT_SINGLE = "SINGLE";
    private static final String MATCH_FORMAT_DOUBLE = "DOUBLE";
    private static final String MATCH_FORMAT_TEAM = "TEAM";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_TIME_LABEL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREAN);
    private static final DateTimeFormatter DATE_LABEL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREAN);

    private final TournamentRecordRepository tournamentRecordRepository;
    private final TournamentApplicationRepository tournamentApplicationRepository;
    private final BracketParticipantRepository bracketParticipantRepository;
    private final ClubProfileRepository clubProfileRepository;
    private final ClubAccessResolver clubAccessResolver;
    private final ClubTournamentPermissionService clubTournamentPermissionService;
    private final ClubContentShareService clubContentShareService;
    private final ImageFileUrlResolver imageFileUrlResolver;

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
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        List<TournamentRecord> tournaments = tournamentRecordRepository
                .findByClubIdAndDeletedFalseOrderByPinnedDescStartDateAscTournamentRecordIdDesc(clubId);
        TournamentContext context = buildTournamentContext(access, tournaments);

        return new ClubAdminTournamentHomeResponse(
                access.club().getClubId(),
                access.club().getName(),
                true,
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
        TournamentDraft draft = toTournamentDraft(request, access, null);
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
        TournamentRecord current = getTournament(clubId, tournamentRecordId);
        ClubTournamentPermissionService.TournamentActionPermission permission =
                clubTournamentPermissionService.getActionPermission(access, current.getAuthorClubProfileId());
        if (!permission.canEdit()) {
            throw new SemoException.ForbiddenException("대회를 수정할 권한이 없습니다.");
        }
        TournamentDraft draft = toTournamentDraft(request, access, current);
        ClubActivityContextHolder.setDetails(
                "대회 '" + current.getTitle() + "'을 수정했습니다.",
                "대회 '" + current.getTitle() + "' 수정에 실패했습니다."
        );
        boolean resubmitRequired = APPROVAL_REJECTED.equals(current.getApprovalStatus());

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
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        if (!clubTournamentPermissionService.canReviewTournament(access)) {
            throw new SemoException.ForbiddenException("대회 승인 검토 권한이 없습니다.");
        }
        TournamentRecord current = getTournament(clubId, tournamentRecordId);
        String approvalStatus = normalizeTournamentApprovalStatus(request.approvalStatus());
        String rejectionReason = trimToNull(request.rejectionReason());
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
                .cancelReason(trimToNull(request == null ? null : request.cancelReason()))
                .deleted(false)
                .build());
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
        List<Long> tournamentApplicationIds = tournamentApplicationRepository
                .findByTournamentRecordIdOrderByCreateDateAscTournamentApplicationIdAsc(tournamentRecordId).stream()
                .map(TournamentApplication::getTournamentApplicationId)
                .toList();
        if (!tournamentApplicationIds.isEmpty()) {
            bracketParticipantRepository.clearSourceTournamentApplicationIds(tournamentApplicationIds);
        }
        tournamentApplicationRepository.deleteByTournamentRecordId(tournamentRecordId);
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
        TournamentRecord tournament = getTournament(clubId, tournamentRecordId);
        validateTournamentVisible(access, tournament);
        if (!isTournamentApproved(tournament)) {
            throw new SemoException.ValidationException("아직 승인되지 않은 대회입니다.");
        }
        if (!isApplicationOpen(tournament)) {
            throw new SemoException.ValidationException("현재 참가 신청을 받을 수 없습니다.");
        }
        if (tournament.getParticipantLimit() != null) {
            long activeApplicationCount = tournamentApplicationRepository
                    .findByTournamentRecordIdOrderByCreateDateAscTournamentApplicationIdAsc(tournamentRecordId).stream()
                    .filter(application -> APPLICATION_APPLIED.equals(application.getApplicationStatus()) || APPLICATION_APPROVED.equals(application.getApplicationStatus()))
                    .count();
            if (activeApplicationCount >= tournament.getParticipantLimit()) {
                throw new SemoException.ValidationException("참가 신청 정원이 마감되었습니다.");
            }
        }
        ClubActivityContextHolder.setDetails(
                "대회 '" + tournament.getTitle() + "'에 참가 신청했습니다.",
                "대회 '" + tournament.getTitle() + "' 참가 신청에 실패했습니다."
        );

        TournamentApplication current = tournamentApplicationRepository
                .findByTournamentRecordIdAndClubProfileId(tournamentRecordId, access.clubProfile().getClubProfileId())
                .orElse(null);
        if (current == null) {
            tournamentApplicationRepository.save(TournamentApplication.builder()
                    .tournamentRecordId(tournamentRecordId)
                    .clubProfileId(access.clubProfile().getClubProfileId())
                    .applicationStatus(APPLICATION_APPLIED)
                    .applicationNote(trimToNull(request == null ? null : request.applicationNote()))
                    .reviewedByClubProfileId(null)
                    .reviewedAt(null)
                    .build());
        } else {
            current.markApplied(trimToNull(request == null ? null : request.applicationNote()));
            tournamentApplicationRepository.save(current);
        }
        return buildTournamentDetail(access, tournament);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "대회관리")
    public TournamentDetailResponse cancelMyApplication(Long clubId, Long tournamentRecordId, String userKey) {
        requireTournamentFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        TournamentRecord tournament = getTournament(clubId, tournamentRecordId);
        validateTournamentVisible(access, tournament);
        TournamentApplication current = tournamentApplicationRepository
                .findByTournamentRecordIdAndClubProfileId(tournamentRecordId, access.clubProfile().getClubProfileId())
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("TournamentApplication", "tournamentRecordId", tournamentRecordId));
        ClubActivityContextHolder.setDetails(
                "대회 '" + tournament.getTitle() + "' 참가 신청을 취소했습니다.",
                "대회 '" + tournament.getTitle() + "' 참가 신청 취소에 실패했습니다."
        );
        current.review(APPLICATION_CANCELLED, access.clubProfile().getClubProfileId(), LocalDateTime.now());
        tournamentApplicationRepository.save(current);
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
        TournamentRecord tournament = getTournament(clubId, tournamentRecordId);
        validateTournamentVisible(access, tournament);
        if (!canManageApplications(access, tournament)) {
            throw new SemoException.ForbiddenException("참가 신청을 검토할 권한이 없습니다.");
        }
        if (!isTournamentApproved(tournament)) {
            throw new SemoException.ValidationException("승인된 대회만 참가 신청을 검토할 수 있습니다.");
        }
        TournamentApplication current = tournamentApplicationRepository.findById(tournamentApplicationId)
                .filter(application -> application.getTournamentRecordId().equals(tournamentRecordId))
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("TournamentApplication", "tournamentApplicationId", tournamentApplicationId));
        String nextStatus = normalizeApplicationReviewStatus(request.applicationStatus());
        ClubActivityContextHolder.setDetails(
                "대회 '" + tournament.getTitle() + "' 참가 신청을 " + nextStatus + " 처리했습니다.",
                "대회 '" + tournament.getTitle() + "' 참가 신청 검토에 실패했습니다."
        );
        current.review(nextStatus, access.clubProfile().getClubProfileId(), LocalDateTime.now());
        tournamentApplicationRepository.save(current);
        return buildTournamentDetail(access, tournament);
    }

    private TournamentDetailResponse buildTournamentDetail(ClubAccessResolver.ClubAccess access, TournamentRecord tournament) {
        List<TournamentApplication> applications = tournamentApplicationRepository
                .findByTournamentRecordIdOrderByCreateDateAscTournamentApplicationIdAsc(tournament.getTournamentRecordId());

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
        Map<Long, ClubProfile> profileById = loadClubProfiles(clubProfileIds);

        ClubTournamentPermissionService.TournamentActionPermission actionPermission =
                clubTournamentPermissionService.getActionPermission(access, tournament.getAuthorClubProfileId());
        TournamentViewerState viewerState = resolveViewerState(access, tournament, applications);
        boolean canReviewTournament = clubTournamentPermissionService.canReviewTournament(access);
        boolean canManageApplications = canManageApplications(access, tournament);
        List<TournamentApplication> approvedApplications = applications.stream()
                .filter(application -> APPLICATION_APPROVED.equals(application.getApplicationStatus()))
                .toList();

        return new TournamentDetailResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                tournament.getTournamentRecordId(),
                tournament.getTitle(),
                tournament.getSummaryText(),
                tournament.getDetailText(),
                tournament.getApprovalStatus(),
                resolveTournamentStatus(tournament),
                resolveDisplayName(profileById.get(tournament.getAuthorClubProfileId())),
                resolveAvatarImageUrl(profileById.get(tournament.getAuthorClubProfileId())),
                resolveAvatarThumbnailUrl(profileById.get(tournament.getAuthorClubProfileId())),
                resolveDisplayName(profileById.get(tournament.getReviewedByClubProfileId())),
                formatDateTimeLabel(tournament.getReviewedAt()),
                tournament.getRejectionReason(),
                formatDateTime(tournament.getApplicationStartAt()),
                formatDateTime(tournament.getApplicationEndAt()),
                formatApplicationWindowLabel(tournament),
                formatDate(tournament.getStartDate()),
                formatDate(tournament.getEndDate()),
                formatTournamentPeriodLabel(tournament),
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
                tournament.getCancelledAt() == null ? null : DATE_TIME_LABEL_FORMATTER.format(tournament.getCancelledAt()),
                tournament.getCancelReason(),
                applications.size(),
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
                        .map(application -> toApplicationSummary(access, application, profileById.get(application.getClubProfileId()), canManageApplications))
                        .toList(),
                approvedApplications.stream()
                        .map(application -> toParticipantSummary(application, profileById.get(application.getClubProfileId())))
                        .toList()
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
                            resolveDisplayName(profileById.get(tournament.getAuthorClubProfileId())),
                            resolveAvatarImageUrl(profileById.get(tournament.getAuthorClubProfileId())),
                            resolveAvatarThumbnailUrl(profileById.get(tournament.getAuthorClubProfileId())),
                            formatApplicationWindowLabel(tournament),
                            formatTournamentPeriodLabel(tournament),
                            formatDate(tournament.getStartDate()),
                            formatDate(tournament.getEndDate()),
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
            boolean canManageApplications
    ) {
        boolean mine = access.clubProfile().getClubProfileId().equals(application.getClubProfileId());
        return new TournamentApplicationSummaryResponse(
                application.getTournamentApplicationId(),
                application.getClubProfileId(),
                resolveDisplayName(profile),
                resolveAvatarImageUrl(profile),
                resolveAvatarThumbnailUrl(profile),
                application.getApplicationStatus(),
                application.getApplicationNote(),
                DATE_TIME_LABEL_FORMATTER.format(application.getCreateDate()),
                mine,
                canManageApplications,
                mine && (APPLICATION_APPLIED.equals(application.getApplicationStatus()) || APPLICATION_APPROVED.equals(application.getApplicationStatus()))
        );
    }

    private TournamentParticipantSummaryResponse toParticipantSummary(TournamentApplication application, ClubProfile profile) {
        return new TournamentParticipantSummaryResponse(
                application.getClubProfileId(),
                resolveDisplayName(profile),
                resolveAvatarImageUrl(profile),
                resolveAvatarThumbnailUrl(profile),
                formatDateTimeLabel(application.getReviewedAt())
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
                && !APPLICATION_REJECTED.equals(myApplication.getApplicationStatus())
                && !APPLICATION_CANCELLED.equals(myApplication.getApplicationStatus());
        boolean applied = myApplication != null
                && !APPLICATION_CANCELLED.equals(myApplication.getApplicationStatus());
        return new TournamentViewerState(
                access.clubProfile().getClubProfileId().equals(tournament.getAuthorClubProfileId()),
                applied,
                myApplication == null ? null : myApplication.getApplicationStatus(),
                participating
        );
    }

    private TournamentUpsertResponse toUpsertResponse(TournamentRecord tournament) {
        return new TournamentUpsertResponse(
                tournament.getTournamentRecordId(),
                tournament.getTitle(),
                formatDate(tournament.getStartDate()),
                formatDate(tournament.getEndDate()),
                tournament.getApprovalStatus(),
                resolveTournamentStatus(tournament)
        );
    }

    private TournamentDraft toTournamentDraft(
            UpsertTournamentRequest request,
            ClubAccessResolver.ClubAccess access,
            TournamentRecord current
    ) {
        if (request == null) {
            throw new SemoException.ValidationException("대회 요청이 비어 있습니다.");
        }
        String matchFormat = normalizeMatchFormat(request.matchFormat());
        Integer teamMemberLimit = normalizeTeamMemberLimit(matchFormat, request.teamMemberLimit());
        LocalDateTime applicationStartAt = parseRequiredDateTime(request.applicationStartAt());
        LocalDateTime applicationEndAt = parseRequiredDateTime(request.applicationEndAt());
        LocalDate startDate = parseRequiredDate(request.startDate());
        LocalDate endDate = parseRequiredDate(request.endDate());
        if (applicationEndAt.isBefore(applicationStartAt)) {
            throw new SemoException.ValidationException("신청 종료일시는 시작일시 이후여야 합니다.");
        }
        if (endDate.isBefore(startDate)) {
            throw new SemoException.ValidationException("대회 종료일은 시작일 이후여야 합니다.");
        }
        if (applicationEndAt.toLocalDate().isAfter(endDate)) {
            throw new SemoException.ValidationException("참가 신청 종료일은 대회 종료일보다 늦을 수 없습니다.");
        }
        boolean feeRequired = request.feeRequired() != null && request.feeRequired();
        Integer feeAmount = feeRequired ? request.feeAmount() : null;
        if (feeRequired && feeAmount != null && feeAmount < 0) {
            throw new SemoException.ValidationException("참가비는 0 이상이어야 합니다.");
        }
        if (request.participantLimit() != null && request.participantLimit() < 2) {
            throw new SemoException.ValidationException("참가 인원 제한은 2 이상이어야 합니다.");
        }
        boolean pinned = request.pinned() != null && request.pinned();
        if (pinned && !clubTournamentPermissionService.canPinTournament(access)) {
            throw new SemoException.ForbiddenException("대회를 고정할 권한이 없습니다.");
        }
        return new TournamentDraft(
                request.title().trim(),
                trimToNull(request.summaryText()),
                trimToNull(request.detailText()),
                applicationStartAt,
                applicationEndAt,
                startDate,
                endDate,
                trimToNull(request.locationLabel()),
                matchFormat,
                teamMemberLimit,
                request.participantLimit(),
                feeRequired,
                feeAmount,
                normalizeCurrencyCode(request.feeCurrencyCode()),
                request.postToBoard() == null || request.postToBoard(),
                request.postToCalendar() == null || request.postToCalendar(),
                pinned
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

    private String formatApplicationWindowLabel(TournamentRecord tournament) {
        return DATE_TIME_LABEL_FORMATTER.format(tournament.getApplicationStartAt())
                + " ~ "
                + DATE_TIME_LABEL_FORMATTER.format(tournament.getApplicationEndAt());
    }

    private String formatTournamentPeriodLabel(TournamentRecord tournament) {
        if (tournament.getStartDate().equals(tournament.getEndDate())) {
            return DATE_LABEL_FORMATTER.format(tournament.getStartDate());
        }
        return DATE_LABEL_FORMATTER.format(tournament.getStartDate())
                + " ~ "
                + DATE_LABEL_FORMATTER.format(tournament.getEndDate());
    }

    private String formatDate(LocalDate date) {
        return date == null ? null : DATE_FORMATTER.format(date);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : DATE_TIME_FORMATTER.format(dateTime);
    }

    private String formatDateTimeLabel(LocalDateTime dateTime) {
        return dateTime == null ? null : DATE_TIME_LABEL_FORMATTER.format(dateTime);
    }

    private String resolveDisplayName(ClubProfile profile) {
        return profile == null ? "알 수 없음" : profile.getDisplayName();
    }

    private String resolveAvatarImageUrl(ClubProfile profile) {
        return profile == null ? null : imageFileUrlResolver.resolveImageUrl(profile.getAvatarFileName());
    }

    private String resolveAvatarThumbnailUrl(ClubProfile profile) {
        return profile == null ? null : imageFileUrlResolver.resolveThumbnailUrl(profile.getAvatarFileName());
    }

    private String normalizeRequiredKey(String value) {
        if (!StringUtils.hasText(value)) {
            throw new SemoException.ValidationException("필수 값이 비어 있습니다.");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeMatchFormat(String value) {
        String normalized = normalizeRequiredKey(value);
        if (!Set.of(MATCH_FORMAT_SINGLE, MATCH_FORMAT_DOUBLE, MATCH_FORMAT_TEAM).contains(normalized)) {
            throw new SemoException.ValidationException("지원하지 않는 경기 형식입니다.");
        }
        return normalized;
    }

    private Integer normalizeTeamMemberLimit(String matchFormat, Integer teamMemberLimit) {
        if (!MATCH_FORMAT_TEAM.equals(matchFormat)) {
            return null;
        }
        if (teamMemberLimit == null || teamMemberLimit < 3) {
            throw new SemoException.ValidationException("단체전은 팀 인원을 3명 이상으로 설정해야 합니다.");
        }
        return teamMemberLimit;
    }

    private String normalizeApplicationReviewStatus(String value) {
        String normalized = normalizeRequiredKey(value);
        if (!Set.of(APPLICATION_APPROVED, APPLICATION_REJECTED).contains(normalized)) {
            throw new SemoException.ValidationException("참가 신청 상태는 APPROVED 또는 REJECTED만 가능합니다.");
        }
        return normalized;
    }

    private String normalizeTournamentApprovalStatus(String value) {
        String normalized = normalizeRequiredKey(value);
        if (!Set.of(APPROVAL_APPROVED, APPROVAL_REJECTED).contains(normalized)) {
            throw new SemoException.ValidationException("대회 검토 상태는 APPROVED 또는 REJECTED만 가능합니다.");
        }
        return normalized;
    }

    private String normalizeCurrencyCode(String value) {
        if (!StringUtils.hasText(value)) {
            return "KRW";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private LocalDate parseRequiredDate(String value) {
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new SemoException.ValidationException("잘못된 날짜 형식입니다.");
        }
    }

    private LocalDateTime parseRequiredDateTime(String value) {
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new SemoException.ValidationException("잘못된 일시 형식입니다.");
        }
    }

    private LocalDateTime parseOptionalDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return parseRequiredDateTime(value.trim());
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean isTournamentApproved(TournamentRecord tournament) {
        return tournament != null && APPROVAL_APPROVED.equals(tournament.getApprovalStatus());
    }

    private boolean canViewTournament(ClubAccessResolver.ClubAccess access, TournamentRecord tournament) {
        return isTournamentApproved(tournament)
                || access.isAdmin()
                || access.clubProfile().getClubProfileId().equals(tournament.getAuthorClubProfileId());
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

    private record TournamentDraft(
            String title,
            String summaryText,
            String detailText,
            LocalDateTime applicationStartAt,
            LocalDateTime applicationEndAt,
            LocalDate startDate,
            LocalDate endDate,
            String locationLabel,
            String matchFormat,
            Integer teamMemberLimit,
            Integer participantLimit,
            boolean feeRequired,
            Integer feeAmount,
            String feeCurrencyCode,
            boolean postToBoard,
            boolean postToCalendar,
            boolean pinned
    ) {
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
}
