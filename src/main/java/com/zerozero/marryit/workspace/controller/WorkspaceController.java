package com.zerozero.marryit.workspace.controller;

import com.zerozero.marryit.auth.oauth.OAuth2LoginSuccessHandler;
import com.zerozero.marryit.schedule.repository.ScheduleRepository;
import com.zerozero.marryit.schedule.service.ScheduleResponse;
import com.zerozero.marryit.workspace.repository.WorkspaceMemberRepository;
import com.zerozero.marryit.workspace.service.CreateWorkspaceRequest;
import com.zerozero.marryit.workspace.service.CreateWorkspaceService;
import com.zerozero.marryit.workspace.service.MeResponse;
import com.zerozero.marryit.workspace.service.WorkspaceQueryService;
import com.zerozero.marryit.workspace.service.WorkspaceSummaryResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class WorkspaceController {

    private final WorkspaceQueryService workspaceQueryService;
    private final CreateWorkspaceService createWorkspaceService;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final ScheduleRepository scheduleRepository;

    public WorkspaceController(
            WorkspaceQueryService workspaceQueryService,
            CreateWorkspaceService createWorkspaceService,
            WorkspaceMemberRepository workspaceMemberRepository,
            ScheduleRepository scheduleRepository
    ) {
        this.workspaceQueryService = workspaceQueryService;
        this.createWorkspaceService = createWorkspaceService;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @GetMapping("/me")
    public MeResponse me(HttpSession session) {
        return workspaceQueryService.getMe(currentUserId(session), currentWorkspaceId(session));
    }

    @GetMapping("/me/schedules")
    public List<ScheduleResponse> allMySchedules(HttpSession session) {
        Long userId = currentUserId(session);
        List<Long> workspaceIds = workspaceMemberRepository.findByUserId(userId)
                .stream()
                .map(m -> m.getWorkspace().getId())
                .toList();
        return scheduleRepository.findByWorkspaceIdInOrderByStartsAtAsc(workspaceIds)
                .stream()
                .map(ScheduleResponse::from)
                .toList();
    }

    @PostMapping("/workspaces")
    public WorkspaceSummaryResponse createWorkspace(@RequestBody CreateWorkspaceRequest request, HttpSession session) {
        var result = createWorkspaceService.create(currentUserId(session), request);
        session.setAttribute(OAuth2LoginSuccessHandler.SESSION_WORKSPACE_ID, result.workspaceId());
        return result;
    }

    private Long currentUserId(HttpSession session) {
        Object userId = session.getAttribute(OAuth2LoginSuccessHandler.SESSION_USER_ID);
        if (!(userId instanceof Long id)) {
            throw new SecurityException("Login is required.");
        }
        return id;
    }

    private Long currentWorkspaceId(HttpSession session) {
        Object workspaceId = session.getAttribute(OAuth2LoginSuccessHandler.SESSION_WORKSPACE_ID);
        return workspaceId instanceof Long id ? id : null;
    }
}
