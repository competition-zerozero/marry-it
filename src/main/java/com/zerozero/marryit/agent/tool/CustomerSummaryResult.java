package com.zerozero.marryit.agent.tool;

import com.zerozero.marryit.customer.domain.Customer;
import java.time.LocalDate;

public record CustomerSummaryResult(
        Long customerId,
        String groomName,
        String brideName,
        LocalDate weddingDate,
        Integer dDay,
        String preferredWeddingArea
) {

    static CustomerSummaryResult from(Customer customer) {
        return new CustomerSummaryResult(
                customer.getId(),
                customer.getGroomName(),
                customer.getBrideName(),
                customer.getWeddingDate(),
                WeddingDDay.daysUntil(customer.getWeddingDate()),
                customer.getPreferredWeddingArea()
        );
    }
}
