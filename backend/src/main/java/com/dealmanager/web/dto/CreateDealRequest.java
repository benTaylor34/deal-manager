package com.dealmanager.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateDealRequest(
        @NotNull Long clientId,
        @NotBlank @Size(max = 200) String name,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @Valid @Size(max = 50) List<PricingLineRequest> pricingLines) {
}
