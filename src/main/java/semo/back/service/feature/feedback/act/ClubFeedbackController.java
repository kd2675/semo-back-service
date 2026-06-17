package semo.back.service.feature.feedback.act;

import auth.common.core.context.RequirePrincipalRole;
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
import semo.back.service.feature.feedback.biz.ClubFeedbackService;
import semo.back.service.feature.feedback.vo.ClubAdminFeedbackResponse;
import semo.back.service.feature.feedback.vo.ClubFeedbackDetailResponse;
import semo.back.service.feature.feedback.vo.ClubFeedbackHomeResponse;
import semo.back.service.feature.feedback.vo.CreateClubFeedbackRequest;
import semo.back.service.feature.feedback.vo.UpdateClubAdminFeedbackRequest;
import web.common.core.response.base.dto.ResponseDataDTO;

@RequirePrincipalRole
@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}")
@RequiredArgsConstructor
public class ClubFeedbackController {
    private final ClubFeedbackService clubFeedbackService;

    @GetMapping("/more/feedback")
    public ResponseDataDTO<ClubFeedbackHomeResponse> getFeedbackHome(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubFeedbackService.getFeedbackHome(clubId, requireUserKey(userContext)),
                "피드백 홈 조회 성공"
        );
    }

    @GetMapping("/more/feedback/{feedbackId}")
    public ResponseDataDTO<ClubFeedbackDetailResponse> getFeedbackDetail(
            @PathVariable Long clubId,
            @PathVariable Long feedbackId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubFeedbackService.getFeedbackDetail(clubId, feedbackId, requireUserKey(userContext)),
                "피드백 상세 조회 성공"
        );
    }

    @PostMapping("/more/feedback")
    public ResponseDataDTO<ClubFeedbackDetailResponse> createFeedback(
            @PathVariable Long clubId,
            @Valid @RequestBody CreateClubFeedbackRequest request,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubFeedbackService.createFeedback(clubId, requireUserKey(userContext), request),
                "피드백 등록 성공"
        );
    }

    @GetMapping("/admin/more/feedback")
    public ResponseDataDTO<ClubAdminFeedbackResponse> getAdminFeedback(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubFeedbackService.getAdminFeedback(clubId, requireUserKey(userContext)),
                "피드백 관리 목록 조회 성공"
        );
    }

    @GetMapping("/admin/more/feedback/{feedbackId}")
    public ResponseDataDTO<ClubFeedbackDetailResponse> getAdminFeedbackDetail(
            @PathVariable Long clubId,
            @PathVariable Long feedbackId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubFeedbackService.getAdminFeedbackDetail(clubId, feedbackId, requireUserKey(userContext)),
                "피드백 관리 상세 조회 성공"
        );
    }

    @PutMapping("/admin/more/feedback/{feedbackId}")
    public ResponseDataDTO<ClubFeedbackDetailResponse> updateAdminFeedback(
            @PathVariable Long clubId,
            @PathVariable Long feedbackId,
            @Valid @RequestBody UpdateClubAdminFeedbackRequest request,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubFeedbackService.updateAdminFeedback(clubId, feedbackId, requireUserKey(userContext), request),
                "피드백 관리 저장 성공"
        );
    }

    private String requireUserKey(UserContext userContext) {
        if (userContext == null || !StringUtils.hasText(userContext.getUserKey())) {
            throw new SemoException.UnauthorizedException("Login required");
        }
        return userContext.getUserKey();
    }

}
