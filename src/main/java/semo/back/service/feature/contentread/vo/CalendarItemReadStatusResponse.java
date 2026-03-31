package semo.back.service.feature.contentread.vo;

import java.util.List;

public record CalendarItemReadStatusResponse(
        Long calendarItemId,
        int readCount,
        int activeMemberCount,
        int unreadCount,
        List<ItemReadMemberResponse> readers
) {
}
