package com.zerozero.marryit.agent.tool;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** D-Day is a countable business rule, not something the model should compute itself. */
final class WeddingDDay {

    private WeddingDDay() {
    }

    static Integer daysUntil(LocalDate weddingDate) {
        if (weddingDate == null) {
            return null;
        }
        return (int) ChronoUnit.DAYS.between(LocalDate.now(), weddingDate);
    }
}
