package com.zerozero.marryit.customer.repository;

import com.zerozero.marryit.customer.domain.Customer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByWorkspaceIdOrderByIdDesc(Long workspaceId);

    Optional<Customer> findByIdAndWorkspaceId(Long id, Long workspaceId);
}
