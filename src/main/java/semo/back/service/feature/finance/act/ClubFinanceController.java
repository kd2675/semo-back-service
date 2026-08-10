package semo.back.service.feature.finance.act;

import auth.common.core.context.RequirePrincipalRole;
import auth.common.core.context.UserContext;
import java.nio.charset.StandardCharsets;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.finance.biz.ClubFinanceService;
import semo.back.service.feature.finance.biz.ClubFinanceExportService;
import semo.back.service.feature.finance.biz.ClubFinanceOperationsService;
import semo.back.service.feature.finance.vo.ClubAdminFinanceHomeResponse;
import semo.back.service.feature.finance.vo.ClubAdminFinanceObligationDetailResponse;
import semo.back.service.feature.finance.vo.ClubAdminFinanceObligationFeedResponse;
import semo.back.service.feature.finance.vo.ClubFinanceExpenseFeedResponse;
import semo.back.service.feature.finance.vo.ClubFinanceExpenseResponse;
import semo.back.service.feature.finance.vo.ClubFinanceHomeResponse;
import semo.back.service.feature.finance.vo.ClubFinanceOperationsResponse;
import semo.back.service.feature.finance.vo.ClubFinancePaymentResponse;
import semo.back.service.feature.finance.vo.ClubFinanceRequestFeedResponse;
import semo.back.service.feature.finance.vo.ClubFinanceRequestResponse;
import semo.back.service.feature.finance.vo.CorrectFinanceExpenseRequest;
import semo.back.service.feature.finance.vo.CreateFinanceExpenseRequest;
import semo.back.service.feature.finance.vo.CreateFinanceObligationRequest;
import semo.back.service.feature.finance.vo.CreateFinanceObligationResponse;
import semo.back.service.feature.finance.vo.CreateFinancePeriodRequest;
import semo.back.service.feature.finance.vo.CreateFinanceRequestRequest;
import semo.back.service.feature.finance.vo.ReviewFinanceRequestRequest;
import semo.back.service.feature.finance.vo.UpdateFinancePaymentStatusRequest;
import semo.back.service.feature.finance.vo.FinanceAccountResponse;
import semo.back.service.feature.finance.vo.FinancePeriodResponse;
import semo.back.service.feature.finance.vo.FinanceExpenseRevisionResponse;
import semo.back.service.feature.finance.vo.UpsertFinanceAccountRequest;
import semo.back.service.feature.finance.vo.UpsertFinanceBudgetRequest;
import semo.back.service.feature.finance.vo.VoidFinanceExpenseRequest;
import web.common.core.response.base.dto.ResponseDataDTO;

@RequirePrincipalRole
@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}")
@RequiredArgsConstructor
public class ClubFinanceController {
    private final ClubFinanceService clubFinanceService;
    private final ClubFinanceExportService clubFinanceExportService;
    private final ClubFinanceOperationsService clubFinanceOperationsService;

    @GetMapping("/more/finance")
    public ResponseDataDTO<ClubFinanceHomeResponse> getFinance(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
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
        return ResponseDataDTO.of(
                clubFinanceService.getAdminFinance(clubId, requireUserKey(userContext)),
                "관리자 재정 조회 성공"
        );
    }

