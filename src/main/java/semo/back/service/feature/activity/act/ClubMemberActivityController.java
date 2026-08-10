package semo.back.service.feature.activity.act;

import auth.common.core.context.RequirePrincipalRole;
import auth.common.core.context.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.activity.biz.ClubMemberActivityService;
import semo.back.service.feature.activity.vo.ClubMemberActivityResponse;
import web.common.core.response.base.dto.ResponseDataDTO;

@RequirePrincipalRole
@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}")
@RequiredArgsConstructor
public class ClubMemberActivityController {
    private final ClubMemberActivityService clubMemberActivityService;

    @GetMapping("/profile/activity")
    public ResponseDataDTO<ClubMemberActivityResponse> getMemberActivity(
            @PathVariable Long clubId,
            @RequestParam(required = false) String cursorCreatedAt,
            @RequestParam(required = false) Long cursorActivityId,
            @RequestParam(required = false) Integer size,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubMemberActivityService.getMemberActivity(
                        clubId,
                        requireUserKey(userContext),
                        cursorCreatedAt,
                        cursorActivityId,
                        size
                ),
                "내 활동 기록 조회 성공"
        );
    }

    private String requireUserKey(UserContext userContext) {
        if (userContext == null || !StringUtils.hasText(userContext.getUserKey())) {
            throw new SemoException.UnauthorizedException("Login required");
        }
        return userContext.getUserKey();
    }
}
