package com.zerozero.marryit.schedule.controller;

import com.zerozero.marryit.auth.oauth.OAuth2LoginSuccessHandler;
import com.zerozero.marryit.schedule.service.ScheduleRequest;
import com.zerozero.marryit.schedule.service.ScheduleResponse;
import com.zerozero.marryit.schedule.service.ScheduleService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleResponse create(
            @PathVariable Long workspaceId,
            @Valid @RequestBody ScheduleRequest request,
            HttpSession session
    ) {
        return scheduleService.create(workspaceId, currentUserId(session), request);
    }

    @GetMapping
    public List<ScheduleResponse> findAll(@PathVariable Long workspaceId, HttpSession session) {
        return scheduleService.findAll(workspaceId, currentUserId(session));
    }

    @GetMapping("/{scheduleId}")
    public ScheduleResponse get(
            @PathVariable Long workspaceId,
            @PathVariable Long scheduleId,
            HttpSession session
    ) {
        return scheduleService.get(workspaceId, currentUserId(session), scheduleId);
    }

    @PutMapping("/{scheduleId}")
    public ScheduleResponse update(
            @PathVariable Long workspaceId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody ScheduleRequest request,
            HttpSession session
    ) {
        return scheduleService.update(workspaceId, currentUserId(session), scheduleId, request);
    }

    @DeleteMapping("/{scheduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long workspaceId,
            @PathVariable Long scheduleId,
            HttpSession session
    ) {
        scheduleService.delete(workspaceId, currentUserId(session), scheduleId);
    }

    private Long currentUserId(HttpSession session) {
        Object userId = session.getAttribute(OAuth2LoginSuccessHandler.SESSION_USER_ID);
        if (!(userId instanceof Long id)) {
            throw new SecurityException("Login is required.");
        }
        return id;
    }
}
