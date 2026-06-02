package com.teampulse.backend.workspace.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest
@ActiveProfiles("demo")
class PublicApiContractTest {

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void frontendVisibleApiContractStaysStable() {
        assertThat(publicMappings()).contains(
                "POST /api/auth/signup",
                "POST /api/auth/login",
                "POST /api/auth/logout",
                "GET /api/health",
                "GET /api/users/me",
                "POST /api/projects",
                "GET /api/projects",
                "GET /api/projects/{projectId}",
                "PATCH /api/projects/{projectId}",
                "GET /api/projects/{projectId}/dashboard",
                "GET /api/projects/{projectId}/members",
                "DELETE /api/projects/{projectId}/members/me",
                "DELETE /api/projects/{projectId}/members/{memberId}",
                "POST /api/projects/{projectId}/invitations",
                "GET /api/invitations/{token}",
                "POST /api/invitations/{token}/accept",
                "GET /api/projects/{projectId}/activity-logs",
                "GET /api/projects/{projectId}/risks",
                "POST /api/projects/{projectId}/reports",
                "GET /api/reports/{reportId}/download",
                "GET /api/projects/{projectId}/meetings",
                "POST /api/projects/{projectId}/meetings",
                "GET /api/meetings/{meetingId}",
                "GET /api/projects/{projectId}/tasks",
                "POST /api/projects/{projectId}/tasks",
                "PATCH /api/tasks/{taskId}",
                "DELETE /api/tasks/{taskId}",
                "PATCH /api/tasks/{taskId}/status",
                "POST /api/tasks/{taskId}/dependencies",
                "DELETE /api/tasks/{taskId}/dependencies/{dependencyId}");
    }

    @Test
    void legacyMvpPathsAreNotExposedAsPublicApi() {
        assertThat(publicMappings())
                .noneMatch(mapping -> mapping.contains("/api/mobile"))
                .noneMatch(mapping -> mapping.contains("/invite-links"))
                .noneMatch(mapping -> mapping.contains("/api/account"))
                .noneMatch(mapping -> mapping.contains("/api/roadmap"));
    }

    @Test
    void workspaceControllersOwnProjectApiHandlers() {
        var handlerPackages = handlerMapping.getHandlerMethods().values().stream()
                .map(handlerMethod -> handlerMethod.getBeanType().getPackageName())
                .collect(Collectors.toSet());

        assertThat(handlerPackages)
                .anyMatch(packageName -> packageName.equals("com.teampulse.backend.workspace.api"))
                .noneMatch(packageName -> packageName.startsWith("com.teampulse.backend.mobile"));
    }

    private Set<String> publicMappings() {
        return handlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(this::mappingEntries)
                .collect(Collectors.toSet());
    }

    private Stream<String> mappingEntries(RequestMappingInfo info) {
        return pathPatterns(info).flatMap(pattern -> methods(info)
                .map(method -> method.name() + " " + pattern));
    }

    private Stream<String> pathPatterns(RequestMappingInfo info) {
        var pathPatterns = info.getPathPatternsCondition();
        if (pathPatterns != null) {
            return pathPatterns.getPatterns().stream()
                    .map(pattern -> pattern.getPatternString());
        }
        var patterns = info.getPatternsCondition();
        if (patterns != null) {
            return patterns.getPatterns().stream();
        }
        return Stream.empty();
    }

    private Stream<RequestMethod> methods(RequestMappingInfo info) {
        return info.getMethodsCondition().getMethods().stream();
    }
}
