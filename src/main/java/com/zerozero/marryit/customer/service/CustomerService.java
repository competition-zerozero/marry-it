package com.zerozero.marryit.customer.service;

import com.zerozero.marryit.auth.domain.User;
import com.zerozero.marryit.auth.repository.UserRepository;
import com.zerozero.marryit.customer.domain.Customer;
import com.zerozero.marryit.customer.repository.CustomerRepository;
import com.zerozero.marryit.workspace.domain.Workspace;
import com.zerozero.marryit.workspace.repository.WorkspaceRepository;
import com.zerozero.marryit.workspace.service.WorkspaceAccessService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final WorkspaceAccessService workspaceAccessService;

    public CustomerService(
            CustomerRepository customerRepository,
            WorkspaceRepository workspaceRepository,
            UserRepository userRepository,
            WorkspaceAccessService workspaceAccessService
    ) {
        this.customerRepository = customerRepository;
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.workspaceAccessService = workspaceAccessService;
    }

    @Transactional
    public CustomerResponse create(Long workspaceId, Long userId, CustomerRequest request) {
        log.info("워크스페이스 멤버 검증 workspaceId={} userId={}", workspaceId, userId);
        workspaceAccessService.validateMember(userId, workspaceId);
        Workspace workspace = getWorkspace(workspaceId);
        User planner = getUser(userId);
        log.info("고객 엔티티 생성 workspaceId={} plannerId={}", workspaceId, planner.getId());

        Customer customer = Customer.create(
                workspace,
                planner,
                request.groomName(),
                request.brideName(),
                request.phoneNumber(),
                request.residenceArea(),
                request.weddingDate(),
                request.preferredWeddingArea(),
                request.expectedGuestCount(),
                request.totalBudget(),
                request.preferredAtmosphere(),
                request.preferredStyle(),
                request.importantConditions(),
                request.avoidConditions(),
                request.itemBudgetMemo(),
                request.consultationMemo(),
                request.todoMemo(),
                request.completedMemo()
        );

        return CustomerResponse.from(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll(Long workspaceId, Long userId) {
        workspaceAccessService.validateMember(userId, workspaceId);
        return customerRepository.findByWorkspaceIdOrderByIdDesc(workspaceId)
                .stream()
                .map(CustomerResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(Long workspaceId, Long userId, Long customerId) {
        workspaceAccessService.validateMember(userId, workspaceId);
        return CustomerResponse.from(getCustomer(customerId, workspaceId));
    }

    @Transactional
    public CustomerResponse update(Long workspaceId, Long userId, Long customerId, CustomerRequest request) {
        workspaceAccessService.validateMember(userId, workspaceId);
        Customer customer = getCustomer(customerId, workspaceId);
        customer.update(
                request.groomName(),
                request.brideName(),
                request.phoneNumber(),
                request.residenceArea(),
                request.weddingDate(),
                request.preferredWeddingArea(),
                request.expectedGuestCount(),
                request.totalBudget(),
                request.preferredAtmosphere(),
                request.preferredStyle(),
                request.importantConditions(),
                request.avoidConditions(),
                request.itemBudgetMemo(),
                request.consultationMemo(),
                request.todoMemo(),
                request.completedMemo()
        );
        return CustomerResponse.from(customer);
    }

    @Transactional
    public void delete(Long workspaceId, Long userId, Long customerId) {
        workspaceAccessService.validateMember(userId, workspaceId);
        Customer customer = getCustomer(customerId, workspaceId);
        customerRepository.delete(customer);
    }

    private Customer getCustomer(Long customerId, Long workspaceId) {
        return customerRepository.findByIdAndWorkspaceId(customerId, workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found."));
    }

    private Workspace getWorkspace(Long workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found."));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }
}
