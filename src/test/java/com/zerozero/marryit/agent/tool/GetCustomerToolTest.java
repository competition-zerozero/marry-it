package com.zerozero.marryit.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zerozero.marryit.auth.domain.OAuthProvider;
import com.zerozero.marryit.auth.domain.User;
import com.zerozero.marryit.auth.repository.UserRepository;
import com.zerozero.marryit.customer.domain.Customer;
import com.zerozero.marryit.customer.repository.CustomerRepository;
import com.zerozero.marryit.workspace.domain.Workspace;
import com.zerozero.marryit.workspace.repository.WorkspaceRepository;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(GetCustomerTool.class)
class GetCustomerToolTest {

    @Autowired
    private GetCustomerTool getCustomerTool;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void returnsCustomerDetailWithServerComputedDDay() {
        User planner = saveUser();
        Workspace workspace = workspaceRepository.save(Workspace.createPersonal(planner.getName()));
        Customer customer = customerRepository.save(sampleCustomer(workspace, planner, LocalDate.now().plusDays(10)));

        Object result = getCustomerTool.execute(
                Map.of("customerId", customer.getId()),
                new AgentToolContext(workspace.getId(), planner.getId())
        );

        assertThat(result).isInstanceOf(CustomerDetailResult.class);
        CustomerDetailResult detail = (CustomerDetailResult) result;
        assertThat(detail.customerId()).isEqualTo(customer.getId());
        assertThat(detail.dDay()).isEqualTo(10);
    }

    @Test
    void doesNotExposeCustomerFromAnotherWorkspace() {
        User planner = saveUser();
        Workspace workspace = workspaceRepository.save(Workspace.createPersonal(planner.getName()));
        Workspace otherWorkspace = workspaceRepository.save(Workspace.createPersonal("다른 워크스페이스"));
        Customer customer = customerRepository.save(sampleCustomer(workspace, planner, LocalDate.now().plusDays(5)));

        assertThatThrownBy(() -> getCustomerTool.execute(
                Map.of("customerId", customer.getId()),
                new AgentToolContext(otherWorkspace.getId(), planner.getId())
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private User saveUser() {
        return userRepository.save(User.createOAuthUser(OAuthProvider.GOOGLE, "google-1", "planner@example.com", "서영", null));
    }

    private Customer sampleCustomer(Workspace workspace, User planner, LocalDate weddingDate) {
        return Customer.create(
                workspace,
                planner,
                "민준",
                "서연",
                "010-1234-5678",
                "서울 강남구",
                weddingDate,
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
