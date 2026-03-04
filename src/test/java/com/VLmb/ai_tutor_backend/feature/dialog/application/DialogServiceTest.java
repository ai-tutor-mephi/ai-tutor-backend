package com.VLmb.ai_tutor_backend.feature.dialog.application;

import com.VLmb.ai_tutor_backend.feature.auth.domain.User;
import com.VLmb.ai_tutor_backend.feature.dialog.domain.Dialog;
import com.VLmb.ai_tutor_backend.feature.dialog.infra.DialogRepository;
import com.VLmb.ai_tutor_backend.feature.file.application.DialogFileResponse;
import com.VLmb.ai_tutor_backend.feature.file.application.FileStorageService;
import com.VLmb.ai_tutor_backend.feature.file.domain.FileMetadata;
import com.VLmb.ai_tutor_backend.feature.file.fileparsing.ExtensionsEnum;
import com.VLmb.ai_tutor_backend.feature.file.fileparsing.FileParser;
import com.VLmb.ai_tutor_backend.feature.file.fileparsing.FileParserFactory;
import com.VLmb.ai_tutor_backend.feature.file.infra.FileMetadataRepository;
import com.VLmb.ai_tutor_backend.feature.rag.application.RagCommunicationService;
import com.VLmb.ai_tutor_backend.shared.error.exceptions.TextExtractionException;
import com.VLmb.ai_tutor_backend.shared.error.exceptions.UpstreamClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DialogServiceTest {

    @Mock
    private DialogRepository dialogRepository;
    @Mock
    private FileMetadataRepository fileMetadataRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private RagCommunicationService ragCommunicationService;

    @InjectMocks
    private DialogService dialogService;

    private User testUser;
    private Dialog testDialog;
    private MockMultipartFile testFile;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);

        testDialog = new Dialog();
        testDialog.setId(100L);
        testDialog.setOwner(testUser);

        testFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "dummy content".getBytes()
        );
    }

    @Test
    public void shouldAddTextFileToDialogWhenParsingAndRagSucceed() throws IOException {
        when(dialogRepository.findById(100L)).thenReturn(Optional.of(testDialog));

        FileMetadata savedMetadata = new FileMetadata();
        savedMetadata.setId(500L);
        savedMetadata.setOriginalFileName("test.pdf");

        when(fileMetadataRepository.save(any(FileMetadata.class))).thenReturn(savedMetadata);
        FileParser mockParser = mock(FileParser.class);
        when(mockParser.parse(any())).thenReturn("Extracted text from PDF");

        try (MockedStatic<FileParserFactory> mockedFactory = mockStatic(FileParserFactory.class)) {
            mockedFactory.when(() -> FileParserFactory.getParser(ExtensionsEnum.PDF))
                    .thenReturn(mockParser);

            List<DialogFileResponse> responses = dialogService.addFilesToDialog(
                    100L, testUser, new MultipartFile[]{testFile}
            );

            assertEquals(1, responses.size());
            assertEquals(500L, responses.get(0).fileId());
            assertEquals("test.pdf", responses.get(0).originalFileName());

            verify(fileStorageService, times(1))
                    .uploadFile(anyString(), any(), anyLong());

            verify(fileMetadataRepository, times(1)).save(any(FileMetadata.class));

            verify(ragCommunicationService, times(1))
                    .loadFileToRag(eq(100L), anyList());
        }
    }

    @Test
    public void shouldThrowTextExtractionExceptionWhenExtractedTextIsBlank() throws IOException {
        when(dialogRepository.findById(100L)).thenReturn(Optional.of(testDialog));

        FileParser mockParser = mock(FileParser.class);
        when(mockParser.parse(any())).thenReturn("  ");

        try (MockedStatic<FileParserFactory> mockedFactory = mockStatic(FileParserFactory.class)) {
            mockedFactory.when(() -> FileParserFactory.getParser(ExtensionsEnum.PDF)).thenReturn(mockParser);

            assertThrows(TextExtractionException.class, () -> dialogService.addFilesToDialog(
                    100L,
                    testUser,
                    new MultipartFile[]{testFile}
            ));

            verifyNoInteractions(fileStorageService);
            verifyNoInteractions(fileMetadataRepository);
            verifyNoInteractions(ragCommunicationService);
        }
    }

    @Test
    public void shouldCleanupStorageAndMetadataWhenRagFails() throws IOException {
        when(dialogRepository.findById(100L)).thenReturn(Optional.of(testDialog));

        FileParser mockParser = mock(FileParser.class);
        when(mockParser.parse(any())).thenReturn("some content");

        try (MockedStatic<FileParserFactory> mockedFactory = mockStatic(FileParserFactory.class)) {
            mockedFactory.when(() -> FileParserFactory.getParser(ExtensionsEnum.PDF)).thenReturn(mockParser);

            when(fileMetadataRepository.save(any(FileMetadata.class)))
                    .thenAnswer(invocation -> {
                        FileMetadata original = invocation.getArgument(0);
                        original.setId(1L);

                        return original;
                    });

            doThrow(new UpstreamClientException(HttpStatus.BAD_REQUEST, "cannot parse document"))
                    .when(ragCommunicationService).loadFileToRag(anyLong(), anyList());

            assertThrows(UpstreamClientException.class, () -> dialogService.addFilesToDialog(
                    100L,
                    testUser,
                    new MultipartFile[]{testFile}
            ));

            verify(fileStorageService, times(1))
                    .uploadFile(anyString(), any(), anyLong());
            verify(fileMetadataRepository, times(1))
                    .save(any());
            verify(fileStorageService, times(1))
                    .deleteFile(anyString());
            verify(fileMetadataRepository, times(1))
                    .deleteById(anyLong());
        }
    }

}









