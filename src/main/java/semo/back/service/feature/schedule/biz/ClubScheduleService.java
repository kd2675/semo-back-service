package semo.back.service.feature.schedule.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubCalendarItem;
import semo.back.service.database.pub.entity.ClubEventParticipant;
import semo.back.service.database.pub.entity.ClubProfile;
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
import semo.back.service.feature.poll.biz.ClubPollPermissionService;
import semo.back.service.feature.schedule.biz.policy.ClubSchedulePermissionService;
import semo.back.service.feature.schedule.biz.support.ClubScheduleCalendarLoader;
import semo.back.service.feature.schedule.biz.support.ClubScheduleCommandSupport;
import semo.back.service.feature.schedule.biz.support.ClubScheduleViewSupport;
import semo.back.service.feature.schedule.vo.ClubCalendarFeedItemResponse;
import semo.back.service.feature.share.biz.ClubContentShareService;
import semo.back.service.feature.schedule.vo.ClubScheduleResponse;
import semo.back.service.feature.schedule.vo.ScheduleEventDetailResponse;
import semo.back.service.feature.schedule.vo.ScheduleEventParticipantSummaryResponse;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubScheduleService {
    private static final String VISIBILITY_STATUS = "CLUB";
    private static final String EVENT_STATUS = "SCHEDULED";
    private static final String PARTICIPATION_GOING = "GOING";
    private static final String PARTICIPATION_NOT_GOING = "NOT_GOING";
    private static final String PARTICIPATION_CANCEL = "CANCEL";

    private final ClubScheduleEventRepository clubScheduleEventRepository;
    private final ClubEventParticipantRepository clubEventParticipantRepository;
    private final ClubScheduleVoteRepository clubScheduleVoteRepository;
    private final ClubScheduleVoteOptionRepository clubScheduleVoteOptionRepository;
    private final ClubScheduleVoteSelectionRepository clubScheduleVoteSelectionRepository;
    private final ClubCalendarItemRepository clubCalendarItemRepository;
    private final ClubAccessResolver clubAccessResolver;
    private final ClubSchedulePermissionService clubSchedulePermissionService;
    private final ClubPollPermissionService clubPollPermissionService;
    private final ClubContentShareService clubContentShareService;
    private final ClubScheduleCalendarLoader clubScheduleCalendarLoader;
    private final ClubScheduleCommandSupport clubScheduleCommandSupport;
    private final ClubScheduleViewSupport clubScheduleViewSupport;

    public ClubScheduleResponse getClubSchedule(Long clubId, String userKey, Integer year, Integer month) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        Long viewerClubProfileId = access.clubProfile().getClubProfileId();
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
        );

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
                items
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
        Map<Long, ClubProfile> authorProfileById = clubScheduleViewSupport.loadAuthorProfiles(
                events.stream().map(ClubScheduleEvent::getAuthorClubProfileId).distinct().toList()
        );

        return events.stream()
                .map(event -> toEventSummaryResponse(
                        access,
                        event,
                        participantsByEventId.getOrDefault(event.getEventId(), List.of()),
                        access.clubProfile().getClubProfileId(),
                        authorProfileById.get(event.getAuthorClubProfileId())
                ))
                .toList();
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "일정관리")
    public ScheduleEventUpsertResponse createScheduleEvent(Long clubId, String userKey, UpsertScheduleEventRequest request) {
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

        return toEventUpsertResponse(event);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "일정관리")
    public ScheduleEventUpsertResponse updateScheduleEvent(
            Long clubId,
            Long eventId,
            String userKey,
            UpsertScheduleEventRequest request
    ) {
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

        return toEventUpsertResponse(updated);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "일정관리")
    public void deleteScheduleEvent(Long clubId, Long eventId, String userKey) {
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
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        ClubScheduleVote vote = getVote(clubId, voteId);
        return buildVoteDetailResponse(access, vote);
    }

    public List<ScheduleVoteSummaryResponse> getVoteSummariesForHome(
            ClubAccessResolver.ClubAccess access,
            List<ClubScheduleVote> votes
    ) {
        return toVoteSummaryResponses(access, votes);
    }

    public boolean isVoteCurrentlyOpen(ClubScheduleVote vote) {
        return isVoteOpen(vote);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "일정관리")
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
        String participationStatus = clubScheduleCommandSupport.normalizeParticipationStatus(request);
        ClubActivityContextHolder.setDetails(
                "일정 '" + event.getTitle() + "'에 " + clubScheduleCommandSupport.toParticipationActivityLabel(participationStatus) + ".",
                "일정 참석 상태 변경에 실패했습니다."
        );

        ClubEventParticipant current = clubEventParticipantRepository
                .findByEventIdAndClubProfileId(eventId, access.clubProfile().getClubProfileId())
                .orElse(null);
        if (PARTICIPATION_CANCEL.equals(participationStatus)) {
            if (current != null) {
                clubEventParticipantRepository.delete(current);
            }
            return buildEventDetailResponse(access, event);
        }

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
    @RecordClubActivity(subject = "투표관리")
    public ScheduleVoteUpsertResponse createScheduleVote(Long clubId, String userKey, UpsertScheduleVoteRequest request) {
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
        return toVoteUpsertResponse(vote, draft.optionLabels().size());
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "투표관리")
    public ScheduleVoteUpsertResponse updateScheduleVote(
            Long clubId,
            Long voteId,
            String userKey,
            UpsertScheduleVoteRequest request
    ) {
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

        clubScheduleVoteSelectionRepository.deleteByVoteId(voteId);
        clubScheduleVoteOptionRepository.deleteByVoteId(voteId);
        saveVoteOptions(voteId, draft.optionLabels());

        return toVoteUpsertResponse(updated, draft.optionLabels().size());
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "투표관리")
    public void deleteScheduleVote(Long clubId, Long voteId, String userKey) {
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
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        ClubScheduleVote current = getVote(clubId, voteId);
        requireVoteClosePermission(access, current.getAuthorClubProfileId());
        ClubActivityContextHolder.setDetails(
                "투표 '" + current.getTitle() + "'를 종료했습니다.",
                "투표 종료에 실패했습니다."
        );
        if (current.getClosedAt() != null) {
            return buildVoteDetailResponse(access, current);
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
        return buildVoteDetailResponse(access, closedVote);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "투표관리")
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

        return buildVoteDetailResponse(access, vote);
    }

    private List<ScheduleVoteSummaryResponse> toVoteSummaryResponses(
            ClubAccessResolver.ClubAccess access,
            List<ClubScheduleVote> votes
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
        Map<Long, ClubProfile> authorProfileById = clubScheduleViewSupport.loadAuthorProfiles(
                votes.stream().map(ClubScheduleVote::getAuthorClubProfileId).distinct().toList()
        );

        return votes.stream()
                .map(vote -> {
                    VoteSelectionSnapshot selection = toVoteSelectionSnapshot(
                            optionsByVoteId.getOrDefault(vote.getVoteId(), List.of()),
                            selectionsByVoteId.getOrDefault(vote.getVoteId(), List.of()),
                            access.clubProfile().getClubProfileId()
                    );
                    ClubPollPermissionService.PollActionPermission actionPermission =
                            clubPollPermissionService.getActionPermission(access, vote.getAuthorClubProfileId());
                    return new ScheduleVoteSummaryResponse(
                            vote.getVoteId(),
                            vote.getTitle(),
                            clubScheduleViewSupport.resolveAuthorDisplayName(authorProfileById.get(vote.getAuthorClubProfileId())),
                            clubScheduleViewSupport.resolveAuthorAvatarImageUrl(authorProfileById.get(vote.getAuthorClubProfileId())),
                            clubScheduleViewSupport.resolveAuthorAvatarThumbnailUrl(authorProfileById.get(vote.getAuthorClubProfileId())),
                            resolveVoteStatus(vote),
                            clubScheduleViewSupport.formatDateValue(vote.getVoteStartDate()),
                            clubScheduleViewSupport.formatDateValue(vote.getVoteEndDate()),
                            clubScheduleViewSupport.formatDateRangeLabel(vote.getVoteStartDate(), vote.getVoteEndDate()),
                            clubScheduleViewSupport.formatVoteTimeLabel(vote.getVoteStartTime(), vote.getVoteEndTime()),
                            selection.options().size(),
                            selection.totalResponses(),
                            vote.isSharedToBoard(),
                            vote.isSharedToCalendar(),
                            vote.isSharedToCalendar(),
                            vote.isPinned(),
                            null,
                            selection.mySelectedOptionId(),
                            selection.options(),
                            isVoteOpen(vote),
                            actionPermission.canEdit(),
                            actionPermission.canDelete()
                    );
                })
                .toList();
    }

    private ScheduleEventDetailResponse buildEventDetailResponse(
            ClubAccessResolver.ClubAccess access,
            ClubScheduleEvent event
    ) {
        ClubSchedulePermissionService.ScheduleEventActionPermission actionPermission =
                clubSchedulePermissionService.getActionPermission(access, event.getAuthorClubProfileId());
        List<ClubEventParticipant> participants = clubEventParticipantRepository.findByEventIdIn(List.of(event.getEventId()));
        EventParticipationSnapshot participation = toParticipationSnapshot(
                participants,
                access.clubProfile().getClubProfileId()
        );
        List<ScheduleEventParticipantSummaryResponse> goingParticipants = toGoingParticipantSummaries(participants);

        return new ScheduleEventDetailResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                event.getEventId(),
                event.getTitle(),
                clubScheduleViewSupport.formatDateValue(event.getStartAt().toLocalDate()),
                event.getEndAt() == null ? null : clubScheduleViewSupport.formatDateValue(event.getEndAt().toLocalDate()),
                clubScheduleViewSupport.formatDateRangeLabel(
                        event.getStartAt().toLocalDate(),
                        event.getEndAt() == null ? null : event.getEndAt().toLocalDate()
                ),
                clubScheduleViewSupport.formatTimeValue(event.getStartAt(), event.getEndAt()),
                clubScheduleViewSupport.formatEndTimeValue(event.getStartAt(), event.getEndAt()),
                clubScheduleViewSupport.formatTimeLabel(event.getStartAt(), event.getEndAt()),
                event.getAttendeeLimit(),
                event.getLocationLabel(),
                event.getParticipationConditionText(),
                event.isParticipationEnabled(),
                event.isFeeRequired(),
                event.getFeeAmount(),
                event.isFeeAmountUndecided(),
                event.isFeeNWaySplit(),
                event.isSharedToBoard(),
                event.isSharedToCalendar(),
                event.isPinned(),
                null,
                participation.myParticipationStatus(),
                participation.goingCount(),
                participation.notGoingCount(),
                goingParticipants,
                actionPermission.canEdit(),
                actionPermission.canDelete()
        );
    }

    private ScheduleVoteDetailResponse buildVoteDetailResponse(
            ClubAccessResolver.ClubAccess access,
            ClubScheduleVote vote
    ) {
        List<ClubScheduleVoteOption> options = clubScheduleVoteOptionRepository.findByVoteIdOrderBySortOrderAscVoteOptionIdAsc(vote.getVoteId());
        ClubPollPermissionService.PollActionPermission actionPermission =
                clubPollPermissionService.getActionPermission(access, vote.getAuthorClubProfileId());
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
                clubScheduleViewSupport.formatDateValue(vote.getVoteStartDate()),
                clubScheduleViewSupport.formatDateValue(vote.getVoteEndDate()),
                clubScheduleViewSupport.formatDateRangeLabel(vote.getVoteStartDate(), vote.getVoteEndDate()),
                clubScheduleViewSupport.formatOptionalTimeValue(vote.getVoteStartTime()),
                clubScheduleViewSupport.formatOptionalTimeValue(vote.getVoteEndTime()),
                clubScheduleViewSupport.formatVoteTimeLabel(vote.getVoteStartTime(), vote.getVoteEndTime()),
                vote.isSharedToBoard(),
                vote.isSharedToCalendar(),
                vote.isSharedToCalendar(),
                vote.isPinned(),
                null,
                selection.mySelectedOptionId(),
                selection.totalResponses(),
                selection.options(),
                actionPermission.canEdit(),
                actionPermission.canDelete(),
                isVoteOpen(vote)
        );
    }

    private ScheduleEventSummaryResponse toEventSummaryResponse(
            ClubAccessResolver.ClubAccess access,
            ClubScheduleEvent event,
            List<ClubEventParticipant> participants,
            Long viewerClubProfileId,
            ClubProfile authorProfile
    ) {
        ClubSchedulePermissionService.ScheduleEventActionPermission actionPermission =
                clubSchedulePermissionService.getActionPermission(access, event.getAuthorClubProfileId());
        EventParticipationSnapshot participation = toParticipationSnapshot(participants, viewerClubProfileId);
        return new ScheduleEventSummaryResponse(
                event.getEventId(),
                event.getTitle(),
                clubScheduleViewSupport.resolveAuthorDisplayName(authorProfile),
                clubScheduleViewSupport.resolveAuthorAvatarImageUrl(authorProfile),
                clubScheduleViewSupport.resolveAuthorAvatarThumbnailUrl(authorProfile),
                clubScheduleViewSupport.formatDateValue(event.getStartAt().toLocalDate()),
                event.getEndAt() == null ? null : clubScheduleViewSupport.formatDateValue(event.getEndAt().toLocalDate()),
                clubScheduleViewSupport.formatDateRangeLabel(
                        event.getStartAt().toLocalDate(),
                        event.getEndAt() == null ? null : event.getEndAt().toLocalDate()
                ),
                clubScheduleViewSupport.formatTimeLabel(event.getStartAt(), event.getEndAt()),
                event.getAttendeeLimit(),
                event.getLocationLabel(),
                event.getParticipationConditionText(),
                event.isParticipationEnabled(),
                event.isFeeRequired(),
                event.getFeeAmount(),
                event.isFeeAmountUndecided(),
                event.isFeeNWaySplit(),
                event.isSharedToBoard(),
                event.isSharedToCalendar(),
                event.isPinned(),
                null,
                participation.myParticipationStatus(),
                participation.goingCount(),
                participation.notGoingCount(),
                actionPermission.canEdit(),
                actionPermission.canDelete()
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

    private List<ScheduleEventParticipantSummaryResponse> toGoingParticipantSummaries(
            List<ClubEventParticipant> participants
    ) {
        List<ClubEventParticipant> goingParticipants = participants.stream()
                .filter(participant -> PARTICIPATION_GOING.equals(participant.getParticipationStatus()))
                .sorted(Comparator.comparing(ClubEventParticipant::getClubEventParticipantId))
                .toList();
        if (goingParticipants.isEmpty()) {
            return List.of();
        }

        Map<Long, ClubProfile> profileById = clubScheduleViewSupport.loadAuthorProfiles(
                goingParticipants.stream()
                        .map(ClubEventParticipant::getClubProfileId)
                        .distinct()
                        .toList()
        );

        return goingParticipants.stream()
                .map(participant -> {
                    ClubProfile profile = profileById.get(participant.getClubProfileId());
                    return new ScheduleEventParticipantSummaryResponse(
                            participant.getClubProfileId(),
                            clubScheduleViewSupport.resolveAuthorDisplayName(profile),
                            clubScheduleViewSupport.resolveAuthorAvatarImageUrl(profile),
                            clubScheduleViewSupport.resolveAuthorAvatarThumbnailUrl(profile)
                    );
                })
                .toList();
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
                null,
                event.getTitle(),
                clubScheduleViewSupport.formatDateValue(event.getStartAt().toLocalDate()),
                event.getEndAt() == null ? null : clubScheduleViewSupport.formatDateValue(event.getEndAt().toLocalDate()),
                clubScheduleViewSupport.formatDateRangeLabel(
                        event.getStartAt().toLocalDate(),
                        event.getEndAt() == null ? null : event.getEndAt().toLocalDate()
                ),
                clubScheduleViewSupport.formatTimeLabel(event.getStartAt(), event.getEndAt()),
                event.isSharedToBoard(),
                event.isSharedToCalendar(),
                event.isPinned()
        );
    }

    private ScheduleVoteUpsertResponse toVoteUpsertResponse(ClubScheduleVote vote, int optionCount) {
        return new ScheduleVoteUpsertResponse(
                vote.getVoteId(),
                null,
                vote.getTitle(),
                clubScheduleViewSupport.formatDateValue(vote.getVoteStartDate()),
                clubScheduleViewSupport.formatDateValue(vote.getVoteEndDate()),
                clubScheduleViewSupport.formatDateRangeLabel(vote.getVoteStartDate(), vote.getVoteEndDate()),
                clubScheduleViewSupport.formatOptionalTimeValue(vote.getVoteStartTime()),
                clubScheduleViewSupport.formatOptionalTimeValue(vote.getVoteEndTime()),
                clubScheduleViewSupport.formatVoteTimeLabel(vote.getVoteStartTime(), vote.getVoteEndTime()),
                optionCount,
                vote.isSharedToBoard(),
                vote.isSharedToCalendar(),
                vote.isSharedToCalendar(),
                vote.isPinned()
        );
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

    private boolean isVoteOpen(ClubScheduleVote vote) {
        return "ONGOING".equals(resolveVoteStatus(vote));
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

    private String resolveVoteStatus(ClubScheduleVote vote) {
        if (vote.getClosedAt() != null) {
            return "CLOSED";
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startAt = clubScheduleCommandSupport.toVoteStartAt(
                vote.getVoteStartDate(),
                vote.getVoteStartTime()
        );
        if (now.isBefore(startAt)) {
            return "WAITING";
        }
        if (now.isAfter(clubScheduleCommandSupport.toVoteEffectiveEndAt(
                vote.getVoteEndDate(),
                vote.getVoteEndTime()
        ))) {
            return "CLOSED";
        }
        return "ONGOING";
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
