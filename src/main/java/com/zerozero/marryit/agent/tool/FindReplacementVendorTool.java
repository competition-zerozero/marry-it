package com.zerozero.marryit.agent.tool;

import com.zerozero.marryit.customer.domain.Customer;
import com.zerozero.marryit.customer.repository.CustomerRepository;
import com.zerozero.marryit.schedule.domain.Schedule;
import com.zerozero.marryit.schedule.domain.ScheduleTargetType;
import com.zerozero.marryit.schedule.repository.ScheduleRepository;
import com.zerozero.marryit.vendor.domain.Vendor;
import com.zerozero.marryit.vendor.domain.VendorCategory;
import com.zerozero.marryit.vendor.repository.VendorRepository;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

@Component
public class FindReplacementVendorTool implements AgentTool {

    private final CustomerRepository customerRepository;
    private final VendorRepository vendorRepository;
    private final ScheduleRepository scheduleRepository;

    public FindReplacementVendorTool(
            CustomerRepository customerRepository,
            VendorRepository vendorRepository,
            ScheduleRepository scheduleRepository
    ) {
        this.customerRepository = customerRepository;
        this.vendorRepository = vendorRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    public String name() {
        return "find_replacement_vendor";
    }

    @Override
    public String description() {
        return "기존 계약 업체가 취소되거나 문제가 발생했을 때 고객의 예식일, 지역, 스타일에 맞는 Workspace 대체 업체를 추천합니다. "
                + "가격은 구조화 데이터가 없으면 UNKNOWN으로 반환하며 임의 추정하지 않습니다.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return JsonSchema.object(
                Map.of(
                        "customerId", JsonSchema.property("integer", "고객 ID"),
                        "customerName", JsonSchema.property("string", "고객 이름 일부. customerId가 없을 때 사용"),
                        "category", JsonSchema.enumProperty(
                                "string",
                                "문제가 발생한 업체 종류",
                                Arrays.stream(VendorCategory.values()).map(Enum::name).toList()
                        ),
                        "canceledVendorId", JsonSchema.property("integer", "취소된 기존 업체 ID"),
                        "problemDescription", JsonSchema.property("string", "취소 또는 문제 상황 설명")
                ),
                List.of("category")
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments, AgentToolContext context) {
        Customer customer = AgentCustomerFinder.find(customerRepository, arguments, context);
        VendorCategory category = VendorCategory.valueOf(AgentToolArguments.optionalString(arguments, "category"));
        Long canceledVendorId = AgentToolArguments.optionalLong(arguments, "canceledVendorId");
        Vendor canceledVendor = canceledVendorId == null ? null : vendorRepository
                .findByIdAndWorkspaceId(canceledVendorId, context.workspaceId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "해당 Workspace에서 canceledVendorId " + canceledVendorId + "를 찾을 수 없습니다."));

        List<ScoredVendor> scoredVendors = vendorRepository
                .findByWorkspaceIdAndCategoryOrderByIdDesc(context.workspaceId(), category)
                .stream()
                .filter(vendor -> canceledVendorId == null || !vendor.getId().equals(canceledVendorId))
                .map(vendor -> score(vendor, customer, context.workspaceId()))
                .sorted(Comparator.comparingInt(ScoredVendor::score).reversed())
                .toList();

        List<ScoredVendor> topVendors = scoredVendors.stream().limit(5).toList();
        List<ReplacementVendorCandidateResult> candidates = IntStream.range(0, topVendors.size())
                .mapToObj(index -> toCandidate(index + 1, topVendors.get(index), context.workspaceId(), customer.getWeddingDate()))
                .toList();

        return new ReplacementVendorResult(
                customer.getId(),
                customer.getBrideName() + "·" + customer.getGroomName(),
                category.name(),
                customer.getWeddingDate(),
                customer.getPreferredWeddingArea(),
                null,
                canceledVendor == null ? null : VendorDetailResult.from(canceledVendor),
                candidates,
                List.of(
                        "계약/지출 도메인이 아직 없어 남은 예산과 기존 업체 가격 차이는 UNKNOWN입니다.",
                        "예약 가능 여부는 업체 일정에 같은 날짜 일정이 있는지만 기준으로 판단합니다."
                )
        );
    }

    private ScoredVendor score(Vendor vendor, Customer customer, Long workspaceId) {
        int score = 0;
        if (vendor.isPartnered()) score += 20;
        if (matchesArea(vendor, customer.getPreferredWeddingArea())) score += 30;
        if (matchesStyle(vendor, customer.getPreferredStyle()) || matchesStyle(vendor, customer.getPreferredAtmosphere())) {
            score += 30;
        }
        if ("AVAILABLE".equals(availability(vendor, workspaceId, customer.getWeddingDate()))) score += 20;
        return new ScoredVendor(vendor, score);
    }

    private ReplacementVendorCandidateResult toCandidate(int rank, ScoredVendor scoredVendor, Long workspaceId, LocalDate weddingDate) {
        Vendor vendor = scoredVendor.vendor();
        String availability = availability(vendor, workspaceId, weddingDate);
        return new ReplacementVendorCandidateResult(
                rank,
                vendor.getId(),
                vendor.getName(),
                vendor.getCategory().name(),
                firstNonBlank(vendor.getRoadAddress(), vendor.getAddress()),
                vendor.isPartnered(),
                replacementReason(vendor, availability),
                "UNKNOWN",
                vendor.getMemo() == null || vendor.getMemo().isBlank() ? "UNKNOWN" : "CHECK_MEMO",
                availability,
                vendor.getMemo() == null || vendor.getMemo().isBlank() ? List.of() : List.of(vendor.getMemo())
        );
    }

    private String replacementReason(Vendor vendor, String availability) {
        StringBuilder reason = new StringBuilder("같은 카테고리의 Workspace 등록 업체");
        if (vendor.isPartnered()) reason.append(" · 제휴 업체");
        reason.append(" · 예약 상태: ").append(availability);
        if (vendor.getMemo() != null && !vendor.getMemo().isBlank()) reason.append(" · ").append(vendor.getMemo());
        return reason.toString();
    }

    private String availability(Vendor vendor, Long workspaceId, LocalDate usageDate) {
        if (usageDate == null) {
            return "UNKNOWN";
        }
        boolean hasConflict = scheduleRepository
                .findByWorkspaceIdAndTargetTypeAndTargetIdOrderByStartsAtAsc(
                        workspaceId, ScheduleTargetType.VENDOR, vendor.getId())
                .stream()
                .map(Schedule::getStartsAt)
                .anyMatch(startsAt -> startsAt.toLocalDate().equals(usageDate));
        return hasConflict ? "UNAVAILABLE" : "AVAILABLE";
    }

    private boolean matchesArea(Vendor vendor, String area) {
        if (area == null || area.isBlank()) return false;
        return contains(vendor.getAddress(), area) || contains(vendor.getRoadAddress(), area);
    }

    private boolean matchesStyle(Vendor vendor, String style) {
        if (style == null || style.isBlank()) return false;
        return Arrays.stream(style.split("[,\\s]+"))
                .filter(token -> !token.isBlank())
                .anyMatch(token -> contains(vendor.getMemo(), token));
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword.trim().toLowerCase());
    }

    private String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private record ScoredVendor(Vendor vendor, int score) {
    }
}
