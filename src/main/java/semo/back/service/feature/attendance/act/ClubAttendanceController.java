package semo.back.service.feature.attendance.act;

import auth.common.core.context.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.attendance.biz.ClubAttendanceService;
import semo.back.service.feature.attendance.vo.AttendanceTodayResponse;
import semo.back.service.feature.attendance.vo.ClubAdminAttendanceResponse;
import semo.back.service.feature.attendance.vo.ClubAttendanceResponse;
import web.common.core.response.base.dto.ResponseDataDTO;

@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}")
@RequiredArgsConstructor
public class ClubAttendanceController {
    private final ClubAttendanceService clubAttendanceService;

    @GetMapping("/more/attendance")
    public ResponseDataDTO<ClubAttendanceResponse> getAttendance(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubAttendanceService.getAttendance(clubId, requireUserKey(userContext)),
                "출석 조회 성공"
        );
    }

    @PostMapping("/more/attendance/check-in")
    public ResponseDataDTO<AttendanceTodayResponse> checkIn(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubAttendanceService.checkIn(clubId, requireUserKey(userContext)),
                "출석 체크 성공"
        );
    }

    @GetMapping("/admin/more/attendance")
    public ResponseDataDTO<ClubAdminAttendanceResponse> getAdminAttendance(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubAttendanceService.getAdminAttendance(clubId, requireUserKey(userContext)),
                "관리자 출석 조회 성공"
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
