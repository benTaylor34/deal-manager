package com.dealmanager.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PricingLineRequest(
        @NotBlank @Size(max = 100) String service,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 15, fraction = 4) BigDecimal standardPrice,
        @NotNull @DecimalMin(value = "0") @Digits(integer = 15, fraction = 4) BigDecimal proposedPrice,
        @NotNull @Min(1) Long committedVolume) {
}
