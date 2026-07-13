package com.zerozero.marryit.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.zerozero.marryit.auth.domain.OAuthProvider;
import com.zerozero.marryit.auth.domain.User;
import com.zerozero.marryit.auth.repository.UserRepository;
import com.zerozero.marryit.customer.domain.Customer;
import com.zerozero.marryit.customer.repository.CustomerRepository;
import com.zerozero.marryit.schedule.domain.Schedule;
import com.zerozero.marryit.schedule.domain.ScheduleTargetType;
import com.zerozero.marryit.schedule.domain.ScheduleType;
import com.zerozero.marryit.schedule.repository.ScheduleRepository;
import com.zerozero.marryit.vendor.domain.Vendor;
import com.zerozero.marryit.vendor.domain.VendorCategory;
import com.zerozero.marryit.vendor.repository.VendorRepository;
import com.zerozero.marryit.workspace.domain.Workspace;
import com.zerozero.marryit.workspace.repository.WorkspaceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({
        GetCustomerWeddingContextTool.class,
        SearchAvailableVendorsTool.class,
        FindReplacementVendorTool.class,
        GetUrgentTasksTool.class,
        GenerateCustomerBriefingTool.class
})
class MarryItMcpToolsTest {

    @Autowired
    private GetCustomerWeddingContextTool getCustomerWeddingContextTool;

    @Autowired
    private SearchAvailableVendorsTool searchAvailableVendorsTool;

    @Autowired
    private FindReplacementVendorTool findReplacementVendorTool;

    @Autowired
    private GetUrgentTasksTool getUrgentTasksTool;

    @Autowired
    private GenerateCustomerBriefingTool generateCustomerBriefingTool;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Test
    void getsCustomerWeddingContextWithoutInventingUnknownBudgetAndContracts() {
        Fixture fixture = saveFixture();

        CustomerWeddingContextResult result = (CustomerWeddingContextResult) getCustomerWeddingContextTool.execute(
                Map.of("customerName", "민지"),
                fixture.context()
        );

        assertThat(result.customerId()).isEqualTo(fixture.customer().getId());
        assertThat(result.weddingArea()).isEqualTo("강남");
        assertThat(result.usedBudget()).isNull();
        assertThat(result.remainingBudget()).isNull();
        assertThat(result.contractedVendors()).isEmpty();
        assertThat(result.missingDataWarnings()).isNotEmpty();
    }

    @Test
    void searchesAvailableVendorsByCategoryAreaStyleAndSchedule() {
        Fixture fixture = saveFixture();

        AvailableVendorResult result = (AvailableVendorResult) searchAvailableVendorsTool.execute(
                Map.of(
                        "category", "FLOWER",
                        "usageDate", fixture.customer().getWeddingDate().toString(),
                        "area", "강남",
                        "preferredStyle", "내추럴",
                        "requireAvailable", true
                ),
                fixture.context()
        );

        assertThat(result.candidates())
                .extracting(AvailableVendorCandidateResult::vendorName)
                .containsExactly("B 플라워", "A 플라워");
        assertThat(result.candidates())
                .extracting(AvailableVendorCandidateResult::availability)
                .containsOnly("AVAILABLE");
    }

    @Test
    void recommendsReplacementVendorWithAvailabilityAndRank() {
        Fixture fixture = saveFixture();

        ReplacementVendorResult result = (ReplacementVendorResult) findReplacementVendorTool.execute(
                Map.of(
                        "customerId", fixture.customer().getId(),
                        "category", "FLOWER",
                        "canceledVendorId", fixture.canceledVendor().getId()
                ),
                fixture.context()
        );

        assertThat(result.candidates()).isNotEmpty();
        ReplacementVendorCandidateResult first = result.candidates().get(0);
        assertThat(first.rank()).isEqualTo(1);
        assertThat(first.vendorName()).isEqualTo("B 플라워");
        assertThat(first.availability()).isEqualTo("AVAILABLE");
        assertThat(first.priceDifference()).isEqualTo("UNKNOWN");
    }

