package com.zerozero.marryit.agent.tool;

import java.time.LocalDate;
import java.util.List;

public record AvailableVendorResult(
        String category,
        LocalDate usageDate,
        String area,
        Long maxBudget,
        String preferredStyle,
        List<AvailableVendorCandidateResult> candidates,
        List<String> missingDataWarnings
) {
}
