package semo.back.service.feature.schedule.biz.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import semo.back.service.database.pub.entity.ClubEventParticipant;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.ClubScheduleEvent;
import semo.back.service.database.pub.entity.ClubScheduleVote;
import semo.back.service.database.pub.entity.ClubScheduleVoteOption;
import semo.back.service.database.pub.entity.ClubScheduleVoteSelection;
import semo.back.service.database.pub.repository.ClubEventParticipantRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteOptionRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteSelectionRepository;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.poll.biz.ClubPollPermissionService;
import semo.back.service.feature.schedule.biz.policy.ClubSchedulePermissionService;
import semo.back.service.feature.schedule.vo.ScheduleEventDetailResponse;
import semo.back.service.feature.schedule.vo.ScheduleEventParticipantSummaryResponse;
import semo.back.service.feature.schedule.vo.ScheduleEventSummaryResponse;
import semo.back.service.feature.schedule.vo.ScheduleEventUpsertResponse;
import semo.back.service.feature.schedule.vo.ScheduleVoteDetailResponse;
import semo.back.service.feature.schedule.vo.ScheduleVoteOptionSummaryResponse;
import semo.back.service.feature.schedule.vo.ScheduleVoteSummaryResponse;
import semo.back.service.feature.schedule.vo.ScheduleVoteUpsertResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ClubScheduleResponseAssembler {
    private static final String PARTICIPATION_GOING = "GOING";
    private static final String PARTICIPATION_NOT_GOING = "NOT_GOING";
    private static final DateTimeFormatter CHECKED_IN_AT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREAN);

    private final ClubEventParticipantRepository clubEventParticipantRepository;
    private final ClubScheduleVoteOptionRepository clubScheduleVoteOptionRepository;
    private final ClubScheduleVoteSelectionRepository clubScheduleVoteSelectionRepository;
    private final ClubFeatureService clubFeatureService;
    private final ClubSchedulePermissionService clubSchedulePermissionService;
    private final ClubPollPermissionService clubPollPermissionService;
    private final ClubScheduleCommandSupport clubScheduleCommandSupport;
    private final ClubScheduleViewSupport clubScheduleViewSupport;

    public List<ScheduleEventSummaryResponse> toEventSummaries(
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
                .map(event -> toEventSummary(
                        access,
                        event,
                        participantsByEventId.getOrDefault(event.getEventId(), List.of()),
                        access.clubProfile().getClubProfileId(),
                        authorProfileById.get(event.getAuthorClubProfileId())
                ))
                .toList();
    }

    public List<ScheduleVoteSummaryResponse> toVoteSummaries(
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

    public ScheduleEventDetailResponse toEventDetail(
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
        boolean attendanceEnabled = clubFeatureService.isFeatureEnabled(access.club().getClubId(), "ATTENDANCE");
        EventAttendanceSnapshot attendance = toAttendanceSnapshot(
                participants,
                access.clubProfile().getClubProfileId()
        );

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
                attendanceEnabled,
                attendanceEnabled && clubSchedulePermissionService.canManageAttendance(access),
                attendance.myAttendanceStatus(),
                attendance.myCheckedInAtLabel(),
                attendance.presentCount(),
                attendance.lateCount(),
                attendance.absentCount(),
                attendance.excusedCount(),
                attendance.unmarkedCount(),
                actionPermission.canEdit(),
                actionPermission.canDelete()
        );
    }

    public ScheduleVoteDetailResponse toVoteDetail(
            ClubAccessResolver.ClubAccess access,
            ClubScheduleVote vote
    ) {
        List<ClubScheduleVoteOption> options = clubScheduleVoteOptionRepository
                .findByVoteIdOrderBySortOrderAscVoteOptionIdAsc(vote.getVoteId());
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

    public ScheduleEventUpsertResponse toEventUpsert(ClubScheduleEvent event) {
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

    public ScheduleVoteUpsertResponse toVoteUpsert(ClubScheduleVote vote, int optionCount) {
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

    public boolean isVoteOpen(ClubScheduleVote vote) {
        return "ONGOING".equals(resolveVoteStatus(vote));
    }

    private ScheduleEventSummaryResponse toEventSummary(
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

    private EventAttendanceSnapshot toAttendanceSnapshot(
            List<ClubEventParticipant> participants,
            Long viewerClubProfileId
    ) {
        String myAttendanceStatus = null;
        String myCheckedInAtLabel = null;
        int presentCount = 0;
        int lateCount = 0;
        int absentCount = 0;
        int excusedCount = 0;
        int unmarkedCount = 0;

        for (ClubEventParticipant participant : participants) {
            if (PARTICIPATION_GOING.equals(participant.getParticipationStatus())
                    && participant.getAttendanceStatus() == null) {
                unmarkedCount++;
            }
            if (participant.getAttendanceStatus() != null) {
                switch (participant.getAttendanceStatus()) {
                    case "PRESENT" -> presentCount++;
                    case "LATE" -> lateCount++;
                    case "ABSENT" -> absentCount++;
                    case "EXCUSED" -> excusedCount++;
                    default -> {
                    }
                }
            }
            if (participant.getClubProfileId().equals(viewerClubProfileId)) {
                myAttendanceStatus = participant.getAttendanceStatus();
                myCheckedInAtLabel = participant.getCheckedInAt() == null
                        ? null
                        : participant.getCheckedInAt().format(CHECKED_IN_AT_FORMATTER);
            }
        }
        return new EventAttendanceSnapshot(
                myAttendanceStatus,
                myCheckedInAtLabel,
                presentCount,
                lateCount,
                absentCount,
                excusedCount,
                unmarkedCount
        );
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
            voteCountByOptionId.computeIfPresent(selection.getVoteOptionId(), (ignored, count) -> count + 1);
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

    private record EventAttendanceSnapshot(
            String myAttendanceStatus,
            String myCheckedInAtLabel,
            int presentCount,
            int lateCount,
            int absentCount,
            int excusedCount,
            int unmarkedCount
    ) {
    }

    private record VoteSelectionSnapshot(
            Long mySelectedOptionId,
            int totalResponses,
            List<ScheduleVoteOptionSummaryResponse> options
    ) {
    }
}
