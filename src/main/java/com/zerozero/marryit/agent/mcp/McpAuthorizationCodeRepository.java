package com.zerozero.marryit.agent.mcp;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface McpAuthorizationCodeRepository extends JpaRepository<McpAuthorizationCode, Long> {

    Optional<McpAuthorizationCode> findByCode(String code);
}
