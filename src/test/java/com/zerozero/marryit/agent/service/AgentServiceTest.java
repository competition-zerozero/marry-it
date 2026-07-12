package com.zerozero.marryit.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zerozero.marryit.agent.tool.AgentToolRegistry;
import com.zerozero.marryit.auth.domain.OAuthProvider;
import com.zerozero.marryit.auth.domain.User;
import com.zerozero.marryit.auth.repository.UserRepository;
import com.zerozero.marryit.external.openai.OpenAiChatClient;
import com.zerozero.marryit.external.openai.OpenAiChatCompletionResponse;
import com.zerozero.marryit.external.openai.OpenAiChatMessage;
import com.zerozero.marryit.external.openai.OpenAiToolDefinition;
import com.zerozero.marryit.workspace.domain.Workspace;
import com.zerozero.marryit.workspace.domain.WorkspaceMember;
import com.zerozero.marryit.workspace.repository.WorkspaceMemberRepository;
import com.zerozero.marryit.workspace.repository.WorkspaceRepository;
import com.zerozero.marryit.workspace.service.WorkspaceAccessService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.ObjectMapper;

@DataJpaTest
@Import({
        AgentService.class,
        AgentOrchestrator.class,
        AgentToolRegistry.class,
        WorkspaceAccessService.class,
        AgentServiceTest.NotConfiguredOpenAiConfig.class
})
class AgentServiceTest {

    @Autowired
    private AgentService agentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Test
    void doesNotInventAnAnswerWhenOpenAiIsNotConfigured() {
        User planner = saveUser();
        Workspace workspace = saveWorkspaceWithOwner(planner);

        AgentResponse response = agentService.respond(workspace.getId(), planner.getId(), new AgentRequest("추천해줘"));

        assertThat(response.vendorRecommendation()).isNull();
        assertThat(response.toolCalls()).isEmpty();
        assertThat(response.answer()).contains("OPENAI_API_KEY");
    }

    @Test
    void blocksNonMemberFromUsingTheAgent() {
        User owner = saveUser();
        User outsider = userRepository.save(User.createOAuthUser(OAuthProvider.GOOGLE, "google-2", "outsider@example.com", "외부인", null));
        Workspace workspace = saveWorkspaceWithOwner(owner);

        assertThatThrownBy(() -> agentService.respond(workspace.getId(), outsider.getId(), new AgentRequest("추천해줘")))
                .isInstanceOf(SecurityException.class);
    }

    private User saveUser() {
        return userRepository.save(User.createOAuthUser(OAuthProvider.GOOGLE, "google-1", "planner@example.com", "서영", null));
    }

    private Workspace saveWorkspaceWithOwner(User owner) {
        Workspace workspace = workspaceRepository.save(Workspace.createPersonal(owner.getName()));
        workspaceMemberRepository.save(WorkspaceMember.owner(owner, workspace));
        return workspace;
    }

    @TestConfiguration
    static class NotConfiguredOpenAiConfig {

        @Bean
        OpenAiChatClient openAiChatClient() {
            return new OpenAiChatClient() {
                @Override
                public boolean isConfigured() {
                    return false;
                }

                @Override
                public OpenAiChatCompletionResponse complete(List<OpenAiChatMessage> messages, List<OpenAiToolDefinition> tools) {
                    throw new UnsupportedOperationException("OpenAI is not configured in this test.");
                }
            };
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
