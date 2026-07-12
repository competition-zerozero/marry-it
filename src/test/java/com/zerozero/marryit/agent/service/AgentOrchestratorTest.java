package com.zerozero.marryit.agent.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.zerozero.marryit.agent.tool.AgentTool;
import com.zerozero.marryit.agent.tool.AgentToolContext;
import com.zerozero.marryit.agent.tool.AgentToolRegistry;
import com.zerozero.marryit.external.openai.OpenAiChatClient;
import com.zerozero.marryit.external.openai.OpenAiChatCompletionResponse;
import com.zerozero.marryit.external.openai.OpenAiChatCompletionResponse.OpenAiChoice;
import com.zerozero.marryit.external.openai.OpenAiChatMessage;
import com.zerozero.marryit.external.openai.OpenAiFunctionCall;
import com.zerozero.marryit.external.openai.OpenAiToolCall;
import com.zerozero.marryit.external.openai.OpenAiToolDefinition;
import com.zerozero.marryit.recommendation.service.VendorCandidateResponse;
import com.zerozero.marryit.recommendation.service.VendorCandidateSource;
import com.zerozero.marryit.vendor.domain.VendorCategory;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AgentOrchestratorTest {

    @Test
    void aggregatesWorkspaceCandidatesFoundThroughToolCalls() {
        AgentTool searchTool = tool("search_workspace_vendors", (arguments, context) -> List.of(
                new VendorCandidateResponse(
                        VendorCandidateSource.WORKSPACE, 1L, "kakao-1", "A홀",
                        VendorCategory.WEDDING_HALL, "서울 강남구", "기존 거래처"
                )
        ));
        FakeOpenAiChatClient client = new FakeOpenAiChatClient(List.of(
                toolCallResponse("call-1", "search_workspace_vendors", "{\"category\":\"WEDDING_HALL\"}"),
                finalResponse("A홀을 추천합니다.")
        ));
        AgentOrchestrator orchestrator = new AgentOrchestrator(client, new AgentToolRegistry(List.of(searchTool)), new ObjectMapper());

        AgentResult result = orchestrator.run("강남 웨딩홀 추천해줘", new AgentToolContext(1L, 1L));

        assertThat(result.answer()).isEqualTo("A홀을 추천합니다.");
        assertThat(result.workspaceCandidates()).hasSize(1);
        assertThat(result.externalCandidates()).isEmpty();
        assertThat(result.toolCalls()).extracting(AgentToolCallLog::tool).containsExactly("search_workspace_vendors");
    }

    @Test
    void surfacesToolFailureInsteadOfLettingTheModelInventData() {
        AgentTool failingTool = tool("get_customer", (arguments, context) -> {
            throw new IllegalArgumentException("해당 Workspace에서 customerId 999를 찾을 수 없습니다.");
        });
        FakeOpenAiChatClient client = new FakeOpenAiChatClient(List.of(
                toolCallResponse("call-1", "get_customer", "{\"customerId\":999}"),
                finalResponse("고객 정보를 확인할 수 없습니다.")
        ));
        AgentOrchestrator orchestrator = new AgentOrchestrator(client, new AgentToolRegistry(List.of(failingTool)), new ObjectMapper());

        AgentResult result = orchestrator.run("999번 고객 알려줘", new AgentToolContext(1L, 1L));

        assertThat(result.answer()).isEqualTo("고객 정보를 확인할 수 없습니다.");
        OpenAiChatMessage toolResultMessage = client.capturedCalls().get(1).get(client.capturedCalls().get(1).size() - 1);
        assertThat(toolResultMessage.role()).isEqualTo("tool");
        assertThat(toolResultMessage.content()).contains("찾을 수 없습니다");
    }

    @Test
    void forcesAFinalAnswerOnceTheIterationCapIsReached() {
        AgentTool noopTool = tool("noop", (arguments, context) -> Map.of("ok", true));
        List<OpenAiChatCompletionResponse> scripted = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            scripted.add(toolCallResponse("call-" + i, "noop", "{}"));
        }
        scripted.add(finalResponse("반복 한도에 도달해 지금까지 확인한 내용으로 답변합니다."));
        FakeOpenAiChatClient client = new FakeOpenAiChatClient(scripted);
        AgentOrchestrator orchestrator = new AgentOrchestrator(client, new AgentToolRegistry(List.of(noopTool)), new ObjectMapper());

        AgentResult result = orchestrator.run("계속 도구를 불러줘", new AgentToolContext(1L, 1L));

        assertThat(result.answer()).isEqualTo("반복 한도에 도달해 지금까지 확인한 내용으로 답변합니다.");
        assertThat(client.capturedCalls()).hasSize(7);
    }

    private AgentTool tool(String name, java.util.function.BiFunction<Map<String, Object>, AgentToolContext, Object> execution) {
        return new AgentTool() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return "test tool";
            }

            @Override
            public Map<String, Object> parametersSchema() {
                return Map.of("type", "object", "properties", Map.of());
            }

            @Override
            public Object execute(Map<String, Object> arguments, AgentToolContext context) {
                return execution.apply(arguments, context);
            }
        };
    }

    private static OpenAiChatCompletionResponse toolCallResponse(String callId, String toolName, String argumentsJson) {
        OpenAiToolCall toolCall = new OpenAiToolCall(callId, "function", new OpenAiFunctionCall(toolName, argumentsJson));
        OpenAiChatMessage message = OpenAiChatMessage.assistant(null, List.of(toolCall));
        return new OpenAiChatCompletionResponse(List.of(new OpenAiChoice(message, "tool_calls")));
    }

    private static OpenAiChatCompletionResponse finalResponse(String content) {
        OpenAiChatMessage message = OpenAiChatMessage.assistant(content, null);
        return new OpenAiChatCompletionResponse(List.of(new OpenAiChoice(message, "stop")));
    }

    private static final class FakeOpenAiChatClient implements OpenAiChatClient {

        private final Deque<OpenAiChatCompletionResponse> scriptedResponses;
        private final List<List<OpenAiChatMessage>> capturedCalls = new ArrayList<>();

        private FakeOpenAiChatClient(List<OpenAiChatCompletionResponse> responses) {
            this.scriptedResponses = new ArrayDeque<>(responses);
        }

        @Override
        public boolean isConfigured() {
            return true;
        }

        @Override
        public OpenAiChatCompletionResponse complete(List<OpenAiChatMessage> messages, List<OpenAiToolDefinition> tools) {
            capturedCalls.add(List.copyOf(messages));
            if (scriptedResponses.isEmpty()) {
                throw new IllegalStateException("No more scripted responses configured for this test.");
            }
            return scriptedResponses.poll();
        }

        private List<List<OpenAiChatMessage>> capturedCalls() {
            return capturedCalls;
        }
    }
}
