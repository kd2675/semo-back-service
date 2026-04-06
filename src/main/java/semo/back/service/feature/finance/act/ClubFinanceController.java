package semo.back.service.feature.finance.act;

import auth.common.core.context.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.finance.biz.ClubFinanceService;
import semo.back.service.feature.finance.vo.ClubAdminFinanceHomeResponse;
import semo.back.service.feature.finance.vo.ClubAdminFinanceObligationDetailResponse;
import semo.back.service.feature.finance.vo.ClubAdminFinanceObligationFeedResponse;
import semo.back.service.feature.finance.vo.ClubFinanceExpenseFeedResponse;
import semo.back.service.feature.finance.vo.ClubFinanceExpenseResponse;
import semo.back.service.feature.finance.vo.ClubFinanceHomeResponse;
import semo.back.service.feature.finance.vo.ClubFinancePaymentResponse;
import semo.back.service.feature.finance.vo.ClubFinanceRequestFeedResponse;
import semo.back.service.feature.finance.vo.ClubFinanceRequestResponse;
import semo.back.service.feature.finance.vo.CreateFinanceExpenseRequest;
import semo.back.service.feature.finance.vo.CreateFinanceObligationRequest;
import semo.back.service.feature.finance.vo.CreateFinanceObligationResponse;
import semo.back.service.feature.finance.vo.CreateFinanceRequestRequest;
import semo.back.service.feature.finance.vo.ReviewFinanceRequestRequest;
import semo.back.service.feature.finance.vo.UpdateFinancePaymentStatusRequest;
import web.common.core.response.base.dto.ResponseDataDTO;

@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}")
@RequiredArgsConstructor
public class ClubFinanceController {
    private final ClubFinanceService clubFinanceService;

    @GetMapping("/more/finance")
    public ResponseDataDTO<ClubFinanceHomeResponse> getFinance(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubFinanceService.getFinance(clubId, requireUserKey(userContext)),
                "재정 조회 성공"
        );
    }

    @GetMapping("/more/finance/requests")
    public ResponseDataDTO<ClubFinanceRequestFeedResponse> getMyFinanceRequests(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubFinanceService.getMyFinanceRequests(clubId, requireUserKey(userContext)),
                "내 재정 요청 조회 성공"
        );
    }

    @PostMapping("/more/finance/requests")
    public ResponseDataDTO<ClubFinanceRequestResponse> createFinanceRequest(
            @PathVariable Long clubId,
            @Valid @RequestBody CreateFinanceRequestRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubFinanceService.createFinanceRequest(clubId, requireUserKey(userContext), request),
                "재정 요청 제출 성공"
        );
    }

    @GetMapping("/admin/more/finance")
    public ResponseDataDTO<ClubAdminFinanceHomeResponse> getAdminFinance(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubFinanceService.getAdminFinance(clubId, requireUserKey(userContext)),
                "관리자 재정 조회 성공"
        );
    }

    @GetMapping("/admin/more/finance/obligations")
    public ResponseDataDTO<ClubAdminFinanceObligationFeedResponse> getAdminFinanceObligations(
            @PathVariable Long clubId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String obligationFilter,
            @RequestParam(required = false) Long cursorObligationId,
            @RequestParam(required = false) Integer size,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubFinanceService.getAdminFinanceObligations(
                        clubId,
                        requireUserKey(userContext),
                        query,
                        obligationFilter,
                        cursorObligationId,
                        size
                ),
                "관리자 재정 항목 목록 조회 성공"
        );
    }

    @GetMapping("/admin/more/finance/requests")
    public ResponseDataDTO<ClubFinanceRequestFeedResponse> getAdminFinanceRequests(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubFinanceService.getAdminFinanceRequests(clubId, requireUserKey(userContext)),
                "관리자 재정 요청 조회 성공"
        );
    }

    @PostMapping("/admin/more/finance/requests/{requestId}/review")
    public ResponseDataDTO<ClubFinanceRequestResponse> reviewFinanceRequest(
            @PathVariable Long clubId,
            @PathVariable Long requestId,
            @Valid @RequestBody ReviewFinanceRequestRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubFinanceService.reviewFinanceRequest(clubId, requestId, requireUserKey(userContext), request),
                "재정 요청 검토 성공"
        );
    }

    @GetMapping("/admin/more/finance/expenses")
    public ResponseDataDTO<ClubFinanceExpenseFeedResponse> getAdminFinanceExpenses(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubFinanceService.getAdminFinanceExpenses(clubId, requireUserKey(userContext)),
                "관리자 지출 조회 성공"
        );
    }

    @PostMapping("/admin/more/finance/expenses")
    public ResponseDataDTO<ClubFinanceExpenseResponse> createFinanceExpense(
            @PathVariable Long clubId,
            @Valid @RequestBody CreateFinanceExpenseRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubFinanceService.createFinanceExpense(clubId, requireUserKey(userContext), request),
                "지출 입력 성공"
        );
    }

    @GetMapping("/admin/more/finance/obligations/{obligationId}/payments")
    public ResponseDataDTO<ClubAdminFinanceObligationDetailResponse> getAdminFinanceObligationDetail(
            @PathVariable Long clubId,
            @PathVariable Long obligationId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubFinanceService.getAdminFinanceObligationDetail(clubId, obligationId, requireUserKey(userContext)),
                "관리자 재정 청구 상세 조회 성공"
        );
    }

    @PostMapping("/admin/more/finance/obligations")
    public ResponseDataDTO<CreateFinanceObligationResponse> createObligation(
            @PathVariable Long clubId,
            @Valid @RequestBody CreateFinanceObligationRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubFinanceService.createObligation(clubId, requireUserKey(userContext), request),
                "재정 항목 발행 성공"
        );
    }

    @PatchMapping("/admin/more/finance/payments/{paymentId}/status")
    public ResponseDataDTO<ClubFinancePaymentResponse> updatePaymentStatus(
            @PathVariable Long clubId,
            @PathVariable Long paymentId,
            @Valid @RequestBody UpdateFinancePaymentStatusRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubFinanceService.updatePaymentStatus(clubId, paymentId, requireUserKey(userContext), request),
                "재정 상태 변경 성공"
        );
    }

    @DeleteMapping("/admin/more/finance/obligations/{obligationId}")
    public ResponseDataDTO<Void> deleteObligation(
            @PathVariable Long clubId,
            @PathVariable Long obligationId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        clubFinanceService.deleteObligation(clubId, obligationId, requireUserKey(userContext));
        return ResponseDataDTO.of(null, "재정 항목 삭제 성공");
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
