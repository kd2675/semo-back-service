package semo.back.service.feature.memberdirectory.act;

import auth.common.core.context.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.memberdirectory.biz.ClubMemberDirectoryService;
import semo.back.service.feature.memberdirectory.vo.ClubAdminMemberDirectoryResponse;
import semo.back.service.feature.memberdirectory.vo.ClubMemberDirectoryResponse;
import semo.back.service.feature.memberdirectory.vo.UpdateClubAdminMemberDirectorySettingsRequest;
import web.common.core.response.base.dto.ResponseDataDTO;

@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}")
@RequiredArgsConstructor
public class ClubMemberDirectoryController {
    private final ClubMemberDirectoryService clubMemberDirectoryService;

    @GetMapping("/more/members")
    public ResponseDataDTO<ClubMemberDirectoryResponse> getMemberDirectory(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubMemberDirectoryService.getMemberDirectory(clubId, requireUserKey(userContext)),
                "회원 디렉터리 조회 성공"
        );
    }

    @GetMapping("/admin/more/members")
    public ResponseDataDTO<ClubAdminMemberDirectoryResponse> getAdminMemberDirectory(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubMemberDirectoryService.getAdminMemberDirectory(clubId, requireUserKey(userContext)),
                "회원 디렉터리 설정 조회 성공"
        );
    }

    @PutMapping("/admin/more/members")
    public ResponseDataDTO<ClubAdminMemberDirectoryResponse> updateAdminMemberDirectory(
            @PathVariable Long clubId,
            @RequestBody UpdateClubAdminMemberDirectorySettingsRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubMemberDirectoryService.updateAdminMemberDirectory(clubId, requireUserKey(userContext), request),
                "회원 디렉터리 설정 저장 성공"
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
