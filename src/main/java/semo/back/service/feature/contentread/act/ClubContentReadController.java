package semo.back.service.feature.contentread.act;

import auth.common.core.context.RequirePrincipalRole;
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
import web.common.core.response.base.dto.ResponseDataDTO;

@RequirePrincipalRole
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
        return ResponseDataDTO.of(
                clubContentReadService.getBoardItemReadStatus(clubId, boardItemId, requireUserKey(userContext)),
                "게시판 읽음 현황 조회 성공"
        );
    }

    private String requireUserKey(UserContext userContext) {
        if (userContext == null || !StringUtils.hasText(userContext.getUserKey())) {
            throw new SemoException.UnauthorizedException("Login required");
        }
        return userContext.getUserKey();
    }

}
