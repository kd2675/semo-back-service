package semo.back.service.feature.position.act;

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
import org.springframework.web.bind.annotation.RestController;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.position.biz.ClubPositionService;
import semo.back.service.feature.position.vo.ClubAdminRoleManagementResponse;
import semo.back.service.feature.position.vo.ClubPositionDetailResponse;
import semo.back.service.feature.position.vo.CreateClubPositionRequest;
import semo.back.service.feature.position.vo.UpdateClubPositionRequest;
import web.common.core.response.base.dto.ResponseDataDTO;

@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}/admin/more/roles")
@RequiredArgsConstructor
public class ClubPositionAdminController {
    private final ClubPositionService clubPositionService;

    @GetMapping
    public ResponseDataDTO<ClubAdminRoleManagementResponse> getRoleManagement(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubPositionService.getRoleManagement(clubId, requireUserKey(userContext)),
                "직책관리 조회 성공"
        );
    }

    @PostMapping
    public ResponseDataDTO<ClubPositionDetailResponse> createPosition(
            @PathVariable Long clubId,
            @Valid @RequestBody CreateClubPositionRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubPositionService.createPosition(clubId, requireUserKey(userContext), request),
                "직책 생성 성공"
        );
    }

    @GetMapping("/{clubPositionId}")
    public ResponseDataDTO<ClubPositionDetailResponse> getPositionDetail(
            @PathVariable Long clubId,
            @PathVariable Long clubPositionId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubPositionService.getPositionDetail(clubId, clubPositionId, requireUserKey(userContext)),
                "직책 상세 조회 성공"
        );
    }

    @PutMapping("/{clubPositionId}")
    public ResponseDataDTO<ClubPositionDetailResponse> updatePosition(
            @PathVariable Long clubId,
            @PathVariable Long clubPositionId,
            @Valid @RequestBody UpdateClubPositionRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubPositionService.updatePosition(clubId, clubPositionId, requireUserKey(userContext), request),
                "직책 수정 성공"
        );
    }

    @DeleteMapping("/{clubPositionId}")
    public ResponseDataDTO<Boolean> deletePosition(
            @PathVariable Long clubId,
            @PathVariable Long clubPositionId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        clubPositionService.deletePosition(clubId, clubPositionId, requireUserKey(userContext));
        return ResponseDataDTO.of(true, "직책 삭제 성공");
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
