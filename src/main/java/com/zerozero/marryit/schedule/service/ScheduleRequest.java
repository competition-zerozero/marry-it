package com.zerozero.marryit.schedule.service;

import com.zerozero.marryit.schedule.domain.ScheduleTargetType;
import com.zerozero.marryit.schedule.domain.ScheduleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ScheduleRequest(
        @NotNull
        ScheduleTargetType targetType,
        @NotNull
        Long targetId,
        @NotNull
        ScheduleType scheduleType,
        @NotBlank
        String title,
        @NotNull
        LocalDateTime startsAt,
        @NotNull
        LocalDateTime endsAt,
        String location
) {
}
