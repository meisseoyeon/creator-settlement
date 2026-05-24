package com.seoyeon.creatorsettlement.domain.sale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.OffsetDateTime;

public record CancelRegisterRequest(
        @NotBlank String id,
        @NotNull @Positive Long refundAmount,
        @NotNull OffsetDateTime canceledAt
) {}