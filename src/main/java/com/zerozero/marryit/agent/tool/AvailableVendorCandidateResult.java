package com.zerozero.marryit.agent.tool;

import java.util.List;

public record AvailableVendorCandidateResult(
        Long vendorId,
        String vendorName,
        String category,
        String address,
        boolean partnered,
        String matchReason,
        String availability,
        String budgetCheck,
        List<String> cautions
) {
}
