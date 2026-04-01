package semo.back.service.feature.dues.act;

import auth.common.core.context.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.dues.biz.ClubDuesService;
import semo.back.service.feature.dues.vo.ClubAdminDuesHomeResponse;
import semo.back.service.feature.dues.vo.ClubDuesHomeResponse;
import semo.back.service.feature.dues.vo.ClubDuesInvoiceResponse;
import semo.back.service.feature.dues.vo.CreateClubDuesChargeRequest;
import semo.back.service.feature.dues.vo.CreateClubDuesChargeResponse;
import semo.back.service.feature.dues.vo.UpdateClubDuesPaymentStatusRequest;
import web.common.core.response.base.dto.ResponseDataDTO;

@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}")
@RequiredArgsConstructor
public class ClubDuesController {
    private final ClubDuesService clubDuesService;

    @GetMapping("/more/dues")
    public ResponseDataDTO<ClubDuesHomeResponse> getDues(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubDuesService.getDues(clubId, requireUserKey(userContext)),
                "회비 조회 성공"
        );
    }

    @GetMapping("/admin/more/dues")
    public ResponseDataDTO<ClubAdminDuesHomeResponse> getAdminDues(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubDuesService.getAdminDues(clubId, requireUserKey(userContext)),
                "관리자 회비 조회 성공"
        );
    }

    @PostMapping("/admin/more/dues/charges")
    public ResponseDataDTO<CreateClubDuesChargeResponse> createCharge(
            @PathVariable Long clubId,
            @Valid @RequestBody CreateClubDuesChargeRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubDuesService.createCharge(clubId, requireUserKey(userContext), request),
                "회비 항목 발행 성공"
        );
    }

    @PutMapping("/admin/more/dues/invoices/{invoiceId}/payment-status")
    public ResponseDataDTO<ClubDuesInvoiceResponse> updatePaymentStatus(
            @PathVariable Long clubId,
            @PathVariable Long invoiceId,
            @Valid @RequestBody UpdateClubDuesPaymentStatusRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubDuesService.updatePaymentStatus(clubId, invoiceId, requireUserKey(userContext), request),
                "회비 상태 변경 성공"
        );
    }

    @DeleteMapping("/admin/more/dues/charges/{chargeId}")
    public ResponseDataDTO<Void> deleteCharge(
            @PathVariable Long clubId,
            @PathVariable Long chargeId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        clubDuesService.deleteCharge(clubId, chargeId, requireUserKey(userContext));
        return ResponseDataDTO.of(null, "회비 항목 삭제 성공");
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
