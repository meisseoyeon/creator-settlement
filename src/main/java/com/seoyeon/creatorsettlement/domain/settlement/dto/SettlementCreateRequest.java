package com.seoyeon.creatorsettlement.domain.settlement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record SettlementCreateRequest(
        @Schema(description = "정산 대상 연월 (yyyy-MM)", example = "2025-03")
        @NotBlank String month
) {}
