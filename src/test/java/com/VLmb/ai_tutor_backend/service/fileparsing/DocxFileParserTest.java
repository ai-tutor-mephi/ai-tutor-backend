package com.VLmb.ai_tutor_backend.service.fileparsing;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class DocxFileParserTest {

    private final DocxFileParser parser = new DocxFileParser();

    @Test
    void parse_shouldReturnParagraphText() throws IOException {
        String expectedText = "DOCX parsing works too";
        MockMultipartFile docxFile = new MockMultipartFile(
                "file",
                "sample.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                createDocxBytes(expectedText)
        );

        String actualText = parser.parse(docxFile);

        assertThat(actualText).contains(expectedText);
    }

    private byte[] createDocxBytes(String text) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.createRun().setText(text);
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
