package com.teampulse.backend.workspace.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class StringListJsonConverterTest {

    private final StringListJsonConverter converter = new StringListJsonConverter();

    @Test
    void convertsNullAndListAttributesToJson() {
        assertThat(converter.convertToDatabaseColumn(null)).isEqualTo("[]");
        assertThat(converter.convertToDatabaseColumn(List.of("alpha", "beta"))).isEqualTo("[\"alpha\",\"beta\"]");
    }

    @Test
    void convertsNullBlankAndJsonColumnsToLists() {
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
        assertThat(converter.convertToEntityAttribute(" ")).isEmpty();
        assertThat(converter.convertToEntityAttribute("[\"alpha\",\"beta\"]")).containsExactly("alpha", "beta");
    }

    @Test
    void rejectsInvalidJsonColumns() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("{not-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed to deserialize string list.");
    }
}
