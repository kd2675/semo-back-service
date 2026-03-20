package semo.back.service.feature.schedule.act;

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
import semo.back.service.feature.schedule.biz.ClubSchedulePermissionService;
import semo.back.service.feature.schedule.vo.ClubAdminScheduleSettingsResponse;
import semo.back.service.feature.schedule.vo.UpdateClubAdminScheduleSettingsRequest;
import web.common.core.response.base.dto.ResponseDataDTO;

@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}")
@RequiredArgsConstructor
public class ClubScheduleAdminController {
    private final ClubSchedulePermissionService clubSchedulePermissionService;

    @GetMapping("/admin/more/schedules")
    public ResponseDataDTO<ClubAdminScheduleSettingsResponse> getAdminScheduleSettings(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubSchedulePermissionService.getAdminSettings(clubId, requireUserKey(userContext)),
                "일정 권한 설정 조회 성공"
        );
    }

    @PutMapping("/admin/more/schedules")
    public ResponseDataDTO<ClubAdminScheduleSettingsResponse> updateAdminScheduleSettings(
            @PathVariable Long clubId,
            @Valid @RequestBody UpdateClubAdminScheduleSettingsRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubSchedulePermissionService.updateAdminSettings(clubId, requireUserKey(userContext), request),
                "일정 권한 설정 저장 성공"
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
