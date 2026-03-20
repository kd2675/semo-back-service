package semo.back.service.feature.notice.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubBoardItem;
import semo.back.service.database.pub.entity.ClubNotice;
import semo.back.service.database.pub.entity.ClubScheduleEvent;
import semo.back.service.database.pub.entity.ClubScheduleVote;
import semo.back.service.database.pub.repository.ClubBoardItemRepository;
import semo.back.service.database.pub.repository.ClubNoticeRepository;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteRepository;
import semo.back.service.feature.club.biz.ClubAccessResolver;
import semo.back.service.feature.notice.vo.ClubBoardFeedItemResponse;
import semo.back.service.feature.notice.vo.ClubNoticeFeedResponse;
import semo.back.service.feature.notice.vo.ClubNoticeSummaryResponse;
import semo.back.service.feature.schedule.biz.ClubScheduleService;
import semo.back.service.feature.schedule.vo.ScheduleEventSummaryResponse;
import semo.back.service.feature.schedule.vo.ScheduleVoteSummaryResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubBoardFeedService {
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 30;
    private static final String CONTENT_NOTICE = "NOTICE";
    private static final String CONTENT_SCHEDULE_EVENT = "SCHEDULE_EVENT";
    private static final String CONTENT_SCHEDULE_VOTE = "SCHEDULE_VOTE";

    private final ClubAccessResolver clubAccessResolver;
    private final ClubBoardItemRepository clubBoardItemRepository;
    private final ClubNoticeRepository clubNoticeRepository;
    private final ClubScheduleEventRepository clubScheduleEventRepository;
    private final ClubScheduleVoteRepository clubScheduleVoteRepository;
    private final ClubNoticeService clubNoticeService;
    private final ClubScheduleService clubScheduleService;

    public ClubNoticeFeedResponse getBoardFeed(
            Long clubId,
            String userKey,
            String query,
            boolean pinnedOnly,
            Long cursorBoardItemId,
            Integer size
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);

        int pageSize = normalizePageSize(size);
        List<ClubBoardItem> rows = clubBoardItemRepository.findFeedItems(
                clubId,
                normalizeQuery(query),
                pinnedOnly,
                normalizeCursorBoardItemId(cursorBoardItemId),
                PageRequest.of(0, pageSize + 1)
        );

        boolean hasNext = rows.size() > pageSize;
        List<ClubBoardItem> pageRows = hasNext ? rows.subList(0, pageSize) : rows;
        Map<Long, ClubNoticeSummaryResponse> noticeById = loadNoticeSummaries(access, pageRows);
        Map<Long, ScheduleEventSummaryResponse> eventById = loadEventSummaries(access, pageRows);
        Map<Long, ScheduleVoteSummaryResponse> voteById = loadVoteSummaries(access, pageRows);

        List<ClubBoardFeedItemResponse> items = pageRows.stream()
                .map(row -> toBoardFeedItemResponse(row, noticeById, eventById, voteById))
                .filter(item -> item != null)
                .toList();

        ClubBoardItem lastRow = hasNext ? pageRows.get(pageRows.size() - 1) : null;
        return new ClubNoticeFeedResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                items,
                lastRow == null ? null : lastRow.getBoardItemId(),
                hasNext
        );
    }

    private Map<Long, ClubNoticeSummaryResponse> loadNoticeSummaries(
            ClubAccessResolver.ClubAccess access,
            List<ClubBoardItem> rows
    ) {
        List<Long> noticeIds = rows.stream()
                .filter(row -> CONTENT_NOTICE.equals(row.getContentType()))
                .map(ClubBoardItem::getContentId)
                .toList();
        if (noticeIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, ClubNotice> noticeById = clubNoticeRepository.findAllByNoticeIdIn(noticeIds).stream()
                .filter(notice -> !notice.isDeleted())
                .collect(Collectors.toMap(ClubNotice::getNoticeId, Function.identity()));
        List<ClubNotice> noticesInOrder = noticeIds.stream()
                .map(noticeById::get)
                .filter(notice -> notice != null)
                .toList();

        return clubNoticeService.toNoticeSummaries(access, noticesInOrder).stream()
                .collect(Collectors.toMap(ClubNoticeSummaryResponse::noticeId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, ScheduleEventSummaryResponse> loadEventSummaries(
            ClubAccessResolver.ClubAccess access,
            List<ClubBoardItem> rows
    ) {
        List<Long> eventIds = rows.stream()
                .filter(row -> CONTENT_SCHEDULE_EVENT.equals(row.getContentType()))
                .map(ClubBoardItem::getContentId)
                .toList();
        if (eventIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, ClubScheduleEvent> eventById = clubScheduleEventRepository.findAllByEventIdIn(eventIds).stream()
                .filter(event -> !"CANCELLED".equals(event.getEventStatus()))
                .collect(Collectors.toMap(ClubScheduleEvent::getEventId, Function.identity()));
        List<ClubScheduleEvent> eventsInOrder = eventIds.stream()
                .map(eventById::get)
                .filter(event -> event != null)
                .toList();

        return clubScheduleService.getEventSummariesForHome(access, eventsInOrder).stream()
                .collect(Collectors.toMap(ScheduleEventSummaryResponse::eventId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, ScheduleVoteSummaryResponse> loadVoteSummaries(
            ClubAccessResolver.ClubAccess access,
            List<ClubBoardItem> rows
    ) {
        List<Long> voteIds = rows.stream()
                .filter(row -> CONTENT_SCHEDULE_VOTE.equals(row.getContentType()))
                .map(ClubBoardItem::getContentId)
                .toList();
        if (voteIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, ClubScheduleVote> voteById = clubScheduleVoteRepository.findAllByVoteIdIn(voteIds).stream()
                .collect(Collectors.toMap(ClubScheduleVote::getVoteId, Function.identity()));
        List<ClubScheduleVote> votesInOrder = voteIds.stream()
                .map(voteById::get)
                .filter(vote -> vote != null)
                .toList();

        return clubScheduleService.getVoteSummariesForHome(access, votesInOrder).stream()
                .collect(Collectors.toMap(ScheduleVoteSummaryResponse::voteId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private ClubBoardFeedItemResponse toBoardFeedItemResponse(
            ClubBoardItem row,
            Map<Long, ClubNoticeSummaryResponse> noticeById,
            Map<Long, ScheduleEventSummaryResponse> eventById,
            Map<Long, ScheduleVoteSummaryResponse> voteById
    ) {
        return switch (row.getContentType()) {
            case CONTENT_NOTICE -> {
                ClubNoticeSummaryResponse notice = noticeById.get(row.getContentId());
                yield notice == null ? null : new ClubBoardFeedItemResponse(
                        row.getBoardItemId(),
                        row.getContentType(),
                        notice,
                        null,
                        null
                );
            }
            case CONTENT_SCHEDULE_EVENT -> {
                ScheduleEventSummaryResponse event = eventById.get(row.getContentId());
                yield event == null ? null : new ClubBoardFeedItemResponse(
                        row.getBoardItemId(),
                        row.getContentType(),
                        null,
                        event,
                        null
                );
            }
            case CONTENT_SCHEDULE_VOTE -> {
                ScheduleVoteSummaryResponse vote = voteById.get(row.getContentId());
                yield vote == null ? null : new ClubBoardFeedItemResponse(
                        row.getBoardItemId(),
                        row.getContentType(),
                        null,
                        null,
                        vote
                );
            }
            default -> null;
        };
    }

    private String normalizeQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }
        return query.trim();
    }

    private Long normalizeCursorBoardItemId(Long cursorBoardItemId) {
        if (cursorBoardItemId == null) {
            return null;
        }
        if (cursorBoardItemId < 1) {
            throw new SemoException.ValidationException("잘못된 커서 값입니다.");
        }
        return cursorBoardItemId;
    }

    private int normalizePageSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
