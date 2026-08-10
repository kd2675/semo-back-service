package semo.back.service.feature.handover.biz;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubFeedback;
import semo.back.service.database.pub.entity.ClubHandoverNote;
import semo.back.service.database.pub.entity.ClubMemberPosition;
import semo.back.service.database.pub.entity.ClubOperatingTerm;
import semo.back.service.database.pub.entity.ClubPosition;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.ClubScheduleEvent;
import semo.back.service.database.pub.entity.ClubTermExecutiveAssignment;
import semo.back.service.database.pub.entity.ClubTermCarryoverItem;
import semo.back.service.database.pub.entity.FinanceExpense;
import semo.back.service.database.pub.entity.FinanceRequest;
import semo.back.service.database.pub.entity.DecisionRecord;
import semo.back.service.database.pub.entity.TodoItem;
import semo.back.service.database.pub.entity.TournamentRecord;
import semo.back.service.database.pub.repository.ClubFeedbackRepository;
import semo.back.service.database.pub.repository.ClubHandoverNoteRepository;
import semo.back.service.database.pub.repository.ClubJoinRequestRepository;
import semo.back.service.database.pub.repository.ClubMemberPositionRepository;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubOperatingTermRepository;
import semo.back.service.database.pub.repository.ClubPositionRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.ClubTermExecutiveAssignmentRepository;
import semo.back.service.database.pub.repository.ClubTermCarryoverItemRepository;
import semo.back.service.database.pub.repository.FinanceExpenseRepository;
import semo.back.service.database.pub.repository.FinanceObligationRepository;
import semo.back.service.database.pub.repository.FinancePaymentRepository;
import semo.back.service.database.pub.repository.FinanceRequestRepository;
import semo.back.service.database.pub.repository.DecisionRecordRepository;
import semo.back.service.database.pub.repository.TodoItemRepository;
import semo.back.service.database.pub.repository.TournamentRecordRepository;
import semo.back.service.feature.activity.biz.ClubActivityContextHolder;
import semo.back.service.feature.activity.biz.RecordClubActivity;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.finance.vo.ClubAdminFinanceSummaryAggregate;
import semo.back.service.feature.handover.vo.ClubExecutiveAssignmentResponse;
import semo.back.service.feature.handover.vo.ClubHandoverCenterResponse;
import semo.back.service.feature.handover.vo.ClubHandoverNoteResponse;
import semo.back.service.feature.handover.vo.ClubOperatingTermResponse;
import semo.back.service.feature.handover.vo.ClubTermMetricsResponse;
import semo.back.service.feature.handover.vo.ClubTermCarryoverItemResponse;
import semo.back.service.feature.handover.vo.CreateOperatingTermRequest;
import semo.back.service.feature.handover.vo.HandoverMemberOptionResponse;
import semo.back.service.feature.handover.vo.HandoverPositionOptionResponse;
import semo.back.service.feature.handover.vo.HandoverQueueItemResponse;
import semo.back.service.feature.handover.vo.HandoverQueueSummaryResponse;
import semo.back.service.feature.handover.vo.HandoverRecentDecisionResponse;
import semo.back.service.feature.handover.vo.UpdateOperatingTermRequest;
import semo.back.service.feature.handover.vo.UpsertExecutiveAssignmentRequest;
import semo.back.service.feature.handover.vo.UpsertHandoverNoteRequest;
import semo.back.service.feature.notification.biz.ClubNotificationPublisher;
import semo.back.service.feature.position.biz.ClubPositionPermissionEvaluator;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubHandoverService {
    public static final String FEATURE_HANDOVER = "HANDOVER";
    private static final Set<String> TERM_TYPES = Set.of("YEAR", "SEMESTER", "SEASON", "CUSTOM");
    private static final Set<String> EDITABLE_NOTE_STATUSES = Set.of("DRAFT", "READY");
    private static final Set<String> OPEN_TODO_STATUSES = Set.of("OPEN", "IN_PROGRESS");
    private static final Set<String> OPEN_FEEDBACK_STATUSES = Set.of("RECEIVED", "IN_REVIEW");
    private static final String REQUEST_STATUS_SUBMITTED = "SUBMITTED";

    private final ClubAccessResolver clubAccessResolver;
    private final ClubFeatureService clubFeatureService;
    private final ClubPositionPermissionEvaluator clubPositionPermissionEvaluator;
    private final ClubRepository clubRepository;
    private final ClubOperatingTermRepository clubOperatingTermRepository;
    private final ClubTermExecutiveAssignmentRepository clubTermExecutiveAssignmentRepository;
    private final ClubTermCarryoverItemRepository clubTermCarryoverItemRepository;
    private final ClubHandoverNoteRepository clubHandoverNoteRepository;
    private final ClubPositionRepository clubPositionRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubProfileRepository clubProfileRepository;
    private final ClubMemberPositionRepository clubMemberPositionRepository;
    private final TodoItemRepository todoItemRepository;
    private final FinancePaymentRepository financePaymentRepository;
    private final FinanceRequestRepository financeRequestRepository;
    private final FinanceObligationRepository financeObligationRepository;
    private final FinanceExpenseRepository financeExpenseRepository;
    private final ClubScheduleEventRepository clubScheduleEventRepository;
    private final ClubFeedbackRepository clubFeedbackRepository;
    private final ClubJoinRequestRepository clubJoinRequestRepository;
    private final TournamentRecordRepository tournamentRecordRepository;
    private final ClubNotificationPublisher clubNotificationPublisher;
    private final DecisionRecordRepository decisionRecordRepository;

    public ClubHandoverCenterResponse getCenter(Long clubId, String userKey, Long selectedTermId) {
        ClubAccessResolver.ClubAccess access = requireViewAccess(clubId, userKey);
        boolean canManage = canManage(access);
        List<ClubOperatingTerm> terms = clubOperatingTermRepository
                .findByClubIdOrderByStartDateDescClubOperatingTermIdDesc(clubId);
        ClubOperatingTerm activeTerm = terms.stream()
                .filter(term -> "ACTIVE".equals(term.getStatusCode()))
                .findFirst()
                .orElse(null);
        ClubOperatingTerm nextTerm = terms.stream()
                .filter(term -> "PLANNED".equals(term.getStatusCode()))
                .filter(term -> activeTerm == null || !term.getStartDate().isBefore(activeTerm.getStartDate()))
                .min(Comparator.comparing(ClubOperatingTerm::getStartDate)
                        .thenComparing(ClubOperatingTerm::getClubOperatingTermId))
                .orElse(null);
        ClubOperatingTerm selectedTerm = resolveSelectedTerm(clubId, selectedTermId, activeTerm, nextTerm, terms);
        List<Long> rosterTermIds = compactIds(activeTerm, nextTerm, selectedTerm);
        List<ClubTermExecutiveAssignment> assignments = rosterTermIds.isEmpty()
                ? List.of()
                : clubTermExecutiveAssignmentRepository
                        .findByClubOperatingTermIdInOrderBySortOrderAscClubTermExecutiveAssignmentIdAsc(rosterTermIds);
        Long noteTermId = selectedTerm == null ? null : selectedTerm.getClubOperatingTermId();
        List<ClubHandoverNote> handoverNotes = clubHandoverNoteRepository.findFeed(clubId, noteTermId);
        List<ClubAccessResolver.ClubMemberSnapshot> members = clubAccessResolver.getActiveMemberSnapshots(clubId);
        List<ClubPosition> allPositions = clubPositionRepository
                .findByClubIdOrderByDisplayNameAscClubPositionIdAsc(clubId);
        List<ClubPosition> activePositions = allPositions.stream()
                .filter(ClubPosition::isActive)
                .toList();
        Set<Long> referencedProfileIds = new LinkedHashSet<>();
        members.stream()
                .map(ClubAccessResolver.ClubMemberSnapshot::clubProfile)
                .map(ClubProfile::getClubProfileId)
                .forEach(referencedProfileIds::add);
        assignments.stream()
                .map(ClubTermExecutiveAssignment::getClubProfileId)
                .forEach(referencedProfileIds::add);
        handoverNotes.forEach(note -> {
            if (note.getAssignedClubProfileId() != null) {
                referencedProfileIds.add(note.getAssignedClubProfileId());
            }
            if (note.getCreatedByClubProfileId() != null) {
                referencedProfileIds.add(note.getCreatedByClubProfileId());
            }
            if (note.getAcknowledgedByClubProfileId() != null) {
                referencedProfileIds.add(note.getAcknowledgedByClubProfileId());
            }
        });
        Map<Long, ClubProfile> profilesById = referencedProfileIds.isEmpty()
                ? Map.of()
                : clubProfileRepository.findAllById(referencedProfileIds).stream()
                        .collect(Collectors.toMap(ClubProfile::getClubProfileId, Function.identity()));
        Map<Long, ClubPosition> positionsById = allPositions.stream()
                .collect(Collectors.toMap(ClubPosition::getClubPositionId, Function.identity()));
        Map<Long, ClubOperatingTerm> termsById = terms.stream()
                .collect(Collectors.toMap(ClubOperatingTerm::getClubOperatingTermId, Function.identity()));

        QueueSnapshot queue = buildQueue(clubId);
        List<HandoverRecentDecisionResponse> recentDecisions = clubFeatureService.isFeatureEnabled(
                clubId,
                "DECISION_LOG"
        ) ? decisionRecordRepository.findRecentConfirmed(clubId, PageRequest.of(0, 3)).stream()
                .map(item -> toRecentDecisionResponse(clubId, item))
                .toList() : List.of();
        return new ClubHandoverCenterResponse(
                clubId,
                access.club().getName(),
                access.isAdmin(),
                canManage,
                access.clubProfile().getClubProfileId(),
                toTermResponse(activeTerm),
                toTermResponse(nextTerm),
                toTermResponse(selectedTerm),
                terms.stream().map(this::toTermResponse).toList(),
                assignments.stream()
                        .map(item -> toAssignmentResponse(item, profilesById, positionsById))
                        .toList(),
                handoverNotes.stream()
                        .map(item -> toNoteResponse(item, termsById, positionsById, profilesById))
                        .toList(),
                selectedTerm == null
                        ? List.of()
                        : clubTermCarryoverItemRepository
                                .findByToTermIdOrderByStatusCodeAscDueAtAscClubTermCarryoverItemIdAsc(
                                        selectedTerm.getClubOperatingTermId()
                                ).stream()
                                .map(item -> toCarryoverResponse(item, termsById))
                                .toList(),
                queue.summary(),
                queue.items(),
                recentDecisions,
                selectedTerm == null ? ClubTermMetricsResponse.empty() : buildTermMetrics(clubId, selectedTerm),
                members.stream()
                        .map(item -> new HandoverMemberOptionResponse(
                                item.membership().getClubMemberId(),
                                item.clubProfile().getClubProfileId(),
                                item.clubProfile().getDisplayName(),
                                item.clubProfile().getAvatarFileName()
                        ))
                        .toList(),
                activePositions.stream()
                        .map(item -> new HandoverPositionOptionResponse(
                                item.getClubPositionId(),
                                item.getDisplayName(),
                                item.getDescription(),
                                item.getIconName(),
                                item.getColorHex()
                        ))
                        .toList()
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "운영임기")
    public ClubOperatingTermResponse createTerm(Long clubId, String userKey, CreateOperatingTermRequest request) {
        ClubAccessResolver.ClubAccess access = requireManageAccess(clubId, userKey);
        lockClub(clubId);
        TermDraft draft = normalizeTermDraft(
                request.termName(),
                request.termType(),
                request.startDate(),
                request.endDate(),
                request.description()
        );
        if (clubOperatingTermRepository.existsByClubIdAndTermNameAndStartDate(
                clubId,
                draft.termName(),
                draft.startDate()
        )) {
            throw new SemoException.ConflictException("같은 이름과 시작일의 운영 임기가 이미 존재합니다.");
        }
        ClubActivityContextHolder.setDetails(
                "운영 임기 '" + draft.termName() + "'을 만들었습니다.",
                "운영 임기를 만들지 못했습니다."
        );
        ClubOperatingTerm term = clubOperatingTermRepository.save(ClubOperatingTerm.builder()
                .clubId(clubId)
                .termName(draft.termName())
                .termType(draft.termType())
                .startDate(draft.startDate())
                .endDate(draft.endDate())
                .statusCode("PLANNED")
                .description(draft.description())
                .createdByClubProfileId(access.clubProfile().getClubProfileId())
                .build());
        return toTermResponse(term);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "운영임기")
    public ClubOperatingTermResponse updateTerm(
            Long clubId,
            Long termId,
            String userKey,
            UpdateOperatingTermRequest request
    ) {
        requireManageAccess(clubId, userKey);
        lockClub(clubId);
        ClubOperatingTerm term = requireTerm(clubId, termId);
        if ("CLOSED".equals(term.getStatusCode())) {
            throw new SemoException.ConflictException("종료된 운영 임기는 수정할 수 없습니다.");
        }
        TermDraft draft = normalizeTermDraft(
                request.termName(),
                request.termType(),
                request.startDate(),
                request.endDate(),
                request.description()
        );
        if (clubOperatingTermRepository.existsByClubIdAndTermNameAndStartDateAndClubOperatingTermIdNot(
                clubId,
                draft.termName(),
                draft.startDate(),
                termId
        )) {
            throw new SemoException.ConflictException("같은 이름과 시작일의 운영 임기가 이미 존재합니다.");
        }
        ClubActivityContextHolder.setDetails(
                "운영 임기 '" + term.getTermName() + "'을 수정했습니다.",
                "운영 임기를 수정하지 못했습니다."
        );
        term.update(draft.termName(), draft.termType(), draft.startDate(), draft.endDate(), draft.description());
        return toTermResponse(term);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "운영임기")
    public ClubOperatingTermResponse activateTerm(Long clubId, Long termId, String userKey) {
        ClubAccessResolver.ClubAccess access = requireManageAccess(clubId, userKey);
        lockClub(clubId);
        ClubOperatingTerm term = requireTerm(clubId, termId);
        if ("CLOSED".equals(term.getStatusCode())) {
            throw new SemoException.ConflictException("종료된 운영 임기는 다시 활성화할 수 없습니다.");
        }
        if ("ACTIVE".equals(term.getStatusCode())) {
            bootstrapExecutiveAssignments(clubId, term, access.clubProfile().getClubProfileId());
            return toTermResponse(term);
        }
        LocalDateTime now = LocalDateTime.now();
        ClubOperatingTerm currentTerm = clubOperatingTermRepository
                .findFirstByClubIdAndStatusCodeOrderByStartDateDescClubOperatingTermIdDesc(clubId, "ACTIVE")
                .filter(current -> !current.getClubOperatingTermId().equals(termId))
                .orElse(null);
        ClubOperatingTerm sourceTerm = currentTerm;
        if (sourceTerm == null) {
            sourceTerm = clubOperatingTermRepository.findByClubIdOrderByStartDateDescClubOperatingTermIdDesc(clubId)
                    .stream()
                    .filter(candidate -> "CLOSED".equals(candidate.getStatusCode()))
                    .filter(candidate -> candidate.getStartDate().isBefore(term.getStartDate()))
                    .findFirst()
                    .orElse(null);
        }
        if (currentTerm != null) {
            currentTerm.close(access.clubProfile().getClubProfileId(), now);
        }
        if (sourceTerm != null) {
            carryOverOpenItems(
                    clubId,
                    sourceTerm,
                    term,
                    buildCarryoverCandidates(clubId),
                    access.clubProfile().getClubProfileId(),
                    now
            );
        }
        term.activate(access.clubProfile().getClubProfileId(), now);
        bootstrapExecutiveAssignments(clubId, term, access.clubProfile().getClubProfileId());
        ClubActivityContextHolder.setDetails(
                "운영 임기 '" + term.getTermName() + "'을 시작했습니다.",
                "운영 임기를 시작하지 못했습니다."
        );
        return toTermResponse(term);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "운영임기")
    public ClubOperatingTermResponse closeTerm(Long clubId, Long termId, String userKey) {
        ClubAccessResolver.ClubAccess access = requireManageAccess(clubId, userKey);
        lockClub(clubId);
        ClubOperatingTerm term = requireTerm(clubId, termId);
        if (!"ACTIVE".equals(term.getStatusCode())) {
            throw new SemoException.ConflictException("진행 중인 운영 임기만 종료할 수 있습니다.");
        }
        term.close(access.clubProfile().getClubProfileId(), LocalDateTime.now());
        ClubActivityContextHolder.setDetails(
                "운영 임기 '" + term.getTermName() + "'을 종료했습니다.",
                "운영 임기를 종료하지 못했습니다."
        );
        return toTermResponse(term);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "집행부구성")
    public ClubExecutiveAssignmentResponse upsertExecutiveAssignment(
            Long clubId,
            Long termId,
            String userKey,
            UpsertExecutiveAssignmentRequest request
    ) {
        ClubAccessResolver.ClubAccess access = requireManageAccess(clubId, userKey);
        lockClub(clubId);
        ClubOperatingTerm term = requireEditableTerm(clubId, termId);
        ClubAccessResolver.ClubMemberSnapshot member = requireActiveMember(clubId, request.clubMemberId());
        ClubPosition position = clubPositionRepository
                .findByClubPositionIdAndClubId(request.clubPositionId(), clubId)
                .filter(ClubPosition::isActive)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "ClubPosition",
                        "clubPositionId",
                        request.clubPositionId()
                ));
        String responsibility = trimToNull(request.responsibility());
        int sortOrder = request.sortOrder() == null ? 0 : request.sortOrder();
        ClubTermExecutiveAssignment assignment = clubTermExecutiveAssignmentRepository
                .findByClubOperatingTermIdAndClubMemberIdAndClubPositionId(
                        termId,
                        request.clubMemberId(),
                        request.clubPositionId()
                )
                .orElseGet(() -> ClubTermExecutiveAssignment.builder()
                        .clubId(clubId)
                        .clubOperatingTermId(termId)
                        .clubMemberId(member.membership().getClubMemberId())
                        .clubProfileId(member.clubProfile().getClubProfileId())
                        .clubPositionId(position.getClubPositionId())
                        .createdByClubProfileId(access.clubProfile().getClubProfileId())
                        .build());
        assignment.update(responsibility, sortOrder, access.clubProfile().getClubProfileId());
        ClubTermExecutiveAssignment saved = clubTermExecutiveAssignmentRepository.save(assignment);
        ClubActivityContextHolder.setDetails(
                term.getTermName() + " 집행부에 " + member.clubProfile().getDisplayName() + " 멤버를 반영했습니다.",
                "집행부 구성을 저장하지 못했습니다."
        );
        return toAssignmentResponse(
                saved,
                Map.of(member.clubProfile().getClubProfileId(), member.clubProfile()),
                Map.of(position.getClubPositionId(), position)
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "집행부구성")
    public void deleteExecutiveAssignment(Long clubId, Long assignmentId, String userKey) {
        requireManageAccess(clubId, userKey);
        lockClub(clubId);
        ClubTermExecutiveAssignment assignment = clubTermExecutiveAssignmentRepository
                .findByClubTermExecutiveAssignmentIdAndClubId(assignmentId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "ClubTermExecutiveAssignment",
                        "assignmentId",
                        assignmentId
                ));
        requireEditableTerm(clubId, assignment.getClubOperatingTermId());
        clubTermExecutiveAssignmentRepository.delete(assignment);
        ClubActivityContextHolder.setDetails("집행부 배정을 제거했습니다.", "집행부 배정을 제거하지 못했습니다.");
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "인수인계")
    public ClubHandoverNoteResponse createHandoverNote(
            Long clubId,
            String userKey,
            UpsertHandoverNoteRequest request
    ) {
        ClubAccessResolver.ClubAccess access = requireManageAccess(clubId, userKey);
        lockClub(clubId);
        NoteDraft draft = normalizeNoteDraft(clubId, request);
        ClubHandoverNote note = clubHandoverNoteRepository.save(ClubHandoverNote.builder()
                .clubId(clubId)
                .fromTermId(draft.fromTermId())
                .toTermId(draft.toTermId())
                .clubPositionId(draft.clubPositionId())
                .assignedClubProfileId(draft.assignedClubProfileId())
                .title(draft.title())
                .content(draft.content())
                .statusCode(draft.statusCode())
                .dueAt(draft.dueAt())
                .createdByClubProfileId(access.clubProfile().getClubProfileId())
                .deleted(false)
                .build());
        notifyAssignee(clubId, note, access.clubProfile().getClubProfileId());
        ClubActivityContextHolder.setDetails(
                "인수인계 메모 '" + note.getTitle() + "'를 작성했습니다.",
                "인수인계 메모를 작성하지 못했습니다."
        );
        return loadNoteResponse(clubId, note);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "인수인계")
    public ClubHandoverNoteResponse updateHandoverNote(
            Long clubId,
            Long noteId,
            String userKey,
            UpsertHandoverNoteRequest request
    ) {
        requireManageAccess(clubId, userKey);
        lockClub(clubId);
        ClubHandoverNote note = requireNote(clubId, noteId);
        NoteDraft draft = normalizeNoteDraft(clubId, request);
        note.update(
                draft.fromTermId(),
                draft.toTermId(),
                draft.clubPositionId(),
                draft.assignedClubProfileId(),
                draft.title(),
                draft.content(),
                draft.statusCode(),
                draft.dueAt()
        );
        ClubActivityContextHolder.setDetails(
                "인수인계 메모 '" + note.getTitle() + "'를 수정했습니다.",
                "인수인계 메모를 수정하지 못했습니다."
        );
        return loadNoteResponse(clubId, note);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "인수인계")
    public ClubHandoverNoteResponse acknowledgeHandoverNote(Long clubId, Long noteId, String userKey) {
        ClubAccessResolver.ClubAccess access = requireViewAccess(clubId, userKey);
        lockClub(clubId);
        ClubHandoverNote note = requireNote(clubId, noteId);
        boolean assignedToActor = note.getAssignedClubProfileId() != null
                && note.getAssignedClubProfileId().equals(access.clubProfile().getClubProfileId());
        if (!assignedToActor && !canManage(access)) {
            throw new SemoException.ForbiddenException("지정된 인계 대상자 또는 운영자만 확인 처리할 수 있습니다.");
        }
        note.acknowledge(access.clubProfile().getClubProfileId(), LocalDateTime.now());
        ClubActivityContextHolder.setDetails(
                "인수인계 메모 '" + note.getTitle() + "'를 확인했습니다.",
                "인수인계 메모를 확인 처리하지 못했습니다."
        );
        return loadNoteResponse(clubId, note);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "인수인계")
    public void deleteHandoverNote(Long clubId, Long noteId, String userKey) {
        ClubAccessResolver.ClubAccess access = requireManageAccess(clubId, userKey);
        lockClub(clubId);
        ClubHandoverNote note = requireNote(clubId, noteId);
        note.markDeleted(access.clubProfile().getClubProfileId(), LocalDateTime.now());
        ClubActivityContextHolder.setDetails(
                "인수인계 메모 '" + note.getTitle() + "'를 삭제했습니다.",
                "인수인계 메모를 삭제하지 못했습니다."
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "인수인계")
    public ClubTermCarryoverItemResponse updateCarryoverStatus(
            Long clubId,
            Long carryoverItemId,
            String userKey,
            boolean resolved
    ) {
        ClubAccessResolver.ClubAccess access = requireManageAccess(clubId, userKey);
        lockClub(clubId);
        ClubTermCarryoverItem item = clubTermCarryoverItemRepository
                .findByClubTermCarryoverItemIdAndClubId(carryoverItemId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "ClubTermCarryoverItem",
                        "carryoverItemId",
                        carryoverItemId
                ));
        if (resolved) {
            item.resolve(access.clubProfile().getClubProfileId(), LocalDateTime.now());
        } else {
            item.reopen();
        }
        Map<Long, ClubOperatingTerm> termsById = clubOperatingTermRepository
                .findByClubIdOrderByStartDateDescClubOperatingTermIdDesc(clubId).stream()
                .collect(Collectors.toMap(ClubOperatingTerm::getClubOperatingTermId, Function.identity()));
        ClubActivityContextHolder.setDetails(
                "이관 항목 '" + item.getTitleSnapshot() + "'을 " + (resolved ? "완료" : "미완료") + " 상태로 변경했습니다.",
                "이관 항목 상태를 변경하지 못했습니다."
        );
        return toCarryoverResponse(item, termsById);
    }

    private ClubAccessResolver.ClubAccess requireViewAccess(Long clubId, String userKey) {
        clubFeatureService.requireFeatureEnabled(clubId, FEATURE_HANDOVER, "인수인계 센터");
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        if (!access.isAdmin() && !clubPositionPermissionEvaluator.hasPermission(
                access,
                ClubPositionPermissionEvaluator.PERMISSION_HANDOVER_VIEW
        )) {
            throw new SemoException.ForbiddenException("인수인계 센터 조회 권한이 필요합니다.");
        }
        return access;
    }

    private ClubAccessResolver.ClubAccess requireManageAccess(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = requireViewAccess(clubId, userKey);
        if (!canManage(access)) {
            throw new SemoException.ForbiddenException("인수인계 센터 관리 권한이 필요합니다.");
        }
        return access;
    }

    private boolean canManage(ClubAccessResolver.ClubAccess access) {
        return access.isAdmin() || clubPositionPermissionEvaluator.hasPermission(
                access,
                ClubPositionPermissionEvaluator.PERMISSION_HANDOVER_MANAGE
        );
    }

    private void lockClub(Long clubId) {
        clubRepository.findForUpdate(clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("Club", "clubId", clubId));
    }

    private ClubOperatingTerm resolveSelectedTerm(
            Long clubId,
            Long selectedTermId,
            ClubOperatingTerm activeTerm,
            ClubOperatingTerm nextTerm,
            List<ClubOperatingTerm> terms
    ) {
        if (selectedTermId != null) {
            return terms.stream()
                    .filter(term -> term.getClubOperatingTermId().equals(selectedTermId))
                    .findFirst()
                    .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                            "ClubOperatingTerm",
                            "clubOperatingTermId",
                            selectedTermId
                    ));
        }
        if (activeTerm != null) {
            return activeTerm;
        }
        if (nextTerm != null) {
            return nextTerm;
        }
        return terms.stream().filter(term -> term.getClubId().equals(clubId)).findFirst().orElse(null);
    }

    private List<Long> compactIds(ClubOperatingTerm... terms) {
        Map<Long, Long> ids = new LinkedHashMap<>();
        for (ClubOperatingTerm term : terms) {
            if (term != null) {
                ids.put(term.getClubOperatingTermId(), term.getClubOperatingTermId());
            }
        }
        return List.copyOf(ids.keySet());
    }

    private ClubOperatingTerm requireTerm(Long clubId, Long termId) {
        return clubOperatingTermRepository.findByClubOperatingTermIdAndClubId(termId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "ClubOperatingTerm",
                        "clubOperatingTermId",
                        termId
                ));
    }

    private ClubOperatingTerm requireEditableTerm(Long clubId, Long termId) {
        ClubOperatingTerm term = requireTerm(clubId, termId);
        if ("CLOSED".equals(term.getStatusCode())) {
            throw new SemoException.ConflictException("종료된 운영 임기의 집행부는 수정할 수 없습니다.");
        }
        return term;
    }

    private ClubAccessResolver.ClubMemberSnapshot requireActiveMember(Long clubId, Long clubMemberId) {
        return clubAccessResolver.getActiveMemberSnapshots(clubId).stream()
                .filter(item -> item.membership().getClubMemberId().equals(clubMemberId))
                .findFirst()
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "ClubMember",
                        "clubMemberId",
                        clubMemberId
                ));
    }

    private TermDraft normalizeTermDraft(
            String termName,
            String termType,
            LocalDate startDate,
            LocalDate endDate,
            String description
    ) {
        String normalizedName = requireText(termName, "운영 임기 이름", 100);
        String normalizedType = requireEnum(termType, TERM_TYPES, "운영 임기 유형");
        if (startDate == null || endDate == null) {
            throw new SemoException.ValidationException("운영 임기의 시작일과 종료일이 필요합니다.");
        }
        if (endDate.isBefore(startDate)) {
            throw new SemoException.ValidationException("운영 임기 종료일은 시작일보다 빠를 수 없습니다.");
        }
        if (startDate.plusYears(5).isBefore(endDate)) {
            throw new SemoException.ValidationException("운영 임기는 최대 5년까지 설정할 수 있습니다.");
        }
        return new TermDraft(normalizedName, normalizedType, startDate, endDate, trimToNull(description));
    }

    private NoteDraft normalizeNoteDraft(Long clubId, UpsertHandoverNoteRequest request) {
        if (request.fromTermId() == null && request.toTermId() == null) {
            throw new SemoException.ValidationException("인수인계 메모에는 현재 임기 또는 다음 임기가 필요합니다.");
        }
        if (request.fromTermId() != null && request.fromTermId().equals(request.toTermId())) {
            throw new SemoException.ValidationException("현재 임기와 다음 임기는 서로 달라야 합니다.");
        }
        if (request.fromTermId() != null) {
            requireTerm(clubId, request.fromTermId());
        }
        if (request.toTermId() != null) {
            requireTerm(clubId, request.toTermId());
        }
        if (request.clubPositionId() != null) {
            clubPositionRepository.findByClubPositionIdAndClubId(request.clubPositionId(), clubId)
                    .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                            "ClubPosition",
                            "clubPositionId",
                            request.clubPositionId()
                    ));
        }
        if (request.assignedClubProfileId() != null) {
            ClubProfile profile = clubProfileRepository.findById(request.assignedClubProfileId())
                    .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                            "ClubProfile",
                            "clubProfileId",
                            request.assignedClubProfileId()
                    ));
            clubMemberRepository.findByClubMemberIdAndClubId(profile.getClubMemberId(), clubId)
                    .filter(member -> ClubAccessResolver.STATUS_ACTIVE.equals(member.getMembershipStatus()))
                    .orElseThrow(() -> new SemoException.ValidationException("인계 대상자는 현재 활동 중인 클럽 멤버여야 합니다."));
        }
        String statusCode = request.statusCode() == null
                ? "DRAFT"
                : requireEnum(request.statusCode(), EDITABLE_NOTE_STATUSES, "인수인계 상태");
        return new NoteDraft(
                request.fromTermId(),
                request.toTermId(),
                request.clubPositionId(),
                request.assignedClubProfileId(),
                requireText(request.title(), "인수인계 제목", 200),
                requireText(request.content(), "인수인계 내용", 10000),
                statusCode,
                request.dueAt()
        );
    }

    private void bootstrapExecutiveAssignments(Long clubId, ClubOperatingTerm term, Long actorClubProfileId) {
        if (clubTermExecutiveAssignmentRepository.countByClubOperatingTermId(term.getClubOperatingTermId()) > 0) {
            return;
        }
        List<ClubPosition> positions = clubPositionRepository
                .findByClubIdAndActiveTrueOrderByDisplayNameAscClubPositionIdAsc(clubId);
        if (positions.isEmpty()) {
            return;
        }
        Map<Long, ClubPosition> positionsById = positions.stream()
                .collect(Collectors.toMap(ClubPosition::getClubPositionId, Function.identity()));
        List<ClubAccessResolver.ClubMemberSnapshot> members = clubAccessResolver.getActiveMemberSnapshots(clubId);
        Map<Long, ClubAccessResolver.ClubMemberSnapshot> membersById = members.stream()
                .collect(Collectors.toMap(item -> item.membership().getClubMemberId(), Function.identity()));
        List<ClubMemberPosition> currentAssignments = clubMemberPositionRepository.findByClubPositionIdIn(
                positions.stream().map(ClubPosition::getClubPositionId).toList()
        );
        int sortOrder = 0;
        for (ClubMemberPosition current : currentAssignments) {
            ClubAccessResolver.ClubMemberSnapshot member = membersById.get(current.getClubMemberId());
            ClubPosition position = positionsById.get(current.getClubPositionId());
            if (member == null || position == null) {
                continue;
            }
            clubTermExecutiveAssignmentRepository.save(ClubTermExecutiveAssignment.builder()
                    .clubId(clubId)
                    .clubOperatingTermId(term.getClubOperatingTermId())
                    .clubMemberId(member.membership().getClubMemberId())
                    .clubProfileId(member.clubProfile().getClubProfileId())
                    .clubPositionId(position.getClubPositionId())
                    .responsibility(trimToNull(position.getDescription()))
                    .sortOrder(sortOrder += 10)
                    .createdByClubProfileId(actorClubProfileId)
                    .updatedByClubProfileId(actorClubProfileId)
                    .build());
        }
    }

    private QueueSnapshot buildQueue(Long clubId) {
        LocalDateTime now = LocalDateTime.now();
        List<TodoItem> openTodos = todoItemRepository.findByClubIdOrderByTodoItemIdDesc(clubId).stream()
                .filter(item -> OPEN_TODO_STATUSES.contains(item.getStatusCode()))
                .toList();
        List<FinanceRequest> pendingFinanceRequests = financeRequestRepository
                .findByClubIdOrderByFinanceRequestIdDesc(clubId).stream()
                .filter(item -> REQUEST_STATUS_SUBMITTED.equals(item.getStatusCode()))
                .toList();
        List<ClubScheduleEvent> upcomingSchedules = clubScheduleEventRepository.findAllActiveEvents(clubId).stream()
                .filter(item -> !item.getStartAt().isBefore(now))
                .toList();
        List<ClubFeedback> openFeedback = clubFeedbackRepository.findFeed(clubId).stream()
                .filter(item -> OPEN_FEEDBACK_STATUSES.contains(item.getStatusCode()))
                .toList();
        int pendingJoinRequestCount = safeCount(
                clubJoinRequestRepository.countByClubIdAndRequestStatus(clubId, "PENDING")
        );
        ClubAdminFinanceSummaryAggregate financeSummary = financePaymentRepository.summarizeAdminFinance(clubId, now);
        int unpaidPaymentCount = financeSummary == null ? 0 : safeCount(financeSummary.pendingPaymentCount());
        int openNoteCount = safeCount(clubHandoverNoteRepository.countByClubIdAndDeletedFalseAndStatusCodeIn(
                clubId,
                List.of("DRAFT", "READY")
        ));
        int openCarryoverCount = safeCount(clubTermCarryoverItemRepository.countByClubIdAndStatusCode(clubId, "OPEN"));

        List<HandoverQueueItemResponse> items = new ArrayList<>();
        openTodos.stream()
                .sorted(Comparator
                        .comparing(TodoItem::getDueAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(TodoItem::getTodoItemId))
                .limit(5)
                .map(item -> new HandoverQueueItemResponse(
                        "TODO_ITEM",
                        item.getTodoItemId(),
                        item.getTitle(),
                        "IN_PROGRESS".equals(item.getStatusCode()) ? "진행 중" : "대기",
                        item.getDueAt(),
                        "/clubs/%d/admin/more/todos".formatted(clubId),
                        item.getDueAt() != null && item.getDueAt().isBefore(now)
                ))
                .forEach(items::add);
        pendingFinanceRequests.stream().limit(3)
                .map(item -> new HandoverQueueItemResponse(
                        "FINANCE_REQUEST",
                        item.getFinanceRequestId(),
                        item.getTitle(),
                        "정산 검토 대기",
                        null,
                        "/clubs/%d/admin/more/finance?tab=requests".formatted(clubId),
                        false
                ))
                .forEach(items::add);
        upcomingSchedules.stream().limit(3)
                .map(item -> new HandoverQueueItemResponse(
                        "SCHEDULE_EVENT",
                        item.getEventId(),
                        item.getTitle(),
                        "예정 일정",
                        item.getStartAt(),
                        "/clubs/%d/schedule/%d".formatted(clubId, item.getEventId()),
                        false
                ))
                .forEach(items::add);
        openFeedback.stream().limit(2)
                .map(item -> new HandoverQueueItemResponse(
                        "FEEDBACK",
                        item.getFeedbackId(),
                        item.getTitle(),
                        "IN_REVIEW".equals(item.getStatusCode()) ? "검토 중" : "접수",
                        null,
                        "/clubs/%d/admin/more/feedback".formatted(clubId),
                        false
                ))
                .forEach(items::add);
        if (pendingJoinRequestCount > 0) {
            items.add(new HandoverQueueItemResponse(
                    "JOIN_REQUEST",
                    null,
                    "가입 신청 " + pendingJoinRequestCount + "건",
                    "승인 대기",
                    null,
                    "/clubs/%d/admin/more/join-requests".formatted(clubId),
                    false
            ));
        }

        return new QueueSnapshot(
                new HandoverQueueSummaryResponse(
                        openTodos.size(),
                        safeCount(openTodos.stream()
                                .filter(item -> item.getDueAt() != null && item.getDueAt().isBefore(now))
                                .count()),
                        unpaidPaymentCount,
                        pendingFinanceRequests.size(),
                        upcomingSchedules.size(),
                        openFeedback.size(),
                        pendingJoinRequestCount,
                        openNoteCount,
                        openCarryoverCount
                ),
                items.stream()
                        .sorted(Comparator
                                .comparing(HandoverQueueItemResponse::urgent).reversed()
                                .thenComparing(
                                        HandoverQueueItemResponse::dueAt,
                                        Comparator.nullsLast(Comparator.naturalOrder())
                                ))
                        .limit(12)
                        .toList()
        );
    }

    private List<HandoverQueueItemResponse> buildCarryoverCandidates(Long clubId) {
        LocalDateTime now = LocalDateTime.now();
        List<HandoverQueueItemResponse> items = new ArrayList<>();
        todoItemRepository.findByClubIdOrderByTodoItemIdDesc(clubId).stream()
                .filter(item -> OPEN_TODO_STATUSES.contains(item.getStatusCode()))
                .map(item -> new HandoverQueueItemResponse(
                        "TODO_ITEM",
                        item.getTodoItemId(),
                        item.getTitle(),
                        "IN_PROGRESS".equals(item.getStatusCode()) ? "진행 중" : "대기",
                        item.getDueAt(),
                        "/clubs/%d/admin/more/todos".formatted(clubId),
                        item.getDueAt() != null && item.getDueAt().isBefore(now)
                ))
                .forEach(items::add);
        financeRequestRepository.findByClubIdOrderByFinanceRequestIdDesc(clubId).stream()
                .filter(item -> REQUEST_STATUS_SUBMITTED.equals(item.getStatusCode()))
                .map(item -> new HandoverQueueItemResponse(
                        "FINANCE_REQUEST",
                        item.getFinanceRequestId(),
                        item.getTitle(),
                        "정산 검토 대기",
                        null,
                        "/clubs/%d/admin/more/finance?tab=requests".formatted(clubId),
                        false
                ))
                .forEach(items::add);
        clubFeedbackRepository.findFeed(clubId).stream()
                .filter(item -> OPEN_FEEDBACK_STATUSES.contains(item.getStatusCode()))
                .map(item -> new HandoverQueueItemResponse(
                        "FEEDBACK",
                        item.getFeedbackId(),
                        item.getTitle(),
                        "IN_REVIEW".equals(item.getStatusCode()) ? "검토 중" : "접수",
                        null,
                        "/clubs/%d/admin/more/feedback".formatted(clubId),
                        false
                ))
                .forEach(items::add);
        return List.copyOf(items);
    }

    private ClubTermMetricsResponse buildTermMetrics(Long clubId, ClubOperatingTerm term) {
        LocalDateTime from = term.getStartDate().atStartOfDay();
        LocalDateTime toExclusive = term.getEndDate().plusDays(1).atStartOfDay();
        int todoCount = safeCount(todoItemRepository.findByClubIdOrderByTodoItemIdDesc(clubId).stream()
                .filter(item -> within(item.getDueAt(), item.getCreateDate(), from, toExclusive))
                .count());
        int scheduleCount = safeCount(clubScheduleEventRepository.findAllActiveEvents(clubId).stream()
                .filter(item -> within(item.getStartAt(), null, from, toExclusive))
                .count());
        int tournamentCount = safeCount(tournamentRecordRepository
                .findByClubIdAndDeletedFalseOrderByPinnedDescStartDateAscTournamentRecordIdDesc(clubId).stream()
                .filter(item -> overlaps(item, term))
                .count());
        int obligationCount = safeCount(financeObligationRepository.findAdminFeed(
                        clubId,
                        null,
                        null,
                        null,
                        org.springframework.data.domain.Pageable.unpaged()
                ).stream()
                .filter(item -> within(item.getDueAt(), item.getCreateDate(), from, toExclusive))
                .count());
        int financeRequestCount = safeCount(financeRequestRepository
                .findByClubIdOrderByFinanceRequestIdDesc(clubId).stream()
                .filter(item -> within(null, item.getCreateDate(), from, toExclusive))
                .count());
        BigDecimal expenseAmount = financeExpenseRepository.findByClubIdOrderBySpentAtDescFinanceExpenseIdDesc(clubId)
                .stream()
                .filter(item -> within(item.getSpentAt(), null, from, toExclusive))
                .map(FinanceExpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ClubTermMetricsResponse(
                todoCount,
                scheduleCount,
                tournamentCount,
                obligationCount,
                financeRequestCount,
                expenseAmount,
                "KRW"
        );
    }

    private boolean within(
            LocalDateTime primary,
            LocalDateTime fallback,
            LocalDateTime from,
            LocalDateTime toExclusive
    ) {
        LocalDateTime value = primary == null ? fallback : primary;
        return value != null && !value.isBefore(from) && value.isBefore(toExclusive);
    }

    private boolean overlaps(TournamentRecord tournament, ClubOperatingTerm term) {
        return !tournament.getEndDate().isBefore(term.getStartDate())
                && !tournament.getStartDate().isAfter(term.getEndDate());
    }

    private ClubHandoverNote requireNote(Long clubId, Long noteId) {
        return clubHandoverNoteRepository.findByClubHandoverNoteIdAndClubIdAndDeletedFalse(noteId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "ClubHandoverNote",
                        "clubHandoverNoteId",
                        noteId
                ));
    }

    private void carryOverOpenItems(
            Long clubId,
            ClubOperatingTerm fromTerm,
            ClubOperatingTerm toTerm,
            List<HandoverQueueItemResponse> queueItems,
            Long actorClubProfileId,
            LocalDateTime transferredAt
    ) {
        Set<String> supportedResourceTypes = Set.of("TODO_ITEM", "FINANCE_REQUEST", "FEEDBACK");
        queueItems.stream()
                .filter(item -> supportedResourceTypes.contains(item.resourceType()))
                .filter(item -> item.resourceId() != null)
                .filter(item -> !clubTermCarryoverItemRepository.existsByToTermIdAndResourceTypeAndResourceId(
                        toTerm.getClubOperatingTermId(),
                        item.resourceType(),
                        item.resourceId()
                ))
                .forEach(item -> clubTermCarryoverItemRepository.save(ClubTermCarryoverItem.builder()
                        .clubId(clubId)
                        .fromTermId(fromTerm.getClubOperatingTermId())
                        .toTermId(toTerm.getClubOperatingTermId())
                        .resourceType(item.resourceType())
                        .resourceId(item.resourceId())
                        .titleSnapshot(item.title())
                        .statusSnapshot(item.statusLabel())
                        .targetPath(item.targetPath())
                        .dueAt(item.dueAt())
                        .statusCode("OPEN")
                        .transferredByClubProfileId(actorClubProfileId)
                        .transferredAt(transferredAt)
                        .build()));
    }

    private ClubHandoverNoteResponse loadNoteResponse(Long clubId, ClubHandoverNote note) {
        Map<Long, ClubOperatingTerm> terms = clubOperatingTermRepository
                .findByClubIdOrderByStartDateDescClubOperatingTermIdDesc(clubId).stream()
                .collect(Collectors.toMap(ClubOperatingTerm::getClubOperatingTermId, Function.identity()));
        Map<Long, ClubPosition> positions = clubPositionRepository
                .findByClubIdOrderByDisplayNameAscClubPositionIdAsc(clubId).stream()
                .collect(Collectors.toMap(ClubPosition::getClubPositionId, Function.identity()));
        Collection<Long> profileIds = java.util.stream.Stream.of(
                        note.getAssignedClubProfileId(),
                        note.getCreatedByClubProfileId(),
                        note.getAcknowledgedByClubProfileId()
                )
                .filter(java.util.Objects::nonNull)
                .toList();
        Map<Long, ClubProfile> profiles = profileIds.isEmpty()
                ? Map.of()
                : clubProfileRepository.findAllById(profileIds).stream()
                        .collect(Collectors.toMap(ClubProfile::getClubProfileId, Function.identity()));
        return toNoteResponse(note, terms, positions, profiles);
    }

    private void notifyAssignee(Long clubId, ClubHandoverNote note, Long actorClubProfileId) {
        if (note.getAssignedClubProfileId() == null || note.getAssignedClubProfileId().equals(actorClubProfileId)) {
            return;
        }
        clubNotificationPublisher.notifyClubProfile(
                note.getAssignedClubProfileId(),
                new ClubNotificationPublisher.NotificationCommand(
                        clubId,
                        "HANDOVER_ASSIGNED",
                        "새 인수인계 메모가 도착했습니다",
                        note.getTitle(),
                        "HANDOVER_NOTE",
                        note.getClubHandoverNoteId(),
                        "/clubs/%d/admin/more/handover".formatted(clubId),
                        "handover-note-assigned:" + note.getClubHandoverNoteId()
                )
        );
    }

    private ClubOperatingTermResponse toTermResponse(ClubOperatingTerm term) {
        if (term == null) {
            return null;
        }
        return new ClubOperatingTermResponse(
                term.getClubOperatingTermId(),
                term.getTermName(),
                term.getTermType(),
                term.getStartDate(),
                term.getEndDate(),
                term.getStatusCode(),
                term.getDescription(),
                term.getActivatedAt(),
                term.getClosedAt()
        );
    }

    private HandoverRecentDecisionResponse toRecentDecisionResponse(Long clubId, DecisionRecord record) {
        return new HandoverRecentDecisionResponse(
                record.getDecisionRecordId(),
                record.getRecordType(),
                record.getTitle(),
                record.getEffectiveDate(),
                record.getConfirmedAt(),
                "/clubs/%d/admin/more/decisions".formatted(clubId)
        );
    }

    private ClubExecutiveAssignmentResponse toAssignmentResponse(
            ClubTermExecutiveAssignment assignment,
            Map<Long, ClubProfile> profilesById,
            Map<Long, ClubPosition> positionsById
    ) {
        ClubProfile profile = profilesById.get(assignment.getClubProfileId());
        ClubPosition position = positionsById.get(assignment.getClubPositionId());
        return new ClubExecutiveAssignmentResponse(
                assignment.getClubTermExecutiveAssignmentId(),
                assignment.getClubOperatingTermId(),
                assignment.getClubMemberId(),
                assignment.getClubProfileId(),
                profile == null ? "알 수 없는 멤버" : profile.getDisplayName(),
                profile == null ? null : profile.getAvatarFileName(),
                assignment.getClubPositionId(),
                position == null ? "삭제된 직책" : position.getDisplayName(),
                position == null ? null : position.getIconName(),
                position == null ? null : position.getColorHex(),
                assignment.getResponsibility(),
                assignment.getSortOrder()
        );
    }

    private ClubHandoverNoteResponse toNoteResponse(
            ClubHandoverNote note,
            Map<Long, ClubOperatingTerm> termsById,
            Map<Long, ClubPosition> positionsById,
            Map<Long, ClubProfile> profilesById
    ) {
        ClubOperatingTerm fromTerm = termsById.get(note.getFromTermId());
        ClubOperatingTerm toTerm = termsById.get(note.getToTermId());
        ClubPosition position = positionsById.get(note.getClubPositionId());
        ClubProfile assigned = profilesById.get(note.getAssignedClubProfileId());
        ClubProfile createdBy = profilesById.get(note.getCreatedByClubProfileId());
        return new ClubHandoverNoteResponse(
                note.getClubHandoverNoteId(),
                note.getFromTermId(),
                fromTerm == null ? null : fromTerm.getTermName(),
                note.getToTermId(),
                toTerm == null ? null : toTerm.getTermName(),
                note.getClubPositionId(),
                position == null ? null : position.getDisplayName(),
                note.getAssignedClubProfileId(),
                assigned == null ? null : assigned.getDisplayName(),
                note.getTitle(),
                note.getContent(),
                note.getStatusCode(),
                note.getDueAt(),
                note.getCreatedByClubProfileId(),
                createdBy == null ? "알 수 없는 멤버" : createdBy.getDisplayName(),
                note.getAcknowledgedByClubProfileId(),
                note.getAcknowledgedAt(),
                note.getCreateDate(),
                note.getUpdateDate()
        );
    }

    private ClubTermCarryoverItemResponse toCarryoverResponse(
            ClubTermCarryoverItem item,
            Map<Long, ClubOperatingTerm> termsById
    ) {
        ClubOperatingTerm fromTerm = termsById.get(item.getFromTermId());
        ClubOperatingTerm toTerm = termsById.get(item.getToTermId());
        return new ClubTermCarryoverItemResponse(
                item.getClubTermCarryoverItemId(),
                item.getFromTermId(),
                fromTerm == null ? null : fromTerm.getTermName(),
                item.getToTermId(),
                toTerm == null ? null : toTerm.getTermName(),
                item.getResourceType(),
                item.getResourceId(),
                item.getTitleSnapshot(),
                item.getStatusSnapshot(),
                item.getTargetPath(),
                item.getDueAt(),
                item.getStatusCode(),
                item.getTransferredAt(),
                item.getResolvedAt()
        );
    }

    private String requireText(String value, String fieldName, int maxLength) {
        if (!StringUtils.hasText(value)) {
            throw new SemoException.ValidationException(fieldName + "을 입력해주세요.");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new SemoException.ValidationException(fieldName + "은 " + maxLength + "자 이하여야 합니다.");
        }
        return normalized;
    }

    private String requireEnum(String value, Set<String> values, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new SemoException.ValidationException(fieldName + "이 필요합니다.");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!values.contains(normalized)) {
            throw new SemoException.ValidationException(fieldName + " 값이 올바르지 않습니다.");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private int safeCount(long value) {
        if (value <= 0) {
            return 0;
        }
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private record TermDraft(
            String termName,
            String termType,
            LocalDate startDate,
            LocalDate endDate,
            String description
    ) {
    }

    private record NoteDraft(
            Long fromTermId,
            Long toTermId,
            Long clubPositionId,
            Long assignedClubProfileId,
            String title,
            String content,
            String statusCode,
            LocalDateTime dueAt
    ) {
    }

    private record QueueSnapshot(
            HandoverQueueSummaryResponse summary,
            List<HandoverQueueItemResponse> items
    ) {
    }
}
