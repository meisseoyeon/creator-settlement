package com.seoyeon.creator_settlement.domain.sale.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.OffsetDateTime;

public record SaleRegisterRequest(
        @Schema(description = "판매 ID", example = "sale-100") @NotBlank String id,
        @Schema(description = "강의 ID", example = "course-1") @NotBlank String courseId,
        @Schema(description = "수강생 ID", example = "student-1") @NotBlank String studentId,
        @Schema(description = "결제 금액 (원)", example = "50000") @NotNull @Positive Long amount,
        @Schema(description = "결제 일시 (ISO-8601, KST 권장)", example = "2025-04-01T10:00:00+09:00") @NotNull OffsetDateTime paidAt
) {}
