package semo.back.service.feature.club.act;

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
import semo.back.service.feature.club.biz.ClubJoinRequestService;
import semo.back.service.feature.club.vo.ClubAdminJoinRequestsResponse;
import semo.back.service.feature.club.vo.ClubJoinActionResponse;
import semo.back.service.feature.club.vo.ReviewClubJoinRequestRequest;
import web.common.core.response.base.dto.ResponseDataDTO;

@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}/admin/join-requests")
@RequiredArgsConstructor
public class ClubAdminJoinRequestController {
    private final ClubJoinRequestService clubJoinRequestService;

    @GetMapping
    public ResponseDataDTO<ClubAdminJoinRequestsResponse> getAdminJoinRequests(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubJoinRequestService.getAdminJoinRequests(clubId, requireUserKey(userContext)),
                "가입 신청 목록 조회 성공"
        );
    }

    @PutMapping("/{clubJoinRequestId}/review")
    public ResponseDataDTO<ClubJoinActionResponse> reviewJoinRequest(
            @PathVariable Long clubId,
            @PathVariable Long clubJoinRequestId,
            @Valid @RequestBody ReviewClubJoinRequestRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubJoinRequestService.reviewJoinRequest(
                        clubId,
                        clubJoinRequestId,
                        requireUserKey(userContext),
                        request
                ),
                "가입 신청 검토 성공"
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
