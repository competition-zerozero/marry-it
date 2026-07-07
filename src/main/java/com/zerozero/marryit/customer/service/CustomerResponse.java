package com.zerozero.marryit.customer.service;

import com.zerozero.marryit.customer.domain.Customer;
import java.time.LocalDate;

public record CustomerResponse(
        Long id,
        Long workspaceId,
        Long plannerUserId,
        String groomName,
        String brideName,
        String phoneNumber,
        String residenceArea,
        LocalDate weddingDate,
        String preferredWeddingArea,
        Integer expectedGuestCount,
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

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getWorkspace().getId(),
                customer.getPlanner().getId(),
                customer.getGroomName(),
                customer.getBrideName(),
                customer.getPhoneNumber(),
                customer.getResidenceArea(),
                customer.getWeddingDate(),
                customer.getPreferredWeddingArea(),
                customer.getExpectedGuestCount(),
                customer.getTotalBudget(),
                customer.getPreferredAtmosphere(),
                customer.getPreferredStyle(),
                customer.getImportantConditions(),
                customer.getAvoidConditions(),
                customer.getItemBudgetMemo(),
                customer.getConsultationMemo(),
                customer.getTodoMemo(),
                customer.getCompletedMemo()
        );
    }
}
