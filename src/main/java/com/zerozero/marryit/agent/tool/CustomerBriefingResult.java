package com.zerozero.marryit.agent.tool;

import java.util.List;

public record CustomerBriefingResult(
        CustomerWeddingContextResult weddingContext,
        String customerPersonality,
        String preferredStyle,
        String budgetStatus,
        String progressSummary,
        List<VendorDetailResult> contractedVendors,
        String incompleteTasks,
        String recentProblems,
        List<String> decisionsForConsultation,
        List<String> cautions
) {
}
