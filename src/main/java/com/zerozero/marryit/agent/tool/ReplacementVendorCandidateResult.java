package com.zerozero.marryit.agent.tool;

import java.util.List;

public record ReplacementVendorCandidateResult(
        int rank,
        Long vendorId,
        String vendorName,
        String category,
        String address,
        boolean partnered,
        String recommendationReason,
        String priceDifference,
        String styleMatch,
        String availability,
        List<String> cautions
) {
}
