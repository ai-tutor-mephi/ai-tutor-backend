package com.VLmb.ai_tutor_backend.service.fileparsing;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FileParsingIntegrationTest {

    @Autowired
    private PdfFileParser pdfFileParser;

    @Autowired
    private DocxFileParser docxFileParser;

    @Autowired
    private TxtFileParser txtFileParser;

    @Test
    void pdfParsingBean_shouldExtractText() throws IOException {
        String expected = "Integration PDF text";
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "integration.pdf",
                "application/pdf",
                createPdfBytes(expected)
        );

        String actual = pdfFileParser.parse(pdfFile);

        assertThat(actual).contains(expected);
    }

    @Test
    void docxParsingBean_shouldExtractText() throws IOException {
        String expected = "Integration DOCX text";
        MockMultipartFile docxFile = new MockMultipartFile(
                "file",
                "integration.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                createDocxBytes(expected)
        );

        String actual = docxFileParser.parse(docxFile);

        assertThat(actual).contains(expected);
    }

    @Test
    void txtParsingBean_shouldExtractText() throws IOException {
        String expected = "Integration\nTXT text";
        MockMultipartFile txtFile = new MockMultipartFile(
                "file",
                "integration.txt",
                "text/plain",
                expected.getBytes()
        );

        String actual = txtFileParser.parse(txtFile);

        assertThat(actual).contains("Integration").contains("TXT text");
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

    private byte[] createDocxBytes(String content) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.createRun().setText(content);
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
