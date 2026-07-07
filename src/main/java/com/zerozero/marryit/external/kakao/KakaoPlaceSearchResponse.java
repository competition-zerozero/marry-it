package com.zerozero.marryit.external.kakao;

import java.util.List;

record KakaoPlaceSearchResponse(
        List<KakaoPlaceDocument> documents
) {
}
