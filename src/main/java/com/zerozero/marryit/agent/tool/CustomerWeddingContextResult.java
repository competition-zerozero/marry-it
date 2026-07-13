package com.zerozero.marryit.agent.tool;

import com.zerozero.marryit.customer.domain.Customer;
import java.time.LocalDate;
import java.util.List;

public record CustomerWeddingContextResult(
        Long customerId,
        String customerName,
        String groomName,
        String brideName,
        LocalDate weddingDate,
        Integer dDay,
        String weddingArea,
        Long totalBudget,
        Long usedBudget,
        Long remainingBudget,
        String preferredAtmosphere,
        String preferredStyle,
        Integer expectedGuestCount,
        List<VendorDetailResult> contractedVendors,
        String incompleteTasks,
        String customerNotes,
        List<ScheduleResult> schedules,
        List<String> missingDataWarnings
) {

    static CustomerWeddingContextResult of(
            Customer customer,
            List<VendorDetailResult> contractedVendors,
            List<ScheduleResult> schedules,
            List<String> missingDataWarnings
    ) {
        return new CustomerWeddingContextResult(
                customer.getId(),
                customer.getBrideName() + "·" + customer.getGroomName(),
                customer.getGroomName(),
                customer.getBrideName(),
                customer.getWeddingDate(),
                WeddingDDay.daysUntil(customer.getWeddingDate()),
                customer.getPreferredWeddingArea(),
                customer.getTotalBudget(),
                null,
                null,
                customer.getPreferredAtmosphere(),
                customer.getPreferredStyle(),
                customer.getExpectedGuestCount(),
                contractedVendors,
                customer.getTodoMemo(),
                customer.getConsultationMemo(),
                schedules,
                missingDataWarnings
        );
    }
}
