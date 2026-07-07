package com.zerozero.marryit.customer.controller;

import com.zerozero.marryit.auth.oauth.OAuth2LoginSuccessHandler;
import com.zerozero.marryit.customer.service.CustomerRequest;
import com.zerozero.marryit.customer.service.CustomerResponse;
import com.zerozero.marryit.customer.service.CustomerService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse create(
            @PathVariable Long workspaceId,
            @Valid @RequestBody CustomerRequest request,
            HttpSession session
    ) {
        return customerService.create(workspaceId, currentUserId(session), request);
    }

    @GetMapping
    public List<CustomerResponse> findAll(@PathVariable Long workspaceId, HttpSession session) {
        return customerService.findAll(workspaceId, currentUserId(session));
    }

    @GetMapping("/{customerId}")
    public CustomerResponse get(
            @PathVariable Long workspaceId,
            @PathVariable Long customerId,
            HttpSession session
    ) {
        return customerService.get(workspaceId, currentUserId(session), customerId);
    }

    @PutMapping("/{customerId}")
    public CustomerResponse update(
            @PathVariable Long workspaceId,
            @PathVariable Long customerId,
            @Valid @RequestBody CustomerRequest request,
            HttpSession session
    ) {
        return customerService.update(workspaceId, currentUserId(session), customerId, request);
    }

    @DeleteMapping("/{customerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long workspaceId,
            @PathVariable Long customerId,
            HttpSession session
    ) {
        customerService.delete(workspaceId, currentUserId(session), customerId);
    }

    private Long currentUserId(HttpSession session) {
        Object userId = session.getAttribute(OAuth2LoginSuccessHandler.SESSION_USER_ID);
        if (!(userId instanceof Long id)) {
            throw new SecurityException("Login is required.");
        }
        return id;
    }
}
