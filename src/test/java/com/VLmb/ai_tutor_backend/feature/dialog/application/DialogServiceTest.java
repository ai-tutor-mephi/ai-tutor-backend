package com.VLmb.ai_tutor_backend.feature.dialog.application;

import com.VLmb.ai_tutor_backend.feature.auth.domain.User;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.SendMessageRequest;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.SendMessageResponse;
import com.VLmb.ai_tutor_backend.feature.dialog.domain.Dialog;
import com.VLmb.ai_tutor_backend.feature.dialog.domain.Message;
import com.VLmb.ai_tutor_backend.feature.dialog.infra.DialogRepository;
import com.VLmb.ai_tutor_backend.feature.dialog.infra.MessageRepository;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
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
    @Mock
    private MessageRepository messageRepository;

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

    @Test
    public void shouldAddAllFilesWhenAllUploadsSucceed() throws IOException {
        when(dialogRepository.findById(100L)).thenReturn(Optional.of(testDialog));

        MockMultipartFile file1 = new MockMultipartFile(
                "files",
                "doc1.pdf",
                "application/pdf",
                "some content 1".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "files",
                "doc2.pdf",
                "application/pdf",
                "some content 2".getBytes()
        );
        MockMultipartFile file3 = new MockMultipartFile(
                "files",
                "doc3.pdf",
                "application/pdf",
                "some content 3".getBytes()
        );

        FileMetadata savedFile1 = new FileMetadata();
        savedFile1.setId(501L);
        savedFile1.setOriginalFileName("doc1.pdf");

        FileMetadata savedFile2 = new FileMetadata();
        savedFile2.setId(502L);
        savedFile2.setOriginalFileName("doc2.pdf");

        FileMetadata savedFile3 = new FileMetadata();
        savedFile2.setId(503L);
        savedFile2.setOriginalFileName("doc3.pdf");

        AtomicLong idGenerator = new AtomicLong(501L);

        when(fileMetadataRepository.save(any(FileMetadata.class)))
                .thenAnswer(invocation -> {
                    FileMetadata metadata = invocation.getArgument(0);
                    metadata.setId(idGenerator.getAndIncrement());
                    return metadata;
                });

        FileParser mockParser = mock(FileParser.class);
        when(mockParser.parse(file1)).thenReturn("Extracted text 1");
        when(mockParser.parse(file2)).thenReturn("Extracted text 2");
        when(mockParser.parse(file3)).thenReturn("Extracted text 3");

        try (MockedStatic<FileParserFactory> mockedFactory = mockStatic(FileParserFactory.class)) {
            mockedFactory.when(() -> FileParserFactory.getParser(ExtensionsEnum.PDF))
                    .thenReturn(mockParser);

            List<DialogFileResponse> responses = dialogService.addFilesToDialog(
                    100L, testUser, new MultipartFile[]{file1, file2, file3}
            );

            assertEquals(3, responses.size());
            assertEquals(501L, responses.get(0).fileId());
            assertEquals(502L, responses.get(1).fileId());
            assertEquals(503L, responses.get(2).fileId());

            verify(fileStorageService, times(3)).uploadFile(anyString(), any(), anyLong());
            verify(fileMetadataRepository, times(3)).save(any(FileMetadata.class));
            verify(ragCommunicationService, times(3)).loadFileToRag(eq(100L), anyList());
        }

    }

    @Test
    public void shouldRollbackPreviouslyUploadedFilesWhenSecondFileFails() throws IOException {
        when(dialogRepository.findById(100L)).thenReturn(Optional.of(testDialog));

        MockMultipartFile file1 = new MockMultipartFile("files", "good.pdf", "application/pdf", "good content".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "bad.pdf", "application/pdf", "bad content".getBytes());

        FileMetadata savedFile1 = new FileMetadata();
        savedFile1.setId(501L);
        savedFile1.setStorageFileName("good-file.pdf");

        when(fileMetadataRepository.save(any(FileMetadata.class)))
                .thenReturn(savedFile1);

        FileParser mockParser = mock(FileParser.class);
        when(mockParser.parse(file1)).thenReturn("Extracted text 1");
        when(mockParser.parse(file2)).thenReturn("  ");

        try (MockedStatic<FileParserFactory> mockedFactory = mockStatic(FileParserFactory.class)) {
            mockedFactory.when(() -> FileParserFactory.getParser(ExtensionsEnum.PDF))
                    .thenReturn(mockParser);

            assertThrows(TextExtractionException.class, () -> {
                dialogService.addFilesToDialog(100L, testUser, new MultipartFile[]{file1, file2});
            });

            verify(fileStorageService, times(1)).uploadFile(anyString(), any(), anyLong());
            verify(fileMetadataRepository, times(1)).save(any(FileMetadata.class));
            verify(ragCommunicationService, times(1)).loadFileToRag(eq(100L), anyList());

            verify(fileMetadataRepository, times(1)).deleteById(501L);
            verify(fileStorageService, times(1)).deleteFile("good-file.pdf");
        }
    }

    @Test
    void shouldSendQuestionWhenRagReturnsAnswer() throws Exception {
        when(dialogRepository.findById(100L)).thenReturn(Optional.of(testDialog));

        SendMessageRequest request = new SendMessageRequest("Как работает Project Loom?");
        SendMessageResponse expectedResponse = new SendMessageResponse("Это виртуальные потоки...");

        when(ragCommunicationService.sendQuestionToRag(eq(100L), any(Message.class)))
                .thenReturn(expectedResponse);

        SendMessageResponse actualResponse = dialogService.sendQuestion(request, testUser, 100L);

        assertEquals("Это виртуальные потоки...", actualResponse.answer());

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

        verify(messageRepository, times(2)).save(messageCaptor.capture());

        List<Message> savedMessages = messageCaptor.getAllValues();

        Message savedQuestion = savedMessages.get(0);
        Message savedAnswer = savedMessages.get(1);

        assertEquals(Message.MessageRole.USER, savedQuestion.getRole());
        assertEquals("Как работает Project Loom?", savedQuestion.getContent());
        assertEquals(100L, savedQuestion.getDialog().getId());

        assertEquals(Message.MessageRole.BOT, savedAnswer.getRole());
        assertEquals("Это виртуальные потоки...", savedAnswer.getContent());
        assertEquals(100L, savedAnswer.getDialog().getId());
    }
}








