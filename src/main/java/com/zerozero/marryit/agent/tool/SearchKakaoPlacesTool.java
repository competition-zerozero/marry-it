package com.zerozero.marryit.agent.tool;

import com.zerozero.marryit.external.kakao.KakaoPlaceClient;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SearchKakaoPlacesTool implements AgentTool {

    private final KakaoPlaceClient kakaoPlaceClient;

    public SearchKakaoPlacesTool(KakaoPlaceClient kakaoPlaceClient) {
        this.kakaoPlaceClient = kakaoPlaceClient;
    }

    @Override
    public String name() {
        return "search_kakao_places";
    }

    @Override
    public String description() {
        return "카카오맵에서 실제 장소를 자유 검색어로 검색합니다. 결과는 marry-it Workspace에 아직 등록되지 않은 "
                + "검증되지 않은 외부 후보입니다. 특정 장소를 확인하거나 주소를 찾을 때 사용하세요. "
                + "업체 조합/대체 추천에는 search_external_vendors를 사용하세요.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return JsonSchema.object(
                Map.of("query", JsonSchema.property("string", "카카오맵 검색어 (예: '강남 웨딩홀', '을지로 드레스샵')")),
                List.of("query")
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments, AgentToolContext context) {
        String query = AgentToolArguments.optionalString(arguments, "query");
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Missing required argument: query");
        }
        return kakaoPlaceClient.searchPlaces(query);
    }
}
