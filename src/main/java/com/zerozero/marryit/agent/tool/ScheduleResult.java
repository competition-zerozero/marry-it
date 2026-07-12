package com.zerozero.marryit.agent.tool;

import com.zerozero.marryit.schedule.domain.Schedule;
import java.time.LocalDateTime;

public record ScheduleResult(
        Long scheduleId,
        String scheduleType,
        String title,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String location
) {

    static ScheduleResult from(Schedule schedule) {
        return new ScheduleResult(
                schedule.getId(),
                schedule.getScheduleType().name(),
                schedule.getTitle(),
                schedule.getStartsAt(),
                schedule.getEndsAt(),
                schedule.getLocation()
        );
    }
}
