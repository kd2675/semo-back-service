package semo.back.service.feature.club.act;

import auth.common.core.context.RequirePrincipalRole;
import auth.common.core.context.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.club.biz.ClubJoinRequestService;
import semo.back.service.feature.club.vo.ClubJoinActionResponse;
import semo.back.service.feature.club.vo.SubmitClubJoinRequestRequest;
import web.common.core.response.base.dto.ResponseDataDTO;

@RequirePrincipalRole
@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}/join-requests")
@RequiredArgsConstructor
public class ClubJoinRequestController {
    private final ClubJoinRequestService clubJoinRequestService;

    @PostMapping
    public ResponseDataDTO<ClubJoinActionResponse> submitJoinRequest(
            @PathVariable Long clubId,
            @Valid @RequestBody(required = false) SubmitClubJoinRequestRequest request,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubJoinRequestService.submitJoinRequest(clubId, requireUserKey(userContext), request),
                "가입 신청 처리 성공"
        );
    }

    @DeleteMapping("/me")
    public ResponseDataDTO<ClubJoinActionResponse> cancelMyJoinRequest(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubJoinRequestService.cancelMyJoinRequest(clubId, requireUserKey(userContext)),
                "가입 신청 취소 성공"
        );
    }

    private String requireUserKey(UserContext userContext) {
        if (userContext == null || !StringUtils.hasText(userContext.getUserKey())) {
            throw new SemoException.UnauthorizedException("Login required");
        }
        return userContext.getUserKey();
    }

}
