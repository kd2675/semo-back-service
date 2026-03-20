package semo.back.service.feature.share.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.database.pub.entity.ClubBoardItem;
import semo.back.service.database.pub.entity.ClubCalendarItem;
import semo.back.service.database.pub.repository.ClubBoardItemRepository;
import semo.back.service.database.pub.repository.ClubCalendarItemRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubContentShareService {
    public static final String CONTENT_NOTICE = "NOTICE";
    public static final String CONTENT_SCHEDULE_EVENT = "SCHEDULE_EVENT";
    public static final String CONTENT_SCHEDULE_VOTE = "SCHEDULE_VOTE";

    private final ClubBoardItemRepository clubBoardItemRepository;
    private final ClubCalendarItemRepository clubCalendarItemRepository;

    @Transactional(transactionManager = "pubTransactionManager")
    public void syncBoardShare(Long clubId, String contentType, Long contentId, boolean shared) {
        if (shared) {
            upsertBoardItem(clubId, contentType, contentId);
            return;
        }
        clubBoardItemRepository.deleteByClubIdAndContentTypeAndContentId(clubId, contentType, contentId);
    }

    @Transactional(transactionManager = "pubTransactionManager")
    public void syncCalendarShare(Long clubId, String contentType, Long contentId, boolean shared) {
        if (shared) {
            upsertCalendarItem(clubId, contentType, contentId);
            return;
        }
        clubCalendarItemRepository.deleteByClubIdAndContentTypeAndContentId(clubId, contentType, contentId);
    }

    @Transactional(transactionManager = "pubTransactionManager")
    public void removeAllShares(Long clubId, String contentType, Long contentId) {
        clubBoardItemRepository.deleteByClubIdAndContentTypeAndContentId(clubId, contentType, contentId);
        clubCalendarItemRepository.deleteByClubIdAndContentTypeAndContentId(clubId, contentType, contentId);
    }

    public List<Long> getBoardContentIds(Long clubId, String contentType) {
        return clubBoardItemRepository.findAllByClubIdAndContentTypeOrderByBoardItemIdDesc(clubId, contentType)
                .stream()
                .map(ClubBoardItem::getContentId)
                .toList();
    }

    public List<Long> getCalendarContentIds(Long clubId, String contentType) {
        return clubCalendarItemRepository.findAllByClubIdAndContentTypeOrderByCalendarItemIdDesc(clubId, contentType)
                .stream()
                .map(ClubCalendarItem::getContentId)
                .toList();
    }

    public boolean isSharedToBoard(Long clubId, String contentType, Long contentId) {
        return clubBoardItemRepository.findByClubIdAndContentTypeAndContentId(clubId, contentType, contentId).isPresent();
    }

    public boolean isSharedToCalendar(Long clubId, String contentType, Long contentId) {
        return clubCalendarItemRepository.findByClubIdAndContentTypeAndContentId(clubId, contentType, contentId).isPresent();
    }

    private void upsertBoardItem(Long clubId, String contentType, Long contentId) {
        if (clubBoardItemRepository.findByClubIdAndContentTypeAndContentId(clubId, contentType, contentId).isPresent()) {
            return;
        }
        clubBoardItemRepository.save(ClubBoardItem.builder()
                .clubId(clubId)
                .contentType(contentType)
                .contentId(contentId)
                .build());
    }

    private void upsertCalendarItem(Long clubId, String contentType, Long contentId) {
        if (clubCalendarItemRepository.findByClubIdAndContentTypeAndContentId(clubId, contentType, contentId).isPresent()) {
            return;
        }
        clubCalendarItemRepository.save(ClubCalendarItem.builder()
                .clubId(clubId)
                .contentType(contentType)
                .contentId(contentId)
                .build());
    }
}
