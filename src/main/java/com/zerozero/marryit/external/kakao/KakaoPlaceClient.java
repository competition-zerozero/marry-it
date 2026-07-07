package com.zerozero.marryit.external.kakao;

import java.util.List;

public interface KakaoPlaceClient {

    List<KakaoPlaceResponse> searchPlaces(String query);
}
