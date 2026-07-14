package com.zerozero.marryit.agent.mcp;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface McpAccessTokenRepository extends JpaRepository<McpAccessToken, Long> {

    Optional<McpAccessToken> findByToken(String token);
}
