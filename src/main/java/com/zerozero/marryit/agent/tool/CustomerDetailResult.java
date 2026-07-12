package com.zerozero.marryit.agent.tool;

import com.zerozero.marryit.customer.domain.Customer;
import java.time.LocalDate;

public record CustomerDetailResult(
        Long customerId,
        String groomName,
        String brideName,
        String phoneNumber,
        String residenceArea,
        LocalDate weddingDate,
        Integer dDay,
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

    static CustomerDetailResult from(Customer customer) {
        return new CustomerDetailResult(
                customer.getId(),
                customer.getGroomName(),
                customer.getBrideName(),
                customer.getPhoneNumber(),
                customer.getResidenceArea(),
                customer.getWeddingDate(),
                WeddingDDay.daysUntil(customer.getWeddingDate()),
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
