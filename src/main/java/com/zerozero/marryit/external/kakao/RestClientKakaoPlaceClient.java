package com.zerozero.marryit.external.kakao;

import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestClientKakaoPlaceClient implements KakaoPlaceClient {

    private final KakaoPlaceClientProperties properties;
    private final KakaoPlaceMapper mapper;
    private final RestClient restClient;

    public RestClientKakaoPlaceClient(
            KakaoPlaceClientProperties properties,
            KakaoPlaceMapper mapper
    ) {
        this.properties = properties;
        this.mapper = mapper;
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl() == null ? "https://dapi.kakao.com" : properties.baseUrl())
                .build();
    }

    @Override
    public List<KakaoPlaceResponse> searchPlaces(String query) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalStateException("Kakao API key is not configured.");
        }

        KakaoPlaceSearchResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/search/keyword.json")
                        .queryParam("query", query)
                        .build()
                )
                .header("Authorization", "KakaoAK " + properties.apiKey())
                .retrieve()
                .body(KakaoPlaceSearchResponse.class);

        if (response == null || response.documents() == null) {
            return List.of();
        }
        return response.documents().stream()
                .map(mapper::map)
                .toList();
    }
}
