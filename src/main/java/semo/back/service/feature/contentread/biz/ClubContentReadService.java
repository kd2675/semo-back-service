package semo.back.service.feature.contentread.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.common.exception.SemoException;
import semo.back.service.common.util.ImageFileUrlResolver;
import semo.back.service.database.pub.entity.ClubBoardItem;
import semo.back.service.database.pub.entity.ClubBoardItemRead;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.repository.BoardItemReadCountRow;
import semo.back.service.database.pub.repository.ClubBoardItemReadRepository;
import semo.back.service.database.pub.repository.ClubBoardItemRepository;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.contentread.vo.BoardItemReadResponse;
import semo.back.service.feature.contentread.vo.BoardItemReadStatusResponse;
import semo.back.service.feature.contentread.vo.ItemReadMemberResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
public class ClubContentReadService {
    private static final DateTimeFormatter DATE_TIME_LABEL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREAN);

    private final ClubAccessResolver clubAccessResolver;
    private final ClubBoardItemRepository clubBoardItemRepository;
    private final ClubBoardItemReadRepository clubBoardItemReadRepository;
    private final ImageFileUrlResolver imageFileUrlResolver;

    @Transactional(transactionManager = "pubTransactionManager")
    public BoardItemReadResponse recordBoardItemRead(Long clubId, Long boardItemId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        ClubBoardItem boardItem = requireBoardItem(clubId, boardItemId);
        LocalDateTime now = LocalDateTime.now();
        ClubBoardItemRead read = clubBoardItemReadRepository.findByBoardItemIdAndClubProfileId(
                        boardItem.getBoardItemId(),
                        access.clubProfile().getClubProfileId()
                )
                .orElseGet(() -> ClubBoardItemRead.builder()
                        .boardItemId(boardItem.getBoardItemId())
                        .clubProfileId(access.clubProfile().getClubProfileId())
                        .firstReadAt(now)
                        .lastReadAt(now)
                        .build());
        read.markRead(now);
        clubBoardItemReadRepository.save(read);
        return new BoardItemReadResponse(
                boardItem.getBoardItemId(),
                Math.toIntExact(clubBoardItemReadRepository.countByBoardItemId(boardItem.getBoardItemId()))
        );
    }

    public Map<Long, Integer> getBoardReadCounts(List<Long> boardItemIds) {
        if (boardItemIds == null || boardItemIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> counts = new LinkedHashMap<>();
        for (BoardItemReadCountRow row : clubBoardItemReadRepository.findReadCountsByBoardItemIdIn(boardItemIds)) {
            counts.put(row.boardItemId(), Math.toIntExact(row.readCount()));
        }
        return counts;
    }

    public BoardItemReadStatusResponse getBoardItemReadStatus(Long clubId, Long boardItemId, String userKey) {
        clubAccessResolver.requireActiveMember(clubId, userKey);
        ClubBoardItem boardItem = requireBoardItem(clubId, boardItemId);
        List<ClubAccessResolver.ClubMemberSnapshot> activeMembers = clubAccessResolver.getActiveMemberSnapshots(clubId);
        Map<Long, ClubAccessResolver.ClubMemberSnapshot> snapshotByClubProfileId = activeMembers.stream()
                .collect(Collectors.toMap(snapshot -> snapshot.clubProfile().getClubProfileId(), Function.identity()));
        List<ItemReadMemberResponse> readers = clubBoardItemReadRepository
                .findAllByBoardItemIdOrderByLastReadAtDescClubBoardItemReadIdDesc(boardItem.getBoardItemId())
                .stream()
                .map(read -> toMemberResponse(read.getClubProfileId(), read.getLastReadAt(), snapshotByClubProfileId.get(read.getClubProfileId())))
                .filter(Objects::nonNull)
                .toList();
        int readCount = readers.size();
        int activeMemberCount = activeMembers.size();
        return new BoardItemReadStatusResponse(
                boardItem.getBoardItemId(),
                readCount,
                activeMemberCount,
                Math.max(activeMemberCount - readCount, 0),
                readers
        );
    }

    private ClubBoardItem requireBoardItem(Long clubId, Long boardItemId) {
        return clubBoardItemRepository.findById(boardItemId)
                .filter(item -> item.getClubId().equals(clubId))
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("ClubBoardItem", "boardItemId", boardItemId));
    }

    private ItemReadMemberResponse toMemberResponse(
            Long clubProfileId,
            LocalDateTime lastReadAt,
            ClubAccessResolver.ClubMemberSnapshot snapshot
    ) {
        if (snapshot == null) {
            return null;
        }
        ClubProfile clubProfile = snapshot.clubProfile();
        return new ItemReadMemberResponse(
                clubProfileId,
                clubProfile.getDisplayName(),
                imageFileUrlResolver.resolveImageUrl(clubProfile.getAvatarFileName()),
                imageFileUrlResolver.resolveThumbnailUrl(clubProfile.getAvatarFileName()),
                snapshot.membership().getRoleCode(),
                lastReadAt.format(DATE_TIME_LABEL_FORMATTER)
        );
    }
}
