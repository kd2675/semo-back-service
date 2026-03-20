package semo.back.service.feature.poll.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.common.util.ImageFileUrlResolver;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.ClubScheduleVote;
import semo.back.service.database.pub.entity.ClubScheduleVoteOption;
import semo.back.service.database.pub.entity.ClubScheduleVoteSelection;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteOptionRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteSelectionRepository;
import semo.back.service.feature.club.biz.ClubAccessResolver;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.poll.vo.ClubPollHomeResponse;
import semo.back.service.feature.poll.vo.ClubPollSummaryResponse;
import semo.back.service.feature.schedule.vo.ScheduleVoteOptionSummaryResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubPollService {
    private static final String FEATURE_POLL = "POLL";
    private static final DateTimeFormatter DATE_REQUEST_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_LABEL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd (E)", Locale.KOREAN);
    private static final DateTimeFormatter TIME_LABEL_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final ClubScheduleVoteRepository clubScheduleVoteRepository;
    private final ClubScheduleVoteOptionRepository clubScheduleVoteOptionRepository;
    private final ClubScheduleVoteSelectionRepository clubScheduleVoteSelectionRepository;
    private final ClubProfileRepository clubProfileRepository;
    private final ClubAccessResolver clubAccessResolver;
    private final ClubFeatureService clubFeatureService;
    private final ClubPollPermissionService clubPollPermissionService;
    private final ImageFileUrlResolver imageFileUrlResolver;

    public ClubPollHomeResponse getPollHome(Long clubId, String userKey, String query) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requirePollFeature(clubId);

        List<ClubScheduleVote> votes = clubScheduleVoteRepository.findAllByClubIdForPollHome(clubId, normalizeQuery(query));
        if (votes.isEmpty()) {
            return new ClubPollHomeResponse(
                    access.club().getClubId(),
                    access.club().getName(),
                    access.isAdmin(),
                    clubPollPermissionService.canCreatePoll(access),
                    0,
                    0,
                    0,
                    List.of()
            );
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

        List<ClubPollSummaryResponse> polls = votes.stream()
                .map(vote -> {
                    VoteSelectionSnapshot selection = toVoteSelectionSnapshot(
                            optionsByVoteId.getOrDefault(vote.getVoteId(), List.of()),
                            selectionsByVoteId.getOrDefault(vote.getVoteId(), List.of()),
                            access.clubProfile().getClubProfileId()
                    );
                    ClubPollPermissionService.PollActionPermission actionPermission =
                            clubPollPermissionService.getActionPermission(access, vote.getAuthorClubProfileId());
                    return new ClubPollSummaryResponse(
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
                            formatVoteWindowLabel(vote),
                            selection.totalResponses(),
                            selection.options().size(),
                            vote.isSharedToBoard(),
                            vote.isSharedToCalendar(),
                            vote.isSharedToCalendar(),
                            actionPermission.canEdit(),
                            actionPermission.canDelete(),
                            selection.mySelectedOptionId(),
                            selection.options()
                    );
                })
                .sorted(pollComparator())
                .toList();

        int waitingCount = (int) polls.stream().filter(poll -> "WAITING".equals(poll.voteStatus())).count();
        int ongoingCount = (int) polls.stream().filter(poll -> "ONGOING".equals(poll.voteStatus())).count();
        int closedCount = (int) polls.stream().filter(poll -> "CLOSED".equals(poll.voteStatus())).count();

        return new ClubPollHomeResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                clubPollPermissionService.canCreatePoll(access),
                waitingCount,
                ongoingCount,
                closedCount,
                polls
        );
    }

    public void requirePollFeature(Long clubId) {
        if (!clubFeatureService.isFeatureEnabled(clubId, FEATURE_POLL)) {
            throw new SemoException.ValidationException("투표 기능이 활성화되지 않았습니다.");
        }
    }

    private String normalizeQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }
        return query.trim();
    }

    private Comparator<ClubPollSummaryResponse> pollComparator() {
        return Comparator
                .comparingInt((ClubPollSummaryResponse poll) -> switch (poll.voteStatus()) {
                    case "ONGOING" -> 0;
                    case "WAITING" -> 1;
                    default -> 2;
                })
                .thenComparing((ClubPollSummaryResponse poll) -> switch (poll.voteStatus()) {
                    case "ONGOING" -> poll.voteEndDate();
                    case "WAITING" -> poll.voteStartDate();
                    default -> poll.voteEndDate();
                }, Comparator.nullsLast(String::compareTo))
                .thenComparing(ClubPollSummaryResponse::voteId, Comparator.reverseOrder());
    }

    private Map<Long, ClubProfile> loadAuthorProfiles(List<Long> clubProfileIds) {
        if (clubProfileIds.isEmpty()) {
            return Map.of();
        }
        return clubProfileRepository.findAllById(clubProfileIds).stream()
                .collect(Collectors.toMap(ClubProfile::getClubProfileId, profile -> profile));
    }

    private String resolveAuthorDisplayName(ClubProfile authorProfile) {
        if (authorProfile == null || !StringUtils.hasText(authorProfile.getDisplayName())) {
            return "멤버";
        }
        return authorProfile.getDisplayName();
    }

    private String resolveAuthorAvatarImageUrl(ClubProfile authorProfile) {
        if (authorProfile == null || !StringUtils.hasText(authorProfile.getAvatarFileName())) {
            return null;
        }
        return imageFileUrlResolver.resolveImageUrl(authorProfile.getAvatarFileName());
    }

    private String resolveAuthorAvatarThumbnailUrl(ClubProfile authorProfile) {
        if (authorProfile == null || !StringUtils.hasText(authorProfile.getAvatarFileName())) {
            return null;
        }
        return imageFileUrlResolver.resolveThumbnailUrl(authorProfile.getAvatarFileName());
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
        LocalDateTime startAt = toVoteStartAt(vote.getVoteStartDate(), vote.getVoteStartTime());
        if (now.isBefore(startAt)) {
            return "WAITING";
        }
        if (now.isAfter(toVoteEffectiveEndAt(vote.getVoteEndDate(), vote.getVoteEndTime()))) {
            return "CLOSED";
        }
        return "ONGOING";
    }

    private LocalDateTime toVoteStartAt(LocalDate startDate, LocalTime startTime) {
        return startDate.atTime(startTime == null ? LocalTime.MIDNIGHT : startTime);
    }

    private LocalDateTime toVoteEffectiveEndAt(LocalDate endDate, LocalTime endTime) {
        return endDate.atTime(endTime == null ? LocalTime.MAX : endTime);
    }

    private String formatDateValue(LocalDate value) {
        return value.format(DATE_REQUEST_FORMATTER);
    }

    private String formatDateRangeLabel(LocalDate startDate, LocalDate endDate) {
        if (endDate == null || endDate.equals(startDate)) {
            return startDate.format(DATE_LABEL_FORMATTER);
        }
        return startDate.format(DATE_LABEL_FORMATTER) + " - " + endDate.format(DATE_LABEL_FORMATTER);
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
        return startTime.format(TIME_LABEL_FORMATTER) + " - " + endTime.format(TIME_LABEL_FORMATTER);
    }

    private String formatVoteWindowLabel(ClubScheduleVote vote) {
        String startDate = vote.getVoteStartDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        String endDate = vote.getVoteEndDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        String startTime = vote.getVoteStartTime() == null ? null : vote.getVoteStartTime().format(TIME_LABEL_FORMATTER);
        String endTime = vote.getVoteEndTime() == null ? null : vote.getVoteEndTime().format(TIME_LABEL_FORMATTER);

        String startLabel = startTime == null ? startDate : startDate + " " + startTime;
        String endLabel = endTime == null ? endDate : endDate + " " + endTime;
        return startLabel + " ~ " + endLabel;
    }

    private record VoteSelectionSnapshot(
            Long mySelectedOptionId,
            int totalResponses,
            List<ScheduleVoteOptionSummaryResponse> options
    ) {
    }
}
