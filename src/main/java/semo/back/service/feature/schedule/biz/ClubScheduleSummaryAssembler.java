package semo.back.service.feature.schedule.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.common.util.ImageFileUrlResolver;
import semo.back.service.database.pub.entity.ClubEventParticipant;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.ClubScheduleEvent;
import semo.back.service.database.pub.entity.ClubScheduleVote;
import semo.back.service.database.pub.entity.ClubScheduleVoteOption;
import semo.back.service.database.pub.entity.ClubScheduleVoteSelection;
import semo.back.service.database.pub.repository.ClubEventParticipantRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteOptionRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteSelectionRepository;
import semo.back.service.feature.club.biz.ClubAccessResolver;
import semo.back.service.feature.poll.biz.ClubPollPermissionService;
import semo.back.service.feature.schedule.vo.ScheduleEventDetailResponse;
import semo.back.service.feature.schedule.vo.ScheduleEventParticipantSummaryResponse;
import semo.back.service.feature.schedule.vo.ScheduleEventSummaryResponse;
import semo.back.service.feature.schedule.vo.ScheduleVoteDetailResponse;
import semo.back.service.feature.schedule.vo.ScheduleVoteOptionSummaryResponse;
import semo.back.service.feature.schedule.vo.ScheduleVoteSummaryResponse;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubScheduleSummaryAssembler {
    private static final String PARTICIPATION_GOING = "GOING";
    private static final String PARTICIPATION_NOT_GOING = "NOT_GOING";

    private final ClubEventParticipantRepository clubEventParticipantRepository;
    private final ClubScheduleVoteOptionRepository clubScheduleVoteOptionRepository;
    private final ClubScheduleVoteSelectionRepository clubScheduleVoteSelectionRepository;
    private final ClubProfileRepository clubProfileRepository;
    private final ClubSchedulePermissionService clubSchedulePermissionService;
    private final ClubPollPermissionService clubPollPermissionService;
    private final ImageFileUrlResolver imageFileUrlResolver;
    private final ClubScheduleFormatter clubScheduleFormatter;

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

    public ScheduleEventDetailResponse toEventDetailResponse(
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
                clubScheduleFormatter.formatDateValue(event.getStartAt().toLocalDate()),
                event.getEndAt() == null ? null : clubScheduleFormatter.formatDateValue(event.getEndAt().toLocalDate()),
                clubScheduleFormatter.formatDateRangeLabel(
                        event.getStartAt().toLocalDate(),
                        event.getEndAt() == null ? null : event.getEndAt().toLocalDate()
                ),
                clubScheduleFormatter.formatTimeValue(event.getStartAt(), event.getEndAt()),
                clubScheduleFormatter.formatEndTimeValue(event.getStartAt(), event.getEndAt()),
                clubScheduleFormatter.formatTimeLabel(event.getStartAt(), event.getEndAt()),
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

    public List<ScheduleVoteSummaryResponse> toVoteSummaryResponses(
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
                            clubScheduleFormatter.resolveVoteStatus(vote),
                            clubScheduleFormatter.formatDateValue(vote.getVoteStartDate()),
                            clubScheduleFormatter.formatDateValue(vote.getVoteEndDate()),
                            clubScheduleFormatter.formatDateRangeLabel(vote.getVoteStartDate(), vote.getVoteEndDate()),
                            clubScheduleFormatter.formatVoteTimeLabel(vote.getVoteStartTime(), vote.getVoteEndTime()),
                            selection.options().size(),
                            selection.totalResponses(),
                            vote.isSharedToBoard(),
                            vote.isSharedToCalendar(),
                            vote.isSharedToCalendar(),
                            vote.isPinned(),
                            null,
                            selection.mySelectedOptionId(),
                            selection.options(),
                            clubScheduleFormatter.isVoteOpen(vote),
                            actionPermission.canEdit(),
                            actionPermission.canDelete()
                    );
                })
                .toList();
    }

    public ScheduleVoteDetailResponse toVoteDetailResponse(
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
                clubScheduleFormatter.resolveVoteStatus(vote),
                clubScheduleFormatter.formatDateValue(vote.getVoteStartDate()),
                clubScheduleFormatter.formatDateValue(vote.getVoteEndDate()),
                clubScheduleFormatter.formatDateRangeLabel(vote.getVoteStartDate(), vote.getVoteEndDate()),
                clubScheduleFormatter.formatOptionalTimeValue(vote.getVoteStartTime()),
                clubScheduleFormatter.formatOptionalTimeValue(vote.getVoteEndTime()),
                clubScheduleFormatter.formatVoteTimeLabel(vote.getVoteStartTime(), vote.getVoteEndTime()),
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
                clubScheduleFormatter.isVoteOpen(vote)
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
                resolveAuthorDisplayName(authorProfile),
                resolveAuthorAvatarImageUrl(authorProfile),
                resolveAuthorAvatarThumbnailUrl(authorProfile),
                clubScheduleFormatter.formatDateValue(event.getStartAt().toLocalDate()),
                event.getEndAt() == null ? null : clubScheduleFormatter.formatDateValue(event.getEndAt().toLocalDate()),
                clubScheduleFormatter.formatDateRangeLabel(
                        event.getStartAt().toLocalDate(),
                        event.getEndAt() == null ? null : event.getEndAt().toLocalDate()
                ),
                clubScheduleFormatter.formatTimeLabel(event.getStartAt(), event.getEndAt()),
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

    private List<ScheduleEventParticipantSummaryResponse> toGoingParticipantSummaries(List<ClubEventParticipant> participants) {
        List<ClubEventParticipant> goingParticipants = participants.stream()
                .filter(participant -> PARTICIPATION_GOING.equals(participant.getParticipationStatus()))
                .sorted(Comparator.comparing(ClubEventParticipant::getClubEventParticipantId))
                .toList();
        if (goingParticipants.isEmpty()) {
            return List.of();
        }

        Map<Long, ClubProfile> profileById = loadAuthorProfiles(
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
                            resolveAuthorDisplayName(profile),
                            resolveAuthorAvatarImageUrl(profile),
                            resolveAuthorAvatarThumbnailUrl(profile)
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
