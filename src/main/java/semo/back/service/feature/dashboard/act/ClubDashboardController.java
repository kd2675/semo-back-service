package semo.back.service.feature.dashboard.act;

import auth.common.core.context.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.dashboard.biz.ClubDashboardService;
import semo.back.service.feature.dashboard.vo.ClubDashboardEditorResponse;
import semo.back.service.feature.dashboard.vo.ClubDashboardWidgetResponse;
import semo.back.service.feature.dashboard.vo.UpdateClubDashboardLayoutRequest;
import web.common.core.response.base.dto.ResponseDataDTO;

import java.util.List;

@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}")
@RequiredArgsConstructor
public class ClubDashboardController {
    private final ClubDashboardService clubDashboardService;

    @GetMapping("/dashboard/widgets")
    public ResponseDataDTO<List<ClubDashboardWidgetResponse>> getDashboardWidgets(
            @PathVariable Long clubId,
            @RequestParam(required = false) String scope,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        String userKey = requireUserKey(userContext);
        return ResponseDataDTO.of(
                clubDashboardService.getDashboardWidgets(clubId, userKey, scope),
                "클럽 홈 위젯 조회 성공"
        );
    }

    @GetMapping("/admin/dashboard/widgets/editor")
    public ResponseDataDTO<ClubDashboardEditorResponse> getDashboardWidgetEditor(
            @PathVariable Long clubId,
            @RequestParam(required = false) String scope,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        String userKey = requireUserKey(userContext);
        return ResponseDataDTO.of(
                clubDashboardService.getDashboardWidgetEditor(clubId, userKey, scope),
                "클럽 홈 위젯 편집 정보 조회 성공"
        );
    }

    @PutMapping("/admin/dashboard/widgets/layout")
    public ResponseDataDTO<ClubDashboardEditorResponse> updateDashboardWidgetLayout(
            @PathVariable Long clubId,
            @RequestBody UpdateClubDashboardLayoutRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        String userKey = requireUserKey(userContext);
        return ResponseDataDTO.of(
                clubDashboardService.updateDashboardWidgetLayout(clubId, userKey, request),
                "클럽 홈 위젯 레이아웃 저장 성공"
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
