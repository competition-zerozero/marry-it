package com.zerozero.marryit.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zerozero.marryit.auth.domain.OAuthProvider;
import com.zerozero.marryit.auth.domain.User;
import com.zerozero.marryit.auth.repository.UserRepository;
import com.zerozero.marryit.customer.repository.CustomerRepository;
import com.zerozero.marryit.workspace.domain.Workspace;
import com.zerozero.marryit.workspace.domain.WorkspaceMember;
import com.zerozero.marryit.workspace.repository.WorkspaceMemberRepository;
import com.zerozero.marryit.workspace.repository.WorkspaceRepository;
import com.zerozero.marryit.workspace.service.WorkspaceAccessService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({CustomerService.class, WorkspaceAccessService.class})
class CustomerServiceTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void createsAndListsCustomersInsideWorkspace() {
        User planner = saveUser("google-1", "planner@example.com", "서영");
        Workspace workspace = saveWorkspaceWithOwner(planner);

        CustomerResponse created = customerService.create(workspace.getId(), planner.getId(), sampleRequest("민준", "서연"));

        assertThat(created.id()).isNotNull();
        assertThat(created.workspaceId()).isEqualTo(workspace.getId());
        assertThat(created.plannerUserId()).isEqualTo(planner.getId());
        assertThat(customerService.findAll(workspace.getId(), planner.getId()))
                .extracting(CustomerResponse::id)
                .containsExactly(created.id());
    }

    @Test
    void blocksNonMemberFromCreatingCustomer() {
        User owner = saveUser("google-1", "owner@example.com", "오너");
        User outsider = saveUser("google-2", "outsider@example.com", "외부인");
        Workspace workspace = saveWorkspaceWithOwner(owner);

        assertThatThrownBy(() -> customerService.create(workspace.getId(), outsider.getId(), sampleRequest("민준", "서연")))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void doesNotReadCustomerThroughDifferentWorkspaceId() {
        User planner = saveUser("google-1", "planner@example.com", "서영");
        Workspace workspace = saveWorkspaceWithOwner(planner);
        Workspace otherWorkspace = workspaceRepository.save(Workspace.createPersonal("다른"));
        workspaceMemberRepository.save(WorkspaceMember.owner(planner, otherWorkspace));

        CustomerResponse created = customerService.create(workspace.getId(), planner.getId(), sampleRequest("민준", "서연"));

        assertThatThrownBy(() -> customerService.get(otherWorkspace.getId(), planner.getId(), created.id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Customer not found");
    }

    @Test
    void updatesAndDeletesCustomerInsideWorkspace() {
        User planner = saveUser("google-1", "planner@example.com", "서영");
        Workspace workspace = saveWorkspaceWithOwner(planner);
        CustomerResponse created = customerService.create(workspace.getId(), planner.getId(), sampleRequest("민준", "서연"));

        CustomerResponse updated = customerService.update(
                workspace.getId(),
                planner.getId(),
                created.id(),
                sampleRequest("준호", "하린")
        );
        customerService.delete(workspace.getId(), planner.getId(), created.id());

        assertThat(updated.groomName()).isEqualTo("준호");
        assertThat(updated.brideName()).isEqualTo("하린");
        assertThat(customerRepository.count()).isZero();
    }

    private User saveUser(String providerUserId, String email, String name) {
        return userRepository.save(User.createOAuthUser(
                OAuthProvider.GOOGLE,
                providerUserId,
                email,
                name,
                null
        ));
    }

    private Workspace saveWorkspaceWithOwner(User owner) {
        Workspace workspace = workspaceRepository.save(Workspace.createPersonal(owner.getName()));
        workspaceMemberRepository.save(WorkspaceMember.owner(owner, workspace));
        return workspace;
    }

    private CustomerRequest sampleRequest(String groomName, String brideName) {
        return new CustomerRequest(
                groomName,
                brideName,
                "010-1234-5678",
                "서울 강남구",
                LocalDate.of(2026, 10, 17),
                "강남",
                200,
                30_000_000L,
                "밝고 자연스러운 분위기",
                "야외 느낌",
                "지방 하객이 많아 교통이 편리해야 함",
                "어두운 웨딩홀",
                "홀 1500만 원, 플라워 300만 원",
                "첫 상담 완료",
                "드레스샵 후보 정리",
                "예식 지역 확정"
        );
    }
}
