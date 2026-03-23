package semo.back.service.feature.schedule.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.common.util.ImageFileUrlResolver;
import semo.back.service.database.pub.entity.ClubCalendarItem;
import semo.back.service.database.pub.entity.ClubEventParticipant;
import semo.back.service.database.pub.entity.ClubNotice;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.ClubScheduleEvent;
import semo.back.service.database.pub.entity.ClubScheduleVote;
import semo.back.service.database.pub.entity.ClubScheduleVoteOption;
import semo.back.service.database.pub.entity.ClubScheduleVoteSelection;
import semo.back.service.database.pub.repository.ClubCalendarItemRepository;
import semo.back.service.database.pub.repository.ClubEventParticipantRepository;
import semo.back.service.database.pub.repository.ClubNoticeRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteOptionRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteSelectionRepository;
import semo.back.service.feature.activity.biz.ClubActivityContextHolder;
import semo.back.service.feature.activity.biz.RecordClubActivity;
import semo.back.service.feature.club.biz.ClubAccessResolver;
import semo.back.service.feature.notice.biz.ClubNoticeService;
import semo.back.service.feature.poll.biz.ClubPollPermissionService;
import semo.back.service.feature.schedule.vo.ClubCalendarFeedItemResponse;
import semo.back.service.feature.notice.vo.ClubNoticeSummaryResponse;
import semo.back.service.feature.share.biz.ClubContentShareService;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubScheduleService {
    private static final String VISIBILITY_STATUS = "CLUB";
    private static final String EVENT_STATUS = "SCHEDULED";
    private static final String PARTICIPATION_GOING = "GOING";
    private static final String PARTICIPATION_NOT_GOING = "NOT_GOING";
    private static final String CONTENT_NOTICE = ClubContentShareService.CONTENT_NOTICE;
    private static final String CONTENT_SCHEDULE_EVENT = ClubContentShareService.CONTENT_SCHEDULE_EVENT;
    private static final String CONTENT_SCHEDULE_VOTE = ClubContentShareService.CONTENT_SCHEDULE_VOTE;
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
    private final ClubCalendarItemRepository clubCalendarItemRepository;
    private final ClubNoticeRepository clubNoticeRepository;
    private final ClubProfileRepository clubProfileRepository;
    private final ClubAccessResolver clubAccessResolver;
    private final ClubSchedulePermissionService clubSchedulePermissionService;
    private final ClubPollPermissionService clubPollPermissionService;
    private final ClubNoticeService clubNoticeService;
    private final ClubContentShareService clubContentShareService;
    private final ImageFileUrlResolver imageFileUrlResolver;

    public ClubScheduleResponse getClubSchedule(Long clubId, String userKey, Integer year, Integer month) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        Long viewerClubProfileId = access.clubProfile().getClubProfileId();
        LocalDate today = LocalDate.now();
        LocalDate monthStartDate = resolveMonthStart(year, month);
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
        Map<Long, ClubNoticeSummaryResponse> noticeById = loadCalendarNoticeSummaries(access, calendarItems);
        Map<Long, ScheduleEventSummaryResponse> eventById = loadCalendarEventSummaries(access, calendarItems);
        Map<Long, ScheduleVoteSummaryResponse> voteById = loadCalendarVoteSummaries(access, calendarItems);

        List<ClubCalendarFeedItemResponse> items = calendarItems.stream()
                .map(item -> toCalendarFeedItemResponse(item, noticeById, eventById, voteById))
                .filter(Objects::nonNull)
                .toList();

        List<ScheduleEventSummaryResponse> monthEvents = items.stream()
                .map(ClubCalendarFeedItemResponse::event)
                .filter(Objects::nonNull)
                .toList();
        List<ScheduleEventSummaryResponse> upcomingEvents = monthEvents.stream()
                .filter(event -> !LocalDate.parse(event.startDate(), DATE_REQUEST_FORMATTER).isBefore(today))
                .toList();
        List<ScheduleEventSummaryResponse> recentEvents = monthEvents.stream()
                .filter(event -> LocalDate.parse(event.startDate(), DATE_REQUEST_FORMATTER).isBefore(today))
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
        Map<Long, ClubProfile> authorProfileById = loadAuthorProfiles(
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
        EventDraft draft = toEventDraft(request);
        ClubActivityContextHolder.setDetails(
                "일정 '" + draft.title() + "'을 생성했습니다.",
                "일정 '" + draft.title() + "' 생성에 실패했습니다."
        );
        boolean postToBoard = shouldPostToBoard(request.postToBoard());
        boolean postToCalendar = shouldPostToCalendar(request.postToCalendar());

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
        EventDraft draft = toEventDraft(request);
        ClubActivityContextHolder.setDetails(
                "일정 '" + current.getTitle() + "'을 수정했습니다.",
                "일정 '" + current.getTitle() + "' 수정에 실패했습니다."
        );
        boolean postToBoard = shouldPostToBoard(request.postToBoard());
        boolean postToCalendar = shouldPostToCalendar(request.postToCalendar());

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
        String participationStatus = normalizeParticipationStatus(request);
        ClubActivityContextHolder.setDetails(
                "일정 '" + event.getTitle() + "'에 " + toParticipationActivityLabel(participationStatus) + ".",
                "일정 참석 상태 변경에 실패했습니다."
        );

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
    @RecordClubActivity(subject = "투표관리")
    public ScheduleVoteUpsertResponse createScheduleVote(Long clubId, String userKey, UpsertScheduleVoteRequest request) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireVoteCreatePermission(access);
        VoteDraft draft = toVoteDraft(request);
        ClubActivityContextHolder.setDetails(
                "투표 '" + draft.title() + "'를 생성했습니다.",
                "투표 '" + draft.title() + "' 생성에 실패했습니다."
        );
        boolean postToBoard = shouldPostToBoard(request.postToBoard());
        boolean postToCalendar = shouldPostVoteToCalendar(request.postToCalendar(), request.postToSchedule());

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
        VoteDraft draft = toVoteDraft(request);
        ClubActivityContextHolder.setDetails(
                "투표 '" + current.getTitle() + "'를 수정했습니다.",
                "투표 '" + current.getTitle() + "' 수정에 실패했습니다."
        );
        boolean postToBoard = shouldPostToBoard(request.postToBoard());
        boolean postToCalendar = shouldPostVoteToCalendar(request.postToCalendar(), request.postToSchedule());

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
        Map<Long, ClubProfile> authorProfileById = loadAuthorProfiles(
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
                            resolveAuthorDisplayName(authorProfileById.get(vote.getAuthorClubProfileId())),
                            resolveAuthorAvatarImageUrl(authorProfileById.get(vote.getAuthorClubProfileId())),
                            resolveAuthorAvatarThumbnailUrl(authorProfileById.get(vote.getAuthorClubProfileId())),
                            resolveVoteStatus(vote),
                            formatDateValue(vote.getVoteStartDate()),
                            formatDateValue(vote.getVoteEndDate()),
                            formatDateRangeLabel(vote.getVoteStartDate(), vote.getVoteEndDate()),
                            formatVoteTimeLabel(vote.getVoteStartTime(), vote.getVoteEndTime()),
                            selection.options().size(),
                            selection.totalResponses(),
                            vote.isSharedToBoard(),
                            vote.isSharedToCalendar(),
                            vote.isSharedToCalendar(),
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
                event.isSharedToBoard(),
                event.isSharedToCalendar(),
                null,
                participation.myParticipationStatus(),
                participation.goingCount(),
                participation.notGoingCount(),
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
                formatDateValue(vote.getVoteStartDate()),
                formatDateValue(vote.getVoteEndDate()),
                formatDateRangeLabel(vote.getVoteStartDate(), vote.getVoteEndDate()),
                formatOptionalTimeValue(vote.getVoteStartTime()),
                formatOptionalTimeValue(vote.getVoteEndTime()),
                formatVoteTimeLabel(vote.getVoteStartTime(), vote.getVoteEndTime()),
                vote.isSharedToBoard(),
                vote.isSharedToCalendar(),
                vote.isSharedToCalendar(),
                null,
                selection.mySelectedOptionId(),
                selection.totalResponses(),
                selection.options(),
                actionPermission.canEdit(),
                actionPermission.canDelete(),
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

    private String toParticipationActivityLabel(String participationStatus) {
        return switch (participationStatus) {
            case PARTICIPATION_GOING -> "참석으로 응답했습니다";
            case PARTICIPATION_NOT_GOING -> "불참으로 응답했습니다";
            default -> "응답했습니다";
        };
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
                resolveAuthorDisplayName(authorProfile),
                resolveAuthorAvatarImageUrl(authorProfile),
                resolveAuthorAvatarThumbnailUrl(authorProfile),
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
                event.isSharedToBoard(),
                event.isSharedToCalendar(),
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
                formatDateValue(event.getStartAt().toLocalDate()),
                event.getEndAt() == null ? null : formatDateValue(event.getEndAt().toLocalDate()),
                formatDateRangeLabel(event.getStartAt().toLocalDate(), event.getEndAt() == null ? null : event.getEndAt().toLocalDate()),
                formatTimeLabel(event.getStartAt(), event.getEndAt()),
                event.isSharedToBoard(),
                event.isSharedToCalendar()
        );
    }

    private ScheduleVoteUpsertResponse toVoteUpsertResponse(ClubScheduleVote vote, int optionCount) {
        return new ScheduleVoteUpsertResponse(
                vote.getVoteId(),
                null,
                vote.getTitle(),
                formatDateValue(vote.getVoteStartDate()),
                formatDateValue(vote.getVoteEndDate()),
                formatDateRangeLabel(vote.getVoteStartDate(), vote.getVoteEndDate()),
                formatOptionalTimeValue(vote.getVoteStartTime()),
                formatOptionalTimeValue(vote.getVoteEndTime()),
                formatVoteTimeLabel(vote.getVoteStartTime(), vote.getVoteEndTime()),
                optionCount,
                vote.isSharedToBoard(),
                vote.isSharedToCalendar(),
                vote.isSharedToCalendar()
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

    private boolean shouldPostToCalendar(Boolean postToCalendar) {
        return postToCalendar == null || postToCalendar;
    }

    private boolean shouldPostVoteToCalendar(Boolean postToCalendar, Boolean postToSchedule) {
        if (postToCalendar != null) {
            return postToCalendar;
        }
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

    private boolean isVoteOpen(ClubScheduleVote vote) {
        return "ONGOING".equals(resolveVoteStatus(vote));
    }

    private Map<Long, ClubProfile> loadAuthorProfiles(List<Long> clubProfileIds) {
        if (clubProfileIds.isEmpty()) {
            return Map.of();
        }
        return clubProfileRepository.findAllById(clubProfileIds).stream()
                .collect(Collectors.toMap(ClubProfile::getClubProfileId, Function.identity()));
    }

    private String resolveAuthorDisplayName(ClubProfile authorProfile) {
        return authorProfile == null ? "Unknown Member" : authorProfile.getDisplayName();
    }

    private String resolveAuthorAvatarImageUrl(ClubProfile authorProfile) {
        return authorProfile == null ? null : imageFileUrlResolver.resolveImageUrl(authorProfile.getAvatarFileName());
    }

    private String resolveAuthorAvatarThumbnailUrl(ClubProfile authorProfile) {
        return authorProfile == null ? null : imageFileUrlResolver.resolveThumbnailUrl(authorProfile.getAvatarFileName());
    }

    private Map<Long, ClubNoticeSummaryResponse> loadCalendarNoticeSummaries(
            ClubAccessResolver.ClubAccess access,
            List<ClubCalendarItem> calendarItems
    ) {
        List<Long> noticeIds = calendarItems.stream()
                .filter(item -> CONTENT_NOTICE.equals(item.getContentType()))
                .map(ClubCalendarItem::getContentId)
                .toList();
        if (noticeIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, ClubNotice> noticeById = clubNoticeRepository.findAllByNoticeIdIn(noticeIds).stream()
                .filter(notice -> !notice.isDeleted())
                .collect(Collectors.toMap(ClubNotice::getNoticeId, Function.identity()));
        List<ClubNotice> noticesInOrder = noticeIds.stream()
                .map(noticeById::get)
                .filter(Objects::nonNull)
                .toList();

        return clubNoticeService.toNoticeSummaries(access, noticesInOrder).stream()
                .collect(Collectors.toMap(ClubNoticeSummaryResponse::noticeId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, ScheduleEventSummaryResponse> loadCalendarEventSummaries(
            ClubAccessResolver.ClubAccess access,
            List<ClubCalendarItem> calendarItems
    ) {
        List<Long> eventIds = calendarItems.stream()
                .filter(item -> CONTENT_SCHEDULE_EVENT.equals(item.getContentType()))
                .map(ClubCalendarItem::getContentId)
                .toList();
        if (eventIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, ClubScheduleEvent> eventById = clubScheduleEventRepository.findAllByEventIdIn(eventIds).stream()
                .filter(event -> !"CANCELLED".equals(event.getEventStatus()))
                .collect(Collectors.toMap(ClubScheduleEvent::getEventId, Function.identity()));
        List<ClubScheduleEvent> eventsInOrder = eventIds.stream()
                .map(eventById::get)
                .filter(Objects::nonNull)
                .toList();

        return getEventSummariesForHome(access, eventsInOrder).stream()
                .collect(Collectors.toMap(ScheduleEventSummaryResponse::eventId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, ScheduleVoteSummaryResponse> loadCalendarVoteSummaries(
            ClubAccessResolver.ClubAccess access,
            List<ClubCalendarItem> calendarItems
    ) {
        List<Long> voteIds = calendarItems.stream()
                .filter(item -> CONTENT_SCHEDULE_VOTE.equals(item.getContentType()))
                .map(ClubCalendarItem::getContentId)
                .toList();
        if (voteIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, ClubScheduleVote> voteById = clubScheduleVoteRepository.findAllByVoteIdIn(voteIds).stream()
                .collect(Collectors.toMap(ClubScheduleVote::getVoteId, Function.identity()));
        List<ClubScheduleVote> votesInOrder = voteIds.stream()
                .map(voteById::get)
                .filter(Objects::nonNull)
                .toList();

        return getVoteSummariesForHome(access, votesInOrder).stream()
                .collect(Collectors.toMap(ScheduleVoteSummaryResponse::voteId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private ClubCalendarFeedItemResponse toCalendarFeedItemResponse(
            ClubCalendarItem calendarItem,
            Map<Long, ClubNoticeSummaryResponse> noticeById,
            Map<Long, ScheduleEventSummaryResponse> eventById,
            Map<Long, ScheduleVoteSummaryResponse> voteById
    ) {
        return switch (calendarItem.getContentType()) {
            case CONTENT_NOTICE -> {
                ClubNoticeSummaryResponse notice = noticeById.get(calendarItem.getContentId());
                yield notice == null ? null : new ClubCalendarFeedItemResponse(
                        calendarItem.getCalendarItemId(),
                        calendarItem.getContentType(),
                        notice,
                        null,
                        null
                );
            }
            case CONTENT_SCHEDULE_EVENT -> {
                ScheduleEventSummaryResponse event = eventById.get(calendarItem.getContentId());
                yield event == null ? null : new ClubCalendarFeedItemResponse(
                        calendarItem.getCalendarItemId(),
                        calendarItem.getContentType(),
                        null,
                        event,
                        null
                );
            }
            case CONTENT_SCHEDULE_VOTE -> {
                ScheduleVoteSummaryResponse vote = voteById.get(calendarItem.getContentId());
                yield vote == null ? null : new ClubCalendarFeedItemResponse(
                        calendarItem.getCalendarItemId(),
                        calendarItem.getContentType(),
                        null,
                        null,
                        vote
                );
            }
            default -> null;
        };
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
