package com.VLmb.ai_tutor_backend.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
public class PdfParsingService {

    public String parsePdf(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             RandomAccessRead rar = new RandomAccessReadBuffer(inputStream);
             PDDocument document = Loader.loadPDF(rar)) {

            PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(document);
        }
    }
}
