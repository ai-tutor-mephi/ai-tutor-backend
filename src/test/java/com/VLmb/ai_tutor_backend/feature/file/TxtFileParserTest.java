package com.VLmb.ai_tutor_backend.feature.file;

import com.VLmb.ai_tutor_backend.feature.file.fileparsing.TxtFileParser;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class TxtFileParserTest {

    private final TxtFileParser parser = new TxtFileParser();

    @Test
    void parse_shouldReturnTextContent() throws IOException {
        String expected = "Line1\nLine2";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-file.txt",
                "text/plain",
                expected.getBytes(StandardCharsets.UTF_8)
        );

        String actual = parser.parse(file);

        assertThat(actual).contains("Line1").contains("Line2");
    }
}
