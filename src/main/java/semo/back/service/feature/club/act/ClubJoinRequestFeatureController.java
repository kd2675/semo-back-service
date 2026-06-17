package semo.back.service.feature.club.act;

import auth.common.core.context.RequirePrincipalRole;
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
import semo.back.service.feature.club.vo.ClubJoinActionResponse;
import semo.back.service.feature.club.vo.ClubJoinRequestInboxResponse;
import semo.back.service.feature.club.vo.ReviewClubJoinRequestRequest;
import web.common.core.response.base.dto.ResponseDataDTO;

@RequirePrincipalRole
@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}")
@RequiredArgsConstructor
public class ClubJoinRequestFeatureController {
    private final ClubJoinRequestService clubJoinRequestService;

    @GetMapping("/more/join-requests")
    public ResponseDataDTO<ClubJoinRequestInboxResponse> getJoinRequestInbox(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubJoinRequestService.getJoinRequestInbox(clubId, requireUserKey(userContext)),
                "가입 신청 대기열 조회 성공"
        );
    }

    @GetMapping("/admin/more/join-requests")
    public ResponseDataDTO<ClubJoinRequestInboxResponse> getAdminJoinRequestInbox(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubJoinRequestService.getAdminJoinRequestInbox(clubId, requireUserKey(userContext)),
                "관리자 가입 신청 대기열 조회 성공"
        );
    }

    @PutMapping("/admin/more/join-requests/{clubJoinRequestId}/review")
    public ResponseDataDTO<ClubJoinActionResponse> reviewJoinRequest(
            @PathVariable Long clubId,
            @PathVariable Long clubJoinRequestId,
            @Valid @RequestBody ReviewClubJoinRequestRequest request,
            UserContext userContext
    ) {
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

}