    @GetMapping("/admin/more/finance/operations")
    public ResponseDataDTO<ClubFinanceOperationsResponse> getFinanceOperations(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubFinanceOperationsService.getOperations(clubId, requireUserKey(userContext)),
                "재정 운영 설정 조회 성공"
        );
    }

    @GetMapping(value = "/admin/more/finance/export.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> exportFinanceCsv(
            @PathVariable Long clubId,
            @RequestParam(required = false) Long financePeriodId,
            UserContext userContext
    ) {
        ClubFinanceExportService.FinanceExportFile exportFile = clubFinanceExportService.exportCsv(
                clubId,
                financePeriodId,
                requireUserKey(userContext)
        );
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(exportFile.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(exportFile.content());
    }

    @PostMapping("/admin/more/finance/accounts")
    public ResponseDataDTO<FinanceAccountResponse> createFinanceAccount(
            @PathVariable Long clubId,
            @Valid @RequestBody UpsertFinanceAccountRequest request,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubFinanceOperationsService.createAccount(clubId, requireUserKey(userContext), request),
                "계좌·결제수단 등록 성공"
        );
    }

    @PutMapping("/admin/more/finance/accounts/{financeAccountId}")
    public ResponseDataDTO<FinanceAccountResponse> updateFinanceAccount(
            @PathVariable Long clubId,
            @PathVariable Long financeAccountId,
            @Valid @RequestBody UpsertFinanceAccountRequest request,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubFinanceOperationsService.updateAccount(
                        clubId,
                        financeAccountId,
                        requireUserKey(userContext),
                        request
                ),
                "계좌·결제수단 수정 성공"
        );
    }

    @DeleteMapping("/admin/more/finance/accounts/{financeAccountId}")
    public ResponseDataDTO<FinanceAccountResponse> deactivateFinanceAccount(
            @PathVariable Long clubId,
            @PathVariable Long financeAccountId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubFinanceOperationsService.deactivateAccount(
                        clubId,
                        financeAccountId,
                        requireUserKey(userContext)
                ),
                "계좌·결제수단 비활성화 성공"
        );
    }

    @PostMapping("/admin/more/finance/periods")
    public ResponseDataDTO<FinancePeriodResponse> createFinancePeriod(
            @PathVariable Long clubId,
            @Valid @RequestBody CreateFinancePeriodRequest request,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubFinanceOperationsService.createPeriod(clubId, requireUserKey(userContext), request),
                "재정 기간 생성 성공"
        );
    }

    @PutMapping("/admin/more/finance/periods/{financePeriodId}/budgets")
    public ResponseDataDTO<FinancePeriodResponse> upsertFinanceBudget(
            @PathVariable Long clubId,
            @PathVariable Long financePeriodId,
            @Valid @RequestBody UpsertFinanceBudgetRequest request,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubFinanceOperationsService.upsertBudget(
                        clubId,
                        financePeriodId,
                        requireUserKey(userContext),
                        request
                ),
                "재정 예산 저장 성공"
        );
    }

    @PostMapping("/admin/more/finance/periods/{financePeriodId}/close")
    public ResponseDataDTO<FinancePeriodResponse> closeFinancePeriod(
            @PathVariable Long clubId,
            @PathVariable Long financePeriodId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubFinanceOperationsService.closePeriod(
                        clubId,
                        financePeriodId,
                        requireUserKey(userContext)
                ),
                "재정 기간 마감 성공"
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
        return ResponseDataDTO.of(
                clubFinanceService.createFinanceExpense(clubId, requireUserKey(userContext), request),
                "지출 입력 성공"
        );
    }

    @GetMapping("/admin/more/finance/expenses/{expenseId}/revisions")
    public ResponseDataDTO<List<FinanceExpenseRevisionResponse>> getFinanceExpenseRevisions(
            @PathVariable Long clubId,
            @PathVariable Long expenseId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubFinanceService.getExpenseRevisions(clubId, expenseId, requireUserKey(userContext)),
                "지출 정정 이력 조회 성공"
        );
    }

    @PutMapping("/admin/more/finance/expenses/{expenseId}")
    public ResponseDataDTO<ClubFinanceExpenseResponse> correctFinanceExpense(
            @PathVariable Long clubId,
            @PathVariable Long expenseId,
            @Valid @RequestBody CorrectFinanceExpenseRequest request,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubFinanceService.correctFinanceExpense(
                        clubId,
                        expenseId,
                        requireUserKey(userContext),
                        request
                ),
                "지출 정정 성공"
        );
    }

    @PostMapping("/admin/more/finance/expenses/{expenseId}/void")
    public ResponseDataDTO<ClubFinanceExpenseResponse> voidFinanceExpense(
            @PathVariable Long clubId,
            @PathVariable Long expenseId,
            @Valid @RequestBody VoidFinanceExpenseRequest request,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubFinanceService.voidFinanceExpense(
                        clubId,
                        expenseId,
                        requireUserKey(userContext),
                        request
                ),
                "지출 취소 성공"
        );
    }

    @GetMapping("/admin/more/finance/obligations/{obligationId}/payments")
    public ResponseDataDTO<ClubAdminFinanceObligationDetailResponse> getAdminFinanceObligationDetail(
            @PathVariable Long clubId,
            @PathVariable Long obligationId,
            UserContext userContext
    ) {
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
        clubFinanceService.deleteObligation(clubId, obligationId, requireUserKey(userContext));
        return ResponseDataDTO.of(null, "재정 항목 삭제 성공");
    }

    private String requireUserKey(UserContext userContext) {
        if (userContext == null || !StringUtils.hasText(userContext.getUserKey())) {
            throw new SemoException.UnauthorizedException("Login required");
        }
        return userContext.getUserKey();
    }

}
