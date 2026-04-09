package com.maintainance.service_center.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Custom deserializer for LocalTime that handles edge cases like "0" or empty strings.
 * Returns null for invalid values instead of throwing an exception.
 */
public class LocalTimeDeserializer extends JsonDeserializer<LocalTime> {

    private static final DateTimeFormatter[] FORMATTERS = {
        DateTimeFormatter.ofPattern("HH:mm:ss"),
        DateTimeFormatter.ofPattern("HH:mm"),
        DateTimeFormatter.ofPattern("H:mm:ss"),
        DateTimeFormatter.ofPattern("H:mm"),
        DateTimeFormatter.ofPattern("HHmmss"),
        DateTimeFormatter.ofPattern("HHmm")
    };

    @Override
    public LocalTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        
        if (node.isNull()) {
            return null;
        }
        
        String value = node.asText();
        
        // Handle empty or whitespace-only strings
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        // Handle "0" or numeric values that might represent midnight
        if ("0".equals(value.trim()) || "00:00".equals(value.trim())) {
            return LocalTime.MIDNIGHT;
        }
        
        // Try parsing with various formats
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalTime.parse(value, formatter);
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }
        
        // If all formatters fail, try the default parse
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            // Return null for invalid values instead of throwing exception
            return null;
        }
    }
}
