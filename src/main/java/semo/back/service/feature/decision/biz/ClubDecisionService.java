package semo.back.service.feature.decision.biz;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubOperatingTerm;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.DecisionParticipant;
import semo.back.service.database.pub.entity.DecisionRecord;
import semo.back.service.database.pub.entity.DecisionResourceLink;
import semo.back.service.database.pub.repository.ClubOperatingTermRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.DecisionParticipantRepository;
import semo.back.service.database.pub.repository.DecisionRecordRepository;
import semo.back.service.database.pub.repository.DecisionResourceLinkRepository;
import semo.back.service.feature.activity.biz.ClubActivityContextHolder;
import semo.back.service.feature.activity.biz.RecordClubActivity;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.decision.vo.ClubDecisionAdminCenterResponse;
import semo.back.service.feature.decision.vo.ClubDecisionLogResponse;
import semo.back.service.feature.decision.vo.DecisionMemberOptionResponse;
import semo.back.service.feature.decision.vo.DecisionParticipantResponse;
import semo.back.service.feature.decision.vo.DecisionRecordResponse;
import semo.back.service.feature.decision.vo.DecisionResourceLinkResponse;
import semo.back.service.feature.decision.vo.DecisionResourceReferenceRequest;
import semo.back.service.feature.decision.vo.DecisionTermOptionResponse;
import semo.back.service.feature.decision.vo.UpsertDecisionRecordRequest;
import semo.back.service.feature.notification.biz.ClubNotificationPublisher;
import semo.back.service.feature.position.biz.ClubPositionPermissionEvaluator;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubDecisionService {
    public static final String FEATURE_DECISION_LOG = "DECISION_LOG";
    private static final Set<String> RECORD_TYPES = Set.of("DECISION", "MEETING_MINUTES");
    private static final Set<String> VISIBILITY_SCOPES = Set.of("MEMBERS", "OPERATORS");
    private static final Set<String> MEMBER_VISIBLE_STATUSES = Set.of("CONFIRMED", "SUPERSEDED");

    private final ClubAccessResolver clubAccessResolver;
    private final ClubFeatureService clubFeatureService;
    private final ClubPositionPermissionEvaluator clubPositionPermissionEvaluator;
    private final ClubRepository clubRepository;
    private final ClubOperatingTermRepository clubOperatingTermRepository;
    private final ClubProfileRepository clubProfileRepository;
    private final DecisionRecordRepository decisionRecordRepository;
    private final DecisionParticipantRepository decisionParticipantRepository;
    private final DecisionResourceLinkRepository decisionResourceLinkRepository;
    private final DecisionResourceResolver decisionResourceResolver;
    private final ClubNotificationPublisher clubNotificationPublisher;

    public ClubDecisionLogResponse getMemberLog(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = requireFeatureMember(clubId, userKey);
        List<DecisionRecord> records = decisionRecordRepository.findMemberFeed(clubId, MEMBER_VISIBLE_STATUSES);
        return new ClubDecisionLogResponse(
                clubId,
                access.club().getName(),
                toResponses(clubId, records)
        );
    }

    public ClubDecisionAdminCenterResponse getAdminCenter(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = requireViewAccess(clubId, userKey);
        List<DecisionRecord> records = decisionRecordRepository
                .findByClubIdAndDeletedFalseOrderByDecisionRecordIdDesc(clubId);
        List<ClubAccessResolver.ClubMemberSnapshot> members = clubAccessResolver.getActiveMemberSnapshots(clubId);
        List<ClubOperatingTerm> terms = clubOperatingTermRepository
                .findByClubIdOrderByStartDateDescClubOperatingTermIdDesc(clubId);
        return new ClubDecisionAdminCenterResponse(
                clubId,
                access.club().getName(),
                access.isAdmin(),
                canManage(access),
                safeCount(records.stream().filter(item -> "DRAFT".equals(item.getStatusCode())).count()),
                safeCount(records.stream().filter(item -> "CONFIRMED".equals(item.getStatusCode())).count()),
                safeCount(decisionRecordRepository.countReviewDue(clubId, LocalDate.now())),
                toResponses(clubId, records),
                members.stream()
                        .map(item -> new DecisionMemberOptionResponse(
                                item.clubProfile().getClubProfileId(),
                                item.clubProfile().getDisplayName(),
                                item.clubProfile().getAvatarFileName()
                        ))
                        .toList(),
                decisionResourceResolver.getOptions(clubId),
                terms.stream()
                        .map(term -> new DecisionTermOptionResponse(
                                term.getClubOperatingTermId(),
                                term.getTermName(),
                                term.getStatusCode(),
                                term.getStartDate(),
                                term.getEndDate()
                        ))
                        .toList()
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "회의록·결정")
    public DecisionRecordResponse createRecord(Long clubId, String userKey, UpsertDecisionRecordRequest request) {
        ClubAccessResolver.ClubAccess access = requireManageAccess(clubId, userKey);
        lockClub(clubId);
        DecisionDraft draft = normalizeDraft(clubId, request, null);
        DecisionRecord saved = decisionRecordRepository.save(DecisionRecord.builder()
                .clubId(clubId)
                .clubOperatingTermId(draft.clubOperatingTermId())
                .recordType(draft.recordType())
                .statusCode("DRAFT")
                .visibilityScope(draft.visibilityScope())
                .title(draft.title())
                .decisionContent(draft.decisionContent())
                .backgroundContext(draft.backgroundContext())
                .rationale(draft.rationale())
                .meetingAt(draft.meetingAt())
                .effectiveDate(draft.effectiveDate())
                .reviewDate(draft.reviewDate())
                .supersedesDecisionRecordId(draft.supersedesDecisionRecordId())
                .createdByClubProfileId(access.clubProfile().getClubProfileId())
                .deleted(false)
                .build());
        replaceChildren(saved.getDecisionRecordId(), draft);
        ClubActivityContextHolder.setDetails(
                "의사결정 초안 %s을 작성했습니다.".formatted(saved.getTitle()),
                "의사결정 초안을 작성하지 못했습니다."
        );
        return loadResponse(clubId, saved.getDecisionRecordId());
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "회의록·결정")
    public DecisionRecordResponse updateRecord(
            Long clubId,
            Long decisionRecordId,
            String userKey,
            UpsertDecisionRecordRequest request
    ) {
        requireManageAccess(clubId, userKey);
        lockClub(clubId);
        DecisionRecord record = requireRecordForUpdate(clubId, decisionRecordId);
        requireDraft(record, "확정되거나 보관된 기록은 수정할 수 없습니다. 대체 결정을 새로 작성해주세요.");
        DecisionDraft draft = normalizeDraft(clubId, request, decisionRecordId);
        record.updateDraft(
                draft.clubOperatingTermId(),
                draft.recordType(),
                draft.visibilityScope(),
                draft.title(),
                draft.decisionContent(),
                draft.backgroundContext(),
                draft.rationale(),
                draft.meetingAt(),
                draft.effectiveDate(),
                draft.reviewDate(),
                draft.supersedesDecisionRecordId()
        );
        replaceChildren(record.getDecisionRecordId(), draft);
        ClubActivityContextHolder.setDetails(
                "의사결정 초안 %s을 수정했습니다.".formatted(record.getTitle()),
                "의사결정 초안을 수정하지 못했습니다."
        );
        return loadResponse(clubId, record.getDecisionRecordId());
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "회의록·결정")
    public DecisionRecordResponse confirmRecord(Long clubId, Long decisionRecordId, String userKey) {
        ClubAccessResolver.ClubAccess access = requireManageAccess(clubId, userKey);
        lockClub(clubId);
        DecisionRecord record = requireRecordForUpdate(clubId, decisionRecordId);
        requireDraft(record, "초안 상태의 기록만 확정할 수 있습니다.");
        List<DecisionParticipant> participants = decisionParticipantRepository
                .findByDecisionRecordIdInOrderByDecisionParticipantIdAsc(List.of(decisionRecordId));
        if (participants.stream().noneMatch(item -> "DECIDER".equals(item.getParticipantRole()))) {
            throw new SemoException.ValidationException("결정자를 한 명 이상 지정해야 확정할 수 있습니다.");
        }
        if (record.getSupersedesDecisionRecordId() != null) {
            DecisionRecord superseded = requireRecordForUpdate(clubId, record.getSupersedesDecisionRecordId());
            if (!"CONFIRMED".equals(superseded.getStatusCode())) {
                throw new SemoException.ConflictException("대체할 이전 결정이 현재 확정 상태가 아닙니다.");
            }
            superseded.markSuperseded();
        }
        record.confirm(access.clubProfile().getClubProfileId(), LocalDateTime.now());
        notifyParticipants(clubId, record, participants, access.clubProfile().getClubProfileId());
        ClubActivityContextHolder.setDetails(
                "의사결정 %s을 확정했습니다.".formatted(record.getTitle()),
                "의사결정을 확정하지 못했습니다."
        );
        return loadResponse(clubId, record.getDecisionRecordId());
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "회의록·결정")
    public DecisionRecordResponse archiveRecord(Long clubId, Long decisionRecordId, String userKey) {
        ClubAccessResolver.ClubAccess access = requireManageAccess(clubId, userKey);
        lockClub(clubId);
        DecisionRecord record = requireRecordForUpdate(clubId, decisionRecordId);
        if (!Set.of("DRAFT", "CONFIRMED").contains(record.getStatusCode())) {
            throw new SemoException.ConflictException("초안 또는 확정 상태의 기록만 보관할 수 있습니다.");
        }
        record.archive(access.clubProfile().getClubProfileId(), LocalDateTime.now());
        ClubActivityContextHolder.setDetails(
                "회의록·결정 기록 %s을 보관했습니다.".formatted(record.getTitle()),
                "회의록·결정 기록을 보관하지 못했습니다."
        );
        return loadResponse(clubId, record.getDecisionRecordId());
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "회의록·결정")
    public void deleteDraft(Long clubId, Long decisionRecordId, String userKey) {
        ClubAccessResolver.ClubAccess access = requireManageAccess(clubId, userKey);
        lockClub(clubId);
        DecisionRecord record = requireRecordForUpdate(clubId, decisionRecordId);
        requireDraft(record, "확정된 기록은 삭제할 수 없습니다. 기록 보관 또는 대체 결정을 사용해주세요.");
        record.markDeleted(access.clubProfile().getClubProfileId(), LocalDateTime.now());
        ClubActivityContextHolder.setDetails(
                "의사결정 초안 %s을 삭제했습니다.".formatted(record.getTitle()),
                "의사결정 초안을 삭제하지 못했습니다."
        );
    }

    public boolean canViewRecord(ClubAccessResolver.ClubAccess access, DecisionRecord record) {
        if (!access.club().getClubId().equals(record.getClubId()) || record.isDeleted()) {
            return false;
        }
        if ("MEMBERS".equals(record.getVisibilityScope())
                && MEMBER_VISIBLE_STATUSES.contains(record.getStatusCode())) {
            return true;
        }
        return canViewAdmin(access);
    }

    public boolean canManage(ClubAccessResolver.ClubAccess access) {
        return access.isAdmin() || clubPositionPermissionEvaluator.hasPermission(
                access,
                ClubPositionPermissionEvaluator.PERMISSION_DECISION_MANAGE
        );
    }

    private ClubAccessResolver.ClubAccess requireFeatureMember(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        clubFeatureService.requireFeatureEnabled(clubId, FEATURE_DECISION_LOG, "회의록·결정");
        return access;
    }

    private ClubAccessResolver.ClubAccess requireViewAccess(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = requireFeatureMember(clubId, userKey);
        if (!canViewAdmin(access)) {
            throw new SemoException.ForbiddenException("회의록·결정 운영 화면을 조회할 권한이 없습니다.");
        }
        return access;
    }

    private ClubAccessResolver.ClubAccess requireManageAccess(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = requireFeatureMember(clubId, userKey);
        if (!canManage(access)) {
            throw new SemoException.ForbiddenException("회의록·결정을 관리할 권한이 없습니다.");
        }
        return access;
    }

    private boolean canViewAdmin(ClubAccessResolver.ClubAccess access) {
        return access.isAdmin()
                || clubPositionPermissionEvaluator.hasPermission(
                        access,
                        ClubPositionPermissionEvaluator.PERMISSION_DECISION_VIEW
                )
                || clubPositionPermissionEvaluator.hasPermission(
                        access,
                        ClubPositionPermissionEvaluator.PERMISSION_DECISION_MANAGE
                );
    }

    private DecisionDraft normalizeDraft(Long clubId, UpsertDecisionRecordRequest request, Long editingRecordId) {
        if (request == null) {
            throw new SemoException.ValidationException("회의록·결정 내용을 입력해주세요.");
        }
        String recordType = normalizeCode(request.recordType(), RECORD_TYPES, "기록 유형");
        String visibilityScope = normalizeCode(request.visibilityScope(), VISIBILITY_SCOPES, "공개 범위");
        String title = requireText(request.title(), 200, "제목");
        String decisionContent = requireText(request.decisionContent(), 20000, "결정 내용");
        String backgroundContext = optionalText(request.backgroundContext(), 20000, "배경");
        String rationale = optionalText(request.rationale(), 20000, "결정 이유");
        if ("MEETING_MINUTES".equals(recordType) && request.meetingAt() == null) {
            throw new SemoException.ValidationException("회의록은 회의 일시를 입력해야 합니다.");
        }
        if (request.effectiveDate() != null
                && request.reviewDate() != null
                && request.reviewDate().isBefore(request.effectiveDate())) {
            throw new SemoException.ValidationException("검토일은 시행일보다 빠를 수 없습니다.");
        }
        validateTerm(clubId, request.clubOperatingTermId());
        validateSupersededDecision(clubId, request.supersedesDecisionRecordId(), editingRecordId);
        Map<Long, ClubProfile> activeProfiles = clubAccessResolver.getActiveMemberSnapshots(clubId).stream()
                .map(ClubAccessResolver.ClubMemberSnapshot::clubProfile)
                .collect(Collectors.toMap(ClubProfile::getClubProfileId, Function.identity()));
        LinkedHashSet<Long> deciderIds = normalizeProfileIds(
                request.deciderClubProfileIds(),
                activeProfiles,
                "결정자"
        );
        if (deciderIds.isEmpty()) {
            throw new SemoException.ValidationException("결정자를 한 명 이상 선택해주세요.");
        }
        LinkedHashSet<Long> participantIds = normalizeProfileIds(
                request.participantClubProfileIds(),
                activeProfiles,
                "참여자"
        );
        participantIds.removeAll(deciderIds);
        List<ResolvedResource> resources = resolveResources(clubId, request.relatedResources(), request.followUpTodoItemIds());
        return new DecisionDraft(
                recordType,
                visibilityScope,
                title,
                decisionContent,
                backgroundContext,
                rationale,
                request.meetingAt(),
                request.effectiveDate(),
                request.reviewDate(),
                request.clubOperatingTermId(),
                request.supersedesDecisionRecordId(),
                deciderIds,
                participantIds,
                activeProfiles,
                resources
        );
    }

    private List<ResolvedResource> resolveResources(
            Long clubId,
            List<DecisionResourceReferenceRequest> relatedResources,
            List<Long> followUpTodoItemIds
    ) {
        Map<String, ResolvedResource> deduplicated = new LinkedHashMap<>();
        safeCollection(relatedResources).forEach(reference -> {
            if (reference == null) {
                throw new SemoException.ValidationException("연결 대상 정보가 비어 있습니다.");
            }
            DecisionResourceResolver.ResourceDescriptor descriptor = decisionResourceResolver.resolve(
                    clubId,
                    reference.resourceType(),
                    reference.resourceId()
            );
            ResolvedResource resolved = ResolvedResource.of("RELATED", descriptor);
            deduplicated.putIfAbsent(resolved.key(), resolved);
        });
        safeCollection(followUpTodoItemIds).forEach(todoItemId -> {
            DecisionResourceResolver.ResourceDescriptor descriptor = decisionResourceResolver.resolve(
                    clubId,
                    DecisionResourceResolver.RESOURCE_TODO_ITEM,
                    todoItemId
            );
            ResolvedResource resolved = ResolvedResource.of("FOLLOW_UP", descriptor);
            deduplicated.putIfAbsent(resolved.key(), resolved);
        });
        return List.copyOf(deduplicated.values());
    }

    private void replaceChildren(Long decisionRecordId, DecisionDraft draft) {
        decisionParticipantRepository.deleteByDecisionRecordId(decisionRecordId);
        decisionResourceLinkRepository.deleteByDecisionRecordId(decisionRecordId);
        List<DecisionParticipant> participants = new ArrayList<>();
        draft.deciderIds().forEach(profileId -> participants.add(toParticipant(
                decisionRecordId,
                profileId,
                "DECIDER",
                draft.activeProfiles()
        )));
        draft.participantIds().forEach(profileId -> participants.add(toParticipant(
                decisionRecordId,
                profileId,
                "PARTICIPANT",
                draft.activeProfiles()
        )));
        decisionParticipantRepository.saveAll(participants);
        decisionResourceLinkRepository.saveAll(draft.resources().stream()
                .map(resource -> DecisionResourceLink.builder()
                        .decisionRecordId(decisionRecordId)
                        .relationType(resource.relationType())
                        .resourceType(resource.resourceType())
                        .resourceId(resource.resourceId())
                        .resourceTitleSnapshot(resource.title())
                        .resourcePathSnapshot(resource.targetPath())
                        .build())
                .toList());
    }

    private DecisionParticipant toParticipant(
            Long decisionRecordId,
            Long profileId,
            String role,
            Map<Long, ClubProfile> activeProfiles
    ) {
        ClubProfile profile = activeProfiles.get(profileId);
        if (profile == null) {
            throw new SemoException.ValidationException("현재 활동 중인 멤버만 결정자와 참여자로 지정할 수 있습니다.");
        }
        return DecisionParticipant.builder()
                .decisionRecordId(decisionRecordId)
                .clubProfileId(profileId)
                .participantRole(role)
                .displayNameSnapshot(profile.getDisplayName())
                .build();
    }

    private List<DecisionRecordResponse> toResponses(Long clubId, List<DecisionRecord> records) {
        if (records.isEmpty()) {
            return List.of();
        }
        List<Long> recordIds = records.stream().map(DecisionRecord::getDecisionRecordId).toList();
        Map<Long, List<DecisionParticipant>> participantsByRecord = decisionParticipantRepository
                .findByDecisionRecordIdInOrderByDecisionParticipantIdAsc(recordIds).stream()
                .collect(Collectors.groupingBy(
                        DecisionParticipant::getDecisionRecordId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        Map<Long, List<DecisionResourceLink>> resourcesByRecord = decisionResourceLinkRepository
                .findByDecisionRecordIdInOrderByDecisionResourceLinkIdAsc(recordIds).stream()
                .collect(Collectors.groupingBy(
                        DecisionResourceLink::getDecisionRecordId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        Set<Long> profileIds = records.stream()
                .flatMap(record -> java.util.stream.Stream.of(
                        record.getCreatedByClubProfileId(),
                        record.getConfirmedByClubProfileId()
                ))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, ClubProfile> profilesById = profileIds.isEmpty()
                ? Map.of()
                : clubProfileRepository.findAllById(profileIds).stream()
                        .collect(Collectors.toMap(ClubProfile::getClubProfileId, Function.identity()));
        Map<Long, ClubOperatingTerm> termsById = clubOperatingTermRepository
                .findByClubIdOrderByStartDateDescClubOperatingTermIdDesc(clubId).stream()
                .collect(Collectors.toMap(ClubOperatingTerm::getClubOperatingTermId, Function.identity()));
        Map<Long, DecisionRecord> decisionsById = records.stream()
                .collect(Collectors.toMap(DecisionRecord::getDecisionRecordId, Function.identity()));
        Set<Long> missingSupersededIds = records.stream()
                .map(DecisionRecord::getSupersedesDecisionRecordId)
                .filter(java.util.Objects::nonNull)
                .filter(id -> !decisionsById.containsKey(id))
                .collect(Collectors.toSet());
        if (!missingSupersededIds.isEmpty()) {
            decisionRecordRepository.findAllById(missingSupersededIds).forEach(item ->
                    decisionsById.put(item.getDecisionRecordId(), item)
            );
        }
        return records.stream()
                .map(record -> toResponse(
                        record,
                        participantsByRecord.getOrDefault(record.getDecisionRecordId(), List.of()),
                        resourcesByRecord.getOrDefault(record.getDecisionRecordId(), List.of()),
                        profilesById,
                        termsById,
                        decisionsById
                ))
                .toList();
    }

    private DecisionRecordResponse toResponse(
            DecisionRecord record,
            List<DecisionParticipant> participants,
            List<DecisionResourceLink> resources,
            Map<Long, ClubProfile> profilesById,
            Map<Long, ClubOperatingTerm> termsById,
            Map<Long, DecisionRecord> decisionsById
    ) {
        ClubProfile createdBy = profilesById.get(record.getCreatedByClubProfileId());
        ClubProfile confirmedBy = profilesById.get(record.getConfirmedByClubProfileId());
        ClubOperatingTerm term = termsById.get(record.getClubOperatingTermId());
        DecisionRecord superseded = decisionsById.get(record.getSupersedesDecisionRecordId());
        return new DecisionRecordResponse(
                record.getDecisionRecordId(),
                record.getClubOperatingTermId(),
                term == null ? null : term.getTermName(),
                record.getRecordType(),
                record.getStatusCode(),
                record.getVisibilityScope(),
                record.getTitle(),
                record.getDecisionContent(),
                record.getBackgroundContext(),
                record.getRationale(),
                record.getMeetingAt(),
                record.getEffectiveDate(),
                record.getReviewDate(),
                record.getSupersedesDecisionRecordId(),
                superseded == null ? null : superseded.getTitle(),
                record.getCreatedByClubProfileId(),
                createdBy == null ? "알 수 없는 멤버" : createdBy.getDisplayName(),
                record.getConfirmedByClubProfileId(),
                confirmedBy == null ? null : confirmedBy.getDisplayName(),
                record.getConfirmedAt(),
                record.getCreateDate(),
                record.getUpdateDate(),
                participants.stream()
                        .map(item -> new DecisionParticipantResponse(
                                item.getClubProfileId(),
                                item.getDisplayNameSnapshot(),
                                item.getParticipantRole()
                        ))
                        .toList(),
                resources.stream()
                        .map(item -> new DecisionResourceLinkResponse(
                                item.getRelationType(),
                                item.getResourceType(),
                                item.getResourceId(),
                                item.getResourceTitleSnapshot(),
                                item.getResourcePathSnapshot()
                        ))
                        .toList()
        );
    }

    private DecisionRecordResponse loadResponse(Long clubId, Long decisionRecordId) {
        DecisionRecord record = decisionRecordRepository
                .findByDecisionRecordIdAndClubIdAndDeletedFalse(decisionRecordId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "DecisionRecord",
                        "decisionRecordId",
                        decisionRecordId
                ));
        return toResponses(clubId, List.of(record)).getFirst();
    }

    private DecisionRecord requireRecordForUpdate(Long clubId, Long decisionRecordId) {
        return decisionRecordRepository.findForUpdate(decisionRecordId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "DecisionRecord",
                        "decisionRecordId",
                        decisionRecordId
                ));
    }

    private void validateTerm(Long clubId, Long termId) {
        if (termId == null) {
            return;
        }
        clubOperatingTermRepository.findByClubOperatingTermIdAndClubId(termId, clubId)
                .orElseThrow(() -> new SemoException.ValidationException("선택한 운영 임기를 찾을 수 없습니다."));
    }

    private void validateSupersededDecision(Long clubId, Long supersedesId, Long editingRecordId) {
        if (supersedesId == null) {
            return;
        }
        if (supersedesId.equals(editingRecordId)) {
            throw new SemoException.ValidationException("기록이 자기 자신을 대체할 수 없습니다.");
        }
        DecisionRecord superseded = decisionRecordRepository
                .findByDecisionRecordIdAndClubIdAndDeletedFalse(supersedesId, clubId)
                .orElseThrow(() -> new SemoException.ValidationException("대체할 이전 결정을 찾을 수 없습니다."));
        if (!"CONFIRMED".equals(superseded.getStatusCode())) {
            throw new SemoException.ValidationException("현재 확정 상태인 결정만 대체할 수 있습니다.");
        }
    }

    private LinkedHashSet<Long> normalizeProfileIds(
            Collection<Long> profileIds,
            Map<Long, ClubProfile> activeProfiles,
            String fieldName
    ) {
        LinkedHashSet<Long> normalized = new LinkedHashSet<>();
        for (Long profileId : safeCollection(profileIds)) {
            if (profileId == null || !activeProfiles.containsKey(profileId)) {
                throw new SemoException.ValidationException(fieldName + "에는 현재 활동 중인 멤버만 선택할 수 있습니다.");
            }
            normalized.add(profileId);
        }
        return normalized;
    }

    private String normalizeCode(String value, Set<String> allowed, String fieldName) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new SemoException.ValidationException(fieldName + " 값이 올바르지 않습니다.");
        }
        return normalized;
    }

    private String requireText(String value, int maxLength, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new SemoException.ValidationException(fieldName + "을 입력해주세요.");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new SemoException.ValidationException(fieldName + "은 " + maxLength + "자 이하로 입력해주세요.");
        }
        return normalized;
    }

    private String optionalText(String value, int maxLength, String fieldName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new SemoException.ValidationException(fieldName + "은 " + maxLength + "자 이하로 입력해주세요.");
        }
        return normalized;
    }

    private void requireDraft(DecisionRecord record, String message) {
        if (!"DRAFT".equals(record.getStatusCode())) {
            throw new SemoException.ConflictException(message);
        }
    }

    private void notifyParticipants(
            Long clubId,
            DecisionRecord record,
            List<DecisionParticipant> participants,
            Long actorClubProfileId
    ) {
        participants.stream()
                .map(DecisionParticipant::getClubProfileId)
                .distinct()
                .filter(profileId -> !profileId.equals(actorClubProfileId))
                .forEach(profileId -> clubNotificationPublisher.notifyClubProfile(
                        profileId,
                        new ClubNotificationPublisher.NotificationCommand(
                                clubId,
                                "DECISION_CONFIRMED",
                                "새 회의록·결정이 확정되었습니다",
                                record.getTitle(),
                                "DECISION_RECORD",
                                record.getDecisionRecordId(),
                                "/clubs/%d/more/decisions".formatted(clubId),
                                "decision-confirmed:" + record.getDecisionRecordId() + ":" + profileId
                        )
                ));
    }

    private void lockClub(Long clubId) {
        clubRepository.findForUpdate(clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("Club", "clubId", clubId));
    }

    private int safeCount(long count) {
        if (count <= 0) {
            return 0;
        }
        return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
    }

    private <T> Collection<T> safeCollection(Collection<T> values) {
        return values == null ? List.of() : values;
    }

    private record DecisionDraft(
            String recordType,
            String visibilityScope,
            String title,
            String decisionContent,
            String backgroundContext,
            String rationale,
            LocalDateTime meetingAt,
            LocalDate effectiveDate,
            LocalDate reviewDate,
            Long clubOperatingTermId,
            Long supersedesDecisionRecordId,
            LinkedHashSet<Long> deciderIds,
            LinkedHashSet<Long> participantIds,
            Map<Long, ClubProfile> activeProfiles,
            List<ResolvedResource> resources
    ) {
    }

    private record ResolvedResource(
            String relationType,
            String resourceType,
            Long resourceId,
            String title,
            String targetPath
    ) {
        static ResolvedResource of(
                String relationType,
                DecisionResourceResolver.ResourceDescriptor descriptor
        ) {
            return new ResolvedResource(
                    relationType,
                    descriptor.resourceType(),
                    descriptor.resourceId(),
                    descriptor.title(),
                    descriptor.targetPath()
            );
        }

        String key() {
            return relationType + ":" + resourceType + ":" + resourceId;
        }
    }
}
