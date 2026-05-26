package com.teampulse.backend.mobile.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.teampulse.backend.domain.risk.RiskSeverity;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class MobileDtoBranchTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void flexibleStringListDeserializerAcceptsStringNullAndArrayForms() throws Exception {
        assertThat(read("{\"values\":\"  one  \"}").values()).containsExactly("one");
        assertThat(read("{\"values\":[\" one \", null, \"\", \"two\"]}").values()).containsExactly("one", "two");

        var parser = objectMapper.createParser("null");
        parser.nextToken();
        assertThat(new FlexibleStringListDeserializer().deserialize(parser, null)).isEmpty();
    }

    @Test
    void flexibleStringListDeserializerRejectsUnsupportedShapes() {
        assertThatThrownBy(() -> read("{\"values\":123}"))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageContaining("Expected a string or string array.");
        assertThatThrownBy(() -> read("{\"values\":[\"one\", 2]}"))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageContaining("Expected string values in array.");
    }

    @Test
    void meetingViewConstructorsNormalizeTimestampsAndActionItems() {
        assertThat(new MeetingView(1L, "No time", "", "Agenda", List.of(), null).createdAt()).isEmpty();
        assertThat(new MeetingView(2L, "Date", "2026-06-01", "Agenda", List.of(), List.of("Action")).createdAt())
                .isEqualTo("2026-06-01T00:00:00");
        assertThat(new MeetingView(3L, "Date time", "2026-06-01T10:30", "Agenda", List.of(), Arrays.asList(" Action ", "", null)))
                .satisfies(meeting -> {
                    assertThat(meeting.createdAt()).isEqualTo("2026-06-01T10:30");
                    assertThat(meeting.actionItems()).containsExactly(new MeetingActionItemView(" Action ", null, null));
                });
    }

    @Test
    void riskViewNormalizesNullableCollectionsAndSuggestedActions() {
        assertThat(new RiskView(1L, RiskSeverity.INFO, "Risk", "Body", "Default", null, null))
                .satisfies(risk -> {
                    assertThat(risk.affectedTaskIds()).isEmpty();
                    assertThat(risk.suggestedActions()).containsExactly("Default");
                });
        assertThat(new RiskView(2L, RiskSeverity.WARNING, "Risk", "Body", "Default", List.of(10L), List.of("Custom")))
                .satisfies(risk -> {
                    assertThat(risk.affectedTaskIds()).containsExactly(10L);
                    assertThat(risk.suggestedActions()).containsExactly("Custom");
                });
    }

    private FlexibleTarget read(String json) throws Exception {
        return objectMapper.readValue(json, FlexibleTarget.class);
    }

    private static class FlexibleTarget {
        @JsonDeserialize(using = FlexibleStringListDeserializer.class)
        private List<String> values;

        List<String> values() {
            return values;
        }
    }
}