    @Test
    void returnsUrgentTasksOrderedByEmergencySignals() {
        Fixture fixture = saveFixture();

        @SuppressWarnings("unchecked")
        List<UrgentTaskResult> result = (List<UrgentTaskResult>) getUrgentTasksTool.execute(
                Map.of("limit", 5),
                fixture.context()
        );

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).urgency()).isEqualTo("HIGH");
        assertThat(result.get(0).task()).contains("취소");
    }

    @Test
    void generatesCustomerBriefingFromKnownCustomerData() {
        Fixture fixture = saveFixture();

        CustomerBriefingResult result = (CustomerBriefingResult) generateCustomerBriefingTool.execute(
                Map.of("customerId", fixture.customer().getId()),
                fixture.context()
        );

        assertThat(result.weddingContext().customerId()).isEqualTo(fixture.customer().getId());
        assertThat(result.budgetStatus()).contains("30000000");
        assertThat(result.recentProblems()).contains("취소");
        assertThat(result.decisionsForConsultation()).isNotEmpty();
    }

    private Fixture saveFixture() {
        User planner = userRepository.save(User.createOAuthUser(
                OAuthProvider.GOOGLE,
                "google-1",
                "planner@example.com",
                "서영",
                null
        ));
        Workspace workspace = workspaceRepository.save(Workspace.createPersonal(planner.getName()));
        LocalDate weddingDate = LocalDate.now().plusDays(3);
        Customer customer = customerRepository.save(Customer.create(
                workspace,
                planner,
                "준호",
                "민지",
                "010-1234-5678",
                "서울 강남구",
                weddingDate,
                "강남",
                200,
                30_000_000L,
                "밝고 자연스러운 분위기",
                "내추럴",
                "교통 편리",
                "어두운 분위기",
                "부케 150만 원",
                "부케 업체 취소 발생",
                "취소된 부케 업체 대체 후보 확인",
                "예식 지역 확정"
        ));

        Vendor canceledVendor = vendorRepository.save(sampleVendor(
                workspace,
                "place-1",
                "A 플라워",
                "서울 강남구",
                true,
                "내추럴 부케. 주말 예약 빠름"
        ));
        Vendor replacementVendor = vendorRepository.save(sampleVendor(
                workspace,
                "place-2",
                "B 플라워",
                "서울 강남구",
                true,
                "내추럴 스타일, 급한 주문 대응 가능"
        ));
        Vendor unavailableVendor = vendorRepository.save(sampleVendor(
                workspace,
                "place-3",
                "C 플라워",
                "서울 강남구",
                false,
                "내추럴 스타일"
        ));
        scheduleRepository.save(Schedule.create(
                workspace,
                ScheduleTargetType.VENDOR,
                unavailableVendor.getId(),
                ScheduleType.CONTRACT,
                "다른 고객 부케 예약",
                LocalDateTime.of(weddingDate, java.time.LocalTime.of(10, 0)),
                LocalDateTime.of(weddingDate, java.time.LocalTime.of(11, 0)),
                "서울 강남구"
        ));
        scheduleRepository.save(Schedule.create(
                workspace,
                ScheduleTargetType.CUSTOMER,
                customer.getId(),
                ScheduleType.CONSULTATION,
                "최종 상담",
                LocalDateTime.now().plusHours(2),
                LocalDateTime.now().plusHours(3),
                "강남"
        ));

        return new Fixture(customer, canceledVendor, replacementVendor, new AgentToolContext(workspace.getId(), planner.getId()));
    }

    private Vendor sampleVendor(
            Workspace workspace,
            String kakaoPlaceId,
            String name,
            String address,
            boolean partnered,
            String memo
    ) {
        return Vendor.create(
                workspace,
                kakaoPlaceId,
                name,
                VendorCategory.FLOWER,
                address,
                address + " 도로명",
                "02-123-4567",
                new BigDecimal("37.4980950"),
                new BigDecimal("127.0276100"),
                "https://place.map.kakao.com/" + kakaoPlaceId,
                partnered,
                "김담당",
                memo
        );
    }

    private record Fixture(
            Customer customer,
            Vendor canceledVendor,
            Vendor replacementVendor,
            AgentToolContext context
    ) {
    }
}
