package semo.back.service.feature.poll.act;

import auth.common.core.context.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.poll.biz.ClubPollPermissionService;
import semo.back.service.feature.poll.vo.ClubAdminPollSettingsResponse;
import semo.back.service.feature.poll.vo.UpdateClubAdminPollSettingsRequest;
import web.common.core.response.base.dto.ResponseDataDTO;

@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}")
@RequiredArgsConstructor
public class ClubPollAdminController {
    private final ClubPollPermissionService clubPollPermissionService;

    @GetMapping("/admin/more/polls")
    public ResponseDataDTO<ClubAdminPollSettingsResponse> getAdminPollSettings(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubPollPermissionService.getAdminSettings(clubId, requireUserKey(userContext)),
                "투표 권한 설정 조회 성공"
        );
    }

    @PutMapping("/admin/more/polls")
    public ResponseDataDTO<ClubAdminPollSettingsResponse> updateAdminPollSettings(
            @PathVariable Long clubId,
            @Valid @RequestBody UpdateClubAdminPollSettingsRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubPollPermissionService.updateAdminSettings(clubId, requireUserKey(userContext), request),
                "투표 권한 설정 저장 성공"
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
