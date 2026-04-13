package semo.back.service.feature.schedule.biz.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.database.pub.entity.ClubCalendarItem;
import semo.back.service.database.pub.entity.ClubNotice;
import semo.back.service.database.pub.entity.ClubScheduleEvent;
import semo.back.service.database.pub.entity.ClubScheduleVote;
import semo.back.service.database.pub.entity.TournamentRecord;
import semo.back.service.database.pub.repository.ClubNoticeRepository;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteRepository;
import semo.back.service.database.pub.repository.TournamentRecordRepository;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.notice.biz.ClubNoticeService;
import semo.back.service.feature.notice.vo.ClubNoticeSummaryResponse;
import semo.back.service.feature.schedule.biz.assembler.ClubScheduleSummaryAssembler;
import semo.back.service.feature.schedule.vo.ClubCalendarFeedItemResponse;
import semo.back.service.feature.schedule.vo.ScheduleEventSummaryResponse;
import semo.back.service.feature.schedule.vo.ScheduleVoteSummaryResponse;
import semo.back.service.feature.share.biz.ClubContentShareService;
import semo.back.service.feature.tournament.biz.ClubTournamentService;
import semo.back.service.feature.tournament.vo.TournamentSummaryResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubScheduleCalendarLoader {
    private static final String CONTENT_NOTICE = ClubContentShareService.CONTENT_NOTICE;
    private static final String CONTENT_SCHEDULE_EVENT = ClubContentShareService.CONTENT_SCHEDULE_EVENT;
    private static final String CONTENT_SCHEDULE_VOTE = ClubContentShareService.CONTENT_SCHEDULE_VOTE;
    private static final String CONTENT_TOURNAMENT = ClubContentShareService.CONTENT_TOURNAMENT;

    private final ClubNoticeRepository clubNoticeRepository;
    private final ClubScheduleEventRepository clubScheduleEventRepository;
    private final ClubScheduleVoteRepository clubScheduleVoteRepository;
    private final TournamentRecordRepository tournamentRecordRepository;
    private final ClubNoticeService clubNoticeService;
    private final ClubTournamentService clubTournamentService;
    private final ClubScheduleSummaryAssembler clubScheduleSummaryAssembler;

    public List<ClubCalendarFeedItemResponse> loadCalendarFeedItems(
            ClubAccessResolver.ClubAccess access,
            List<ClubCalendarItem> calendarItems
    ) {
        Map<Long, ClubNoticeSummaryResponse> noticeById = loadCalendarNoticeSummaries(access, calendarItems);
        Map<Long, ScheduleEventSummaryResponse> eventById = loadCalendarEventSummaries(access, calendarItems);
        Map<Long, ScheduleVoteSummaryResponse> voteById = loadCalendarVoteSummaries(access, calendarItems);
        Map<Long, TournamentSummaryResponse> tournamentById = loadCalendarTournamentSummaries(access, calendarItems);

        return calendarItems.stream()
                .map(item -> toCalendarFeedItemResponse(item, noticeById, eventById, voteById, tournamentById))
                .filter(Objects::nonNull)
                .toList();
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
                .collect(Collectors.toMap(
                        ClubNoticeSummaryResponse::noticeId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
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

        return clubScheduleSummaryAssembler.toEventSummaries(access, eventsInOrder).stream()
                .collect(Collectors.toMap(
                        ScheduleEventSummaryResponse::eventId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
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

        return clubScheduleSummaryAssembler.toVoteSummaryResponses(access, votesInOrder).stream()
                .collect(Collectors.toMap(
                        ScheduleVoteSummaryResponse::voteId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private Map<Long, TournamentSummaryResponse> loadCalendarTournamentSummaries(
            ClubAccessResolver.ClubAccess access,
            List<ClubCalendarItem> calendarItems
    ) {
        List<Long> tournamentIds = calendarItems.stream()
                .filter(item -> CONTENT_TOURNAMENT.equals(item.getContentType()))
                .map(ClubCalendarItem::getContentId)
                .toList();
        if (tournamentIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, TournamentRecord> tournamentById = tournamentRecordRepository.findAllByTournamentRecordIdIn(tournamentIds).stream()
                .filter(tournament -> !tournament.isDeleted())
                .collect(Collectors.toMap(TournamentRecord::getTournamentRecordId, Function.identity()));
        List<TournamentRecord> tournamentsInOrder = tournamentIds.stream()
                .map(tournamentById::get)
                .filter(Objects::nonNull)
                .toList();

        return clubTournamentService.getTournamentSummariesForDisplay(access, tournamentsInOrder).stream()
                .collect(Collectors.toMap(
                        TournamentSummaryResponse::tournamentRecordId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private ClubCalendarFeedItemResponse toCalendarFeedItemResponse(
            ClubCalendarItem calendarItem,
            Map<Long, ClubNoticeSummaryResponse> noticeById,
            Map<Long, ScheduleEventSummaryResponse> eventById,
            Map<Long, ScheduleVoteSummaryResponse> voteById,
            Map<Long, TournamentSummaryResponse> tournamentById
    ) {
        return switch (calendarItem.getContentType()) {
            case CONTENT_NOTICE -> {
                ClubNoticeSummaryResponse notice = noticeById.get(calendarItem.getContentId());
                yield notice == null ? null : new ClubCalendarFeedItemResponse(
                        calendarItem.getCalendarItemId(),
                        calendarItem.getContentType(),
                        notice,
                        null,
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
                        null,
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
                        vote,
                        null
                );
            }
            case CONTENT_TOURNAMENT -> {
                TournamentSummaryResponse tournament = tournamentById.get(calendarItem.getContentId());
                yield tournament == null ? null : new ClubCalendarFeedItemResponse(
                        calendarItem.getCalendarItemId(),
                        calendarItem.getContentType(),
                        null,
                        null,
                        null,
                        tournament
                );
            }
            default -> null;
        };
    }
}
