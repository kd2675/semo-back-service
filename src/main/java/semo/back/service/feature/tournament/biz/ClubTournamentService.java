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
import semo.back.service.database.pub.entity.TournamentEntry;
import semo.back.service.database.pub.entity.TournamentEntryMember;
import semo.back.service.database.pub.entity.TournamentMatch;
import semo.back.service.database.pub.entity.TournamentMatchSide;
import semo.back.service.database.pub.entity.TournamentRecord;
import semo.back.service.database.pub.entity.TournamentRound;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.TournamentApplicationRepository;
import semo.back.service.database.pub.repository.TournamentEntryMemberRepository;
import semo.back.service.database.pub.repository.TournamentEntryRepository;
import semo.back.service.database.pub.repository.TournamentMatchRepository;
import semo.back.service.database.pub.repository.TournamentMatchSideRepository;
import semo.back.service.database.pub.repository.TournamentRecordRepository;
import semo.back.service.database.pub.repository.TournamentRoundRepository;
import semo.back.service.feature.activity.biz.ClubActivityContextHolder;
import semo.back.service.feature.activity.biz.RecordClubActivity;
import semo.back.service.feature.club.biz.ClubAccessResolver;
import semo.back.service.feature.share.biz.ClubContentShareService;
import semo.back.service.feature.tournament.vo.CancelTournamentRequest;
import semo.back.service.feature.tournament.vo.ClubAdminTournamentHomeResponse;
import semo.back.service.feature.tournament.vo.ClubTournamentHomeResponse;
import semo.back.service.feature.tournament.vo.GenerateTournamentBracketRequest;
import semo.back.service.feature.tournament.vo.ReviewTournamentApplicationRequest;
import semo.back.service.feature.tournament.vo.SubmitTournamentApplicationRequest;
import semo.back.service.feature.tournament.vo.TournamentApplicationSummaryResponse;
import semo.back.service.feature.tournament.vo.TournamentBracketMatchResponse;
import semo.back.service.feature.tournament.vo.TournamentBracketRoundResponse;
import semo.back.service.feature.tournament.vo.TournamentBracketSideResponse;
import semo.back.service.feature.tournament.vo.TournamentDetailResponse;
import semo.back.service.feature.tournament.vo.TournamentEntryDraftMemberRequest;
import semo.back.service.feature.tournament.vo.TournamentEntryMemberResponse;
import semo.back.service.feature.tournament.vo.TournamentEntrySummaryResponse;
import semo.back.service.feature.tournament.vo.TournamentSummaryResponse;
import semo.back.service.feature.tournament.vo.TournamentUpsertResponse;
import semo.back.service.feature.tournament.vo.UpdateTournamentBracketDraftMatchRequest;
import semo.back.service.feature.tournament.vo.UpdateTournamentBracketDraftRequest;
import semo.back.service.feature.tournament.vo.UpdateTournamentBracketDraftSideRequest;
import semo.back.service.feature.tournament.vo.UpdateTournamentEntriesRequest;
import semo.back.service.feature.tournament.vo.UpsertTournamentEntryDraftRequest;
import semo.back.service.feature.tournament.vo.UpsertTournamentRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
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

    private static final String APPLICATION_APPLIED = "APPLIED";
    private static final String APPLICATION_APPROVED = "APPROVED";
    private static final String APPLICATION_REJECTED = "REJECTED";
    private static final String APPLICATION_CANCELLED = "CANCELLED";

    private static final String ENTRY_ACTIVE = "ACTIVE";
    private static final String ENTRY_WITHDRAWN = "WITHDRAWN";
    private static final String ENTRY_WINNER = "WINNER";
    private static final String ENTRY_INDIVIDUAL = "INDIVIDUAL";
    private static final String ENTRY_PAIR = "PAIR";
    private static final String ENTRY_TEAM = "TEAM";

    private static final String MATCH_FORMAT_SINGLE = "SINGLE";
    private static final String MATCH_FORMAT_DOUBLE = "DOUBLE";
    private static final String MATCH_FORMAT_TEAM = "TEAM";

    private static final String BRACKET_MODE_RANDOM = "RANDOM";
    private static final String BRACKET_MODE_MANUAL = "MANUAL";

    private static final String ROUND_TYPE_BRACKET = "BRACKET";
    private static final String MATCH_STATUS_PLANNED = "PLANNED";
    private static final String MATCH_STATUS_COMPLETED = "COMPLETED";
    private static final String SIDE_RESULT_PENDING = "PENDING";
    private static final String SIDE_RESULT_WINNER = "WINNER";
    private static final String SIDE_RESULT_LOSER = "LOSER";
    private static final String SIDE_RESULT_BYE = "BYE";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_TIME_LABEL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREAN);
    private static final DateTimeFormatter DATE_LABEL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREAN);

    private final TournamentRecordRepository tournamentRecordRepository;
    private final TournamentApplicationRepository tournamentApplicationRepository;
    private final TournamentEntryRepository tournamentEntryRepository;
    private final TournamentEntryMemberRepository tournamentEntryMemberRepository;
    private final TournamentRoundRepository tournamentRoundRepository;
    private final TournamentMatchRepository tournamentMatchRepository;
    private final TournamentMatchSideRepository tournamentMatchSideRepository;
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

        List<TournamentSummaryResponse> archived = context.summaries().stream()
                .filter(summary -> isArchived(summary.tournamentStatus()))
                .toList();
        List<TournamentSummaryResponse> visible = context.summaries().stream()
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
                tournaments.size(),
                (int) context.summaries().stream().filter(summary -> STATUS_APPLICATION_OPEN.equals(summary.tournamentStatus())).count(),
                (int) context.summaries().stream().filter(summary -> STATUS_ONGOING.equals(summary.tournamentStatus())).count(),
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
                clubTournamentPermissionService.canCreateTournament(access),
                tournaments.size(),
                (int) context.summaries().stream().filter(summary -> !isArchived(summary.tournamentStatus())).count(),
                (int) context.summaries().stream().filter(summary -> STATUS_COMPLETED.equals(summary.tournamentStatus())).count(),
                (int) context.summaries().stream().filter(summary -> STATUS_APPLICATION_OPEN.equals(summary.tournamentStatus())).count(),
                (int) tournaments.stream().filter(TournamentRecord::isBracketConfirmed).count(),
                context.summaries()
        );
    }

    public TournamentDetailResponse getTournamentDetail(Long clubId, Long tournamentRecordId, String userKey) {
        requireTournamentFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        TournamentRecord tournament = getTournament(clubId, tournamentRecordId);
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
                .tournamentStatus(resolveTournamentStatus(draft.applicationStartAt(), draft.applicationEndAt(), draft.startDate(), draft.endDate(), null, false))
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
                .bracketMode(draft.bracketMode())
                .bracketConfirmed(false)
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

        TournamentRecord saved = tournamentRecordRepository.save(TournamentRecord.builder()
                .tournamentRecordId(current.getTournamentRecordId())
                .clubId(current.getClubId())
                .authorClubProfileId(current.getAuthorClubProfileId())
                .title(draft.title())
                .summaryText(draft.summaryText())
                .detailText(draft.detailText())
                .tournamentStatus(resolveTournamentStatus(draft.applicationStartAt(), draft.applicationEndAt(), draft.startDate(), draft.endDate(), current.getCancelledAt(), current.isBracketConfirmed()))
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
                .bracketMode(draft.bracketMode())
                .bracketConfirmed(current.isBracketConfirmed())
                .cancelledAt(current.getCancelledAt())
                .cancelReason(current.getCancelReason())
                .deleted(false)
                .build());
        syncTournamentShares(saved);
        return toUpsertResponse(saved);
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
                .bracketMode(current.getBracketMode())
                .bracketConfirmed(current.isBracketConfirmed())
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
        clearBracket(tournamentRecordId);
        clearEntries(tournamentRecordId);
        tournamentApplicationRepository.deleteByTournamentRecordId(tournamentRecordId);
        tournamentRecordRepository.save(TournamentRecord.builder()
                .tournamentRecordId(current.getTournamentRecordId())
                .clubId(current.getClubId())
                .authorClubProfileId(current.getAuthorClubProfileId())
                .title(current.getTitle())
                .summaryText(current.getSummaryText())
                .detailText(current.getDetailText())
                .tournamentStatus(current.getTournamentStatus())
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
                .bracketMode(current.getBracketMode())
                .bracketConfirmed(false)
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
        TournamentApplication current = tournamentApplicationRepository
                .findByTournamentRecordIdAndClubProfileId(tournamentRecordId, access.clubProfile().getClubProfileId())
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("TournamentApplication", "tournamentRecordId", tournamentRecordId));
        if (tournament.isBracketConfirmed() && APPLICATION_APPROVED.equals(current.getApplicationStatus())) {
            throw new SemoException.ValidationException("대진표가 확정된 뒤에는 참가 신청을 취소할 수 없습니다.");
        }
        ClubActivityContextHolder.setDetails(
                "대회 '" + tournament.getTitle() + "' 참가 신청을 취소했습니다.",
                "대회 '" + tournament.getTitle() + "' 참가 신청 취소에 실패했습니다."
        );
        current.review(APPLICATION_CANCELLED, access.clubProfile().getClubProfileId(), LocalDateTime.now());
        tournamentApplicationRepository.save(current);
        if (!tournament.isBracketConfirmed()) {
            removeEntriesBySourceApplicationIds(List.of(current.getTournamentApplicationId()));
        }
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
        if (!clubTournamentPermissionService.canReviewApplications(access)) {
            throw new SemoException.ForbiddenException("참가 신청을 검토할 권한이 없습니다.");
        }
        TournamentRecord tournament = getTournament(clubId, tournamentRecordId);
        TournamentApplication current = tournamentApplicationRepository.findById(tournamentApplicationId)
                .filter(application -> application.getTournamentRecordId().equals(tournamentRecordId))
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("TournamentApplication", "tournamentApplicationId", tournamentApplicationId));
        String nextStatus = normalizeApplicationReviewStatus(request.applicationStatus());
        ClubActivityContextHolder.setDetails(
                "대회 '" + tournament.getTitle() + "' 참가 신청을 " + nextStatus + " 처리했습니다.",
                "대회 '" + tournament.getTitle() + "' 참가 신청 검토에 실패했습니다."
        );
        current.review(nextStatus, access.clubProfile().getClubProfileId(), LocalDateTime.now());
        TournamentApplication saved = tournamentApplicationRepository.save(current);
        if (!tournament.isBracketConfirmed() && APPLICATION_REJECTED.equals(saved.getApplicationStatus())) {
            removeEntriesBySourceApplicationIds(List.of(saved.getTournamentApplicationId()));
        }
        return buildTournamentDetail(access, tournament);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "대회관리")
    public TournamentDetailResponse updateEntries(
            Long clubId,
            Long tournamentRecordId,
            String userKey,
            UpdateTournamentEntriesRequest request
    ) {
        requireTournamentFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        if (!clubTournamentPermissionService.canManageEntries(access)) {
            throw new SemoException.ForbiddenException("엔트리를 편성할 권한이 없습니다.");
        }
        TournamentRecord tournament = getTournament(clubId, tournamentRecordId);
        if (tournament.isBracketConfirmed()) {
            throw new SemoException.ValidationException("대진표 확정 후에는 엔트리를 변경할 수 없습니다.");
        }
        List<UpsertTournamentEntryDraftRequest> entryRequests = request == null || request.entries() == null
                ? List.of()
                : request.entries();
        validateEntryRequests(tournament, entryRequests);
        ClubActivityContextHolder.setDetails(
                "대회 '" + tournament.getTitle() + "' 엔트리를 편성했습니다.",
                "대회 '" + tournament.getTitle() + "' 엔트리 편성에 실패했습니다."
        );
        clearBracket(tournamentRecordId);
        clearEntries(tournamentRecordId);

        int sortOrder = 10;
        for (UpsertTournamentEntryDraftRequest entryRequest : entryRequests) {
            TournamentEntry entry = tournamentEntryRepository.save(TournamentEntry.builder()
                    .tournamentRecordId(tournamentRecordId)
                    .entryType(resolveEntryType(tournament.getMatchFormat()))
                    .displayName(entryRequest.displayName().trim())
                    .sourceApplicationId(resolveSourceApplicationId(tournamentRecordId, entryRequest.members()))
                    .entryStatus(ENTRY_ACTIVE)
                    .seedNumber(entryRequest.seedNumber())
                    .sortOrder(sortOrder)
                    .build());
            int memberSortOrder = 10;
            for (TournamentEntryDraftMemberRequest memberRequest : entryRequest.members()) {
                tournamentEntryMemberRepository.save(TournamentEntryMember.builder()
                        .tournamentEntryId(entry.getTournamentEntryId())
                        .clubProfileId(memberRequest.clubProfileId())
                        .memberRole(normalizeMemberRole(memberRequest.memberRole()))
                        .sortOrder(memberSortOrder)
                        .build());
                memberSortOrder += 10;
            }
            sortOrder += 10;
        }
        return buildTournamentDetail(access, tournament);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "대회관리")
    public TournamentDetailResponse generateBracket(
            Long clubId,
            Long tournamentRecordId,
            String userKey,
            GenerateTournamentBracketRequest request
    ) {
        requireTournamentFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        if (!clubTournamentPermissionService.canManageBracket(access)) {
            throw new SemoException.ForbiddenException("대진표를 관리할 권한이 없습니다.");
        }
        TournamentRecord tournament = getTournament(clubId, tournamentRecordId);
        if (tournament.isBracketConfirmed()) {
            throw new SemoException.ValidationException("이미 확정된 대진표입니다.");
        }
        List<TournamentEntry> entries = tournamentEntryRepository
                .findByTournamentRecordIdOrderBySortOrderAscTournamentEntryIdAsc(tournamentRecordId);
        if (entries.size() < 2) {
            throw new SemoException.ValidationException("대진표를 만들려면 엔트리가 2개 이상 필요합니다.");
        }
        ClubActivityContextHolder.setDetails(
                "대회 '" + tournament.getTitle() + "' 대진표 초안을 생성했습니다.",
                "대회 '" + tournament.getTitle() + "' 대진표 생성에 실패했습니다."
        );
        clearBracket(tournamentRecordId);

        List<TournamentEntry> orderedEntries = new ArrayList<>(entries);
        if (request != null && Boolean.TRUE.equals(request.randomize())) {
            Collections.shuffle(orderedEntries, ThreadLocalRandom.current());
        } else {
            orderedEntries.sort(Comparator
                    .comparing((TournamentEntry entry) -> entry.getSeedNumber() == null ? Integer.MAX_VALUE : entry.getSeedNumber())
                    .thenComparingInt(TournamentEntry::getSortOrder)
                    .thenComparing(TournamentEntry::getTournamentEntryId));
        }

        int bracketSize = nextPowerOfTwo(orderedEntries.size());
        int roundCount = Integer.numberOfTrailingZeros(bracketSize);
        List<TournamentRound> rounds = new ArrayList<>();
        for (int roundIndex = 0; roundIndex < roundCount; roundIndex++) {
            int matchCount = bracketSize / (int) Math.pow(2, roundIndex + 1);
            rounds.add(tournamentRoundRepository.save(TournamentRound.builder()
                    .tournamentRecordId(tournamentRecordId)
                    .roundKey(resolveRoundKey(bracketSize, roundIndex))
                    .displayName(resolveRoundLabel(bracketSize, roundIndex))
                    .roundType(ROUND_TYPE_BRACKET)
                    .sortOrder((roundIndex + 1) * 10)
                    .build()));
            if (matchCount <= 0) {
                break;
            }
        }

        List<TournamentEntry> seededEntries = new ArrayList<>(orderedEntries);
        while (seededEntries.size() < bracketSize) {
            seededEntries.add(null);
        }

        int sortOrder = 10;
        for (int roundIndex = 0; roundIndex < rounds.size(); roundIndex++) {
            TournamentRound round = rounds.get(roundIndex);
            int matchCount = bracketSize / (int) Math.pow(2, roundIndex + 1);
            for (int matchIndex = 0; matchIndex < matchCount; matchIndex++) {
                TournamentMatch match = tournamentMatchRepository.save(TournamentMatch.builder()
                        .tournamentRecordId(tournamentRecordId)
                        .tournamentRoundId(round.getTournamentRoundId())
                        .matchStatus(MATCH_STATUS_PLANNED)
                        .title(resolveMatchTitle(round.getDisplayName(), matchIndex))
                        .scheduledAt(null)
                        .endedAt(null)
                        .locationLabel(null)
                        .winnerEntryId(null)
                        .sortOrder(sortOrder)
                        .build());

                Long firstEntryId = null;
                Long secondEntryId = null;
                if (roundIndex == 0) {
                    TournamentEntry left = seededEntries.get(matchIndex * 2);
                    TournamentEntry right = seededEntries.get(matchIndex * 2 + 1);
                    firstEntryId = left == null ? null : left.getTournamentEntryId();
                    secondEntryId = right == null ? null : right.getTournamentEntryId();
                }

                tournamentMatchSideRepository.save(TournamentMatchSide.builder()
                        .tournamentMatchId(match.getTournamentMatchId())
                        .sideNo(1)
                        .tournamentEntryId(firstEntryId)
                        .scoreSummary(null)
                        .resultStatus(resolveInitialSideStatus(firstEntryId, secondEntryId))
                        .build());
                tournamentMatchSideRepository.save(TournamentMatchSide.builder()
                        .tournamentMatchId(match.getTournamentMatchId())
                        .sideNo(2)
                        .tournamentEntryId(secondEntryId)
                        .scoreSummary(null)
                        .resultStatus(resolveInitialSideStatus(secondEntryId, firstEntryId))
                        .build());
                sortOrder += 10;
            }
        }
        return buildTournamentDetail(access, tournament);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "대회관리")
    public TournamentDetailResponse updateBracketDraft(
            Long clubId,
            Long tournamentRecordId,
            String userKey,
            UpdateTournamentBracketDraftRequest request
    ) {
        requireTournamentFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        if (!clubTournamentPermissionService.canManageBracket(access)) {
            throw new SemoException.ForbiddenException("대진표를 관리할 권한이 없습니다.");
        }
        TournamentRecord tournament = getTournament(clubId, tournamentRecordId);
        if (tournament.isBracketConfirmed()) {
            throw new SemoException.ValidationException("확정된 대진표는 수정할 수 없습니다.");
        }
        List<UpdateTournamentBracketDraftMatchRequest> matchRequests = request == null || request.matches() == null
                ? List.of()
                : request.matches();
        validateBracketDraftUpdate(tournamentRecordId, matchRequests);
        ClubActivityContextHolder.setDetails(
                "대회 '" + tournament.getTitle() + "' 대진표 초안을 수정했습니다.",
                "대회 '" + tournament.getTitle() + "' 대진표 수정에 실패했습니다."
        );

        Map<Long, TournamentMatch> matchById = tournamentMatchRepository.findByTournamentRecordIdOrderBySortOrderAscTournamentMatchIdAsc(tournamentRecordId)
                .stream()
                .collect(Collectors.toMap(TournamentMatch::getTournamentMatchId, Function.identity()));
        Map<Long, TournamentMatchSide> sideById = tournamentMatchSideRepository.findByTournamentMatchIdInOrderBySideNoAsc(
                        new ArrayList<>(matchById.keySet())
                ).stream()
                .collect(Collectors.toMap(TournamentMatchSide::getTournamentMatchSideId, Function.identity()));

        for (UpdateTournamentBracketDraftMatchRequest matchRequest : matchRequests) {
            TournamentMatch currentMatch = matchById.get(matchRequest.tournamentMatchId());
            if (currentMatch == null) {
                throw new SemoException.ValidationException("존재하지 않는 브래킷 경기입니다.");
            }
            tournamentMatchRepository.save(TournamentMatch.builder()
                    .tournamentMatchId(currentMatch.getTournamentMatchId())
                    .tournamentRecordId(currentMatch.getTournamentRecordId())
                    .tournamentRoundId(currentMatch.getTournamentRoundId())
                    .matchStatus(MATCH_STATUS_PLANNED)
                    .title(trimToNull(matchRequest.title()))
                    .scheduledAt(parseOptionalDateTime(matchRequest.scheduledAt()))
                    .endedAt(currentMatch.getEndedAt())
                    .locationLabel(trimToNull(matchRequest.locationLabel()))
                    .winnerEntryId(null)
                    .sortOrder(currentMatch.getSortOrder())
                    .build());

            List<UpdateTournamentBracketDraftSideRequest> sideRequests = matchRequest.sides() == null ? List.of() : matchRequest.sides();
            for (UpdateTournamentBracketDraftSideRequest sideRequest : sideRequests) {
                TournamentMatchSide currentSide = sideById.get(sideRequest.tournamentMatchSideId());
                if (currentSide == null || !currentSide.getTournamentMatchId().equals(currentMatch.getTournamentMatchId())) {
                    throw new SemoException.ValidationException("존재하지 않는 브래킷 side 입니다.");
                }
                tournamentMatchSideRepository.save(TournamentMatchSide.builder()
                        .tournamentMatchSideId(currentSide.getTournamentMatchSideId())
                        .tournamentMatchId(currentSide.getTournamentMatchId())
                        .sideNo(currentSide.getSideNo())
                        .tournamentEntryId(sideRequest.tournamentEntryId())
                        .scoreSummary(trimToNull(sideRequest.scoreSummary()))
                        .resultStatus(normalizeSideResultStatus(sideRequest.resultStatus()))
                        .build());
            }
        }
        return buildTournamentDetail(access, tournament);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "대회관리")
    public TournamentDetailResponse confirmBracket(Long clubId, Long tournamentRecordId, String userKey) {
        requireTournamentFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        if (!clubTournamentPermissionService.canManageBracket(access)) {
            throw new SemoException.ForbiddenException("대진표를 확정할 권한이 없습니다.");
        }
        TournamentRecord current = getTournament(clubId, tournamentRecordId);
        List<TournamentRound> rounds = tournamentRoundRepository.findByTournamentRecordIdOrderBySortOrderAscTournamentRoundIdAsc(tournamentRecordId);
        if (rounds.isEmpty()) {
            throw new SemoException.ValidationException("확정할 대진표가 없습니다.");
        }
        ClubActivityContextHolder.setDetails(
                "대회 '" + current.getTitle() + "' 대진표를 확정했습니다.",
                "대회 '" + current.getTitle() + "' 대진표 확정에 실패했습니다."
        );
        TournamentRecord saved = tournamentRecordRepository.save(TournamentRecord.builder()
                .tournamentRecordId(current.getTournamentRecordId())
                .clubId(current.getClubId())
                .authorClubProfileId(current.getAuthorClubProfileId())
                .title(current.getTitle())
                .summaryText(current.getSummaryText())
                .detailText(current.getDetailText())
                .tournamentStatus(resolveTournamentStatus(current.getApplicationStartAt(), current.getApplicationEndAt(), current.getStartDate(), current.getEndDate(), current.getCancelledAt(), true))
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
                .bracketMode(current.getBracketMode())
                .bracketConfirmed(true)
                .cancelledAt(current.getCancelledAt())
                .cancelReason(current.getCancelReason())
                .deleted(false)
                .build());
        return buildTournamentDetail(access, saved);
    }

    private TournamentDetailResponse buildTournamentDetail(ClubAccessResolver.ClubAccess access, TournamentRecord tournament) {
        List<TournamentApplication> applications = tournamentApplicationRepository
                .findByTournamentRecordIdOrderByCreateDateAscTournamentApplicationIdAsc(tournament.getTournamentRecordId());
        List<TournamentEntry> entries = tournamentEntryRepository
                .findByTournamentRecordIdOrderBySortOrderAscTournamentEntryIdAsc(tournament.getTournamentRecordId());
        Map<Long, List<TournamentEntryMember>> entryMembersByEntryId = loadEntryMembersByEntryId(entries);
        List<TournamentRound> rounds = tournamentRoundRepository
                .findByTournamentRecordIdOrderBySortOrderAscTournamentRoundIdAsc(tournament.getTournamentRecordId());
        Map<Long, List<TournamentMatch>> matchesByRoundId = loadMatchesByRoundId(rounds);
        Map<Long, List<TournamentMatchSide>> sidesByMatchId = loadSidesByMatchId(matchesByRoundId.values().stream().flatMap(List::stream).toList());

        Set<Long> clubProfileIds = new HashSet<>();
        clubProfileIds.add(tournament.getAuthorClubProfileId());
        applications.forEach(application -> {
            clubProfileIds.add(application.getClubProfileId());
            if (application.getReviewedByClubProfileId() != null) {
                clubProfileIds.add(application.getReviewedByClubProfileId());
            }
        });
        entryMembersByEntryId.values().forEach(members -> members.forEach(member -> clubProfileIds.add(member.getClubProfileId())));
        Map<Long, ClubProfile> profileById = loadClubProfiles(clubProfileIds);

        ClubTournamentPermissionService.TournamentActionPermission actionPermission =
                clubTournamentPermissionService.getActionPermission(access, tournament.getAuthorClubProfileId());
        TournamentViewerState viewerState = resolveViewerState(access, tournament, applications, entryMembersByEntryId);

        return new TournamentDetailResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                tournament.getTournamentRecordId(),
                tournament.getTitle(),
                tournament.getSummaryText(),
                tournament.getDetailText(),
                resolveTournamentStatus(tournament),
                resolveDisplayName(profileById.get(tournament.getAuthorClubProfileId())),
                resolveAvatarImageUrl(profileById.get(tournament.getAuthorClubProfileId())),
                resolveAvatarThumbnailUrl(profileById.get(tournament.getAuthorClubProfileId())),
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
                tournament.getBracketMode(),
                tournament.isBracketConfirmed(),
                tournament.getCancelledAt() == null ? null : DATE_TIME_LABEL_FORMATTER.format(tournament.getCancelledAt()),
                tournament.getCancelReason(),
                applications.size(),
                (int) applications.stream().filter(application -> APPLICATION_APPROVED.equals(application.getApplicationStatus())).count(),
                (int) entries.stream().filter(entry -> ENTRY_ACTIVE.equals(entry.getEntryStatus())).count(),
                isApplicationOpen(tournament),
                !viewerState.applied() && isApplicationOpen(tournament),
                viewerState.applied(),
                viewerState.applicationStatus(),
                viewerState.participating(),
                actionPermission.canEdit(),
                actionPermission.canCancel(),
                clubTournamentPermissionService.canDeleteTournament(access),
                clubTournamentPermissionService.canReviewApplications(access),
                clubTournamentPermissionService.canManageEntries(access),
                clubTournamentPermissionService.canManageBracket(access),
                applications.stream()
                        .map(application -> toApplicationSummary(access, application, profileById.get(application.getClubProfileId())))
                        .toList(),
                entries.stream()
                        .map(entry -> toEntrySummary(entry, entryMembersByEntryId.getOrDefault(entry.getTournamentEntryId(), List.of()), profileById))
                        .toList(),
                rounds.stream()
                        .map(round -> toRoundResponse(round, matchesByRoundId.getOrDefault(round.getTournamentRoundId(), List.of()), sidesByMatchId, entries, entryMembersByEntryId, profileById))
                        .toList()
        );
    }

    private TournamentContext buildTournamentContext(ClubAccessResolver.ClubAccess access, List<TournamentRecord> tournaments) {
        List<TournamentApplication> applications = tournaments.isEmpty()
                ? List.of()
                : tournamentApplicationRepository.findByTournamentRecordIdIn(tournaments.stream().map(TournamentRecord::getTournamentRecordId).toList());
        List<TournamentEntry> entries = tournaments.isEmpty()
                ? List.of()
                : tournamentEntryRepository.findByTournamentRecordIdIn(tournaments.stream().map(TournamentRecord::getTournamentRecordId).toList());
        Map<Long, List<TournamentApplication>> applicationsByTournamentId = applications.stream()
                .collect(Collectors.groupingBy(TournamentApplication::getTournamentRecordId));
        Map<Long, List<TournamentEntry>> entriesByTournamentId = entries.stream()
                .collect(Collectors.groupingBy(TournamentEntry::getTournamentRecordId));
        Map<Long, List<TournamentEntryMember>> entryMembersByEntryId = loadEntryMembersByEntryId(entries);
        Set<Long> profileIds = new HashSet<>();
        tournaments.forEach(tournament -> profileIds.add(tournament.getAuthorClubProfileId()));
        applications.forEach(application -> profileIds.add(application.getClubProfileId()));
        entryMembersByEntryId.values().forEach(members -> members.forEach(member -> profileIds.add(member.getClubProfileId())));
        Map<Long, ClubProfile> profileById = loadClubProfiles(profileIds);

        List<TournamentSummaryResponse> summaries = tournaments.stream()
                .map(tournament -> {
                    List<TournamentApplication> tournamentApplications = applicationsByTournamentId.getOrDefault(tournament.getTournamentRecordId(), List.of());
                    List<TournamentEntry> tournamentEntries = entriesByTournamentId.getOrDefault(tournament.getTournamentRecordId(), List.of());
                    ClubTournamentPermissionService.TournamentActionPermission permission =
                            clubTournamentPermissionService.getActionPermission(access, tournament.getAuthorClubProfileId());
                    TournamentViewerState viewerState = resolveViewerState(access, tournament, tournamentApplications, filterEntryMembersForTournament(tournamentEntries, entryMembersByEntryId));
                    return new TournamentSummaryResponse(
                            tournament.getTournamentRecordId(),
                            tournament.getTitle(),
                            tournament.getSummaryText(),
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
                            (int) tournamentApplications.stream().filter(application -> APPLICATION_APPROVED.equals(application.getApplicationStatus())).count(),
                            (int) tournamentEntries.stream().filter(entry -> ENTRY_ACTIVE.equals(entry.getEntryStatus())).count(),
                            tournament.isFeeRequired(),
                            tournament.getFeeAmount(),
                            tournament.getFeeCurrencyCode(),
                            tournament.isSharedToBoard(),
                            tournament.isSharedToCalendar(),
                            tournament.isPinned(),
                            tournament.getBracketMode(),
                            tournament.isBracketConfirmed(),
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

    private Map<Long, List<TournamentEntryMember>> filterEntryMembersForTournament(
            List<TournamentEntry> entries,
            Map<Long, List<TournamentEntryMember>> entryMembersByEntryId
    ) {
        return entries.stream()
                .collect(Collectors.toMap(
                        TournamentEntry::getTournamentEntryId,
                        entry -> entryMembersByEntryId.getOrDefault(entry.getTournamentEntryId(), List.of())
                ));
    }

    private TournamentApplicationSummaryResponse toApplicationSummary(
            ClubAccessResolver.ClubAccess access,
            TournamentApplication application,
            ClubProfile profile
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
                clubTournamentPermissionService.canReviewApplications(access),
                mine && (APPLICATION_APPLIED.equals(application.getApplicationStatus()) || APPLICATION_APPROVED.equals(application.getApplicationStatus()))
        );
    }

    private TournamentEntrySummaryResponse toEntrySummary(
            TournamentEntry entry,
            List<TournamentEntryMember> members,
            Map<Long, ClubProfile> profileById
    ) {
        return new TournamentEntrySummaryResponse(
                entry.getTournamentEntryId(),
                entry.getEntryType(),
                entry.getDisplayName(),
                entry.getEntryStatus(),
                entry.getSeedNumber(),
                entry.getSortOrder(),
                members.stream()
                        .sorted(Comparator.comparingInt(TournamentEntryMember::getSortOrder))
                        .map(member -> toEntryMemberResponse(member, profileById.get(member.getClubProfileId())))
                        .toList()
        );
    }

    private TournamentBracketRoundResponse toRoundResponse(
            TournamentRound round,
            List<TournamentMatch> matches,
            Map<Long, List<TournamentMatchSide>> sidesByMatchId,
            List<TournamentEntry> entries,
            Map<Long, List<TournamentEntryMember>> entryMembersByEntryId,
            Map<Long, ClubProfile> profileById
    ) {
        Map<Long, TournamentEntry> entryById = entries.stream()
                .collect(Collectors.toMap(TournamentEntry::getTournamentEntryId, Function.identity(), (left, right) -> left));

        return new TournamentBracketRoundResponse(
                round.getTournamentRoundId(),
                round.getRoundKey(),
                round.getDisplayName(),
                round.getRoundType(),
                round.getSortOrder(),
                matches.stream()
                        .map(match -> new TournamentBracketMatchResponse(
                                match.getTournamentMatchId(),
                                match.getTournamentRoundId(),
                                match.getTitle(),
                                match.getMatchStatus(),
                                formatDateTime(match.getScheduledAt()),
                                match.getScheduledAt() == null ? null : DATE_TIME_LABEL_FORMATTER.format(match.getScheduledAt()),
                                match.getLocationLabel(),
                                match.getWinnerEntryId(),
                                match.getSortOrder(),
                                sidesByMatchId.getOrDefault(match.getTournamentMatchId(), List.of()).stream()
                                        .sorted(Comparator.comparingInt(TournamentMatchSide::getSideNo))
                                        .map(side -> {
                                            TournamentEntry entry = side.getTournamentEntryId() == null ? null : entryById.get(side.getTournamentEntryId());
                                            List<TournamentEntryMember> members = entry == null
                                                    ? List.of()
                                                    : entryMembersByEntryId.getOrDefault(entry.getTournamentEntryId(), List.of());
                                            return new TournamentBracketSideResponse(
                                                    side.getTournamentMatchSideId(),
                                                    side.getSideNo(),
                                                    side.getTournamentEntryId(),
                                                    entry == null ? null : entry.getDisplayName(),
                                                    entry == null ? null : entry.getSeedNumber(),
                                                    side.getScoreSummary(),
                                                    side.getResultStatus(),
                                                    members.stream()
                                                            .sorted(Comparator.comparingInt(TournamentEntryMember::getSortOrder))
                                                            .map(member -> toEntryMemberResponse(member, profileById.get(member.getClubProfileId())))
                                                            .toList()
                                            );
                                        })
                                        .toList()
                        ))
                        .toList()
        );
    }

    private TournamentEntryMemberResponse toEntryMemberResponse(TournamentEntryMember member, ClubProfile profile) {
        return new TournamentEntryMemberResponse(
                member.getClubProfileId(),
                resolveDisplayName(profile),
                resolveAvatarImageUrl(profile),
                resolveAvatarThumbnailUrl(profile),
                member.getMemberRole()
        );
    }

    private Map<Long, List<TournamentEntryMember>> loadEntryMembersByEntryId(List<TournamentEntry> entries) {
        if (entries.isEmpty()) {
            return Map.of();
        }
        return tournamentEntryMemberRepository.findByTournamentEntryIdIn(
                        entries.stream().map(TournamentEntry::getTournamentEntryId).toList()
                ).stream()
                .collect(Collectors.groupingBy(TournamentEntryMember::getTournamentEntryId));
    }

    private Map<Long, List<TournamentMatch>> loadMatchesByRoundId(List<TournamentRound> rounds) {
        if (rounds.isEmpty()) {
            return Map.of();
        }
        return tournamentMatchRepository.findByTournamentRoundIdInOrderBySortOrderAscTournamentMatchIdAsc(
                        rounds.stream().map(TournamentRound::getTournamentRoundId).toList()
                ).stream()
                .collect(Collectors.groupingBy(TournamentMatch::getTournamentRoundId, LinkedHashMap::new, Collectors.toList()));
    }

    private Map<Long, List<TournamentMatchSide>> loadSidesByMatchId(List<TournamentMatch> matches) {
        if (matches.isEmpty()) {
            return Map.of();
        }
        return tournamentMatchSideRepository.findByTournamentMatchIdInOrderBySideNoAsc(
                        matches.stream().map(TournamentMatch::getTournamentMatchId).toList()
                ).stream()
                .collect(Collectors.groupingBy(TournamentMatchSide::getTournamentMatchId, LinkedHashMap::new, Collectors.toList()));
    }

    private TournamentViewerState resolveViewerState(
            ClubAccessResolver.ClubAccess access,
            TournamentRecord tournament,
            List<TournamentApplication> applications,
            Map<Long, List<TournamentEntryMember>> entryMembersByEntryId
    ) {
        Long viewerClubProfileId = access.clubProfile().getClubProfileId();
        TournamentApplication myApplication = applications.stream()
                .filter(application -> application.getClubProfileId().equals(viewerClubProfileId))
                .findFirst()
                .orElse(null);
        boolean participating = entryMembersByEntryId.values().stream()
                .flatMap(Collection::stream)
                .anyMatch(member -> member.getClubProfileId().equals(viewerClubProfileId))
                || (myApplication != null && !APPLICATION_REJECTED.equals(myApplication.getApplicationStatus()) && !APPLICATION_CANCELLED.equals(myApplication.getApplicationStatus()));
        return new TournamentViewerState(
                access.clubProfile().getClubProfileId().equals(tournament.getAuthorClubProfileId()),
                myApplication != null,
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
                resolveTournamentStatus(tournament),
                tournament.isBracketConfirmed()
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
        if (current != null && current.isBracketConfirmed() && !Objects.equals(matchFormat, current.getMatchFormat())) {
            throw new SemoException.ValidationException("대진표 확정 후에는 경기 형식을 변경할 수 없습니다.");
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
                request.postToBoard() != null && request.postToBoard(),
                request.postToCalendar() == null || request.postToCalendar(),
                pinned,
                normalizeBracketMode(request.bracketMode())
        );
    }

    private void validateEntryRequests(TournamentRecord tournament, List<UpsertTournamentEntryDraftRequest> entries) {
        Set<Long> approvedProfileIds = tournamentApplicationRepository
                .findByTournamentRecordIdAndApplicationStatusOrderByCreateDateAscTournamentApplicationIdAsc(
                        tournament.getTournamentRecordId(),
                        APPLICATION_APPROVED
                ).stream()
                .map(TournamentApplication::getClubProfileId)
                .collect(Collectors.toSet());
        Set<Long> usedProfileIds = new HashSet<>();
        for (UpsertTournamentEntryDraftRequest entry : entries) {
            if (!StringUtils.hasText(entry.displayName())) {
                throw new SemoException.ValidationException("엔트리 이름은 비어 있을 수 없습니다.");
            }
            List<TournamentEntryDraftMemberRequest> members = entry.members() == null ? List.of() : entry.members();
            if (members.isEmpty()) {
                throw new SemoException.ValidationException("엔트리 멤버는 한 명 이상 필요합니다.");
            }
            validateEntryMemberCount(tournament, members.size());
            for (TournamentEntryDraftMemberRequest member : members) {
                if (!approvedProfileIds.contains(member.clubProfileId())) {
                    throw new SemoException.ValidationException("승인되지 않은 신청자는 엔트리에 포함할 수 없습니다.");
                }
                if (!usedProfileIds.add(member.clubProfileId())) {
                    throw new SemoException.ValidationException("같은 멤버를 여러 엔트리에 중복 배치할 수 없습니다.");
                }
            }
        }
    }

    private void validateBracketDraftUpdate(Long tournamentRecordId, List<UpdateTournamentBracketDraftMatchRequest> matches) {
        Set<Long> usedEntryIds = new HashSet<>();
        Set<Long> validEntryIds = tournamentEntryRepository.findByTournamentRecordIdOrderBySortOrderAscTournamentEntryIdAsc(tournamentRecordId)
                .stream()
                .map(TournamentEntry::getTournamentEntryId)
                .collect(Collectors.toSet());
        for (UpdateTournamentBracketDraftMatchRequest match : matches) {
            List<UpdateTournamentBracketDraftSideRequest> sides = match.sides() == null ? List.of() : match.sides();
            for (UpdateTournamentBracketDraftSideRequest side : sides) {
                if (side.tournamentEntryId() == null) {
                    continue;
                }
                if (!validEntryIds.contains(side.tournamentEntryId())) {
                    throw new SemoException.ValidationException("존재하지 않는 엔트리를 대진표에 배치할 수 없습니다.");
                }
                if (!usedEntryIds.add(side.tournamentEntryId())) {
                    throw new SemoException.ValidationException("하나의 엔트리를 여러 경기 side에 중복 배치할 수 없습니다.");
                }
            }
        }
    }

    private void validateEntryMemberCount(TournamentRecord tournament, int memberCount) {
        switch (normalizeMatchFormat(tournament.getMatchFormat())) {
            case MATCH_FORMAT_SINGLE -> {
                if (memberCount != 1) {
                    throw new SemoException.ValidationException("개인전 엔트리는 1명이어야 합니다.");
                }
            }
            case MATCH_FORMAT_DOUBLE -> {
                if (memberCount != 2) {
                    throw new SemoException.ValidationException("복식 엔트리는 2명이어야 합니다.");
                }
            }
            case MATCH_FORMAT_TEAM -> {
                if (memberCount < 3) {
                    throw new SemoException.ValidationException("단체전 엔트리는 3명 이상이어야 합니다.");
                }
                if (tournament.getTeamMemberLimit() != null && memberCount != tournament.getTeamMemberLimit()) {
                    throw new SemoException.ValidationException("단체전 엔트리 인원은 설정된 팀 인원과 같아야 합니다.");
                }
            }
            default -> throw new SemoException.ValidationException("지원하지 않는 경기 형식입니다.");
        }
    }

    private Long resolveSourceApplicationId(Long tournamentRecordId, List<TournamentEntryDraftMemberRequest> members) {
        if (members == null || members.isEmpty()) {
            return null;
        }
        if (members.size() != 1) {
            return null;
        }
        return tournamentApplicationRepository.findByTournamentRecordIdAndClubProfileId(
                        tournamentRecordId,
                        members.get(0).clubProfileId()
                )
                .map(TournamentApplication::getTournamentApplicationId)
                .orElse(null);
    }

    private void removeEntriesBySourceApplicationIds(List<Long> applicationIds) {
        if (applicationIds.isEmpty()) {
            return;
        }
        List<TournamentEntry> entries = tournamentEntryRepository.findAll().stream()
                .filter(entry -> entry.getSourceApplicationId() != null && applicationIds.contains(entry.getSourceApplicationId()))
                .toList();
        if (entries.isEmpty()) {
            return;
        }
        List<Long> entryIds = entries.stream().map(TournamentEntry::getTournamentEntryId).toList();
        tournamentEntryMemberRepository.deleteByTournamentEntryIdIn(entryIds);
        tournamentEntryRepository.deleteAll(entries);
    }

    private void clearEntries(Long tournamentRecordId) {
        List<TournamentEntry> entries = tournamentEntryRepository.findByTournamentRecordIdOrderBySortOrderAscTournamentEntryIdAsc(tournamentRecordId);
        if (entries.isEmpty()) {
            return;
        }
        List<Long> entryIds = entries.stream().map(TournamentEntry::getTournamentEntryId).toList();
        tournamentEntryMemberRepository.deleteByTournamentEntryIdIn(entryIds);
        tournamentEntryRepository.deleteByTournamentRecordId(tournamentRecordId);
    }

    private void clearBracket(Long tournamentRecordId) {
        List<TournamentRound> rounds = tournamentRoundRepository.findByTournamentRecordIdOrderBySortOrderAscTournamentRoundIdAsc(tournamentRecordId);
        if (rounds.isEmpty()) {
            return;
        }
        List<Long> roundIds = rounds.stream().map(TournamentRound::getTournamentRoundId).toList();
        List<TournamentMatch> matches = tournamentMatchRepository.findByTournamentRoundIdInOrderBySortOrderAscTournamentMatchIdAsc(roundIds);
        if (!matches.isEmpty()) {
            tournamentMatchSideRepository.deleteByTournamentMatchIdIn(matches.stream().map(TournamentMatch::getTournamentMatchId).toList());
            tournamentMatchRepository.deleteByTournamentRecordId(tournamentRecordId);
        }
        tournamentRoundRepository.deleteByTournamentRecordId(tournamentRecordId);
    }

    private void syncTournamentShares(TournamentRecord tournament) {
        clubContentShareService.syncBoardShare(
                tournament.getClubId(),
                CONTENT_TOURNAMENT,
                tournament.getTournamentRecordId(),
                tournament.isSharedToBoard()
        );
        clubContentShareService.syncCalendarShare(
                tournament.getClubId(),
                CONTENT_TOURNAMENT,
                tournament.getTournamentRecordId(),
                tournament.isSharedToCalendar()
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
        String status = resolveTournamentStatus(tournament);
        return STATUS_APPLICATION_OPEN.equals(status);
    }

    private boolean isArchived(String status) {
        return STATUS_COMPLETED.equals(status) || STATUS_CANCELLED.equals(status);
    }

    private String resolveTournamentStatus(TournamentRecord tournament) {
        return resolveTournamentStatus(
                tournament.getApplicationStartAt(),
                tournament.getApplicationEndAt(),
                tournament.getStartDate(),
                tournament.getEndDate(),
                tournament.getCancelledAt(),
                tournament.isBracketConfirmed()
        );
    }

    private String resolveTournamentStatus(
            LocalDateTime applicationStartAt,
            LocalDateTime applicationEndAt,
            LocalDate startDate,
            LocalDate endDate,
            LocalDateTime cancelledAt,
            boolean bracketConfirmed
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
        if (bracketConfirmed) {
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

    private String normalizeBracketMode(String value) {
        String normalized = normalizeRequiredKey(value);
        if (!Set.of(BRACKET_MODE_RANDOM, BRACKET_MODE_MANUAL).contains(normalized)) {
            throw new SemoException.ValidationException("지원하지 않는 대진표 모드입니다.");
        }
        return normalized;
    }

    private String normalizeApplicationReviewStatus(String value) {
        String normalized = normalizeRequiredKey(value);
        if (!Set.of(APPLICATION_APPROVED, APPLICATION_REJECTED).contains(normalized)) {
            throw new SemoException.ValidationException("참가 신청 상태는 APPROVED 또는 REJECTED만 가능합니다.");
        }
        return normalized;
    }

    private String normalizeSideResultStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return SIDE_RESULT_PENDING;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!Set.of(SIDE_RESULT_PENDING, SIDE_RESULT_WINNER, SIDE_RESULT_LOSER, SIDE_RESULT_BYE).contains(normalized)) {
            throw new SemoException.ValidationException("지원하지 않는 side 결과 상태입니다.");
        }
        return normalized;
    }

    private String normalizeCurrencyCode(String value) {
        if (!StringUtils.hasText(value)) {
            return "KRW";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String resolveEntryType(String matchFormat) {
        return switch (normalizeMatchFormat(matchFormat)) {
            case MATCH_FORMAT_SINGLE -> ENTRY_INDIVIDUAL;
            case MATCH_FORMAT_DOUBLE -> ENTRY_PAIR;
            case MATCH_FORMAT_TEAM -> ENTRY_TEAM;
            default -> throw new SemoException.ValidationException("지원하지 않는 경기 형식입니다.");
        };
    }

    private String normalizeMemberRole(String value) {
        if (!StringUtils.hasText(value)) {
            return "PLAYER";
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

    private int nextPowerOfTwo(int value) {
        int power = 1;
        while (power < value) {
            power <<= 1;
        }
        return power;
    }

    private String resolveRoundKey(int bracketSize, int roundIndex) {
        int entrants = bracketSize / (int) Math.pow(2, roundIndex);
        return switch (entrants) {
            case 2 -> "FINAL";
            case 4 -> "SEMI_FINAL";
            case 8 -> "QUARTER_FINAL";
            default -> "ROUND_" + entrants;
        };
    }

    private String resolveRoundLabel(int bracketSize, int roundIndex) {
        int entrants = bracketSize / (int) Math.pow(2, roundIndex);
        return switch (entrants) {
            case 2 -> "Final";
            case 4 -> "Semifinals";
            case 8 -> "Quarterfinals";
            case 16 -> "Round of 16";
            case 32 -> "Round of 32";
            default -> "Round of " + entrants;
        };
    }

    private String resolveMatchTitle(String roundLabel, int matchIndex) {
        return roundLabel + " Match " + (matchIndex + 1);
    }

    private String resolveInitialSideStatus(Long currentEntryId, Long opponentEntryId) {
        if (currentEntryId == null) {
            return opponentEntryId == null ? SIDE_RESULT_PENDING : SIDE_RESULT_BYE;
        }
        if (opponentEntryId == null) {
            return SIDE_RESULT_WINNER;
        }
        return SIDE_RESULT_PENDING;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
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
            boolean pinned,
            String bracketMode
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
