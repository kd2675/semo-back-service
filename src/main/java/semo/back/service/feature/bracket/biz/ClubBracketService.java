package semo.back.service.feature.bracket.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.common.util.ImageFileUrlResolver;
import semo.back.service.database.pub.entity.BracketParticipant;
import semo.back.service.database.pub.entity.BracketRecord;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.TournamentApplication;
import semo.back.service.database.pub.entity.TournamentRecord;
import semo.back.service.database.pub.repository.BracketParticipantRepository;
import semo.back.service.database.pub.repository.BracketRecordRepository;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.TournamentApplicationRepository;
import semo.back.service.database.pub.repository.TournamentRecordRepository;
import semo.back.service.feature.activity.biz.ClubActivityContextHolder;
import semo.back.service.feature.activity.biz.RecordClubActivity;
import semo.back.service.feature.bracket.vo.BracketDetailResponse;
import semo.back.service.feature.bracket.vo.BracketImportParticipantCandidateResponse;
import semo.back.service.feature.bracket.vo.BracketImportTournamentResponse;
import semo.back.service.feature.bracket.vo.BracketMatchResponse;
import semo.back.service.feature.bracket.vo.BracketParticipantResponse;
import semo.back.service.feature.bracket.vo.BracketRoundResponse;
import semo.back.service.feature.bracket.vo.BracketSummaryResponse;
import semo.back.service.feature.bracket.vo.BracketUpsertResponse;
import semo.back.service.feature.bracket.vo.ClubAdminBracketHomeResponse;
import semo.back.service.feature.bracket.vo.ClubBracketHomeResponse;
import semo.back.service.feature.bracket.vo.ReviewBracketRequest;
import semo.back.service.feature.bracket.vo.UpsertBracketParticipantRequest;
import semo.back.service.feature.bracket.vo.UpsertBracketRequest;
import semo.back.service.feature.club.biz.ClubAccessResolver;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubBracketService {
    private static final String BRACKET_TYPE_SINGLE_ELIMINATION = "SINGLE_ELIMINATION";
    private static final String PARTICIPANT_TYPE_MEMBER = "MEMBER";
    private static final String PARTICIPANT_TYPE_GUEST = "GUEST";
    private static final String PARTICIPANT_TYPE_MIXED = "MIXED";
    private static final String SOURCE_TYPE_DIRECT = "DIRECT";
    private static final String SOURCE_TYPE_TOURNAMENT = "TOURNAMENT";
    private static final String APPROVAL_DRAFT = "DRAFT";
    private static final String APPROVAL_PENDING = "PENDING";
    private static final String APPROVAL_APPROVED = "APPROVED";
    private static final String APPROVAL_REJECTED = "REJECTED";
    private static final String PARTICIPANT_ROLE_PLAYER = "PLAYER";
    private static final String ENTRY_SOURCE_DIRECT = "DIRECT";
    private static final String ENTRY_SOURCE_TOURNAMENT = "TOURNAMENT";
    private static final String CLUB_MEMBERSHIP_ACTIVE = "ACTIVE";
    private static final DateTimeFormatter DATE_TIME_LABEL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREAN);
    private static final DateTimeFormatter DATE_LABEL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREAN);

    private final BracketRecordRepository bracketRecordRepository;
    private final BracketParticipantRepository bracketParticipantRepository;
    private final TournamentRecordRepository tournamentRecordRepository;
    private final TournamentApplicationRepository tournamentApplicationRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubProfileRepository clubProfileRepository;
    private final ClubAccessResolver clubAccessResolver;
    private final ClubBracketPermissionService clubBracketPermissionService;
    private final ImageFileUrlResolver imageFileUrlResolver;

    public ClubBracketHomeResponse getBracketHome(Long clubId, String userKey) {
        requireBracketFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        List<BracketRecord> brackets = bracketRecordRepository.findByClubIdAndDeletedFalseOrderByCreateDateDescBracketRecordIdDesc(clubId);
        BracketSnapshot snapshot = loadSnapshot(brackets);
        List<BracketSummaryResponse> summaries = brackets.stream()
                .filter(bracket -> isVisible(access, bracket))
                .map(bracket -> toSummary(access, bracket, snapshot))
                .toList();
        List<BracketSummaryResponse> publishedBrackets = summaries.stream()
                .filter(summary -> APPROVAL_APPROVED.equals(summary.approvalStatus()))
                .toList();
        List<BracketSummaryResponse> myBrackets = summaries.stream()
                .filter(BracketSummaryResponse::mine)
                .toList();

        return new ClubBracketHomeResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                clubBracketPermissionService.canCreateBracket(access),
                publishedBrackets.size(),
                (int) summaries.stream().filter(summary -> APPROVAL_PENDING.equals(summary.approvalStatus())).count(),
                publishedBrackets.isEmpty() ? null : publishedBrackets.get(0),
                publishedBrackets,
                myBrackets,
                loadImportableTournaments(clubId)
        );
    }

    public ClubAdminBracketHomeResponse getAdminBracketHome(Long clubId, String userKey) {
        requireBracketFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        List<BracketRecord> brackets = bracketRecordRepository.findByClubIdAndDeletedFalseOrderByCreateDateDescBracketRecordIdDesc(clubId);
        BracketSnapshot snapshot = loadSnapshot(brackets);
        List<BracketSummaryResponse> summaries = brackets.stream()
                .map(bracket -> toSummary(access, bracket, snapshot))
                .toList();
        return new ClubAdminBracketHomeResponse(
                access.club().getClubId(),
                access.club().getName(),
                true,
                summaries.size(),
                (int) summaries.stream().filter(summary -> APPROVAL_DRAFT.equals(summary.approvalStatus())).count(),
                (int) summaries.stream().filter(summary -> APPROVAL_PENDING.equals(summary.approvalStatus())).count(),
                (int) summaries.stream().filter(summary -> APPROVAL_APPROVED.equals(summary.approvalStatus())).count(),
                (int) summaries.stream().filter(summary -> APPROVAL_REJECTED.equals(summary.approvalStatus())).count(),
                summaries
        );
    }

    public BracketDetailResponse getBracketDetail(Long clubId, Long bracketRecordId, String userKey) {
        requireBracketFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        BracketRecord bracket = getBracket(clubId, bracketRecordId);
        validateVisible(access, bracket);
        return buildDetail(access, bracket);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "대진표")
    public BracketUpsertResponse createBracket(Long clubId, String userKey, UpsertBracketRequest request) {
        requireBracketFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        if (!clubBracketPermissionService.canCreateBracket(access)) {
            throw new SemoException.ForbiddenException("대진표를 생성할 권한이 없습니다.");
        }
        BracketDraft draft = toDraft(clubId, request);
        ClubActivityContextHolder.setDetails(
                "대진표 '" + draft.title() + "' 초안을 생성했습니다.",
                "대진표 '" + draft.title() + "' 초안 생성에 실패했습니다."
        );
        BracketRecord saved = bracketRecordRepository.save(BracketRecord.builder()
                .clubId(clubId)
                .authorClubProfileId(access.clubProfile().getClubProfileId())
                .title(draft.title())
                .summaryText(draft.summaryText())
                .bracketType(draft.bracketType())
                .participantType(draft.participantType())
                .sourceType(draft.sourceType())
                .sourceTournamentRecordId(draft.sourceTournamentRecordId())
                .approvalStatus(APPROVAL_DRAFT)
                .reviewedByClubProfileId(null)
                .reviewedAt(null)
                .rejectionReason(null)
                .participantCount(draft.participants().size())
                .deleted(false)
                .build());
        replaceParticipants(saved.getBracketRecordId(), draft.participants());
        return toUpsertResponse(saved);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "대진표")
    public BracketUpsertResponse updateBracket(
            Long clubId,
            Long bracketRecordId,
            String userKey,
            UpsertBracketRequest request
    ) {
        requireBracketFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        BracketRecord current = getBracket(clubId, bracketRecordId);
        if (!clubBracketPermissionService.canEditOwnBracket(access, current.getAuthorClubProfileId())) {
            throw new SemoException.ForbiddenException("대진표를 수정할 권한이 없습니다.");
        }
        if (APPROVAL_PENDING.equals(current.getApprovalStatus()) || APPROVAL_APPROVED.equals(current.getApprovalStatus())) {
            throw new SemoException.ValidationException("제출 또는 승인된 대진표는 수정할 수 없습니다.");
        }
        BracketDraft draft = toDraft(clubId, request);
        ClubActivityContextHolder.setDetails(
                "대진표 '" + current.getTitle() + "' 초안을 수정했습니다.",
                "대진표 '" + current.getTitle() + "' 초안 수정에 실패했습니다."
        );
        BracketRecord saved = bracketRecordRepository.save(BracketRecord.builder()
                .bracketRecordId(current.getBracketRecordId())
                .clubId(current.getClubId())
                .authorClubProfileId(current.getAuthorClubProfileId())
                .title(draft.title())
                .summaryText(draft.summaryText())
                .bracketType(draft.bracketType())
                .participantType(draft.participantType())
                .sourceType(draft.sourceType())
                .sourceTournamentRecordId(draft.sourceTournamentRecordId())
                .approvalStatus(APPROVAL_DRAFT)
                .reviewedByClubProfileId(null)
                .reviewedAt(null)
                .rejectionReason(null)
                .participantCount(draft.participants().size())
                .deleted(false)
                .build());
        replaceParticipants(saved.getBracketRecordId(), draft.participants());
        return toUpsertResponse(saved);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "대진표")
    public BracketDetailResponse submitBracket(Long clubId, Long bracketRecordId, String userKey) {
        requireBracketFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        BracketRecord current = getBracket(clubId, bracketRecordId);
        if (!clubBracketPermissionService.canEditOwnBracket(access, current.getAuthorClubProfileId())) {
            throw new SemoException.ForbiddenException("대진표를 제출할 권한이 없습니다.");
        }
        if (current.getParticipantCount() < 2) {
            throw new SemoException.ValidationException("대진표 제출에는 최소 2명의 참가자가 필요합니다.");
        }
        if (!APPROVAL_DRAFT.equals(current.getApprovalStatus()) && !APPROVAL_REJECTED.equals(current.getApprovalStatus())) {
            throw new SemoException.ValidationException("제출 가능한 상태의 대진표가 아닙니다.");
        }
        ClubActivityContextHolder.setDetails(
                "대진표 '" + current.getTitle() + "'을 승인 요청했습니다.",
                "대진표 '" + current.getTitle() + "' 승인 요청에 실패했습니다."
        );
        BracketRecord saved = bracketRecordRepository.save(BracketRecord.builder()
                .bracketRecordId(current.getBracketRecordId())
                .clubId(current.getClubId())
                .authorClubProfileId(current.getAuthorClubProfileId())
                .title(current.getTitle())
                .summaryText(current.getSummaryText())
                .bracketType(current.getBracketType())
                .participantType(current.getParticipantType())
                .sourceType(current.getSourceType())
                .sourceTournamentRecordId(current.getSourceTournamentRecordId())
                .approvalStatus(APPROVAL_PENDING)
                .reviewedByClubProfileId(null)
                .reviewedAt(null)
                .rejectionReason(null)
                .participantCount(current.getParticipantCount())
                .deleted(false)
                .build());
        return buildDetail(access, saved);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "대진표")
    public BracketDetailResponse reviewBracket(
            Long clubId,
            Long bracketRecordId,
            String userKey,
            ReviewBracketRequest request
    ) {
        requireBracketFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        if (!clubBracketPermissionService.canReviewBracket(access)) {
            throw new SemoException.ForbiddenException("대진표를 승인할 권한이 없습니다.");
        }
        BracketRecord current = getBracket(clubId, bracketRecordId);
        if (!APPROVAL_PENDING.equals(current.getApprovalStatus())) {
            throw new SemoException.ValidationException("승인 대기 중인 대진표만 검토할 수 있습니다.");
        }
        String approvalStatus = normalizeApprovalStatus(request == null ? null : request.approvalStatus());
        if (!APPROVAL_APPROVED.equals(approvalStatus) && !APPROVAL_REJECTED.equals(approvalStatus)) {
            throw new SemoException.ValidationException("지원하지 않는 승인 상태입니다.");
        }
        String rejectionReason = trimToNull(request == null ? null : request.rejectionReason());
        if (APPROVAL_REJECTED.equals(approvalStatus) && !StringUtils.hasText(rejectionReason)) {
            throw new SemoException.ValidationException("반려 사유를 입력해 주세요.");
        }
        ClubActivityContextHolder.setDetails(
                "대진표 '" + current.getTitle() + "'을 " + (APPROVAL_APPROVED.equals(approvalStatus) ? "승인" : "반려") + "했습니다.",
                "대진표 '" + current.getTitle() + "' 검토에 실패했습니다."
        );
        BracketRecord saved = bracketRecordRepository.save(BracketRecord.builder()
                .bracketRecordId(current.getBracketRecordId())
                .clubId(current.getClubId())
                .authorClubProfileId(current.getAuthorClubProfileId())
                .title(current.getTitle())
                .summaryText(current.getSummaryText())
                .bracketType(current.getBracketType())
                .participantType(current.getParticipantType())
                .sourceType(current.getSourceType())
                .sourceTournamentRecordId(current.getSourceTournamentRecordId())
                .approvalStatus(approvalStatus)
                .reviewedByClubProfileId(access.clubProfile().getClubProfileId())
                .reviewedAt(LocalDateTime.now())
                .rejectionReason(APPROVAL_REJECTED.equals(approvalStatus) ? rejectionReason : null)
                .participantCount(current.getParticipantCount())
                .deleted(false)
                .build());
        return buildDetail(access, saved);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "대진표")
    public void deleteBracket(Long clubId, Long bracketRecordId, String userKey) {
        requireBracketFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        if (!clubBracketPermissionService.canDeleteBracket(access)) {
            throw new SemoException.ForbiddenException("대진표를 삭제할 권한이 없습니다.");
        }
        BracketRecord current = getBracket(clubId, bracketRecordId);
        ClubActivityContextHolder.setDetails(
                "대진표 '" + current.getTitle() + "'을 삭제했습니다.",
                "대진표 '" + current.getTitle() + "' 삭제에 실패했습니다."
        );
        bracketRecordRepository.save(BracketRecord.builder()
                .bracketRecordId(current.getBracketRecordId())
                .clubId(current.getClubId())
                .authorClubProfileId(current.getAuthorClubProfileId())
                .title(current.getTitle())
                .summaryText(current.getSummaryText())
                .bracketType(current.getBracketType())
                .participantType(current.getParticipantType())
                .sourceType(current.getSourceType())
                .sourceTournamentRecordId(current.getSourceTournamentRecordId())
                .approvalStatus(current.getApprovalStatus())
                .reviewedByClubProfileId(current.getReviewedByClubProfileId())
                .reviewedAt(current.getReviewedAt())
                .rejectionReason(current.getRejectionReason())
                .participantCount(current.getParticipantCount())
                .deleted(true)
                .build());
    }

    private BracketSnapshot loadSnapshot(List<BracketRecord> brackets) {
        List<Long> bracketIds = brackets.stream().map(BracketRecord::getBracketRecordId).toList();
        Map<Long, List<BracketParticipant>> participantsByBracketId = bracketParticipantRepository.findByBracketRecordIdIn(bracketIds).stream()
                .collect(Collectors.groupingBy(BracketParticipant::getBracketRecordId));
        Set<Long> clubProfileIds = brackets.stream()
                .flatMap(bracket -> {
                    List<Long> ids = new ArrayList<>();
                    ids.add(bracket.getAuthorClubProfileId());
                    if (bracket.getReviewedByClubProfileId() != null) {
                        ids.add(bracket.getReviewedByClubProfileId());
                    }
                    participantsByBracketId.getOrDefault(bracket.getBracketRecordId(), List.of()).stream()
                            .map(BracketParticipant::getClubProfileId)
                            .filter(id -> id != null)
                            .forEach(ids::add);
                    return ids.stream();
                })
                .collect(Collectors.toSet());
        Map<Long, ClubProfile> clubProfileById = clubProfileRepository.findAllById(clubProfileIds).stream()
                .collect(Collectors.toMap(ClubProfile::getClubProfileId, profile -> profile));
        Map<Long, TournamentRecord> tournamentsById = loadTournamentsById(brackets.stream()
                .map(BracketRecord::getSourceTournamentRecordId)
                .filter(id -> id != null)
                .toList());
        return new BracketSnapshot(participantsByBracketId, clubProfileById, tournamentsById);
    }

    private BracketDetailResponse buildDetail(ClubAccessResolver.ClubAccess access, BracketRecord bracket) {
        List<BracketParticipant> participants = bracketParticipantRepository.findByBracketRecordIdOrderBySeedNumberAscBracketParticipantIdAsc(
                bracket.getBracketRecordId()
        );
        Set<Long> clubProfileIds = participants.stream()
                .map(BracketParticipant::getClubProfileId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        clubProfileIds.add(bracket.getAuthorClubProfileId());
        if (bracket.getReviewedByClubProfileId() != null) {
            clubProfileIds.add(bracket.getReviewedByClubProfileId());
        }
        Map<Long, ClubProfile> clubProfileById = clubProfileRepository.findAllById(clubProfileIds).stream()
                .collect(Collectors.toMap(ClubProfile::getClubProfileId, profile -> profile));
        Map<Long, TournamentRecord> tournamentsById = loadTournamentsById(bracket.getSourceTournamentRecordId() == null
                ? List.of()
                : List.of(bracket.getSourceTournamentRecordId()));
        boolean canEdit = clubBracketPermissionService.canEditOwnBracket(access, bracket.getAuthorClubProfileId())
                && !APPROVAL_PENDING.equals(bracket.getApprovalStatus())
                && !APPROVAL_APPROVED.equals(bracket.getApprovalStatus());
        return new BracketDetailResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                bracket.getBracketRecordId(),
                bracket.getTitle(),
                bracket.getSummaryText(),
                bracket.getApprovalStatus(),
                bracket.getBracketType(),
                bracket.getParticipantType(),
                bracket.getSourceType(),
                bracket.getSourceTournamentRecordId(),
                resolveTournamentTitle(getTournamentById(tournamentsById, bracket.getSourceTournamentRecordId())),
                resolveProfileName(getClubProfileById(clubProfileById, bracket.getAuthorClubProfileId())),
                resolveAvatarImage(getClubProfileById(clubProfileById, bracket.getAuthorClubProfileId())),
                resolveAvatarThumbnail(getClubProfileById(clubProfileById, bracket.getAuthorClubProfileId())),
                resolveProfileName(getClubProfileById(clubProfileById, bracket.getReviewedByClubProfileId())),
                formatDateTime(bracket.getReviewedAt()),
                bracket.getRejectionReason(),
                participants.size(),
                access.clubProfile().getClubProfileId().equals(bracket.getAuthorClubProfileId()),
                canEdit,
                canEdit && participants.size() >= 2,
                clubBracketPermissionService.canDeleteBracket(access),
                clubBracketPermissionService.canReviewBracket(access),
                participants.stream().map(this::toParticipantResponse).toList(),
                buildRounds(participants)
        );
    }

    private BracketSummaryResponse toSummary(
            ClubAccessResolver.ClubAccess access,
            BracketRecord bracket,
            BracketSnapshot snapshot
    ) {
        boolean mine = access.clubProfile().getClubProfileId().equals(bracket.getAuthorClubProfileId());
        boolean canEdit = clubBracketPermissionService.canEditOwnBracket(access, bracket.getAuthorClubProfileId())
                && !APPROVAL_PENDING.equals(bracket.getApprovalStatus())
                && !APPROVAL_APPROVED.equals(bracket.getApprovalStatus());
        return new BracketSummaryResponse(
                bracket.getBracketRecordId(),
                bracket.getTitle(),
                bracket.getSummaryText(),
                bracket.getApprovalStatus(),
                bracket.getBracketType(),
                bracket.getParticipantType(),
                bracket.getSourceType(),
                bracket.getSourceTournamentRecordId(),
                resolveTournamentTitle(getTournamentById(snapshot.tournamentsById(), bracket.getSourceTournamentRecordId())),
                resolveProfileName(getClubProfileById(snapshot.clubProfileById(), bracket.getAuthorClubProfileId())),
                resolveAvatarImage(getClubProfileById(snapshot.clubProfileById(), bracket.getAuthorClubProfileId())),
                resolveAvatarThumbnail(getClubProfileById(snapshot.clubProfileById(), bracket.getAuthorClubProfileId())),
                resolveProfileName(getClubProfileById(snapshot.clubProfileById(), bracket.getReviewedByClubProfileId())),
                formatDateTime(bracket.getReviewedAt()),
                bracket.getRejectionReason(),
                bracket.getParticipantCount(),
                mine,
                canEdit,
                canEdit && bracket.getParticipantCount() >= 2,
                clubBracketPermissionService.canDeleteBracket(access)
        );
    }

    private List<BracketImportTournamentResponse> loadImportableTournaments(Long clubId) {
        List<TournamentRecord> tournaments = tournamentRecordRepository
                .findByClubIdAndDeletedFalseOrderByPinnedDescStartDateAscTournamentRecordIdDesc(clubId).stream()
                .filter(tournament -> APPROVAL_APPROVED.equals(tournament.getApprovalStatus()))
                .toList();
        if (tournaments.isEmpty()) {
            return List.of();
        }
        Map<Long, List<TournamentApplication>> applicationsByTournamentId = tournamentApplicationRepository
                .findByTournamentRecordIdIn(tournaments.stream().map(TournamentRecord::getTournamentRecordId).toList()).stream()
                .filter(application -> APPROVAL_APPROVED.equals(application.getApplicationStatus()))
                .collect(Collectors.groupingBy(TournamentApplication::getTournamentRecordId));
        Set<Long> clubProfileIds = applicationsByTournamentId.values().stream()
                .flatMap(Collection::stream)
                .map(TournamentApplication::getClubProfileId)
                .collect(Collectors.toSet());
        Map<Long, ClubProfile> clubProfileById = clubProfileRepository.findAllById(clubProfileIds).stream()
                .collect(Collectors.toMap(ClubProfile::getClubProfileId, profile -> profile));
        return tournaments.stream()
                .map(tournament -> {
                    List<BracketImportParticipantCandidateResponse> participants = applicationsByTournamentId
                            .getOrDefault(tournament.getTournamentRecordId(), List.of()).stream()
                            .map(application -> new BracketImportParticipantCandidateResponse(
                                    application.getTournamentApplicationId(),
                                    application.getClubProfileId(),
                                    resolveProfileName(clubProfileById.get(application.getClubProfileId()))
                            ))
                            .filter(candidate -> StringUtils.hasText(candidate.displayName()))
                            .toList();
                    return new BracketImportTournamentResponse(
                            tournament.getTournamentRecordId(),
                            tournament.getTitle(),
                            tournament.getSummaryText(),
                            formatTournamentPeriod(tournament.getStartDate(), tournament.getEndDate()),
                            participants.size(),
                            participants
                    );
                })
                .toList();
    }

    private BracketDraft toDraft(Long clubId, UpsertBracketRequest request) {
        if (request == null) {
            throw new SemoException.ValidationException("대진표 요청이 비어 있습니다.");
        }
        String title = requireTitle(request.title());
        String bracketType = normalizeBracketType(request.bracketType());
        String participantType = normalizeParticipantType(request.participantType());
        String sourceType = normalizeSourceType(request.sourceType());
        Long sourceTournamentRecordId = request.sourceTournamentRecordId();
        List<BracketDraftParticipant> participants = normalizeParticipants(
                clubId,
                participantType,
                sourceType,
                sourceTournamentRecordId,
                request.participants()
        );
        if (participants.size() < 2) {
            throw new SemoException.ValidationException("대진표에는 최소 2명의 참가자가 필요합니다.");
        }
        return new BracketDraft(
                title,
                trimToNull(request.summaryText()),
                bracketType,
                participantType,
                sourceType,
                sourceTournamentRecordId,
                participants
        );
    }

    private List<BracketDraftParticipant> normalizeParticipants(
            Long clubId,
            String participantType,
            String sourceType,
            Long sourceTournamentRecordId,
            List<UpsertBracketParticipantRequest> requestParticipants
    ) {
        List<UpsertBracketParticipantRequest> safeParticipants = requestParticipants == null ? List.of() : requestParticipants;
        if (SOURCE_TYPE_TOURNAMENT.equals(sourceType) && !PARTICIPANT_TYPE_MEMBER.equals(participantType)) {
            throw new SemoException.ValidationException("대회 불러오기 대진표는 회원 참가자만 사용할 수 있습니다.");
        }
        Map<Long, TournamentApplication> approvedApplicationById = Map.of();
        Map<Long, ClubProfile> clubProfileById = Map.of();
        if (SOURCE_TYPE_TOURNAMENT.equals(sourceType)) {
            if (sourceTournamentRecordId == null) {
                throw new SemoException.ValidationException("대회 불러오기 대진표에는 sourceTournamentRecordId가 필요합니다.");
            }
            TournamentRecord tournament = tournamentRecordRepository
                    .findByTournamentRecordIdAndClubIdAndDeletedFalse(sourceTournamentRecordId, clubId)
                    .orElseThrow(() -> new SemoException.ResourceNotFoundException("TournamentRecord", "tournamentRecordId", sourceTournamentRecordId));
            if (!APPROVAL_APPROVED.equals(tournament.getApprovalStatus())) {
                throw new SemoException.ValidationException("승인된 대회만 대진표로 불러올 수 있습니다.");
            }
            List<TournamentApplication> approvedApplications = tournamentApplicationRepository
                    .findByTournamentRecordIdAndApplicationStatusOrderByCreateDateAscTournamentApplicationIdAsc(
                            sourceTournamentRecordId,
                            APPROVAL_APPROVED
                    );
            approvedApplicationById = approvedApplications.stream()
                    .collect(Collectors.toMap(TournamentApplication::getTournamentApplicationId, application -> application));
            clubProfileById = clubProfileRepository.findAllById(approvedApplications.stream()
                            .map(TournamentApplication::getClubProfileId)
                            .toList())
                    .stream()
                    .collect(Collectors.toMap(ClubProfile::getClubProfileId, profile -> profile));
	        } else if (sourceTournamentRecordId != null) {
            throw new SemoException.ValidationException("직접 작성 대진표에는 sourceTournamentRecordId를 지정할 수 없습니다.");
        }

        if (!SOURCE_TYPE_TOURNAMENT.equals(sourceType) && safeParticipants.stream()
                .map(item -> item == null ? null : item.sourceTournamentApplicationId())
                .anyMatch(id -> id != null)) {
            throw new SemoException.ValidationException("직접 작성 대진표에는 sourceTournamentApplicationId를 지정할 수 없습니다.");
        }

        if (!SOURCE_TYPE_TOURNAMENT.equals(sourceType)) {
            clubProfileById = resolveActiveClubProfiles(
                    clubId,
                    safeParticipants.stream()
                            .map(item -> item == null ? null : item.clubProfileId())
                            .filter(id -> id != null)
                            .toList()
            );
        }

        List<BracketDraftParticipant> normalized = new ArrayList<>();
        for (int index = 0; index < safeParticipants.size(); index++) {
            UpsertBracketParticipantRequest item = safeParticipants.get(index);
            int seedNumber = item == null || item.seedNumber() == null || item.seedNumber() <= 0
                    ? index + 1
                    : item.seedNumber();
            Long clubProfileId = item == null ? null : item.clubProfileId();
            Long sourceTournamentApplicationId = item == null ? null : item.sourceTournamentApplicationId();
            String displayName = trimToNull(item == null ? null : item.displayName());
            boolean guestEntry = false;
            String entrySourceType = ENTRY_SOURCE_DIRECT;

            if (SOURCE_TYPE_TOURNAMENT.equals(sourceType)) {
                TournamentApplication application = approvedApplicationById.get(sourceTournamentApplicationId);
                if (application == null) {
                    throw new SemoException.ValidationException("대회 승인 참가자만 대진표에 불러올 수 있습니다.");
                }
                clubProfileId = application.getClubProfileId();
                displayName = displayName == null
                        ? resolveProfileName(clubProfileById.get(application.getClubProfileId()))
                        : displayName;
                entrySourceType = ENTRY_SOURCE_TOURNAMENT;
            } else if (clubProfileId != null) {
                ClubProfile clubProfile = clubProfileById.get(clubProfileId);
                if (clubProfile == null) {
                    throw new SemoException.ValidationException("현재 클럽 소속 멤버만 참가자로 지정할 수 있습니다.");
                }
            }

            if (!StringUtils.hasText(displayName)) {
                if (clubProfileId == null) {
                    throw new SemoException.ValidationException("참가자 이름을 입력해 주세요.");
                }
                Long requiredClubProfileId = clubProfileId;
                ClubProfile clubProfile = clubProfileById.get(requiredClubProfileId);
                if (clubProfile == null) {
                    throw new SemoException.ValidationException("현재 클럽 소속 멤버만 참가자로 지정할 수 있습니다.");
                }
                displayName = resolveProfileName(clubProfile);
            }

            if (PARTICIPANT_TYPE_GUEST.equals(participantType)) {
                if (clubProfileId != null) {
                    throw new SemoException.ValidationException("게스트 전용 대진표에는 멤버 프로필을 지정할 수 없습니다.");
                }
                clubProfileId = null;
                guestEntry = true;
            } else if (PARTICIPANT_TYPE_MEMBER.equals(participantType)) {
                if (clubProfileId == null) {
                    throw new SemoException.ValidationException("회원 참가자는 현재 클럽 멤버를 지정해야 합니다.");
                }
                guestEntry = false;
            } else if (PARTICIPANT_TYPE_MIXED.equals(participantType)) {
                guestEntry = clubProfileId == null;
            }

            normalized.add(new BracketDraftParticipant(
                    seedNumber,
                    clubProfileId,
                    displayName,
                    PARTICIPANT_ROLE_PLAYER,
                    entrySourceType,
                    sourceTournamentApplicationId,
                    guestEntry
            ));
        }
        ensureUniqueSeeds(normalized);
        return normalized.stream()
                .sorted(Comparator.comparingInt(BracketDraftParticipant::seedNumber))
                .toList();
    }

    private Map<Long, ClubProfile> resolveActiveClubProfiles(Long clubId, List<Long> clubProfileIds) {
        List<Long> requestedIds = clubProfileIds.stream()
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (requestedIds.isEmpty()) {
            return Map.of();
        }
        List<ClubProfile> profiles = clubProfileRepository.findAllById(requestedIds);
        Map<Long, ClubProfile> profileById = profiles.stream()
                .collect(Collectors.toMap(ClubProfile::getClubProfileId, profile -> profile));
        List<Long> clubMemberIds = profiles.stream()
                .map(ClubProfile::getClubMemberId)
                .distinct()
                .toList();
        Set<Long> activeClubMemberIds = clubMemberRepository
                .findByClubIdAndClubMemberIdInAndMembershipStatus(clubId, clubMemberIds, CLUB_MEMBERSHIP_ACTIVE)
                .stream()
                .map(clubMember -> clubMember.getClubMemberId())
                .collect(Collectors.toSet());
        return profiles.stream()
                .filter(profile -> activeClubMemberIds.contains(profile.getClubMemberId()))
                .collect(Collectors.toMap(ClubProfile::getClubProfileId, profile -> profile));
    }

    private void replaceParticipants(Long bracketRecordId, List<BracketDraftParticipant> participants) {
        bracketParticipantRepository.deleteByBracketRecordId(bracketRecordId);
        for (BracketDraftParticipant participant : participants) {
            bracketParticipantRepository.save(BracketParticipant.builder()
                    .bracketRecordId(bracketRecordId)
                    .seedNumber(participant.seedNumber())
                    .clubProfileId(participant.clubProfileId())
                    .displayName(participant.displayName())
                    .participantRole(participant.participantRole())
                    .entrySourceType(participant.entrySourceType())
                    .sourceTournamentApplicationId(participant.sourceTournamentApplicationId())
                    .guestEntry(participant.guestEntry())
                    .build());
        }
    }

    private List<BracketRoundResponse> buildRounds(List<BracketParticipant> participants) {
        if (participants.isEmpty()) {
            return List.of();
        }
        int bracketSize = 1;
        while (bracketSize < participants.size()) {
            bracketSize *= 2;
        }
        List<String> currentSlots = new ArrayList<>();
        for (BracketParticipant participant : participants.stream()
                .sorted(Comparator.comparingInt(BracketParticipant::getSeedNumber))
                .toList()) {
            currentSlots.add(participant.getDisplayName());
        }
        while (currentSlots.size() < bracketSize) {
            currentSlots.add(null);
        }

        List<BracketRoundResponse> rounds = new ArrayList<>();
        int roundNumber = 1;
        while (currentSlots.size() >= 2) {
            List<BracketMatchResponse> matches = new ArrayList<>();
            List<String> nextRoundSlots = new ArrayList<>();
            for (int index = 0; index < currentSlots.size(); index += 2) {
                matches.add(new BracketMatchResponse(
                        (index / 2) + 1,
                        currentSlots.get(index),
                        currentSlots.get(index + 1)
                ));
                nextRoundSlots.add("Winner M" + ((index / 2) + 1));
            }
            rounds.add(new BracketRoundResponse(roundNumber, resolveRoundTitle(matches.size()), matches));
            currentSlots = nextRoundSlots;
            roundNumber += 1;
        }
        return rounds;
    }

    private String resolveRoundTitle(int matchCount) {
        return switch (matchCount) {
            case 1 -> "결승";
            case 2 -> "4강";
            case 4 -> "8강";
            default -> matchCount * 2 + "강";
        };
    }

    private BracketParticipantResponse toParticipantResponse(BracketParticipant participant) {
        return new BracketParticipantResponse(
                participant.getBracketParticipantId(),
                participant.getSeedNumber(),
                participant.getClubProfileId(),
                participant.getDisplayName(),
                participant.isGuestEntry(),
                participant.getParticipantRole(),
                participant.getEntrySourceType(),
                participant.getSourceTournamentApplicationId()
        );
    }

    private Map<Long, TournamentRecord> loadTournamentsById(List<Long> tournamentIds) {
        List<Long> ids = tournamentIds.stream()
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return tournamentRecordRepository.findAllByTournamentRecordIdIn(ids).stream()
                .collect(Collectors.toMap(TournamentRecord::getTournamentRecordId, tournament -> tournament));
    }

    private BracketRecord getBracket(Long clubId, Long bracketRecordId) {
        return bracketRecordRepository.findByBracketRecordIdAndClubIdAndDeletedFalse(bracketRecordId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("BracketRecord", "bracketRecordId", bracketRecordId));
    }

    private boolean isVisible(ClubAccessResolver.ClubAccess access, BracketRecord bracket) {
        if (access.isAdmin()) {
            return true;
        }
        if (APPROVAL_APPROVED.equals(bracket.getApprovalStatus())) {
            return true;
        }
        return access.clubProfile().getClubProfileId().equals(bracket.getAuthorClubProfileId());
    }

    private void validateVisible(ClubAccessResolver.ClubAccess access, BracketRecord bracket) {
        if (!isVisible(access, bracket)) {
            throw new SemoException.ForbiddenException("해당 대진표를 조회할 수 없습니다.");
        }
    }

    private void requireBracketFeature(Long clubId) {
        if (!clubBracketPermissionService.isBracketEnabled(clubId)) {
            throw new SemoException.ValidationException("대진표 기능이 활성화되지 않았습니다.");
        }
    }

    private String requireTitle(String title) {
        String trimmed = trimToNull(title);
        if (!StringUtils.hasText(trimmed)) {
            throw new SemoException.ValidationException("대진표 제목은 필수입니다.");
        }
        return trimmed;
    }

    private String normalizeBracketType(String bracketType) {
        String normalized = normalizeToken(bracketType);
        if (!BRACKET_TYPE_SINGLE_ELIMINATION.equals(normalized)) {
            throw new SemoException.ValidationException("현재는 SINGLE_ELIMINATION 형식만 지원합니다.");
        }
        return normalized;
    }

    private String normalizeParticipantType(String participantType) {
        String normalized = normalizeToken(participantType);
        return switch (normalized) {
            case PARTICIPANT_TYPE_MEMBER, PARTICIPANT_TYPE_GUEST, PARTICIPANT_TYPE_MIXED -> normalized;
            default -> throw new SemoException.ValidationException("지원하지 않는 참가자 유형입니다.");
        };
    }

    private String normalizeSourceType(String sourceType) {
        String normalized = normalizeToken(sourceType);
        return switch (normalized) {
            case SOURCE_TYPE_DIRECT, SOURCE_TYPE_TOURNAMENT -> normalized;
            default -> throw new SemoException.ValidationException("지원하지 않는 대진표 소스 유형입니다.");
        };
    }

    private String normalizeApprovalStatus(String approvalStatus) {
        return normalizeToken(approvalStatus);
    }

    private void ensureUniqueSeeds(List<BracketDraftParticipant> participants) {
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        for (BracketDraftParticipant participant : participants) {
            counts.merge(participant.seedNumber(), 1, Integer::sum);
        }
        Integer duplicatedSeed = counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
        if (duplicatedSeed != null) {
            throw new SemoException.ValidationException("중복된 시드 번호가 있습니다: " + duplicatedSeed);
        }
    }

    private String resolveProfileName(ClubProfile clubProfile) {
        return clubProfile == null ? null : clubProfile.getDisplayName();
    }

    private String resolveAvatarImage(ClubProfile clubProfile) {
        return clubProfile == null ? null : imageFileUrlResolver.resolveImageUrl(clubProfile.getAvatarFileName());
    }

    private String resolveAvatarThumbnail(ClubProfile clubProfile) {
        return clubProfile == null ? null : imageFileUrlResolver.resolveThumbnailUrl(clubProfile.getAvatarFileName());
    }

    private String resolveTournamentTitle(TournamentRecord tournament) {
        return tournament == null ? null : tournament.getTitle();
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_LABEL_FORMATTER);
    }

    private String formatTournamentPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            return null;
        }
        if (endDate == null || startDate.equals(endDate)) {
            return startDate.format(DATE_LABEL_FORMATTER);
        }
        return startDate.format(DATE_LABEL_FORMATTER) + " - " + endDate.format(DATE_LABEL_FORMATTER);
    }

    private BracketUpsertResponse toUpsertResponse(BracketRecord bracket) {
        return new BracketUpsertResponse(
                bracket.getBracketRecordId(),
                bracket.getTitle(),
                bracket.getApprovalStatus(),
                bracket.getParticipantCount()
        );
    }

    private TournamentRecord getTournamentById(Map<Long, TournamentRecord> tournamentsById, Long tournamentRecordId) {
        if (tournamentRecordId == null) {
            return null;
        }
        return tournamentsById.get(tournamentRecordId);
    }

    private ClubProfile getClubProfileById(Map<Long, ClubProfile> clubProfileById, Long clubProfileId) {
        if (clubProfileId == null) {
            return null;
        }
        return clubProfileById.get(clubProfileId);
    }

    private String normalizeToken(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private record BracketSnapshot(
            Map<Long, List<BracketParticipant>> participantsByBracketId,
            Map<Long, ClubProfile> clubProfileById,
            Map<Long, TournamentRecord> tournamentsById
    ) {
    }

    private record BracketDraft(
            String title,
            String summaryText,
            String bracketType,
            String participantType,
            String sourceType,
            Long sourceTournamentRecordId,
            List<BracketDraftParticipant> participants
    ) {
    }

    private record BracketDraftParticipant(
            int seedNumber,
            Long clubProfileId,
            String displayName,
            String participantRole,
            String entrySourceType,
            Long sourceTournamentApplicationId,
            boolean guestEntry
    ) {
    }
}
