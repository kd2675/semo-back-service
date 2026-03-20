package semo.back.service.feature.notice.act;

import auth.common.core.context.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.notice.biz.ClubBoardFeedService;
import semo.back.service.feature.notice.biz.ClubNoticeService;
import semo.back.service.feature.notice.vo.ClubNoticeDetailResponse;
import semo.back.service.feature.notice.vo.ClubNoticeFeedResponse;
import semo.back.service.feature.notice.vo.ClubNoticeUpsertResponse;
import semo.back.service.feature.notice.vo.UpsertClubNoticeRequest;
import web.common.core.response.base.dto.ResponseDataDTO;

@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}/board/notices")
@RequiredArgsConstructor
public class ClubNoticeController {
    private final ClubBoardFeedService clubBoardFeedService;
    private final ClubNoticeService clubNoticeService;

    @GetMapping
    public ResponseDataDTO<ClubNoticeFeedResponse> getNoticeFeed(
            @PathVariable Long clubId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "false") boolean pinnedOnly,
            @RequestParam(required = false) Long cursorBoardItemId,
            @RequestParam(required = false) Integer size,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        String userKey = requireUserKey(userContext);
        ClubNoticeFeedResponse feed = clubBoardFeedService.getBoardFeed(
                clubId,
                userKey,
                query,
                pinnedOnly,
                cursorBoardItemId,
                size
        );
        return ResponseDataDTO.of(feed, "게시판 피드 조회 성공");
    }

    @GetMapping("/{noticeId}")
    public ResponseDataDTO<ClubNoticeDetailResponse> getNoticeDetail(
            @PathVariable Long clubId,
            @PathVariable Long noticeId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubNoticeService.getNoticeDetail(clubId, noticeId, requireUserKey(userContext)),
                "게시글 상세 조회 성공"
        );
    }

    @PostMapping
    public ResponseDataDTO<ClubNoticeUpsertResponse> createNotice(
            @PathVariable Long clubId,
            @Valid @RequestBody UpsertClubNoticeRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubNoticeService.createNotice(clubId, requireUserKey(userContext), request),
                "게시글 작성 성공"
        );
    }

    @PutMapping("/{noticeId}")
    public ResponseDataDTO<ClubNoticeUpsertResponse> updateNotice(
            @PathVariable Long clubId,
            @PathVariable Long noticeId,
            @Valid @RequestBody UpsertClubNoticeRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubNoticeService.updateNotice(clubId, noticeId, requireUserKey(userContext), request),
                "게시글 수정 성공"
        );
    }

    @DeleteMapping("/{noticeId}")
    public ResponseDataDTO<Void> deleteNotice(
            @PathVariable Long clubId,
            @PathVariable Long noticeId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        clubNoticeService.deleteNotice(clubId, noticeId, requireUserKey(userContext));
        return ResponseDataDTO.of(null, "게시글 삭제 성공");
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
