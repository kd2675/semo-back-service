package semo.back.service.feature.schedule.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubCalendarItem;
import semo.back.service.database.pub.entity.ClubEventParticipant;
import semo.back.service.database.pub.entity.ClubScheduleEvent;
import semo.back.service.database.pub.entity.ClubScheduleVote;
import semo.back.service.database.pub.entity.ClubScheduleVoteOption;
import semo.back.service.database.pub.entity.ClubScheduleVoteSelection;
import semo.back.service.database.pub.repository.ClubCalendarItemRepository;
import semo.back.service.database.pub.repository.ClubEventParticipantRepository;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteOptionRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteSelectionRepository;
import semo.back.service.feature.activity.biz.ClubActivityContextHolder;
import semo.back.service.feature.activity.biz.RecordClubActivity;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.poll.biz.ClubPollPermissionService;
import semo.back.service.feature.schedule.biz.policy.ClubSchedulePermissionService;
import semo.back.service.feature.schedule.biz.support.ClubScheduleCalendarLoader;
import semo.back.service.feature.schedule.biz.support.ClubScheduleCommandSupport;
import semo.back.service.feature.schedule.biz.support.ClubScheduleResponseAssembler;
import semo.back.service.feature.schedule.biz.support.ClubScheduleViewSupport;
import semo.back.service.feature.schedule.vo.ClubCalendarFeedItemResponse;
import semo.back.service.feature.share.biz.ClubContentShareService;
import semo.back.service.feature.schedule.vo.ClubScheduleResponse;
import semo.back.service.feature.schedule.vo.ScheduleEventDetailResponse;
import semo.back.service.feature.schedule.vo.ScheduleEventSummaryResponse;
import semo.back.service.feature.schedule.vo.ScheduleEventUpsertResponse;
import semo.back.service.feature.schedule.vo.ScheduleOverviewResponse;
import semo.back.service.feature.schedule.vo.ScheduleVoteDetailResponse;
import semo.back.service.feature.schedule.vo.ScheduleVoteSummaryResponse;
import semo.back.service.feature.schedule.vo.ScheduleVoteUpsertResponse;
import semo.back.service.feature.schedule.vo.SubmitScheduleVoteSelectionRequest;
import semo.back.service.feature.schedule.vo.UpdateScheduleEventParticipationRequest;
import semo.back.service.feature.schedule.vo.UpsertScheduleEventRequest;
import semo.back.service.feature.schedule.vo.UpsertScheduleVoteRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubScheduleService {
    private static final String FEATURE_POLL = "POLL";
    private static final String FEATURE_SCHEDULE_MANAGE = "SCHEDULE_MANAGE";
    private static final String FEATURE_NOTICE = "NOTICE";
    private static final String FEATURE_TOURNAMENT_RECORD = "TOURNAMENT_RECORD";
    private static final String VISIBILITY_STATUS = "CLUB";
    private static final String EVENT_STATUS = "SCHEDULED";
    private static final String PARTICIPATION_GOING = "GOING";
    private static final String PARTICIPATION_CANCELED = "CANCELED";

    private final ClubScheduleEventRepository clubScheduleEventRepository;
    private final ClubEventParticipantRepository clubEventParticipantRepository;
    private final ClubScheduleVoteRepository clubScheduleVoteRepository;
    private final ClubScheduleVoteOptionRepository clubScheduleVoteOptionRepository;
    private final ClubScheduleVoteSelectionRepository clubScheduleVoteSelectionRepository;
    private final ClubCalendarItemRepository clubCalendarItemRepository;
    private final ClubAccessResolver clubAccessResolver;
    private final ClubFeatureService clubFeatureService;
    private final ClubSchedulePermissionService clubSchedulePermissionService;
    private final ClubPollPermissionService clubPollPermissionService;
    private final ClubContentShareService clubContentShareService;
    private final ClubScheduleCalendarLoader clubScheduleCalendarLoader;
    private final ClubScheduleCommandSupport clubScheduleCommandSupport;
    private final ClubScheduleResponseAssembler clubScheduleResponseAssembler;
    private final ClubScheduleViewSupport clubScheduleViewSupport;

    public ClubScheduleResponse getClubSchedule(Long clubId, String userKey, Integer year, Integer month) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        Set<String> enabledFeatureKeys = clubFeatureService.getEnabledFeatureKeys(clubId);
        LocalDate today = LocalDate.now();
        LocalDate monthStartDate = clubScheduleViewSupport.resolveMonthStart(year, month);
        LocalDate monthEndDate = monthStartDate.withDayOfMonth(monthStartDate.lengthOfMonth());
        LocalDateTime monthStartAt = monthStartDate.atStartOfDay();
        LocalDateTime monthEndExclusive = monthEndDate.plusDays(1).atStartOfDay();

        List<ClubCalendarItem> calendarItems = clubCalendarItemRepository.findMonthItems(
                clubId,
                monthStartAt,
                monthEndExclusive,
                monthStartDate,
                monthEndDate
        );
        List<ClubCalendarFeedItemResponse> items = clubScheduleCalendarLoader.loadCalendarFeedItems(
                        access,
                        calendarItems
                ).stream()
                .filter(item -> isCalendarContentEnabled(enabledFeatureKeys, item.contentType()))
                .toList();

        List<ScheduleEventSummaryResponse> monthEvents = items.stream()
                .map(ClubCalendarFeedItemResponse::event)
                .filter(Objects::nonNull)
                .toList();
        List<ScheduleEventSummaryResponse> upcomingEvents = monthEvents.stream()
                .filter(event -> !LocalDate.parse(event.startDate()).isBefore(today))
                .toList();
        List<ScheduleEventSummaryResponse> recentEvents = monthEvents.stream()
                .filter(event -> LocalDate.parse(event.startDate()).isBefore(today))
                .sorted(Comparator.comparing(ScheduleEventSummaryResponse::startDate).reversed())
                .toList();
        List<ScheduleVoteSummaryResponse> votes = items.stream()
                .map(ClubCalendarFeedItemResponse::vote)
                .filter(Objects::nonNull)
                .toList();

        return new ClubScheduleResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                enabledFeatureKeys.contains(FEATURE_SCHEDULE_MANAGE)
                        && clubSchedulePermissionService.canCreateSchedule(access),
                enabledFeatureKeys.contains(FEATURE_POLL)
                        && clubPollPermissionService.canCreatePoll(access),
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
                                .filter(event -> event.myParticipationStatus() == null
                                        || PARTICIPATION_CANCELED.equals(event.myParticipationStatus()))
                                .count(),
                        (int) votes.stream()
                                .filter(ScheduleVoteSummaryResponse::votingOpen)
                                .filter(vote -> vote.mySelectedOptionId() == null)
                                .count()
                ),
                items
        );
    }

    public ScheduleEventDetailResponse getScheduleEventDetail(Long clubId, Long eventId, String userKey) {
        requireScheduleFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        ClubScheduleEvent event = getEvent(clubId, eventId);
        return clubScheduleResponseAssembler.toEventDetail(access, event);
    }

    public List<ScheduleEventSummaryResponse> getEventSummariesForHome(
            ClubAccessResolver.ClubAccess access,
            List<ClubScheduleEvent> events
    ) {
        return clubScheduleResponseAssembler.toEventSummaries(access, events);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "일정관리")
    public ScheduleEventUpsertResponse createScheduleEvent(Long clubId, String userKey, UpsertScheduleEventRequest request) {
        requireScheduleFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireEventCreatePermission(access);
        ClubScheduleCommandSupport.EventDraft draft = clubScheduleCommandSupport.toEventDraft(request);
        ClubActivityContextHolder.setDetails(
                "일정 '" + draft.title() + "'을 생성했습니다.",
                "일정 '" + draft.title() + "' 생성에 실패했습니다."
        );
        boolean postToBoard = clubScheduleCommandSupport.shouldPostToBoard(request.postToBoard());
        boolean postToCalendar = clubScheduleCommandSupport.shouldPostToCalendar(request.postToCalendar());
        boolean pinned = clubScheduleCommandSupport.shouldPin(request.pinned());

        ClubScheduleEvent event = clubScheduleEventRepository.save(ClubScheduleEvent.builder()
                .clubId(clubId)
                .authorClubProfileId(access.clubProfile().getClubProfileId())
                .linkedNoticeId(null)
                .sharedToBoard(postToBoard)
                .sharedToCalendar(postToCalendar)
                .categoryKey("GENERAL")
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
                .pinned(pinned)
                .visibilityStatus(VISIBILITY_STATUS)
                .eventStatus(EVENT_STATUS)
                .build());
        syncEventShares(event);

        return clubScheduleResponseAssembler.toEventUpsert(event);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "일정관리")
    public ScheduleEventUpsertResponse updateScheduleEvent(
            Long clubId,
            Long eventId,
            String userKey,
            UpsertScheduleEventRequest request
    ) {
        requireScheduleFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        ClubScheduleEvent current = getEvent(clubId, eventId);
        requireEventEditPermission(access, current.getAuthorClubProfileId());
        ClubScheduleCommandSupport.EventDraft draft = clubScheduleCommandSupport.toEventDraft(request);
        ClubActivityContextHolder.setDetails(
                "일정 '" + current.getTitle() + "'을 수정했습니다.",
                "일정 '" + current.getTitle() + "' 수정에 실패했습니다."
        );
        boolean postToBoard = clubScheduleCommandSupport.shouldPostToBoard(request.postToBoard());
        boolean postToCalendar = clubScheduleCommandSupport.shouldPostToCalendar(request.postToCalendar());
        boolean pinned = clubScheduleCommandSupport.shouldPin(request.pinned());

        ClubScheduleEvent updated = clubScheduleEventRepository.save(ClubScheduleEvent.builder()
                .eventId(current.getEventId())
                .clubId(current.getClubId())
                .authorClubProfileId(current.getAuthorClubProfileId())
                .linkedNoticeId(null)
                .sharedToBoard(postToBoard)
                .sharedToCalendar(postToCalendar)
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
                .pinned(pinned)
                .visibilityStatus(current.getVisibilityStatus())
                .eventStatus(EVENT_STATUS)
                .build());
        syncEventShares(updated);

        return clubScheduleResponseAssembler.toEventUpsert(updated);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "일정관리")
    public void deleteScheduleEvent(Long clubId, Long eventId, String userKey) {
        requireScheduleFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        ClubScheduleEvent current = getEvent(clubId, eventId);
        requireEventDeletePermission(access, current.getAuthorClubProfileId());
        ClubActivityContextHolder.setDetails(
                "일정 '" + current.getTitle() + "'을 삭제했습니다.",
                "일정 '" + current.getTitle() + "' 삭제에 실패했습니다."
        );
        clubEventParticipantRepository.deleteByEventId(current.getEventId());
        clubContentShareService.removeAllShares(clubId, ClubContentShareService.CONTENT_SCHEDULE_EVENT, eventId);
        clubScheduleEventRepository.delete(current);
    }

    public ScheduleVoteDetailResponse getScheduleVoteDetail(Long clubId, Long voteId, String userKey) {
        requirePollFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        ClubScheduleVote vote = getVote(clubId, voteId);
        return clubScheduleResponseAssembler.toVoteDetail(access, vote);
    }

    public List<ScheduleVoteSummaryResponse> getVoteSummariesForHome(
            ClubAccessResolver.ClubAccess access,
            List<ClubScheduleVote> votes
    ) {
        return clubScheduleResponseAssembler.toVoteSummaries(access, votes);
    }

    public boolean isVoteCurrentlyOpen(ClubScheduleVote vote) {
        return clubScheduleResponseAssembler.isVoteOpen(vote);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "일정관리")
    public ScheduleEventDetailResponse updateScheduleEventParticipation(
            Long clubId,
            Long eventId,
            String userKey,
            UpdateScheduleEventParticipationRequest request
    ) {
        requireScheduleFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        ClubScheduleEvent event = getEvent(clubId, eventId);
        if (!event.isParticipationEnabled()) {
            throw new SemoException.ValidationException("이 일정은 참석 응답을 받지 않습니다.");
        }
        String participationStatus = clubScheduleCommandSupport.normalizeParticipationStatus(request);
        ClubActivityContextHolder.setDetails(
                "일정 '" + event.getTitle() + "'에 " + clubScheduleCommandSupport.toParticipationActivityLabel(participationStatus) + ".",
                "일정 참석 상태 변경에 실패했습니다."
        );

        ClubEventParticipant current = clubEventParticipantRepository
                .findForUpdateByEventIdAndClubProfileId(eventId, access.clubProfile().getClubProfileId())
                .orElse(null);
        boolean preserveAttendance = PARTICIPATION_GOING.equals(participationStatus);

        clubEventParticipantRepository.save(ClubEventParticipant.builder()
                .clubEventParticipantId(current == null ? null : current.getClubEventParticipantId())
                .eventId(eventId)
                .clubProfileId(access.clubProfile().getClubProfileId())
                .participationStatus(participationStatus)
                .checkedInAt(preserveAttendance && current != null ? current.getCheckedInAt() : null)
                .attendanceStatus(preserveAttendance && current != null ? current.getAttendanceStatus() : null)
                .verifiedByClubProfileId(preserveAttendance && current != null
                        ? current.getVerifiedByClubProfileId()
                        : null)
                .attendanceNote(preserveAttendance && current != null ? current.getAttendanceNote() : null)
                .build());

        return clubScheduleResponseAssembler.toEventDetail(access, event);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "투표관리")
    public ScheduleVoteUpsertResponse createScheduleVote(Long clubId, String userKey, UpsertScheduleVoteRequest request) {
        requirePollFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireVoteCreatePermission(access);
        ClubScheduleCommandSupport.VoteDraft draft = clubScheduleCommandSupport.toVoteDraft(request);
        ClubActivityContextHolder.setDetails(
                "투표 '" + draft.title() + "'를 생성했습니다.",
                "투표 '" + draft.title() + "' 생성에 실패했습니다."
        );
        boolean postToBoard = clubScheduleCommandSupport.shouldPostToBoard(request.postToBoard());
        boolean postToCalendar = clubScheduleCommandSupport.shouldPostVoteToCalendar(request.postToCalendar(), request.postToSchedule());
        boolean pinned = clubScheduleCommandSupport.shouldPin(request.pinned());
        ClubScheduleVote vote = clubScheduleVoteRepository.save(ClubScheduleVote.builder()
                .clubId(clubId)
                .authorClubProfileId(access.clubProfile().getClubProfileId())
                .linkedNoticeId(null)
                .sharedToBoard(postToBoard)
                .sharedToCalendar(postToCalendar)
                .title(draft.title())
                .voteStartDate(draft.voteStartDate())
                .voteEndDate(draft.voteEndDate())
                .voteStartTime(draft.voteStartTime())
                .voteEndTime(draft.voteEndTime())
                .pinned(pinned)
                .closedAt(null)
                .build());
        syncVoteShares(vote);
        saveVoteOptions(vote.getVoteId(), draft.optionLabels());
        return clubScheduleResponseAssembler.toVoteUpsert(vote, draft.optionLabels().size());
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "투표관리")
    public ScheduleVoteUpsertResponse updateScheduleVote(
            Long clubId,
            Long voteId,
            String userKey,
            UpsertScheduleVoteRequest request
    ) {
        requirePollFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        ClubScheduleVote current = getVote(clubId, voteId);
        requireVoteEditPermission(access, current.getAuthorClubProfileId());
        ClubScheduleCommandSupport.VoteDraft draft = clubScheduleCommandSupport.toVoteDraft(request);
        ClubActivityContextHolder.setDetails(
                "투표 '" + current.getTitle() + "'를 수정했습니다.",
                "투표 '" + current.getTitle() + "' 수정에 실패했습니다."
        );
        boolean postToBoard = clubScheduleCommandSupport.shouldPostToBoard(request.postToBoard());
        boolean postToCalendar = clubScheduleCommandSupport.shouldPostVoteToCalendar(request.postToCalendar(), request.postToSchedule());
        boolean pinned = clubScheduleCommandSupport.shouldPin(request.pinned());
        List<String> currentOptionLabels = clubScheduleVoteOptionRepository
                .findByVoteIdOrderBySortOrderAscVoteOptionIdAsc(voteId)
                .stream()
                .map(ClubScheduleVoteOption::getOptionLabel)
                .toList();
        boolean optionsChanged = !currentOptionLabels.equals(draft.optionLabels());
        if (optionsChanged && clubScheduleVoteSelectionRepository.existsByVoteId(voteId)) {
            throw new SemoException.ValidationException("응답이 시작된 투표의 선택지는 변경할 수 없습니다.");
        }

        ClubScheduleVote updated = clubScheduleVoteRepository.save(ClubScheduleVote.builder()
                .voteId(current.getVoteId())
                .clubId(current.getClubId())
                .authorClubProfileId(current.getAuthorClubProfileId())
                .linkedNoticeId(null)
                .sharedToBoard(postToBoard)
                .sharedToCalendar(postToCalendar)
                .title(draft.title())
                .voteStartDate(draft.voteStartDate())
                .voteEndDate(draft.voteEndDate())
                .voteStartTime(draft.voteStartTime())
                .voteEndTime(draft.voteEndTime())
                .pinned(pinned)
                .closedAt(current.getClosedAt())
                .build());
        syncVoteShares(updated);

        if (optionsChanged) {
            clubScheduleVoteOptionRepository.deleteByVoteId(voteId);
            saveVoteOptions(voteId, draft.optionLabels());
        }

        return clubScheduleResponseAssembler.toVoteUpsert(updated, draft.optionLabels().size());
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "투표관리")
    public void deleteScheduleVote(Long clubId, Long voteId, String userKey) {
        requirePollFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        ClubScheduleVote current = getVote(clubId, voteId);
        requireVoteDeletePermission(access, current.getAuthorClubProfileId());
        ClubActivityContextHolder.setDetails(
                "투표 '" + current.getTitle() + "'를 삭제했습니다.",
                "투표 '" + current.getTitle() + "' 삭제에 실패했습니다."
        );
        clubScheduleVoteSelectionRepository.deleteByVoteId(voteId);
        clubScheduleVoteOptionRepository.deleteByVoteId(voteId);
        clubContentShareService.removeAllShares(clubId, ClubContentShareService.CONTENT_SCHEDULE_VOTE, voteId);
        clubScheduleVoteRepository.delete(current);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "투표관리")
    public ScheduleVoteDetailResponse closeScheduleVote(Long clubId, Long voteId, String userKey) {
        requirePollFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        ClubScheduleVote current = getVote(clubId, voteId);
        requireVoteClosePermission(access, current.getAuthorClubProfileId());
        ClubActivityContextHolder.setDetails(
                "투표 '" + current.getTitle() + "'를 종료했습니다.",
                "투표 종료에 실패했습니다."
        );
        if (current.getClosedAt() != null) {
            return clubScheduleResponseAssembler.toVoteDetail(access, current);
        }

        LocalDateTime closedAt = LocalDateTime.now();
        ClubScheduleVote closedVote = clubScheduleVoteRepository.save(ClubScheduleVote.builder()
                .voteId(current.getVoteId())
                .clubId(current.getClubId())
                .authorClubProfileId(current.getAuthorClubProfileId())
                .linkedNoticeId(null)
                .sharedToBoard(current.isSharedToBoard())
                .sharedToCalendar(current.isSharedToCalendar())
                .title(current.getTitle())
                .voteStartDate(current.getVoteStartDate())
                .voteEndDate(current.getVoteEndDate())
                .voteStartTime(current.getVoteStartTime())
                .voteEndTime(current.getVoteEndTime())
                .pinned(current.isPinned())
                .closedAt(closedAt)
                .build());
        syncVoteShares(closedVote);
        return clubScheduleResponseAssembler.toVoteDetail(access, closedVote);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "투표관리")
    public ScheduleVoteDetailResponse submitScheduleVoteSelection(
            Long clubId,
            Long voteId,
            String userKey,
            SubmitScheduleVoteSelectionRequest request
    ) {
        requirePollFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        ClubScheduleVote vote = getVote(clubId, voteId);
        if (!clubScheduleResponseAssembler.isVoteOpen(vote)) {
            throw new SemoException.ValidationException("현재 투표 기간이 아닙니다.");
        }
        List<ClubScheduleVoteOption> options = clubScheduleVoteOptionRepository.findByVoteIdOrderBySortOrderAscVoteOptionIdAsc(voteId);
        ClubScheduleVoteOption selectedOption = options.stream()
                .filter(option -> option.getVoteOptionId().equals(request.voteOptionId()))
                .findFirst()
                .orElseThrow(() -> new SemoException.ValidationException("해당 투표의 항목이 아닙니다."));
        ClubActivityContextHolder.setDetails(
                "투표 '" + vote.getTitle() + "'에서 '" + selectedOption.getOptionLabel() + "' 항목을 선택했습니다.",
                "투표 선택 제출에 실패했습니다."
        );

        ClubScheduleVoteSelection current = clubScheduleVoteSelectionRepository
                .findByVoteIdAndClubProfileId(voteId, access.clubProfile().getClubProfileId())
                .orElse(null);
        clubScheduleVoteSelectionRepository.save(ClubScheduleVoteSelection.builder()
                .voteSelectionId(current == null ? null : current.getVoteSelectionId())
                .voteId(voteId)
                .voteOptionId(selectedOption.getVoteOptionId())
                .clubProfileId(access.clubProfile().getClubProfileId())
                .build());

        return clubScheduleResponseAssembler.toVoteDetail(access, vote);
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

    private ClubScheduleEvent getEvent(Long clubId, Long eventId) {
        return clubScheduleEventRepository.findByEventIdAndClubId(eventId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("ScheduleEvent", "eventId", eventId));
    }

    private ClubScheduleVote getVote(Long clubId, Long voteId) {
        return clubScheduleVoteRepository.findByVoteIdAndClubId(voteId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("ScheduleVote", "voteId", voteId));
    }

    private void requireScheduleFeature(Long clubId) {
        clubFeatureService.requireFeatureEnabled(clubId, FEATURE_SCHEDULE_MANAGE, "일정");
    }

    private void requirePollFeature(Long clubId) {
        clubFeatureService.requireFeatureEnabled(clubId, FEATURE_POLL, "투표");
    }

    private boolean isCalendarContentEnabled(Set<String> enabledFeatureKeys, String contentType) {
        String featureKey = switch (contentType) {
            case "NOTICE" -> FEATURE_NOTICE;
            case "SCHEDULE_EVENT" -> FEATURE_SCHEDULE_MANAGE;
            case "SCHEDULE_VOTE" -> FEATURE_POLL;
            case "TOURNAMENT" -> FEATURE_TOURNAMENT_RECORD;
            default -> null;
        };
        return featureKey != null && enabledFeatureKeys.contains(featureKey);
    }

    private void requireEventCreatePermission(ClubAccessResolver.ClubAccess access) {
        if (!clubSchedulePermissionService.canCreateSchedule(access)) {
            throw new SemoException.ForbiddenException("일정 작성 권한이 없습니다.");
        }
    }

    private void requireEventEditPermission(ClubAccessResolver.ClubAccess access, Long authorClubProfileId) {
        if (!clubSchedulePermissionService.getActionPermission(access, authorClubProfileId).canEdit()) {
            throw new SemoException.ForbiddenException("일정 수정 권한이 없습니다.");
        }
    }

    private void requireEventDeletePermission(ClubAccessResolver.ClubAccess access, Long authorClubProfileId) {
        if (!clubSchedulePermissionService.getActionPermission(access, authorClubProfileId).canDelete()) {
            throw new SemoException.ForbiddenException("일정 삭제 권한이 없습니다.");
        }
    }

    private void requireVoteCreatePermission(ClubAccessResolver.ClubAccess access) {
        if (!clubPollPermissionService.canCreatePoll(access)) {
            throw new SemoException.ForbiddenException("투표 작성 권한이 없습니다.");
        }
    }

    private void requireVoteEditPermission(ClubAccessResolver.ClubAccess access, Long authorClubProfileId) {
        if (!clubPollPermissionService.getActionPermission(access, authorClubProfileId).canEdit()) {
            throw new SemoException.ForbiddenException("투표 수정 권한이 없습니다.");
        }
    }

    private void requireVoteDeletePermission(ClubAccessResolver.ClubAccess access, Long authorClubProfileId) {
        if (!clubPollPermissionService.getActionPermission(access, authorClubProfileId).canDelete()) {
            throw new SemoException.ForbiddenException("투표 삭제 권한이 없습니다.");
        }
    }

    private void requireVoteClosePermission(ClubAccessResolver.ClubAccess access, Long authorClubProfileId) {
        if (!clubPollPermissionService.getActionPermission(access, authorClubProfileId).canEdit()) {
            throw new SemoException.ForbiddenException("투표 종료 권한이 없습니다.");
        }
    }

    private void syncEventShares(ClubScheduleEvent event) {
        boolean activeEvent = !"CANCELLED".equals(event.getEventStatus());
        clubContentShareService.syncBoardShare(
                event.getClubId(),
                ClubContentShareService.CONTENT_SCHEDULE_EVENT,
                event.getEventId(),
                event.isSharedToBoard() && activeEvent
        );
        clubContentShareService.syncCalendarShare(
                event.getClubId(),
                ClubContentShareService.CONTENT_SCHEDULE_EVENT,
                event.getEventId(),
                event.isSharedToCalendar() && activeEvent
        );
    }

    private void syncVoteShares(ClubScheduleVote vote) {
        clubContentShareService.syncBoardShare(
                vote.getClubId(),
                ClubContentShareService.CONTENT_SCHEDULE_VOTE,
                vote.getVoteId(),
                vote.isSharedToBoard()
        );
        clubContentShareService.syncCalendarShare(
                vote.getClubId(),
                ClubContentShareService.CONTENT_SCHEDULE_VOTE,
                vote.getVoteId(),
                vote.isSharedToCalendar()
        );
    }

}
