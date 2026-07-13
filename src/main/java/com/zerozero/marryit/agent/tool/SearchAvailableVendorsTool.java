package com.zerozero.marryit.agent.tool;

import com.zerozero.marryit.schedule.domain.Schedule;
import com.zerozero.marryit.schedule.domain.ScheduleTargetType;
import com.zerozero.marryit.schedule.repository.ScheduleRepository;
import com.zerozero.marryit.vendor.domain.Vendor;
import com.zerozero.marryit.vendor.domain.VendorCategory;
import com.zerozero.marryit.vendor.repository.VendorRepository;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SearchAvailableVendorsTool implements AgentTool {

    private final VendorRepository vendorRepository;
    private final ScheduleRepository scheduleRepository;

    public SearchAvailableVendorsTool(VendorRepository vendorRepository, ScheduleRepository scheduleRepository) {
        this.vendorRepository = vendorRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    public String name() {
        return "search_available_vendors";
    }

    @Override
    public String description() {
        return "고객의 날짜, 지역, 예산, 스타일 조건에 맞는 Workspace 등록 업체를 검색합니다. "
                + "가격과 예약 가능 여부는 구조화 데이터가 있는 범위에서만 판단하고, 없으면 unknown으로 반환합니다.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return JsonSchema.object(
                Map.of(
                        "category", JsonSchema.enumProperty(
                                "string",
                                "업체 종류",
                                Arrays.stream(VendorCategory.values()).map(Enum::name).toList()
                        ),
                        "usageDate", JsonSchema.property("string", "이용 날짜. yyyy-MM-dd"),
                        "area", JsonSchema.property("string", "지역 키워드"),
                        "maxBudget", JsonSchema.property("integer", "최대 예산"),
                        "preferredStyle", JsonSchema.property("string", "선호 스타일"),
                        "requiredConditions", JsonSchema.property("string", "필수 조건"),
                        "excludedConditions", JsonSchema.property("string", "제외 조건"),
                        "requireAvailable", JsonSchema.property("boolean", "예약 가능 업체만 볼지 여부")
                ),
                List.of("category")
        );
    }

    @Override
    public Object execute(Map<String, Object> arguments, AgentToolContext context) {
        VendorCategory category = VendorCategory.valueOf(AgentToolArguments.optionalString(arguments, "category"));
        LocalDate usageDate = optionalDate(arguments, "usageDate");
        String area = AgentToolArguments.optionalString(arguments, "area");
        Long maxBudget = AgentToolArguments.optionalLong(arguments, "maxBudget");
        String preferredStyle = AgentToolArguments.optionalString(arguments, "preferredStyle");
        String requiredConditions = AgentToolArguments.optionalString(arguments, "requiredConditions");
        String excludedConditions = AgentToolArguments.optionalString(arguments, "excludedConditions");
        boolean requireAvailable = optionalBoolean(arguments, "requireAvailable");

        List<AvailableVendorCandidateResult> candidates = vendorRepository
                .findByWorkspaceIdAndCategoryOrderByIdDesc(context.workspaceId(), category)
                .stream()
                .filter(vendor -> matchesArea(vendor, area))
                .filter(vendor -> matchesText(vendor, preferredStyle))
                .filter(vendor -> matchesText(vendor, requiredConditions))
                .filter(vendor -> doesNotContain(vendor, excludedConditions))
                .map(vendor -> toCandidate(vendor, context.workspaceId(), usageDate))
                .filter(candidate -> !requireAvailable || "AVAILABLE".equals(candidate.availability()))
                .toList();

        return new AvailableVendorResult(
                category.name(),
                usageDate,
                area,
                maxBudget,
                preferredStyle,
                candidates,
                List.of("업체 가격이 구조화되어 있지 않아 maxBudget은 후보 필터링에 사용하지 않고 budgetCheck를 UNKNOWN으로 반환합니다.")
        );
    }

    private AvailableVendorCandidateResult toCandidate(Vendor vendor, Long workspaceId, LocalDate usageDate) {
        String availability = availability(vendor, workspaceId, usageDate);
        return new AvailableVendorCandidateResult(
                vendor.getId(),
                vendor.getName(),
                vendor.getCategory().name(),
                firstNonBlank(vendor.getRoadAddress(), vendor.getAddress()),
                vendor.isPartnered(),
                buildReason(vendor),
                availability,
                "UNKNOWN",
                vendor.getMemo() == null || vendor.getMemo().isBlank() ? List.of() : List.of(vendor.getMemo())
        );
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

    private boolean matchesArea(Vendor vendor, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        return contains(vendor.getAddress(), keyword) || contains(vendor.getRoadAddress(), keyword);
    }

    private boolean matchesText(Vendor vendor, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        return contains(vendor.getName(), keyword)
                || contains(vendor.getMemo(), keyword)
                || contains(vendor.getContactPerson(), keyword);
    }

    private boolean doesNotContain(Vendor vendor, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        return !matchesText(vendor, keyword);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword.trim().toLowerCase());
    }

    private String buildReason(Vendor vendor) {
        StringBuilder reason = new StringBuilder("Workspace 등록 업체");
        if (vendor.isPartnered()) reason.append(" · 제휴 업체");
        if (vendor.getMemo() != null && !vendor.getMemo().isBlank()) reason.append(" · ").append(vendor.getMemo());
        return reason.toString();
    }

    private String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private LocalDate optionalDate(Map<String, Object> arguments, String key) {
        String value = AgentToolArguments.optionalString(arguments, key);
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }

    private boolean optionalBoolean(Map<String, Object> arguments, String key) {
        Object value = arguments.get(key);
        return value instanceof Boolean booleanValue ? booleanValue : value != null && Boolean.parseBoolean(value.toString());
    }
}
