package semo.back.service.feature.contentread.vo;

import java.util.List;

public record BoardItemReadStatusResponse(
        Long boardItemId,
        int readCount,
        int activeMemberCount,
        int unreadCount,
        List<ItemReadMemberResponse> readers
) {
}
