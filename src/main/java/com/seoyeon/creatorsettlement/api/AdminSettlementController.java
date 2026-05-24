package com.seoyeon.creatorsettlement.api;

import com.seoyeon.creatorsettlement.domain.settlement.dto.AdminSettlementResponse;
import com.seoyeon.creatorsettlement.service.settlement.AdminSettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "운영자 정산 집계", description = "기간 내 전체 크리에이터 정산 현황 집계 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminSettlementController {

    private final AdminSettlementService adminSettlementService;

    @Operation(
            summary = "기간별 전체 정산 집계",
            description = """
            지정한 기간의 전체 크리에이터 정산 현황을 집계합니다.

            - 기준: 판매는 결제 일시(`paidAt`), 취소는 취소 일시(`canceledAt`)
            - 기간 해석: KST 반열린 구간 `[from 00:00, to+1일 00:00)` (종료일 종일 포함)
            - 활동(판매 또는 취소)이 있는 크리에이터만 포함하며 `creatorId` 오름차순 정렬
            - `total` 에 전체 합계를 함께 반환합니다.
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "집계 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 날짜 형식 또는 from > to")
    })
    @GetMapping("/settlements")
    public AdminSettlementResponse aggregate(
            @Parameter(description = "시작일 (yyyy-MM-dd, KST)", example = "2025-03-01")
            @RequestParam String from,
            @Parameter(description = "종료일 (yyyy-MM-dd, KST, 해당일 포함)", example = "2025-03-31")
            @RequestParam String to) {
        return adminSettlementService.aggregate(from, to);
    }
}
