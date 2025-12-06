package com.VLmb.ai_tutor_backend.service.fileparsing;

import java.util.Arrays;
import java.util.Optional;

public enum ExtensionsEnum {
    PDF("pdf"),
    DOCX("docx"),
    TXT("txt");

    private final String value;

    ExtensionsEnum(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static Optional<ExtensionsEnum> fromValue(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(ext -> ext.value.equalsIgnoreCase(value))
                .findFirst();
    }
}
