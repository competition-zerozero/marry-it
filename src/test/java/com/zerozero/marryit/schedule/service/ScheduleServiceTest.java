package com.zerozero.marryit.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zerozero.marryit.auth.domain.OAuthProvider;
import com.zerozero.marryit.auth.domain.User;
import com.zerozero.marryit.auth.repository.UserRepository;
import com.zerozero.marryit.schedule.domain.ScheduleTargetType;
import com.zerozero.marryit.schedule.domain.ScheduleType;
import com.zerozero.marryit.schedule.repository.ScheduleRepository;
import com.zerozero.marryit.workspace.domain.Workspace;
import com.zerozero.marryit.workspace.domain.WorkspaceMember;
import com.zerozero.marryit.workspace.repository.WorkspaceMemberRepository;
import com.zerozero.marryit.workspace.repository.WorkspaceRepository;
import com.zerozero.marryit.workspace.service.WorkspaceAccessService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({ScheduleService.class, WorkspaceAccessService.class})
class ScheduleServiceTest {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Test
    void createsScheduleWhenTimeDoesNotConflict() {
        User planner = saveUser("google-1", "planner@example.com", "서영");
        Workspace workspace = saveWorkspaceWithOwner(planner);

        ScheduleResponse response = scheduleService.create(workspace.getId(), planner.getId(), request(planner.getId(),
                LocalDateTime.of(2026, 10, 17, 10, 0),
                LocalDateTime.of(2026, 10, 17, 11, 0)
        ));

        assertThat(response.id()).isNotNull();
        assertThat(scheduleRepository.count()).isEqualTo(1);
    }

    @Test
    void blocksOverlappingScheduleForSameTarget() {
        User planner = saveUser("google-1", "planner@example.com", "서영");
        Workspace workspace = saveWorkspaceWithOwner(planner);
        scheduleService.create(workspace.getId(), planner.getId(), request(planner.getId(),
                LocalDateTime.of(2026, 10, 17, 10, 0),
                LocalDateTime.of(2026, 10, 17, 11, 0)
        ));

        assertThatThrownBy(() -> scheduleService.create(workspace.getId(), planner.getId(), request(planner.getId(),
                LocalDateTime.of(2026, 10, 17, 10, 30),
                LocalDateTime.of(2026, 10, 17, 11, 30)
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("conflicts");
    }

    @Test
    void blocksScheduleForPlannerOutsideWorkspace() {
        User planner = saveUser("google-1", "planner@example.com", "서영");
        Workspace workspace = saveWorkspaceWithOwner(planner);

        assertThatThrownBy(() -> scheduleService.create(
                workspace.getId(),
                planner.getId(),
                new ScheduleRequest(
                        ScheduleTargetType.PLANNER,
                        999L,
                        ScheduleType.CONSULTATION,
                        "외부 플래너 일정",
                        LocalDateTime.of(2026, 10, 17, 10, 0),
                        LocalDateTime.of(2026, 10, 17, 11, 0),
                        "서울 강남구"
                )
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("planner target not found");
    }

    @Test
    void allowsAdjacentSchedulesForSameTarget() {
        User planner = saveUser("google-1", "planner@example.com", "서영");
        Workspace workspace = saveWorkspaceWithOwner(planner);
        scheduleService.create(workspace.getId(), planner.getId(), request(planner.getId(),
                LocalDateTime.of(2026, 10, 17, 10, 0),
                LocalDateTime.of(2026, 10, 17, 11, 0)
        ));

        scheduleService.create(workspace.getId(), planner.getId(), request(planner.getId(),
                LocalDateTime.of(2026, 10, 17, 11, 0),
                LocalDateTime.of(2026, 10, 17, 12, 0)
        ));

        assertThat(scheduleRepository.count()).isEqualTo(2);
    }

    @Test
    void updatesAndDeletesScheduleInsideWorkspace() {
        User planner = saveUser("google-1", "planner@example.com", "서영");
        Workspace workspace = saveWorkspaceWithOwner(planner);
        ScheduleResponse created = scheduleService.create(workspace.getId(), planner.getId(), request(planner.getId(),
                LocalDateTime.of(2026, 10, 17, 10, 0),
                LocalDateTime.of(2026, 10, 17, 11, 0)
        ));

        ScheduleResponse updated = scheduleService.update(workspace.getId(), planner.getId(), created.id(), request(planner.getId(),
                LocalDateTime.of(2026, 10, 17, 13, 0),
                LocalDateTime.of(2026, 10, 17, 14, 0)
        ));
        scheduleService.delete(workspace.getId(), planner.getId(), created.id());

        assertThat(updated.startsAt()).isEqualTo(LocalDateTime.of(2026, 10, 17, 13, 0));
        assertThat(scheduleRepository.count()).isZero();
    }

    private User saveUser(String providerUserId, String email, String name) {
        return userRepository.save(User.createOAuthUser(OAuthProvider.GOOGLE, providerUserId, email, name, null));
    }

    private Workspace saveWorkspaceWithOwner(User owner) {
        Workspace workspace = workspaceRepository.save(Workspace.createPersonal(owner.getName()));
        workspaceMemberRepository.save(WorkspaceMember.owner(owner, workspace));
        return workspace;
    }

    private ScheduleRequest request(Long plannerId, LocalDateTime startsAt, LocalDateTime endsAt) {
        return new ScheduleRequest(
                ScheduleTargetType.PLANNER,
                plannerId,
                ScheduleType.CONSULTATION,
                "고객 상담",
                startsAt,
                endsAt,
                "서울 강남구"
        );
    }
}
