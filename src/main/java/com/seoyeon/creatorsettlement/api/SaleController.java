package com.seoyeon.creatorsettlement.api;

import com.seoyeon.creatorsettlement.domain.sale.CancelRecord;
import com.seoyeon.creatorsettlement.domain.sale.SaleRecord;
import com.seoyeon.creatorsettlement.domain.sale.dto.CancelRegisterRequest;
import com.seoyeon.creatorsettlement.domain.sale.dto.CancelResponse;
import com.seoyeon.creatorsettlement.domain.sale.dto.SaleRegisterRequest;
import com.seoyeon.creatorsettlement.domain.sale.dto.SaleResponse;
import com.seoyeon.creatorsettlement.service.sale.CancelRegistrationService;
import com.seoyeon.creatorsettlement.service.sale.SaleQueryService;
import com.seoyeon.creatorsettlement.service.sale.SaleRegistrationService;
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
import java.util.List;

@Tag(name = "판매/취소 관리", description = "판매 등록, 취소(환불) 등록, 판매 내역 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class SaleController {

    private final SaleRegistrationService saleRegistrationService;
    private final CancelRegistrationService cancelRegistrationService;
    private final SaleQueryService saleQueryService;

    @Operation(
            summary = "판매 내역 등록",
            description = """
            강의 ID, 수강생 ID, 결제 금액, 결제 일시를 받아 새로운 판매 내역을 생성합니다.
            
            - 결제 일시(`paidAt`)는 ISO-8601 형식이며 KST 오프셋(+09:00)을 권장합니다.
            - 판매 ID는 클라이언트가 지정합니다.
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "필수 필드 누락 또는 형식 오류"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 강의(courseId)"),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 판매 ID")
    })
    @PostMapping("/sales")
    public ResponseEntity<SaleResponse> registerSale(
            @Valid @RequestBody SaleRegisterRequest req) {
        SaleRecord saved = saleRegistrationService.register(req);
        return ResponseEntity
                .created(URI.create("/api/sales/" + saved.getId()))
                .body(SaleResponse.from(saved));
    }

    @Operation(
            summary = "취소(환불) 내역 등록",
            description = """
            원본 판매(`saleId`)를 참조하여 환불 금액과 취소 일시를 등록합니다.
            
            **검증 규칙**
            - 취소 일시는 결제 일시 이후여야 합니다.
            - 동일 판매에 대한 환불 누적액은 원결제 금액을 초과할 수 없습니다.
            - 부분 환불(원결제 > 환불액) 및 다회 환불을 허용합니다.
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "취소 등록 성공"),
            @ApiResponse(responseCode = "400", description = "취소 일시 오류 또는 환불 누적 초과"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 판매(saleId)"),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 취소 ID")
    })
    @PostMapping("/sales/{saleId}/cancels")
    public ResponseEntity<CancelResponse> registerCancel(
            @PathVariable String saleId,
            @Valid @RequestBody CancelRegisterRequest req) {
        CancelRecord saved = cancelRegistrationService.register(saleId, req);
        return ResponseEntity
                .created(URI.create("/api/cancels/" + saved.getId()))
                .body(CancelResponse.from(saved));
    }

    @Operation(
            summary = "크리에이터별 판매 내역 조회",
            description = """
            특정 크리에이터의 판매 내역을 결제 일시 오름차순으로 반환합니다.
            
            **기간 필터(선택)**
            - `from`, `to`는 함께 지정해야 합니다(하나만 지정 시 400).
            - 형식: `yyyy-MM-dd` (KST 기준)
            - 해석: 반열린 구간 `[from 00:00, to+1일 00:00)` — 종료일을 종일 포함합니다.
            - 미지정 시 해당 크리에이터의 전체 판매 내역을 반환합니다.
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 날짜 형식 또는 from > to"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 크리에이터")
    })
    @GetMapping("/creators/{creatorId}/sales")
    public List<SaleResponse> listByCreator(
            @Parameter(description = "크리에이터 ID", example = "creator-1")
            @PathVariable String creatorId,
            @Parameter(description = "조회 시작일 (yyyy-MM-dd, KST)", example = "2025-03-01")
            @RequestParam(required = false) String from,
            @Parameter(description = "조회 종료일 (yyyy-MM-dd, KST, 해당일 포함)", example = "2025-03-31")
            @RequestParam(required = false) String to) {
        return saleQueryService.findByCreator(creatorId, from, to).stream()
                .map(SaleResponse::from)
                .toList();
    }
}
