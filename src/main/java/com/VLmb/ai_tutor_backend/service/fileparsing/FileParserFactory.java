package com.VLmb.ai_tutor_backend.service.fileparsing;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FileParserFactory {

    public FileParser getParser(ExtensionsEnum extension) {
        return switch (extension) {
            case PDF -> new PdfFileParser();
            case DOCX -> new DocxFileParser();
            case TXT -> new TxtFileParser();
        };
    }
}
