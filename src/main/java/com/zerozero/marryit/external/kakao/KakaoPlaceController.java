package com.zerozero.marryit.external.kakao;

import com.zerozero.marryit.auth.oauth.OAuth2LoginSuccessHandler;
import com.zerozero.marryit.workspace.service.WorkspaceAccessService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/external/kakao/places")
public class KakaoPlaceController {

    private final KakaoPlaceClient kakaoPlaceClient;
    private final WorkspaceAccessService workspaceAccessService;

    public KakaoPlaceController(KakaoPlaceClient kakaoPlaceClient, WorkspaceAccessService workspaceAccessService) {
        this.kakaoPlaceClient = kakaoPlaceClient;
        this.workspaceAccessService = workspaceAccessService;
    }

    @GetMapping
    public List<KakaoPlaceResponse> search(
            @PathVariable Long workspaceId,
            @RequestParam String query,
            HttpSession session
    ) {
        workspaceAccessService.validateMember(currentUserId(session), workspaceId);
        return kakaoPlaceClient.searchPlaces(query);
    }

    private Long currentUserId(HttpSession session) {
        Object userId = session.getAttribute(OAuth2LoginSuccessHandler.SESSION_USER_ID);
        if (!(userId instanceof Long id)) {
            throw new SecurityException("Login is required.");
        }
        return id;
    }
}
