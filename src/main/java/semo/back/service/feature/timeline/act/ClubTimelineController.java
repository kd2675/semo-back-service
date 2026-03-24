package semo.back.service.feature.timeline.act;

import auth.common.core.context.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.timeline.biz.ClubTimelineService;
import semo.back.service.feature.timeline.vo.ClubAdminTimelineResponse;
import semo.back.service.feature.timeline.vo.ClubTimelineResponse;
import web.common.core.response.base.dto.ResponseDataDTO;

@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}")
@RequiredArgsConstructor
public class ClubTimelineController {
    private final ClubTimelineService clubTimelineService;

    @GetMapping("/more/timeline")
    public ResponseDataDTO<ClubTimelineResponse> getTimeline(
            @PathVariable Long clubId,
            @RequestParam(required = false) String cursorCreatedAt,
            @RequestParam(required = false) Long cursorActivityId,
            @RequestParam(required = false) Integer size,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubTimelineService.getTimeline(
                        clubId,
                        requireUserKey(userContext),
                        cursorCreatedAt,
                        cursorActivityId,
                        size
                ),
                "타임라인 조회 성공"
        );
    }

    @GetMapping("/admin/more/timeline")
    public ResponseDataDTO<ClubAdminTimelineResponse> getAdminTimeline(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubTimelineService.getAdminTimeline(clubId, requireUserKey(userContext)),
                "타임라인 설정 조회 성공"
        );
    }

    @PutMapping("/admin/more/timeline")
    public ResponseDataDTO<ClubAdminTimelineResponse> updateAdminTimeline(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubTimelineService.updateAdminTimeline(clubId, requireUserKey(userContext)),
                "타임라인 설정 저장 성공"
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
