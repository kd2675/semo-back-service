package semo.back.service.feature.notice.act;

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
import semo.back.service.feature.notice.biz.ClubNoticePermissionService;
import semo.back.service.feature.notice.vo.ClubAdminNoticeSettingsResponse;
import semo.back.service.feature.notice.vo.UpdateClubAdminNoticeSettingsRequest;
import web.common.core.response.base.dto.ResponseDataDTO;

@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}")
@RequiredArgsConstructor
public class ClubNoticeAdminController {
    private final ClubNoticePermissionService clubNoticePermissionService;

    @GetMapping("/admin/more/notices")
    public ResponseDataDTO<ClubAdminNoticeSettingsResponse> getAdminNoticeSettings(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubNoticePermissionService.getAdminSettings(clubId, requireUserKey(userContext)),
                "공지 권한 설정 조회 성공"
        );
    }

    @PutMapping("/admin/more/notices")
    public ResponseDataDTO<ClubAdminNoticeSettingsResponse> updateAdminNoticeSettings(
            @PathVariable Long clubId,
            @Valid @RequestBody UpdateClubAdminNoticeSettingsRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubNoticePermissionService.updateAdminSettings(clubId, requireUserKey(userContext), request),
                "공지 권한 설정 저장 성공"
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
