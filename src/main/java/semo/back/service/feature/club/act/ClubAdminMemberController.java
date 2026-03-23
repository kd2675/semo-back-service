package semo.back.service.feature.club.act;

import auth.common.core.context.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.club.biz.ClubAdminMemberService;
import semo.back.service.feature.club.vo.ClubAdminMemberResponse;
import semo.back.service.feature.club.vo.ClubAdminMembersResponse;
import semo.back.service.feature.club.vo.UpdateClubAdminMemberRoleRequest;
import semo.back.service.feature.club.vo.UpdateClubAdminMemberStatusRequest;
import semo.back.service.feature.position.vo.UpdateClubMemberPositionsRequest;
import web.common.core.response.base.dto.ResponseDataDTO;

@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}/admin/members")
@RequiredArgsConstructor
public class ClubAdminMemberController {
    private final ClubAdminMemberService clubAdminMemberService;

    @GetMapping
    public ResponseDataDTO<ClubAdminMembersResponse> getAdminMembers(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubAdminMemberService.getAdminMembers(clubId, requireUserKey(userContext)),
                "회원 목록 조회 성공"
        );
    }

    @PutMapping("/{clubMemberId}/role")
    public ResponseDataDTO<ClubAdminMemberResponse> updateMemberRole(
            @PathVariable Long clubId,
            @PathVariable Long clubMemberId,
            @Valid @RequestBody UpdateClubAdminMemberRoleRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubAdminMemberService.updateMemberRole(clubId, clubMemberId, requireUserKey(userContext), request),
                "회원 역할 변경 성공"
        );
    }

    @PutMapping("/{clubMemberId}/status")
    public ResponseDataDTO<ClubAdminMemberResponse> updateMemberStatus(
            @PathVariable Long clubId,
            @PathVariable Long clubMemberId,
            @Valid @RequestBody UpdateClubAdminMemberStatusRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubAdminMemberService.updateMemberStatus(clubId, clubMemberId, requireUserKey(userContext), request),
                "회원 상태 변경 성공"
        );
    }

    @PostMapping("/{clubMemberId}/approve")
    public ResponseDataDTO<ClubAdminMemberResponse> approvePendingMember(
            @PathVariable Long clubId,
            @PathVariable Long clubMemberId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubAdminMemberService.approvePendingMember(clubId, clubMemberId, requireUserKey(userContext)),
                "가입 승인 성공"
        );
    }

    @PutMapping("/{clubMemberId}/positions")
    public ResponseDataDTO<ClubAdminMemberResponse> updateMemberPositions(
            @PathVariable Long clubId,
            @PathVariable Long clubMemberId,
            @RequestBody UpdateClubMemberPositionsRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubAdminMemberService.updateMemberPositions(clubId, clubMemberId, requireUserKey(userContext), request),
                "회원 직책 변경 성공"
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
