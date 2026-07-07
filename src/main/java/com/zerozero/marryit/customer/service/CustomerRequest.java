package com.zerozero.marryit.customer.service;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

public record CustomerRequest(
        @NotBlank
        String groomName,
        @NotBlank
        String brideName,
        String phoneNumber,
        String residenceArea,
        LocalDate weddingDate,
        String preferredWeddingArea,
        @Min(0)
        Integer expectedGuestCount,
        @PositiveOrZero
        Long totalBudget,
        String preferredAtmosphere,
        String preferredStyle,
        String importantConditions,
        String avoidConditions,
        String itemBudgetMemo,
        String consultationMemo,
        String todoMemo,
        String completedMemo
) {
}
