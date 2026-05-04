package com.teampulse.backend.mobile.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FlexibleStringListDeserializer extends JsonDeserializer<List<String>> {

    @Override
    public List<String> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        var token = parser.currentToken();
        if (token == JsonToken.VALUE_NULL) {
            return List.of();
        }
        if (token == JsonToken.VALUE_STRING) {
            return normalize(List.of(parser.getValueAsString()));
        }
        if (token == JsonToken.START_ARRAY) {
            var values = new ArrayList<String>();
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                if (parser.currentToken() == JsonToken.VALUE_NULL) {
                    continue;
                }
                if (parser.currentToken() != JsonToken.VALUE_STRING) {
                    throw JsonMappingException.from(parser, "Expected string values in array.");
                }
                values.add(parser.getValueAsString());
            }
            return normalize(values);
        }
        throw JsonMappingException.from(parser, "Expected a string or string array.");
    }

    private List<String> normalize(List<String> values) {
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
    }
}
