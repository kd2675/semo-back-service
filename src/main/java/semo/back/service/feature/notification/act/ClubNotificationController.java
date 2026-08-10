package semo.back.service.feature.notification.act;

import auth.common.core.context.RequirePrincipalRole;
import auth.common.core.context.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.notification.biz.ClubNotificationService;
import semo.back.service.feature.notification.vo.ClubNotificationFeedResponse;
import semo.back.service.feature.notification.vo.ClubNotificationReadResponse;
import semo.back.service.feature.notification.vo.ClubNotificationSummaryResponse;
import web.common.core.response.base.dto.ResponseDataDTO;

@RequirePrincipalRole
@RestController
@RequestMapping("/api/semo/v1/notifications")
@RequiredArgsConstructor
public class ClubNotificationController {
    private final ClubNotificationService clubNotificationService;

    @GetMapping("/summary")
    public ResponseDataDTO<ClubNotificationSummaryResponse> getSummary(UserContext userContext) {
        return ResponseDataDTO.of(
                clubNotificationService.getSummary(requireUserKey(userContext)),
                "알림 요약 조회 성공"
        );
    }

    @GetMapping
    public ResponseDataDTO<ClubNotificationFeedResponse> getNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(required = false) Integer size,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubNotificationService.getNotifications(requireUserKey(userContext), unreadOnly, beforeId, size),
                "알림함 조회 성공"
        );
    }

    @PutMapping("/{notificationId}/read")
    public ResponseDataDTO<ClubNotificationReadResponse> markRead(
            @PathVariable Long notificationId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubNotificationService.markRead(requireUserKey(userContext), notificationId),
                "알림 읽음 처리 성공"
        );
    }

    @PutMapping("/read-all")
    public ResponseDataDTO<ClubNotificationReadResponse> markAllRead(UserContext userContext) {
        return ResponseDataDTO.of(
                clubNotificationService.markAllRead(requireUserKey(userContext)),
                "알림 전체 읽음 처리 성공"
        );
    }

    private String requireUserKey(UserContext userContext) {
        if (userContext == null || !StringUtils.hasText(userContext.getUserKey())) {
            throw new SemoException.UnauthorizedException("Login required");
        }
        return userContext.getUserKey();
    }
}
