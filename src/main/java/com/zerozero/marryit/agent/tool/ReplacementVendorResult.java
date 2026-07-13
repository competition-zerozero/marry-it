package com.zerozero.marryit.agent.tool;

import java.time.LocalDate;
import java.util.List;

public record ReplacementVendorResult(
        Long customerId,
        String customerName,
        String category,
        LocalDate weddingDate,
        String weddingArea,
        Long remainingBudget,
        VendorDetailResult canceledVendor,
        List<ReplacementVendorCandidateResult> candidates,
        List<String> missingDataWarnings
) {
}
