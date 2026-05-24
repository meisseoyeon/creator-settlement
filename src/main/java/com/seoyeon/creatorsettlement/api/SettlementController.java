package com.seoyeon.creatorsettlement.api;

import com.seoyeon.creatorsettlement.domain.settlement.dto.MonthlySettlementResponse;
import com.seoyeon.creatorsettlement.domain.settlement.dto.SettlementCreateRequest;
import com.seoyeon.creatorsettlement.domain.settlement.dto.SettlementResponse;
import com.seoyeon.creatorsettlement.service.settlement.CreatorSettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Tag(name = "정산 관리", description = "크리에이터 월별 정산 조회 및 확정(PENDING→CONFIRMED→PAID) API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class SettlementController {

    private final CreatorSettlementService settlementService;

    @Operation(
            summary = "크리에이터 월별 정산 조회",
            description = """
            지정한 연월의 정산 금액을 즉시 계산하여 반환합니다(영속화하지 않음).

            - 기준: 판매는 결제 일시(`paidAt`), 취소는 취소 일시(`canceledAt`)
            - 월 경계: KST 기준 해당 월 1일 00:00 ~ 말일 24:00 (반열린 구간)
            - 순 판매 = 총 판매 - 환불, 수수료 = max(0, 순 판매) × 수수료율(소수점 버림)
            - 판매/취소가 없는 월은 모든 금액 0으로 응답합니다.
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 연월 형식"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 크리에이터")
    })
    @GetMapping("/creators/{creatorId}/settlements")
    public MonthlySettlementResponse getMonthly(
            @Parameter(description = "크리에이터 ID", example = "creator-1")
            @PathVariable String creatorId,
            @Parameter(description = "조회 연월 (yyyy-MM)", example = "2025-03")
            @RequestParam String month) {
        return settlementService.getMonthly(creatorId, month);
    }

    @Operation(
            summary = "월별 정산 확정 스냅샷 생성",
            description = """
            현재 계산 결과를 스냅샷으로 저장하고 상태를 PENDING 으로 생성합니다.
            동일 (크리에이터, 월) 정산이 이미 있으면 409 로 거부합니다(중복 정산 방지).
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공 (PENDING)"),
            @ApiResponse(responseCode = "400", description = "잘못된 연월 형식"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 크리에이터"),
            @ApiResponse(responseCode = "409", description = "이미 생성된 월")
    })
    @PostMapping("/creators/{creatorId}/settlements")
    public ResponseEntity<SettlementResponse> create(
            @PathVariable String creatorId,
            @Valid @RequestBody SettlementCreateRequest req) {
        SettlementResponse res = settlementService.create(creatorId, req.month());
        return ResponseEntity
                .created(URI.create("/api/settlements/" + res.id()))
                .body(res);
    }

    @Operation(summary = "정산 확정", description = "PENDING → CONFIRMED 전이")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "확정 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 정산"),
            @ApiResponse(responseCode = "409", description = "PENDING 상태가 아님")
    })
    @PostMapping("/settlements/{id}/confirm")
    public SettlementResponse confirm(@PathVariable Long id) {
        return settlementService.confirm(id);
    }

    @Operation(summary = "정산 지급", description = "CONFIRMED → PAID 전이")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "지급 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 정산"),
            @ApiResponse(responseCode = "409", description = "CONFIRMED 상태가 아님")
    })
    @PostMapping("/settlements/{id}/pay")
    public SettlementResponse pay(@PathVariable Long id) {
        return settlementService.pay(id);
    }

    @Operation(summary = "정산 단건 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 정산")
    })
    @GetMapping("/settlements/{id}")
    public SettlementResponse get(@PathVariable Long id) {
        return settlementService.get(id);
    }
}
