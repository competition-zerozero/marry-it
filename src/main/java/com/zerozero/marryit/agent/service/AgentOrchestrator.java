package com.zerozero.marryit.agent.service;

import tools.jackson.databind.ObjectMapper;
import com.zerozero.marryit.agent.tool.AgentTool;
import com.zerozero.marryit.agent.tool.AgentToolContext;
import com.zerozero.marryit.agent.tool.AgentToolRegistry;
import com.zerozero.marryit.external.openai.OpenAiChatClient;
import com.zerozero.marryit.external.openai.OpenAiChatCompletionResponse;
import com.zerozero.marryit.external.openai.OpenAiChatMessage;
import com.zerozero.marryit.external.openai.OpenAiFunctionDefinition;
import com.zerozero.marryit.external.openai.OpenAiToolCall;
import com.zerozero.marryit.external.openai.OpenAiToolDefinition;
import com.zerozero.marryit.recommendation.service.VendorCandidateResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class AgentOrchestrator {

    private static final int MAX_TOOL_ITERATIONS = 6;

    private static final String SYSTEM_PROMPT = """
            당신은 웨딩플래너의 실제 업무를 돕는 marry-it의 AI Agent입니다.
            현재 로그인한 사용자와 워크스페이스는 이미 서버에서 확인 및 격리되었으므로, 당신은 도구를 통해 항상 그 범위 안의 데이터만 볼 수 있습니다.

            원칙:
            - 확인할 수 없는 가격, 일정, 계약 조건, 예약 가능 여부, 업체 특성을 절대 지어내지 마세요. 도구 결과에 없는 정보는 "확인이 필요합니다"라고 답하세요.
            - 업체를 추천할 때는 항상 search_workspace_vendors로 기존 거래처를 먼저 확인하세요. 기존 거래처로 충분하면 search_external_vendors나 search_kakao_places는 호출하지 마세요.
            - 기존 거래처만으로 부족할 때만 search_external_vendors(카카오맵)를 사용하고, 그 결과가 검증되지 않은 외부 후보임을 답변에 반드시 명시하세요.
            - 업체를 추천하기 전에 가능하면 get_vendor_experiences로 플래너들의 실제 경험을 확인해 근거로 삼으세요.
            - 신규 커플 업체 조합을 요청받으면 단일 업체가 아니라 예식장/스튜디오/드레스/메이크업 등 여러 카테고리를 조합해서 제시하세요.
            - 돌발상황(업체 취소 등) 요청에는 고객의 D-Day, 취향, 예산을 먼저 확인하고 기존 거래처 우선으로 대체 후보를 찾으세요.
            - 고객 이름만 언급되고 customerId를 모르면 list_customers로 먼저 찾으세요.
            - 최종 답변은 한국어로, 플래너가 바로 업무에 쓸 수 있도록 근거와 함께 간결하게 작성하세요.
            """;

    private final OpenAiChatClient openAiChatClient;
    private final AgentToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    public AgentOrchestrator(
            OpenAiChatClient openAiChatClient,
            AgentToolRegistry toolRegistry,
            ObjectMapper objectMapper
    ) {
        this.openAiChatClient = openAiChatClient;
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
    }

    public boolean isAvailable() {
        return openAiChatClient.isConfigured();
    }

    public AgentResult run(String userMessage, AgentToolContext context) {
        List<OpenAiChatMessage> messages = new ArrayList<>();
        messages.add(OpenAiChatMessage.system(SYSTEM_PROMPT));
        messages.add(OpenAiChatMessage.user(userMessage));

        List<OpenAiToolDefinition> toolDefinitions = toolRegistry.tools().stream()
                .map(this::toDefinition)
                .toList();

        List<AgentToolCallLog> toolCallLog = new ArrayList<>();
        List<VendorCandidateResponse> workspaceCandidates = new ArrayList<>();
        List<VendorCandidateResponse> externalCandidates = new ArrayList<>();

        for (int iteration = 0; iteration < MAX_TOOL_ITERATIONS; iteration++) {
            OpenAiChatCompletionResponse response = openAiChatClient.complete(messages, toolDefinitions);
            OpenAiChatMessage message = response.choices().get(0).message();

            if (message.toolCalls() == null || message.toolCalls().isEmpty()) {
                return new AgentResult(message.content(), toolCallLog, workspaceCandidates, externalCandidates);
            }

            messages.add(OpenAiChatMessage.assistant(message.content(), message.toolCalls()));

            for (OpenAiToolCall toolCall : message.toolCalls()) {
                Map<String, Object> arguments = parseArguments(toolCall.function().arguments());
                Object result;
                try {
                    result = toolRegistry.execute(toolCall.function().name(), arguments, context);
                } catch (Exception exception) {
                    result = Map.of("error", exception.getMessage() == null
                            ? "도구 실행 중 오류가 발생했습니다."
                            : exception.getMessage());
                }
                toolCallLog.add(new AgentToolCallLog(toolCall.function().name(), arguments.toString()));
                collectCandidates(toolCall.function().name(), result, workspaceCandidates, externalCandidates);
                messages.add(OpenAiChatMessage.toolResult(toolCall.id(), writeJson(result)));
            }
        }

        OpenAiChatCompletionResponse finalResponse = openAiChatClient.complete(messages, List.of());
        String finalAnswer = finalResponse.choices().get(0).message().content();
        return new AgentResult(finalAnswer, toolCallLog, workspaceCandidates, externalCandidates);
    }

    private OpenAiToolDefinition toDefinition(AgentTool tool) {
        return OpenAiToolDefinition.function(
                new OpenAiFunctionDefinition(tool.name(), tool.description(), tool.parametersSchema())
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseArguments(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(argumentsJson, Map.class);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Tool call arguments를 파싱할 수 없습니다: " + argumentsJson, exception);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Tool 결과를 직렬화할 수 없습니다.", exception);
        }
    }

    private void collectCandidates(
            String toolName,
            Object result,
            List<VendorCandidateResponse> workspaceCandidates,
            List<VendorCandidateResponse> externalCandidates
    ) {
        if (!(result instanceof List<?> list)) {
            return;
        }
        if ("search_workspace_vendors".equals(toolName)) {
            list.stream()
                    .filter(VendorCandidateResponse.class::isInstance)
                    .map(VendorCandidateResponse.class::cast)
                    .forEach(candidate -> addIfAbsent(workspaceCandidates, candidate, VendorCandidateResponse::vendorId));
        } else if ("search_external_vendors".equals(toolName)) {
            list.stream()
                    .filter(VendorCandidateResponse.class::isInstance)
                    .map(VendorCandidateResponse.class::cast)
                    .forEach(candidate -> addIfAbsent(externalCandidates, candidate, VendorCandidateResponse::kakaoPlaceId));
        }
    }

    private <T> void addIfAbsent(
            List<VendorCandidateResponse> candidates,
            VendorCandidateResponse candidate,
            Function<VendorCandidateResponse, T> key
    ) {
        boolean exists = candidates.stream()
                .anyMatch(existing -> Objects.equals(key.apply(existing), key.apply(candidate)));
        if (!exists) {
            candidates.add(candidate);
        }
    }
}
