package com.VLmb.ai_tutor_backend.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class PdfParsingServiceTest {

    private final PdfParsingService pdfParsingService = new PdfParsingService();

    @Test
    void parsePdf_shouldReturnPdfText() throws IOException {
        String expectedText = "PDF parsing works";
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "sample.pdf",
                "application/pdf",
                createPdfBytes(expectedText)
        );

        String actualText = pdfParsingService.parsePdf(pdfFile);

        assertThat(actualText).contains(expectedText);
    }

    private byte[] createPdfBytes(String content) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();

                PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                contentStream.setFont(font, 12);

                contentStream.newLineAtOffset(25, 700);
                contentStream.showText(content);
                contentStream.endText();
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }
}
