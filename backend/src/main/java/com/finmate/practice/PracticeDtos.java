package com.finmate.practice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public final class PracticeDtos {
    private PracticeDtos() {}
    public record ProductSpec(String id, String name, String assetClass, String mechanism,
            String driver, double duration, double annualFeePercent, double leverage,
            boolean foreignCurrency, String conceptSlug, String explanation) {}
    public record Scenario(String id, String name, String description, Shocks shocks) {}
    public record Shocks(
            @NotNull @DecimalMin("-80") @DecimalMax("100") Double equityPercent,
            @NotNull @DecimalMin("-2") @DecimalMax("2") Double ratePoints,
            @NotNull @DecimalMin("-2") @DecimalMax("3") Double creditSpreadPoints,
            @NotNull @DecimalMin("-80") @DecimalMax("100") Double goldPercent,
            @NotNull @DecimalMin("-80") @DecimalMax("100") Double realEstatePercent,
            @NotNull @DecimalMin("-50") @DecimalMax("100") Double fxPercent,
            @NotNull @Min(1) @Max(365) Integer days) {}
    public record Holding(
            @NotBlank @Size(max=80) String rowId,
            @NotBlank String productId,
            @NotBlank @Size(max=60) String label,
            @NotNull @DecimalMin("1000") @DecimalMax("10000000000") Double amount,
            @NotNull @DecimalMin("0") @DecimalMax("5") Double annualFeePercent,
            @NotNull @DecimalMin("0.1") @DecimalMax("20") Double duration,
            @NotNull @DecimalMin("1") @DecimalMax("20") Double leverage,
            @NotNull @DecimalMin("10") @DecimalMax("200") Double strike,
            @NotNull @DecimalMin("0.1") @DecimalMax("100") Double premium,
            @NotNull @DecimalMin("0") @DecimalMax("20") Double borrowingRatePercent) {}
    public record Portfolio(
            @NotNull @DecimalMin("1000") @DecimalMax("10000000000") Double budget,
            @NotNull @Size(max=20) List<@NotNull @Valid Holding> holdings) {}
    public record AnalyzeRequest(@NotNull @Valid Portfolio portfolio, @NotNull @Valid Shocks shocks) {}
    public record CompareRequest(@NotNull @Valid Portfolio before, @NotNull @Valid Portfolio after,
                                 @NotNull @Valid Shocks shocks) {}
    public record Effect(String label, double amount, String formula, String meaning) {}
    public record PositionResult(String rowId, String productId, String label, String assetClass,
            String conceptSlug, double invested, double weightPercent, double economicExposure,
            double endingValue, double profitLoss, double returnPercent, List<Effect> effects,
            List<String> observations) {}
    public record Allocation(String assetClass, double amount, double weightPercent) {}
    public record Analysis(String modelVersion, double budget, double unallocatedCash,
            double endingValue, double profitLoss, double returnPercent,
            double linearExposureRatio, double optionPremium,
            List<Allocation> allocation, List<PositionResult> positions, List<String> lessons,
            List<String> limitations) {}
    public record Comparison(Analysis before, Analysis after, double profitLossDifference, String interpretation) {}
    public record Source(String title, String url) {}
    public record Methodology(String modelVersion, String label, List<String> assumptions, List<Source> sources) {}
}
