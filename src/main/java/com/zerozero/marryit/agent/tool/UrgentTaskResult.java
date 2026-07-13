package com.zerozero.marryit.agent.tool;

import java.time.LocalDate;

public record UrgentTaskResult(
        Long customerId,
        String customerName,
        String task,
        String urgency,
        String urgentReason,
        LocalDate dueDate,
        Integer daysUntilWedding
) {
}
