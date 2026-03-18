package semo.back.service.feature.schedule.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubEventParticipant;
import semo.back.service.database.pub.entity.ClubNotice;
import semo.back.service.database.pub.entity.ClubScheduleEvent;
import semo.back.service.database.pub.entity.ClubScheduleVote;
import semo.back.service.database.pub.entity.ClubScheduleVoteOption;
import semo.back.service.database.pub.entity.ClubScheduleVoteSelection;
import semo.back.service.database.pub.repository.ClubEventParticipantRepository;
import semo.back.service.database.pub.repository.ClubNoticeRepository;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteOptionRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteSelectionRepository;
import semo.back.service.feature.club.biz.ClubAccessResolver;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.schedule.vo.ClubScheduleResponse;
import semo.back.service.feature.schedule.vo.ScheduleEventDetailResponse;
import semo.back.service.feature.schedule.vo.ScheduleEventSummaryResponse;
import semo.back.service.feature.schedule.vo.ScheduleEventUpsertResponse;
import semo.back.service.feature.schedule.vo.ScheduleOverviewResponse;
import semo.back.service.feature.schedule.vo.ScheduleVoteDetailResponse;
import semo.back.service.feature.schedule.vo.ScheduleVoteOptionSummaryResponse;
import semo.back.service.feature.schedule.vo.ScheduleVoteSummaryResponse;
import semo.back.service.feature.schedule.vo.ScheduleVoteUpsertResponse;
import semo.back.service.feature.schedule.vo.SubmitScheduleVoteSelectionRequest;
import semo.back.service.feature.schedule.vo.UpdateScheduleEventParticipationRequest;
import semo.back.service.feature.schedule.vo.UpsertScheduleEventRequest;
import semo.back.service.feature.schedule.vo.UpsertScheduleVoteRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubScheduleService {
    private static final String NOTICE_CATEGORY_KEY = "GENERAL";
    private static final String FEATURE_NOTICE = "NOTICE";
    private static final String FEATURE_POLL = "POLL";
    private static final String VISIBILITY_STATUS = "CLUB";
    private static final String EVENT_STATUS = "SCHEDULED";
    private static final String PARTICIPATION_GOING = "GOING";
    private static final String PARTICIPATION_NOT_GOING = "NOT_GOING";
    private static final DateTimeFormatter DATE_REQUEST_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_LABEL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd (E)", Locale.KOREAN);
    private static final DateTimeFormatter TIME_REQUEST_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_LABEL_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final ClubScheduleEventRepository clubScheduleEventRepository;
    private final ClubEventParticipantRepository clubEventParticipantRepository;
    private final ClubScheduleVoteRepository clubScheduleVoteRepository;
    private final ClubScheduleVoteOptionRepository clubScheduleVoteOptionRepository;
    private final ClubScheduleVoteSelectionRepository clubScheduleVoteSelectionRepository;
    private final ClubNoticeRepository clubNoticeRepository;
    private final ClubAccessResolver clubAccessResolver;
    private final ClubFeatureService clubFeatureService;

    public ClubScheduleResponse getClubSchedule(Long clubId, String userKey, Integer year, Integer month) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        Long viewerClubProfileId = access.clubProfile().getClubProfileId();
        LocalDate today = LocalDate.now();
        LocalDate monthStartDate = resolveMonthStart(year, month);
        LocalDate monthEndDate = monthStartDate.withDayOfMonth(monthStartDate.lengthOfMonth());
        LocalDateTime monthStartAt = monthStartDate.atStartOfDay();
        LocalDateTime monthEndExclusive = monthEndDate.plusDays(1).atStartOfDay();

        boolean noticeFeatureEnabled = clubFeatureService.isFeatureEnabled(clubId, FEATURE_NOTICE);
        List<ClubScheduleEvent> events = clubScheduleEventRepository.findScheduledBetween(clubId, monthStartAt, monthEndExclusive)
                .stream()
                .filter(event -> noticeFeatureEnabled || event.getLinkedNoticeId() == null)
                .toList();
        Map<Long, List<ClubEventParticipant>> participantsByEventId = clubEventParticipantRepository.findByEventIdIn(
                        events.stream().map(ClubScheduleEvent::getEventId).toList()
                ).stream()
                .collect(Collectors.groupingBy(ClubEventParticipant::getEventId));

        List<ScheduleEventSummaryResponse> monthEvents = events.stream()
                .map(event -> toEventSummaryResponse(
                        event,
                        participantsByEventId.getOrDefault(event.getEventId(), List.of()),
                        viewerClubProfileId
                ))
                .toList();

        List<ScheduleEventSummaryResponse> upcomingEvents = monthEvents.stream()
                .filter(event -> !LocalDate.parse(event.startDate(), DATE_REQUEST_FORMATTER).isBefore(today))
                .toList();
        List<ScheduleEventSummaryResponse> recentEvents = monthEvents.stream()
                .filter(event -> LocalDate.parse(event.startDate(), DATE_REQUEST_FORMATTER).isBefore(today))
                .sorted(Comparator.comparing(ScheduleEventSummaryResponse::startDate).reversed())
                .toList();

        boolean pollFeatureEnabled = clubFeatureService.isFeatureEnabled(clubId, FEATURE_POLL);
        List<ScheduleVoteSummaryResponse> votes = pollFeatureEnabled
                ? getVoteSummaries(clubId, viewerClubProfileId, monthStartDate, monthEndDate)
                : List.of();

        return new ClubScheduleResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                monthStartDate.getYear(),
                monthStartDate.getMonthValue(),
                new ScheduleOverviewResponse(
                        upcomingEvents.size(),
                        recentEvents.size(),
                        votes.size(),
                        (int) monthEvents.stream().filter(ScheduleEventSummaryResponse::postedToBoard).count(),
                        (int) votes.stream().filter(ScheduleVoteSummaryResponse::postedToBoard).count(),
                        (int) upcomingEvents.stream()
                                .filter(ScheduleEventSummaryResponse::participationEnabled)
                                .filter(event -> event.myParticipationStatus() == null)
                                .count(),
                        (int) votes.stream()
                                .filter(ScheduleVoteSummaryResponse::votingOpen)
                                .filter(vote -> vote.mySelectedOptionId() == null)
                                .count()
                ),
                monthEvents,
                votes
        );
    }

    public ScheduleEventDetailResponse getScheduleEventDetail(Long clubId, Long eventId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        ClubScheduleEvent event = getEvent(clubId, eventId);
        return buildEventDetailResponse(access, event);
    }

    public List<ScheduleEventSummaryResponse> getEventSummariesForHome(
            ClubAccessResolver.ClubAccess access,
            List<ClubScheduleEvent> events
    ) {
        if (events.isEmpty()) {
            return List.of();
        }

        Map<Long, List<ClubEventParticipant>> participantsByEventId = clubEventParticipantRepository.findByEventIdIn(
                        events.stream().map(ClubScheduleEvent::getEventId).toList()
                ).stream()
                .collect(Collectors.groupingBy(ClubEventParticipant::getEventId));

        return events.stream()
                .map(event -> toEventSummaryResponse(
                        event,
                        participantsByEventId.getOrDefault(event.getEventId(), List.of()),
                        access.clubProfile().getClubProfileId()
                ))
                .toList();
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public ScheduleEventUpsertResponse createScheduleEvent(Long clubId, String userKey, UpsertScheduleEventRequest request) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        EventDraft draft = toEventDraft(request);
        Long linkedNoticeId = shouldPostToBoard(request.postToBoard())
                ? upsertLinkedNotice(
                clubId,
                access.clubProfile().getClubProfileId(),
                null,
                draft.title(),
                buildEventNoticeContent(draft),
                draft.startAt(),
                draft.endAt()
        )
                : null;

        ClubScheduleEvent event = clubScheduleEventRepository.save(ClubScheduleEvent.builder()
                .clubId(clubId)
                .authorClubProfileId(access.clubProfile().getClubProfileId())
                .linkedNoticeId(linkedNoticeId)
                .categoryKey(NOTICE_CATEGORY_KEY)
                .title(draft.title())
                .description(null)
                .locationLabel(draft.locationLabel())
                .participationConditionText(draft.participationConditionText())
                .startAt(draft.startAt())
                .endAt(draft.endAt())
                .attendeeLimit(draft.attendeeLimit())
                .participationEnabled(draft.participationEnabled())
                .feeRequired(draft.feeRequired())
                .feeAmount(draft.feeAmount())
                .feeAmountUndecided(draft.feeAmountUndecided())
                .feeNWaySplit(draft.feeNWaySplit())
                .visibilityStatus(VISIBILITY_STATUS)
                .eventStatus(EVENT_STATUS)
                .build());

        return toEventUpsertResponse(event);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public ScheduleEventUpsertResponse updateScheduleEvent(
            Long clubId,
            Long eventId,
            String userKey,
            UpsertScheduleEventRequest request
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        ClubScheduleEvent current = getEvent(clubId, eventId);
        requireManagePermission(access, current.getAuthorClubProfileId());
        EventDraft draft = toEventDraft(request);

        Long linkedNoticeId = current.getLinkedNoticeId();
        if (linkedNoticeId != null && !shouldPostToBoard(request.postToBoard())) {
            softDeleteNotice(linkedNoticeId);
            linkedNoticeId = null;
        } else if (shouldPostToBoard(request.postToBoard())) {
            linkedNoticeId = upsertLinkedNotice(
                    current.getClubId(),
                    current.getAuthorClubProfileId(),
                    linkedNoticeId,
                    draft.title(),
                    buildEventNoticeContent(draft),
                    draft.startAt(),
                    draft.endAt()
            );
        }

        ClubScheduleEvent updated = clubScheduleEventRepository.save(ClubScheduleEvent.builder()
                .eventId(current.getEventId())
                .clubId(current.getClubId())
                .authorClubProfileId(current.getAuthorClubProfileId())
                .linkedNoticeId(linkedNoticeId)
                .categoryKey(current.getCategoryKey())
                .title(draft.title())
                .description(null)
                .locationLabel(draft.locationLabel())
                .participationConditionText(draft.participationConditionText())
                .startAt(draft.startAt())
                .endAt(draft.endAt())
                .attendeeLimit(draft.attendeeLimit())
                .participationEnabled(draft.participationEnabled())
                .feeRequired(draft.feeRequired())
                .feeAmount(draft.feeAmount())
                .feeAmountUndecided(draft.feeAmountUndecided())
                .feeNWaySplit(draft.feeNWaySplit())
                .visibilityStatus(current.getVisibilityStatus())
                .eventStatus(EVENT_STATUS)
                .build());

        return toEventUpsertResponse(updated);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public void deleteScheduleEvent(Long clubId, Long eventId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        ClubScheduleEvent current = getEvent(clubId, eventId);
        requireManagePermission(access, current.getAuthorClubProfileId());
        if (current.getLinkedNoticeId() != null) {
            softDeleteNotice(current.getLinkedNoticeId());
        }
        clubEventParticipantRepository.deleteByEventId(current.getEventId());
        clubScheduleEventRepository.delete(current);
    }

    public ScheduleVoteDetailResponse getScheduleVoteDetail(Long clubId, Long voteId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        ClubScheduleVote vote = getVote(clubId, voteId);
        return buildVoteDetailResponse(access, vote);
    }

    public List<ScheduleVoteSummaryResponse> getVoteSummariesForHome(
            ClubAccessResolver.ClubAccess access,
            List<ClubScheduleVote> votes
    ) {
        return toVoteSummaryResponses(votes, access.clubProfile().getClubProfileId());
    }

    public boolean isVoteCurrentlyOpen(ClubScheduleVote vote) {
        return isVoteOpen(vote);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public ScheduleEventDetailResponse updateScheduleEventParticipation(
            Long clubId,
            Long eventId,
            String userKey,
            UpdateScheduleEventParticipationRequest request
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        ClubScheduleEvent event = getEvent(clubId, eventId);
        if (!event.isParticipationEnabled()) {
            throw new SemoException.ValidationException("이 일정은 참석 응답을 받지 않습니다.");
        }
        String participationStatus = normalizeParticipationStatus(request);

        ClubEventParticipant current = clubEventParticipantRepository
                .findByEventIdAndClubProfileId(eventId, access.clubProfile().getClubProfileId())
                .orElse(null);
        clubEventParticipantRepository.save(ClubEventParticipant.builder()
                .clubEventParticipantId(current == null ? null : current.getClubEventParticipantId())
                .eventId(eventId)
                .clubProfileId(access.clubProfile().getClubProfileId())
                .participationStatus(participationStatus)
                .checkedInAt(current == null ? null : current.getCheckedInAt())
                .build());

        return buildEventDetailResponse(access, event);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public ScheduleVoteUpsertResponse createScheduleVote(Long clubId, String userKey, UpsertScheduleVoteRequest request) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        VoteDraft draft = toVoteDraft(request);
        Long linkedNoticeId = shouldPostToBoard(request.postToBoard())
                ? upsertLinkedNotice(
                clubId,
                access.clubProfile().getClubProfileId(),
                null,
                draft.title(),
                buildVoteNoticeContent(draft),
                toVoteNoticeStartAt(draft.voteStartDate(), draft.voteStartTime()),
                toVoteNoticeEndAt(draft.voteEndDate(), draft.voteEndTime())
        )
                : null;

        ClubScheduleVote vote = clubScheduleVoteRepository.save(ClubScheduleVote.builder()
                .clubId(clubId)
                .authorClubProfileId(access.clubProfile().getClubProfileId())
                .linkedNoticeId(linkedNoticeId)
                .sharedToSchedule(shouldPostToSchedule(request.postToSchedule()))
                .title(draft.title())
                .voteStartDate(draft.voteStartDate())
                .voteEndDate(draft.voteEndDate())
                .voteStartTime(draft.voteStartTime())
                .voteEndTime(draft.voteEndTime())
                .closedAt(null)
                .build());
        saveVoteOptions(vote.getVoteId(), draft.optionLabels());
        return toVoteUpsertResponse(vote, draft.optionLabels().size());
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public ScheduleVoteUpsertResponse updateScheduleVote(
            Long clubId,
            Long voteId,
            String userKey,
            UpsertScheduleVoteRequest request
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        ClubScheduleVote current = getVote(clubId, voteId);
        requireManagePermission(access, current.getAuthorClubProfileId());
        VoteDraft draft = toVoteDraft(request);

        Long linkedNoticeId = current.getLinkedNoticeId();
        if (linkedNoticeId != null && !shouldPostToBoard(request.postToBoard())) {
            softDeleteNotice(linkedNoticeId);
            linkedNoticeId = null;
        } else if (shouldPostToBoard(request.postToBoard())) {
            linkedNoticeId = upsertLinkedNotice(
                    current.getClubId(),
                    current.getAuthorClubProfileId(),
                    linkedNoticeId,
                    draft.title(),
                    buildVoteNoticeContent(draft),
                    toVoteNoticeStartAt(draft.voteStartDate(), draft.voteStartTime()),
                    current.getClosedAt() == null
                            ? toVoteNoticeEndAt(draft.voteEndDate(), draft.voteEndTime())
                            : current.getClosedAt()
            );
        }

        ClubScheduleVote updated = clubScheduleVoteRepository.save(ClubScheduleVote.builder()
                .voteId(current.getVoteId())
                .clubId(current.getClubId())
                .authorClubProfileId(current.getAuthorClubProfileId())
                .linkedNoticeId(linkedNoticeId)
                .sharedToSchedule(shouldPostToSchedule(request.postToSchedule()))
                .title(draft.title())
                .voteStartDate(draft.voteStartDate())
                .voteEndDate(draft.voteEndDate())
                .voteStartTime(draft.voteStartTime())
                .voteEndTime(draft.voteEndTime())
                .closedAt(current.getClosedAt())
                .build());

        clubScheduleVoteSelectionRepository.deleteByVoteId(voteId);
        clubScheduleVoteOptionRepository.deleteByVoteId(voteId);
        saveVoteOptions(voteId, draft.optionLabels());

        return toVoteUpsertResponse(updated, draft.optionLabels().size());
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public void deleteScheduleVote(Long clubId, Long voteId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        ClubScheduleVote current = getVote(clubId, voteId);
        requireManagePermission(access, current.getAuthorClubProfileId());
        if (current.getLinkedNoticeId() != null) {
            softDeleteNotice(current.getLinkedNoticeId());
        }
        clubScheduleVoteSelectionRepository.deleteByVoteId(voteId);
        clubScheduleVoteOptionRepository.deleteByVoteId(voteId);
        clubScheduleVoteRepository.delete(current);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public ScheduleVoteDetailResponse closeScheduleVote(Long clubId, Long voteId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        ClubScheduleVote current = getVote(clubId, voteId);
        requireManagePermission(access, current.getAuthorClubProfileId());
        if (current.getClosedAt() != null) {
            return buildVoteDetailResponse(access, current);
        }

        LocalDateTime closedAt = LocalDateTime.now();
        ClubScheduleVote closedVote = clubScheduleVoteRepository.save(ClubScheduleVote.builder()
                .voteId(current.getVoteId())
                .clubId(current.getClubId())
                .authorClubProfileId(current.getAuthorClubProfileId())
                .linkedNoticeId(current.getLinkedNoticeId())
                .sharedToSchedule(current.isSharedToSchedule())
                .title(current.getTitle())
                .voteStartDate(current.getVoteStartDate())
                .voteEndDate(current.getVoteEndDate())
                .voteStartTime(current.getVoteStartTime())
                .voteEndTime(current.getVoteEndTime())
                .closedAt(closedAt)
                .build());
        updateLinkedNoticeScheduleEndAt(closedVote.getLinkedNoticeId(), closedAt);
        return buildVoteDetailResponse(access, closedVote);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public ScheduleVoteDetailResponse submitScheduleVoteSelection(
            Long clubId,
            Long voteId,
            String userKey,
            SubmitScheduleVoteSelectionRequest request
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        ClubScheduleVote vote = getVote(clubId, voteId);
        if (!isVoteOpen(vote)) {
            throw new SemoException.ValidationException("현재 투표 기간이 아닙니다.");
        }
        List<ClubScheduleVoteOption> options = clubScheduleVoteOptionRepository.findByVoteIdOrderBySortOrderAscVoteOptionIdAsc(voteId);
        ClubScheduleVoteOption selectedOption = options.stream()
                .filter(option -> option.getVoteOptionId().equals(request.voteOptionId()))
                .findFirst()
                .orElseThrow(() -> new SemoException.ValidationException("해당 투표의 항목이 아닙니다."));

        ClubScheduleVoteSelection current = clubScheduleVoteSelectionRepository
                .findByVoteIdAndClubProfileId(voteId, access.clubProfile().getClubProfileId())
                .orElse(null);
        clubScheduleVoteSelectionRepository.save(ClubScheduleVoteSelection.builder()
                .voteSelectionId(current == null ? null : current.getVoteSelectionId())
                .voteId(voteId)
                .voteOptionId(selectedOption.getVoteOptionId())
                .clubProfileId(access.clubProfile().getClubProfileId())
                .build());

        return buildVoteDetailResponse(access, vote);
    }

    private List<ScheduleVoteSummaryResponse> getVoteSummaries(
            Long clubId,
            Long viewerClubProfileId,
            LocalDate monthStartDate,
            LocalDate monthEndDate
    ) {
        List<ClubScheduleVote> votes = clubScheduleVoteRepository.findAllByClubIdForMonth(clubId, monthStartDate, monthEndDate);
        return toVoteSummaryResponses(votes, viewerClubProfileId);
    }

    private List<ScheduleVoteSummaryResponse> toVoteSummaryResponses(
            List<ClubScheduleVote> votes,
            Long viewerClubProfileId
    ) {
        if (votes.isEmpty()) {
            return List.of();
        }

        Map<Long, List<ClubScheduleVoteOption>> optionsByVoteId = clubScheduleVoteOptionRepository.findByVoteIdIn(
                        votes.stream().map(ClubScheduleVote::getVoteId).toList()
                ).stream()
                .sorted(Comparator.comparing(ClubScheduleVoteOption::getSortOrder)
                        .thenComparing(ClubScheduleVoteOption::getVoteOptionId))
                .collect(Collectors.groupingBy(ClubScheduleVoteOption::getVoteId, LinkedHashMap::new, Collectors.toList()));

        Map<Long, List<ClubScheduleVoteSelection>> selectionsByVoteId = clubScheduleVoteSelectionRepository.findByVoteIdIn(
                        votes.stream().map(ClubScheduleVote::getVoteId).toList()
                ).stream()
                .collect(Collectors.groupingBy(ClubScheduleVoteSelection::getVoteId));

        return votes.stream()
                .map(vote -> {
                    VoteSelectionSnapshot selection = toVoteSelectionSnapshot(
                            optionsByVoteId.getOrDefault(vote.getVoteId(), List.of()),
                            selectionsByVoteId.getOrDefault(vote.getVoteId(), List.of()),
                            viewerClubProfileId
                    );
                    return new ScheduleVoteSummaryResponse(
                            vote.getVoteId(),
                            vote.getTitle(),
                            resolveVoteStatus(vote),
                            formatDateValue(vote.getVoteStartDate()),
                            formatDateValue(vote.getVoteEndDate()),
                            formatDateRangeLabel(vote.getVoteStartDate(), vote.getVoteEndDate()),
                            formatVoteTimeLabel(vote.getVoteStartTime(), vote.getVoteEndTime()),
                            selection.options().size(),
                            selection.totalResponses(),
                            vote.getLinkedNoticeId() != null,
                            vote.isSharedToSchedule(),
                            vote.getLinkedNoticeId(),
                            selection.mySelectedOptionId(),
                            selection.options(),
                            isVoteOpen(vote)
                    );
                })
                .toList();
    }

    private ScheduleEventDetailResponse buildEventDetailResponse(
            ClubAccessResolver.ClubAccess access,
            ClubScheduleEvent event
    ) {
        EventParticipationSnapshot participation = toParticipationSnapshot(
                clubEventParticipantRepository.findByEventIdIn(List.of(event.getEventId())),
                access.clubProfile().getClubProfileId()
        );

        return new ScheduleEventDetailResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                event.getEventId(),
                event.getTitle(),
                formatDateValue(event.getStartAt().toLocalDate()),
                event.getEndAt() == null ? null : formatDateValue(event.getEndAt().toLocalDate()),
                formatDateRangeLabel(event.getStartAt().toLocalDate(), event.getEndAt() == null ? null : event.getEndAt().toLocalDate()),
                formatTimeValue(event.getStartAt(), event.getEndAt()),
                formatEndTimeValue(event.getStartAt(), event.getEndAt()),
                formatTimeLabel(event.getStartAt(), event.getEndAt()),
                event.getAttendeeLimit(),
                event.getLocationLabel(),
                event.getParticipationConditionText(),
                event.isParticipationEnabled(),
                event.isFeeRequired(),
                event.getFeeAmount(),
                event.isFeeAmountUndecided(),
                event.isFeeNWaySplit(),
                event.getLinkedNoticeId() != null,
                event.getLinkedNoticeId(),
                participation.myParticipationStatus(),
                participation.goingCount(),
                participation.notGoingCount(),
                canManage(access, event.getAuthorClubProfileId())
        );
    }

    private ScheduleVoteDetailResponse buildVoteDetailResponse(
            ClubAccessResolver.ClubAccess access,
            ClubScheduleVote vote
    ) {
        List<ClubScheduleVoteOption> options = clubScheduleVoteOptionRepository.findByVoteIdOrderBySortOrderAscVoteOptionIdAsc(vote.getVoteId());
        VoteSelectionSnapshot selection = toVoteSelectionSnapshot(
                options,
                clubScheduleVoteSelectionRepository.findByVoteIdIn(List.of(vote.getVoteId())),
                access.clubProfile().getClubProfileId()
        );

        return new ScheduleVoteDetailResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                vote.getVoteId(),
                vote.getTitle(),
                resolveVoteStatus(vote),
                formatDateValue(vote.getVoteStartDate()),
                formatDateValue(vote.getVoteEndDate()),
                formatDateRangeLabel(vote.getVoteStartDate(), vote.getVoteEndDate()),
                formatOptionalTimeValue(vote.getVoteStartTime()),
                formatOptionalTimeValue(vote.getVoteEndTime()),
                formatVoteTimeLabel(vote.getVoteStartTime(), vote.getVoteEndTime()),
                vote.getLinkedNoticeId() != null,
                vote.isSharedToSchedule(),
                vote.getLinkedNoticeId(),
                selection.mySelectedOptionId(),
                selection.totalResponses(),
                selection.options(),
                canManage(access, vote.getAuthorClubProfileId()),
                isVoteOpen(vote)
        );
    }

    private EventDraft toEventDraft(UpsertScheduleEventRequest request) {
        if (request == null) {
            throw new SemoException.ValidationException("일정 요청이 비어 있습니다.");
        }

        LocalDate startDate = parseDate(request.startDate());
        LocalDate endDate = parseOptionalDate(request.endDate());
        LocalTime startTime = parseOptionalTime(request.startTime());
        LocalTime endTime = parseOptionalTime(request.endTime());
        if (startTime == null && endTime != null) {
            throw new SemoException.ValidationException("종료 시간만 단독으로 입력할 수 없습니다.");
        }

        LocalDate resolvedEndDate = endDate == null ? startDate : endDate;
        LocalDateTime startAt = startDate.atTime(startTime == null ? LocalTime.MIDNIGHT : startTime);
        LocalDateTime endAt = endDate == null && endTime == null
                ? null
                : resolvedEndDate.atTime(endTime == null ? LocalTime.MIDNIGHT : endTime);
        if (endAt != null && endAt.isBefore(startAt)) {
            throw new SemoException.ValidationException("종료 시간은 시작 시간보다 빠를 수 없습니다.");
        }

        boolean participationEnabled = Boolean.TRUE.equals(request.participationEnabled());
        boolean feeRequired = Boolean.TRUE.equals(request.feeRequired());
        boolean feeAmountUndecided = feeRequired && Boolean.TRUE.equals(request.feeAmountUndecided());
        Integer feeAmount = feeRequired && !feeAmountUndecided ? request.feeAmount() : null;
        if (feeRequired && !feeAmountUndecided && feeAmount == null) {
            throw new SemoException.ValidationException("참가비를 입력하거나 금액 미정을 선택해야 합니다.");
        }
        return new EventDraft(
                trimRequired(request.title(), "일정 제목은 필수입니다."),
                startAt,
                endAt,
                participationEnabled ? request.attendeeLimit() : null,
                trimToNull(request.locationLabel()),
                participationEnabled ? trimToNull(request.participationConditionText()) : null,
                participationEnabled,
                feeRequired,
                feeAmount,
                feeAmountUndecided,
                feeRequired && participationEnabled && Boolean.TRUE.equals(request.feeNWaySplit())
        );
    }

    private VoteDraft toVoteDraft(UpsertScheduleVoteRequest request) {
        if (request == null) {
            throw new SemoException.ValidationException("투표 요청이 비어 있습니다.");
        }

        List<String> optionLabels = request.optionLabels() == null
                ? List.of()
                : request.optionLabels().stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .toList();
        if (optionLabels.size() < 2) {
            throw new SemoException.ValidationException("투표 항목은 최소 2개 이상이어야 합니다.");
        }
        if (optionLabels.size() != optionLabels.stream().distinct().count()) {
            throw new SemoException.ValidationException("투표 항목은 중복될 수 없습니다.");
        }

        LocalDate voteStartDate = parseDate(request.voteStartDate());
        LocalDate voteEndDate = parseDate(request.voteEndDate());
        LocalTime voteStartTime = parseOptionalTime(request.voteStartTime());
        LocalTime voteEndTime = parseOptionalTime(request.voteEndTime());
        if (voteStartTime == null && voteEndTime != null) {
            throw new SemoException.ValidationException("투표 종료 시간만 단독으로 입력할 수 없습니다.");
        }

        LocalDateTime voteStartAt = toVoteStartAt(voteStartDate, voteStartTime);
        LocalDateTime voteEndAt = toVoteEffectiveEndAt(voteEndDate, voteEndTime);
        if (voteEndAt.isBefore(voteStartAt)) {
            throw new SemoException.ValidationException("투표 종료일시는 시작일시보다 빠를 수 없습니다.");
        }

        return new VoteDraft(
                trimRequired(request.title(), "투표 제목은 필수입니다."),
                voteStartDate,
                voteEndDate,
                voteStartTime,
                voteEndTime,
                optionLabels
        );
    }

    private String normalizeParticipationStatus(UpdateScheduleEventParticipationRequest request) {
        if (request == null || !StringUtils.hasText(request.participationStatus())) {
            throw new SemoException.ValidationException("참석 상태는 필수입니다.");
        }
        String normalized = request.participationStatus().trim().toUpperCase(Locale.ROOT);
        if (!PARTICIPATION_GOING.equals(normalized) && !PARTICIPATION_NOT_GOING.equals(normalized)) {
            throw new SemoException.ValidationException("지원하지 않는 참석 상태입니다.");
        }
        return normalized;
    }

    private ScheduleEventSummaryResponse toEventSummaryResponse(
            ClubScheduleEvent event,
            List<ClubEventParticipant> participants,
            Long viewerClubProfileId
    ) {
        EventParticipationSnapshot participation = toParticipationSnapshot(participants, viewerClubProfileId);
        return new ScheduleEventSummaryResponse(
                event.getEventId(),
                event.getTitle(),
                formatDateValue(event.getStartAt().toLocalDate()),
                event.getEndAt() == null ? null : formatDateValue(event.getEndAt().toLocalDate()),
                formatDateRangeLabel(event.getStartAt().toLocalDate(), event.getEndAt() == null ? null : event.getEndAt().toLocalDate()),
                formatTimeLabel(event.getStartAt(), event.getEndAt()),
                event.getAttendeeLimit(),
                event.getLocationLabel(),
                event.getParticipationConditionText(),
                event.isParticipationEnabled(),
                event.isFeeRequired(),
                event.getFeeAmount(),
                event.isFeeAmountUndecided(),
                event.isFeeNWaySplit(),
                event.getLinkedNoticeId() != null,
                event.getLinkedNoticeId(),
                participation.myParticipationStatus(),
                participation.goingCount(),
                participation.notGoingCount()
        );
    }

    private EventParticipationSnapshot toParticipationSnapshot(
            List<ClubEventParticipant> participants,
            Long viewerClubProfileId
    ) {
        int goingCount = 0;
        int notGoingCount = 0;
        String myParticipationStatus = null;

        for (ClubEventParticipant participant : participants) {
            switch (participant.getParticipationStatus()) {
                case PARTICIPATION_GOING -> goingCount++;
                case PARTICIPATION_NOT_GOING -> notGoingCount++;
                default -> {
                }
            }
            if (participant.getClubProfileId().equals(viewerClubProfileId)) {
                myParticipationStatus = participant.getParticipationStatus();
            }
        }

        return new EventParticipationSnapshot(myParticipationStatus, goingCount, notGoingCount);
    }

    private VoteSelectionSnapshot toVoteSelectionSnapshot(
            List<ClubScheduleVoteOption> options,
            List<ClubScheduleVoteSelection> selections,
            Long viewerClubProfileId
    ) {
        Map<Long, Integer> voteCountByOptionId = new LinkedHashMap<>();
        for (ClubScheduleVoteOption option : options) {
            voteCountByOptionId.put(option.getVoteOptionId(), 0);
        }

        Long mySelectedOptionId = null;
        for (ClubScheduleVoteSelection selection : selections) {
            voteCountByOptionId.computeIfPresent(
                    selection.getVoteOptionId(),
                    (ignored, count) -> count + 1
            );
            if (selection.getClubProfileId().equals(viewerClubProfileId)) {
                mySelectedOptionId = selection.getVoteOptionId();
            }
        }

        List<ScheduleVoteOptionSummaryResponse> optionResponses = options.stream()
                .map(option -> new ScheduleVoteOptionSummaryResponse(
                        option.getVoteOptionId(),
                        option.getOptionLabel(),
                        option.getSortOrder(),
                        voteCountByOptionId.getOrDefault(option.getVoteOptionId(), 0)
                ))
                .toList();

        return new VoteSelectionSnapshot(mySelectedOptionId, selections.size(), optionResponses);
    }

    private void saveVoteOptions(Long voteId, List<String> optionLabels) {
        for (int index = 0; index < optionLabels.size(); index++) {
            clubScheduleVoteOptionRepository.save(ClubScheduleVoteOption.builder()
                    .voteId(voteId)
                    .optionLabel(optionLabels.get(index))
                    .sortOrder(index + 1)
                    .build());
        }
    }

    private Long upsertLinkedNotice(
            Long clubId,
            Long authorClubProfileId,
            Long linkedNoticeId,
            String title,
            String content,
            LocalDateTime scheduleAt,
            LocalDateTime scheduleEndAt
    ) {
        ClubNotice current = linkedNoticeId == null ? null : clubNoticeRepository.findById(linkedNoticeId).orElse(null);
        ClubNotice saved = clubNoticeRepository.save(ClubNotice.builder()
                .noticeId(current == null ? null : current.getNoticeId())
                .clubId(clubId)
                .authorClubProfileId(authorClubProfileId)
                .categoryKey(NOTICE_CATEGORY_KEY)
                .title(title)
                .content(content)
                .locationLabel(null)
                .scheduleAt(scheduleAt)
                .scheduleEndAt(scheduleEndAt)
                .pinned(current != null && current.isPinned())
                .publishedAt(current == null ? LocalDateTime.now() : current.getPublishedAt())
                .deleted(false)
                .build());
        return saved.getNoticeId();
    }

    private void softDeleteNotice(Long noticeId) {
        clubNoticeRepository.findById(noticeId).ifPresent(current -> clubNoticeRepository.save(ClubNotice.builder()
                .noticeId(current.getNoticeId())
                .clubId(current.getClubId())
                .authorClubProfileId(current.getAuthorClubProfileId())
                .categoryKey(current.getCategoryKey())
                .title(current.getTitle())
                .content(current.getContent())
                .locationLabel(current.getLocationLabel())
                .scheduleAt(current.getScheduleAt())
                .scheduleEndAt(current.getScheduleEndAt())
                .pinned(current.isPinned())
                .publishedAt(current.getPublishedAt())
                .deleted(true)
                .build()));
    }

    private ClubScheduleEvent getEvent(Long clubId, Long eventId) {
        return clubScheduleEventRepository.findByEventIdAndClubId(eventId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("ScheduleEvent", "eventId", eventId));
    }

    private ClubScheduleVote getVote(Long clubId, Long voteId) {
        return clubScheduleVoteRepository.findByVoteIdAndClubId(voteId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("ScheduleVote", "voteId", voteId));
    }

    private ScheduleEventUpsertResponse toEventUpsertResponse(ClubScheduleEvent event) {
        return new ScheduleEventUpsertResponse(
                event.getEventId(),
                event.getLinkedNoticeId(),
                event.getTitle(),
                formatDateValue(event.getStartAt().toLocalDate()),
                event.getEndAt() == null ? null : formatDateValue(event.getEndAt().toLocalDate()),
                formatDateRangeLabel(event.getStartAt().toLocalDate(), event.getEndAt() == null ? null : event.getEndAt().toLocalDate()),
                formatTimeLabel(event.getStartAt(), event.getEndAt()),
                event.getLinkedNoticeId() != null
        );
    }

    private ScheduleVoteUpsertResponse toVoteUpsertResponse(ClubScheduleVote vote, int optionCount) {
        return new ScheduleVoteUpsertResponse(
                vote.getVoteId(),
                vote.getLinkedNoticeId(),
                vote.getTitle(),
                formatDateValue(vote.getVoteStartDate()),
                formatDateValue(vote.getVoteEndDate()),
                formatDateRangeLabel(vote.getVoteStartDate(), vote.getVoteEndDate()),
                formatOptionalTimeValue(vote.getVoteStartTime()),
                formatOptionalTimeValue(vote.getVoteEndTime()),
                formatVoteTimeLabel(vote.getVoteStartTime(), vote.getVoteEndTime()),
                optionCount,
                vote.getLinkedNoticeId() != null,
                vote.isSharedToSchedule()
        );
    }

    private boolean canManage(ClubAccessResolver.ClubAccess access, Long authorClubProfileId) {
        return access.isAdmin() || access.clubProfile().getClubProfileId().equals(authorClubProfileId);
    }

    private void requireManagePermission(ClubAccessResolver.ClubAccess access, Long authorClubProfileId) {
        if (!canManage(access, authorClubProfileId)) {
            throw new SemoException.ForbiddenException("작성자 또는 관리자만 관리할 수 있습니다.");
        }
    }

    private String buildEventNoticeContent(EventDraft draft) {
        List<String> lines = new java.util.ArrayList<>();
        lines.add("일정이 등록되었습니다.");
        lines.add("날짜: " + formatDateRangeLabel(
                draft.startAt().toLocalDate(),
                draft.endAt() == null ? null : draft.endAt().toLocalDate()
        ));
        if (formatTimeLabel(draft.startAt(), draft.endAt()) != null) {
            lines.add("시간: " + formatTimeLabel(draft.startAt(), draft.endAt()));
        }
        if (draft.attendeeLimit() != null) {
            lines.add("인원: 최대 " + draft.attendeeLimit() + "명");
        }
        if (draft.participationEnabled()) {
            lines.add("참석 응답: 참석 / 불참 사용");
        }
        if (draft.locationLabel() != null) {
            lines.add("장소: " + draft.locationLabel());
        }
        if (draft.participationConditionText() != null) {
            lines.add("참여 조건: " + draft.participationConditionText());
        }
        if (draft.feeRequired()) {
            lines.add("비용: " + formatFeeSummary(draft.feeAmount(), draft.feeAmountUndecided(), draft.feeNWaySplit()));
        }
        return String.join("\n", lines);
    }

    private String formatFeeSummary(Integer feeAmount, boolean feeAmountUndecided, boolean feeNWaySplit) {
        String feeText = feeAmountUndecided
                ? "금액 미정"
                : feeAmount == null ? "비용 발생" : String.format(Locale.KOREA, "%,d원", feeAmount);
        if (feeNWaySplit) {
            return feeText + " · 1/n 정산";
        }
        return feeText;
    }

    private String buildVoteNoticeContent(VoteDraft draft) {
        List<String> lines = new java.util.ArrayList<>();
        lines.add("투표가 등록되었습니다.");
        lines.add("");
        lines.add("기간: " + formatDateRangeLabel(draft.voteStartDate(), draft.voteEndDate()));
        String timeLabel = formatVoteTimeLabel(draft.voteStartTime(), draft.voteEndTime());
        if (timeLabel != null) {
            lines.add("시간: " + timeLabel);
        }
        lines.add("");
        lines.add("항목");
        lines.addAll(draft.optionLabels().stream()
                .map(option -> "- " + option)
                .toList());
        return String.join("\n", lines).trim();
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value, DATE_REQUEST_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new SemoException.ValidationException("잘못된 날짜 형식입니다.");
        }
    }

    private LocalDate parseOptionalDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return parseDate(value);
    }

    private LocalTime parseOptionalTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalTime.parse(value, TIME_REQUEST_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new SemoException.ValidationException("잘못된 시간 형식입니다.");
        }
    }

    private String formatDateValue(LocalDate value) {
        return value.format(DATE_REQUEST_FORMATTER);
    }

    private String formatDateLabel(LocalDate value) {
        return value.format(DATE_LABEL_FORMATTER);
    }

    private String formatDateRangeLabel(LocalDate startDate, LocalDate endDate) {
        if (endDate == null || endDate.equals(startDate)) {
            return formatDateLabel(startDate);
        }
        return formatDateLabel(startDate) + " - " + formatDateLabel(endDate);
    }

    private String formatOptionalTimeValue(LocalTime value) {
        if (value == null) {
            return null;
        }
        return value.format(TIME_REQUEST_FORMATTER);
    }

    private String formatVoteTimeLabel(LocalTime startTime, LocalTime endTime) {
        if (startTime == null && endTime == null) {
            return null;
        }
        if (startTime == null) {
            return null;
        }
        if (endTime == null) {
            return startTime.format(TIME_LABEL_FORMATTER);
        }
        return startTime.format(TIME_LABEL_FORMATTER)
                + " - "
                + endTime.format(TIME_LABEL_FORMATTER);
    }

    private String formatTimeValue(LocalDateTime startAt, LocalDateTime endAt) {
        if (!hasExplicitTime(startAt, endAt)) {
            return null;
        }
        return startAt.toLocalTime().format(TIME_REQUEST_FORMATTER);
    }

    private String formatEndTimeValue(LocalDateTime startAt, LocalDateTime endAt) {
        if (endAt == null || !hasExplicitTime(startAt, endAt)) {
            return null;
        }
        return endAt.toLocalTime().format(TIME_REQUEST_FORMATTER);
    }

    private String formatTimeLabel(LocalDateTime startAt, LocalDateTime endAt) {
        if (!hasExplicitTime(startAt, endAt)) {
            return null;
        }
        if (endAt == null) {
            return startAt.toLocalTime().format(TIME_LABEL_FORMATTER);
        }
        return startAt.toLocalTime().format(TIME_LABEL_FORMATTER)
                + " - "
                + endAt.toLocalTime().format(TIME_LABEL_FORMATTER);
    }

    private boolean hasExplicitTime(LocalDateTime startAt, LocalDateTime endAt) {
        return !LocalTime.MIDNIGHT.equals(startAt.toLocalTime())
                || (endAt != null && !LocalTime.MIDNIGHT.equals(endAt.toLocalTime()));
    }

    private String trimRequired(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new SemoException.ValidationException(message);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean shouldPostToBoard(Boolean postToBoard) {
        return Boolean.TRUE.equals(postToBoard);
    }

    private boolean shouldPostToSchedule(Boolean postToSchedule) {
        return Boolean.TRUE.equals(postToSchedule);
    }

    private LocalDate resolveMonthStart(Integer year, Integer month) {
        LocalDate today = LocalDate.now();
        int resolvedYear = year == null ? today.getYear() : year;
        int resolvedMonth = month == null ? today.getMonthValue() : month;
        if (resolvedMonth < 1 || resolvedMonth > 12) {
            throw new SemoException.ValidationException("조회 월은 1월부터 12월 사이여야 합니다.");
        }
        return LocalDate.of(resolvedYear, resolvedMonth, 1);
    }

    private LocalDateTime toVoteStartAt(LocalDate startDate, LocalTime startTime) {
        return startDate.atTime(startTime == null ? LocalTime.MIDNIGHT : startTime);
    }

    private LocalDateTime toVoteEffectiveEndAt(LocalDate endDate, LocalTime endTime) {
        return endDate.atTime(endTime == null ? LocalTime.MAX : endTime);
    }

    private LocalDateTime toVoteNoticeStartAt(LocalDate startDate, LocalTime startTime) {
        return toVoteStartAt(startDate, startTime);
    }

    private LocalDateTime toVoteNoticeEndAt(LocalDate endDate, LocalTime endTime) {
        return endDate.atTime(endTime == null ? LocalTime.MIDNIGHT : endTime);
    }

    private void updateLinkedNoticeScheduleEndAt(Long linkedNoticeId, LocalDateTime scheduleEndAt) {
        if (linkedNoticeId == null) {
            return;
        }
        clubNoticeRepository.findById(linkedNoticeId).ifPresent(current -> clubNoticeRepository.save(ClubNotice.builder()
                .noticeId(current.getNoticeId())
                .clubId(current.getClubId())
                .authorClubProfileId(current.getAuthorClubProfileId())
                .categoryKey(current.getCategoryKey())
                .title(current.getTitle())
                .content(current.getContent())
                .locationLabel(current.getLocationLabel())
                .scheduleAt(current.getScheduleAt())
                .scheduleEndAt(scheduleEndAt)
                .pinned(current.isPinned())
                .publishedAt(current.getPublishedAt())
                .deleted(current.isDeleted())
                .build()));
    }

    private boolean isVoteOpen(ClubScheduleVote vote) {
        return "ONGOING".equals(resolveVoteStatus(vote));
    }

    private String resolveVoteStatus(ClubScheduleVote vote) {
        if (vote.getClosedAt() != null) {
            return "CLOSED";
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startAt = toVoteStartAt(vote.getVoteStartDate(), vote.getVoteStartTime());
        if (now.isBefore(startAt)) {
            return "WAITING";
        }
        if (now.isAfter(toVoteEffectiveEndAt(vote.getVoteEndDate(), vote.getVoteEndTime()))) {
            return "CLOSED";
        }
        return "ONGOING";
    }

    private record EventDraft(
            String title,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Integer attendeeLimit,
            String locationLabel,
            String participationConditionText,
            boolean participationEnabled,
            boolean feeRequired,
            Integer feeAmount,
            boolean feeAmountUndecided,
            boolean feeNWaySplit
    ) {
    }

    private record VoteDraft(
            String title,
            LocalDate voteStartDate,
            LocalDate voteEndDate,
            LocalTime voteStartTime,
            LocalTime voteEndTime,
            List<String> optionLabels
    ) {
    }

    private record EventParticipationSnapshot(
            String myParticipationStatus,
            int goingCount,
            int notGoingCount
    ) {
    }

    private record VoteSelectionSnapshot(
            Long mySelectedOptionId,
            int totalResponses,
            List<ScheduleVoteOptionSummaryResponse> options
    ) {
    }
}
