package semo.back.service.feature.contentread.act;

import auth.common.core.context.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.contentread.biz.ClubContentReadService;
import semo.back.service.feature.contentread.vo.BoardItemReadResponse;
import semo.back.service.feature.contentread.vo.BoardItemReadStatusResponse;
import semo.back.service.feature.contentread.vo.CalendarItemReadResponse;
import semo.back.service.feature.contentread.vo.CalendarItemReadStatusResponse;
import web.common.core.response.base.dto.ResponseDataDTO;

@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}")
@RequiredArgsConstructor
public class ClubContentReadController {
    private final ClubContentReadService clubContentReadService;

    @PostMapping("/board/items/{boardItemId}/read")
    public ResponseDataDTO<BoardItemReadResponse> recordBoardItemRead(
            @PathVariable Long clubId,
            @PathVariable Long boardItemId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubContentReadService.recordBoardItemRead(clubId, boardItemId, requireUserKey(userContext)),
                "게시판 읽음 기록 성공"
        );
    }

    @GetMapping("/board/items/{boardItemId}/read-status")
    public ResponseDataDTO<BoardItemReadStatusResponse> getBoardItemReadStatus(
            @PathVariable Long clubId,
            @PathVariable Long boardItemId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubContentReadService.getBoardItemReadStatus(clubId, boardItemId, requireUserKey(userContext)),
                "게시판 읽음 현황 조회 성공"
        );
    }

    @PostMapping("/schedule/items/{calendarItemId}/read")
    public ResponseDataDTO<CalendarItemReadResponse> recordCalendarItemRead(
            @PathVariable Long clubId,
            @PathVariable Long calendarItemId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubContentReadService.recordCalendarItemRead(clubId, calendarItemId, requireUserKey(userContext)),
                "캘린더 읽음 기록 성공"
        );
    }

    @GetMapping("/schedule/items/{calendarItemId}/read-status")
    public ResponseDataDTO<CalendarItemReadStatusResponse> getCalendarItemReadStatus(
            @PathVariable Long clubId,
            @PathVariable Long calendarItemId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubContentReadService.getCalendarItemReadStatus(clubId, calendarItemId, requireUserKey(userContext)),
                "캘린더 읽음 현황 조회 성공"
        );
    }

    private String requireUserKey(UserContext userContext) {
        if (userContext == null || !StringUtils.hasText(userContext.getUserKey())) {
            throw new SemoException.UnauthorizedException("Login required");
        }
        return userContext.getUserKey();
    }

    private void requireUserRole(UserContext userContext) {
        if (userContext == null || !userContext.isAuthenticated()) {
            throw new SemoException.UnauthorizedException("Login required");
        }
        if (!userContext.isUser()) {
            throw new SemoException.ForbiddenException("USER role required");
        }
    }
}
