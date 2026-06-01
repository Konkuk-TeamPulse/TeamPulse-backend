package com.teampulse.backend.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CommonApiResponseTest {

    @Test
    void apiResponseFactoriesCreateSuccessAndFailureShapes() {
        assertThat(ApiResponse.ok("data"))
                .satisfies(response -> {
                    assertThat(response.success()).isTrue();
                    assertThat(response.data()).isEqualTo("data");
                    assertThat(response.meta()).isNull();
                    assertThat(response.error()).isNull();
                });
        assertThat(ApiResponse.ok("data", Map.of("page", 1)))
                .satisfies(response -> {
                    assertThat(response.success()).isTrue();
                    assertThat(response.data()).isEqualTo("data");
                    assertThat(response.meta()).isEqualTo(Map.of("page", 1));
                });
        assertThat(ApiResponse.fail("CODE", "message"))
                .satisfies(response -> {
                    assertThat(response.success()).isFalse();
                    assertThat(response.error()).isEqualTo(new ApiError("CODE", "message", null));
                });
        assertThat(ApiResponse.fail("CODE", "message", Map.of("field", "name")))
                .satisfies(response -> {
                    assertThat(response.success()).isFalse();
                    assertThat(response.error()).isEqualTo(new ApiError("CODE", "message", Map.of("field", "name")));
                });
    }

    @Test
    void specResponseFactoriesCreateSuccessAndFailureShapes() {
        assertThat(SpecResponse.ok("ok", "result"))
                .satisfies(response -> {
                    assertThat(response.isSuccess()).isTrue();
                    assertThat(response.responseCode()).isEqualTo(1000);
                    assertThat(response.responseMessage()).isEqualTo("ok");
                    assertThat(response.result()).isEqualTo("result");
                });
        assertThat(SpecResponse.fail(2020, "fail", null))
                .satisfies(response -> {
                    assertThat(response.isSuccess()).isFalse();
                    assertThat(response.responseCode()).isEqualTo(2020);
                    assertThat(response.responseMessage()).isEqualTo("fail");
                    assertThat(response.result()).isNull();
                });
    }

    @Test
    void healthControllerExposesRuntimeSummaryForFrontendShell() {
        var controller = new HealthController();
        ReflectionTestUtils.setField(controller, "storageMode", "mysql");

        var response = controller.health();

        assertThat(response.success()).isTrue();
        assertThat(response.error()).isNull();
        assertThat(response.data())
                .containsEntry("status", "UP")
                .containsEntry("service", "teampulse-backend")
                .containsEntry("storageMode", "mysql")
                .containsEntry("publicApi", true);
        assertThat(response.data().get("deploymentTarget")).isInstanceOf(Map.class);
        assertThat(response.data().get("enums")).isInstanceOf(Map.class);
    }
}
