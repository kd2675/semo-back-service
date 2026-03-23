package semo.back.service.feature.activity.act;

import auth.common.core.context.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.activity.biz.ClubActivityService;
import semo.back.service.feature.activity.vo.ClubAdminActivityFeedResponse;
import web.common.core.response.base.dto.ResponseDataDTO;

@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}/admin/activity")
@RequiredArgsConstructor
public class ClubActivityAdminController {
    private final ClubActivityService clubActivityService;

    @GetMapping
    public ResponseDataDTO<ClubAdminActivityFeedResponse> getRecentAdminActivities(
            @PathVariable Long clubId,
            @RequestParam(required = false) String cursorCreatedAt,
            @RequestParam(required = false) Long cursorActivityId,
            @RequestParam(required = false) Integer size,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubActivityService.getRecentAdminActivities(
                        clubId,
                        requireUserKey(userContext),
                        cursorCreatedAt,
                        cursorActivityId,
                        size
                ),
                "최근 활동 조회 성공"
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
