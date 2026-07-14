package com.zerozero.marryit.agent.mcp;

import com.zerozero.marryit.auth.oauth.OAuth2LoginSuccessHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("mcp")
public class McpOAuthController {

    public static final String SESSION_PENDING_MCP_OAUTH_REQUEST = "PENDING_MCP_OAUTH_REQUEST";

    private final McpOAuthService mcpOAuthService;
    private final String issuer;

    public McpOAuthController(
            McpOAuthService mcpOAuthService,
            @Value("${mcp.issuer:http://localhost:8000}") String issuer
    ) {
        this.mcpOAuthService = mcpOAuthService;
        this.issuer = issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;
    }

    @GetMapping({
            "/.well-known/oauth-authorization-server",
            "/.well-known/oauth-authorization-server/mcp"
    })
    public Map<String, Object> authorizationServerMetadata() {
        return Map.of(
                "issuer", issuer,
                "authorization_endpoint", issuer + "/mcp/oauth/authorize",
                "token_endpoint", issuer + "/mcp/oauth/token",
                "response_types_supported", java.util.List.of("code"),
                "grant_types_supported", java.util.List.of("authorization_code"),
                "code_challenge_methods_supported", java.util.List.of("plain", "S256"),
                "token_endpoint_auth_methods_supported", java.util.List.of("none")
        );
    }

    @GetMapping("/.well-known/oauth-protected-resource")
    public Map<String, Object> protectedResourceMetadata() {
        return Map.of(
                "resource", issuer + "/mcp",
                "authorization_servers", java.util.List.of(issuer)
        );
    }

    @GetMapping("/mcp/oauth/authorize")
    public void authorize(
            @RequestParam(name = "response_type", required = false) String responseType,
            @RequestParam(name = "redirect_uri") String redirectUri,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(name = "code_challenge", required = false) String codeChallenge,
            @RequestParam(name = "code_challenge_method", required = false) String codeChallengeMethod,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        if (responseType != null && !"code".equals(responseType)) {
            redirectWithError(response, redirectUri, state, "unsupported_response_type");
            return;
        }

        HttpSession session = request.getSession(true);
        session.setAttribute(
                SESSION_PENDING_MCP_OAUTH_REQUEST,
                new McpOAuthRequest(redirectUri, state, codeChallenge, codeChallengeMethod)
        );

        Object userId = session.getAttribute(OAuth2LoginSuccessHandler.SESSION_USER_ID);
        if (userId instanceof Long id) {
            redirectWithCode(response, id, redirectUri, state, codeChallenge, codeChallengeMethod);
            return;
        }

        response.sendRedirect("/oauth2/authorization/google");
    }

    @PostMapping(
            value = "/mcp/oauth/token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> token(@RequestParam MultiValueMap<String, String> form) {
        if (!"authorization_code".equals(form.getFirst("grant_type"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "unsupported_grant_type"));
        }

        try {
            return ResponseEntity.ok(mcpOAuthService.exchangeAuthorizationCode(
                    form.getFirst("code"),
                    form.getFirst("redirect_uri"),
                    form.getFirst("code_verifier")
            ));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "invalid_grant",
                    "error_description", exception.getMessage()
            ));
        }
    }

    public void completePendingAuthorization(HttpServletResponse response, Long userId, McpOAuthRequest pendingRequest)
            throws IOException {
        redirectWithCode(
                response,
                userId,
                pendingRequest.redirectUri(),
                pendingRequest.state(),
                pendingRequest.codeChallenge(),
                pendingRequest.codeChallengeMethod()
        );
    }

    private void redirectWithCode(
            HttpServletResponse response,
            Long userId,
            String redirectUri,
            String state,
            String codeChallenge,
            String codeChallengeMethod
    ) throws IOException {
        String code = mcpOAuthService.createAuthorizationCode(userId, redirectUri, codeChallenge, codeChallengeMethod);
        Map<String, String> params = new LinkedHashMap<>();
        params.put("code", code);
        if (state != null && !state.isBlank()) {
            params.put("state", state);
        }
        response.sendRedirect(redirectUri + "?" + encodeParams(params));
    }

    private void redirectWithError(HttpServletResponse response, String redirectUri, String state, String error)
            throws IOException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("error", error);
        if (state != null && !state.isBlank()) {
            params.put("state", state);
        }
        response.sendRedirect(redirectUri + "?" + encodeParams(params));
    }

    private String encodeParams(Map<String, String> params) {
        return params.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                        + "="
                        + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }
}
