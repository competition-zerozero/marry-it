package com.zerozero.marryit.agent.tool;

import com.zerozero.marryit.external.kakao.KakaoPlaceClient;
import com.zerozero.marryit.recommendation.service.VendorCandidateResponse;
import com.zerozero.marryit.vendor.domain.VendorCategory;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SearchExternalVendorsTool implements AgentTool {

    private final KakaoPlaceClient kakaoPlaceClient;

    public SearchExternalVendorsTool(KakaoPlaceClient kakaoPlaceClient) {
        this.kakaoPlaceClient = kakaoPlaceClient;
    }

    @Override
    public String name() {
        return "search_external_vendors";
    }

    @Override
    public String description() {
        return "search_workspace_vendors로 기존 거래처를 찾을 수 없거나 부족할 때만 호출하세요. "
                + "카테고리와 희망 지역으로 카카오맵에서 신규 업체 후보를 찾습니다. "
                + "결과는 검증되지 않은 외부 후보이므로 반드시 기존 업체와 구분해서 안내하세요.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return JsonSchema.object(
                Map.of(
                        "category", JsonSchema.enumProperty(
                                "string",
                                "찾으려는 업체 카테고리",
                                Arrays.stream(VendorCategory.values()).map(Enum::name).toList()
                        ),
                        "areaKeyword", JsonSchema.property("string", "희망 지역 키워드 (예: '강남')")
                ),
                List.of("category")
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments, AgentToolContext context) {
        String categoryValue = AgentToolArguments.optionalString(arguments, "category");
        if (categoryValue == null) {
            throw new IllegalArgumentException("Missing required argument: category");
        }
        VendorCategory category = VendorCategory.valueOf(categoryValue);
        String areaKeyword = AgentToolArguments.optionalString(arguments, "areaKeyword");

        String query = areaKeyword == null || areaKeyword.isBlank()
                ? category.name()
                : areaKeyword + " " + category.name();

        return kakaoPlaceClient.searchPlaces(query).stream()
                .map(place -> VendorCandidateResponse.kakaoExternal(place, category))
                .toList();
    }
}
