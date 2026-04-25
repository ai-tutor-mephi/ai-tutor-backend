package com.VLmb.ai_tutor_backend.feature.dialog.application;

import com.VLmb.ai_tutor_backend.feature.auth.domain.User;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.DialogSummaryResponse;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.DialogMessageResponse;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.GetDialogMessagesResponse;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.CreateDialogResponse;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.SendMessageRequest;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.SendMessageResponse;
import com.VLmb.ai_tutor_backend.feature.dialog.domain.Dialog;
import com.VLmb.ai_tutor_backend.feature.dialog.domain.Message;
import com.VLmb.ai_tutor_backend.feature.dialog.infra.DialogRepository;
import com.VLmb.ai_tutor_backend.feature.dialog.infra.MessageRepository;
import com.VLmb.ai_tutor_backend.feature.file.application.DialogFileResponse;
import com.VLmb.ai_tutor_backend.feature.file.application.FileStorageService;
import com.VLmb.ai_tutor_backend.feature.file.domain.FileMetadata;
import com.VLmb.ai_tutor_backend.shared.error.exceptions.FileUploadException;
import com.VLmb.ai_tutor_backend.shared.error.exceptions.ResourceNotFoundException;
import com.VLmb.ai_tutor_backend.feature.file.infra.FileMetadataRepository;
import com.VLmb.ai_tutor_backend.feature.file.fileparsing.ExtensionsEnum;
import com.VLmb.ai_tutor_backend.feature.file.fileparsing.FileParserFactory;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagFileRequest;
import com.VLmb.ai_tutor_backend.feature.rag.application.RagCommunicationService;
import com.VLmb.ai_tutor_backend.shared.error.exceptions.TextExtractionException;
import com.VLmb.ai_tutor_backend.shared.error.exceptions.UnsupportedFileExtension;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DialogService {

    private static final Set<ExtensionsEnum> TEXT_BASED_EXTENSIONS = Set.of(
            ExtensionsEnum.PDF,
            ExtensionsEnum.DOCX,
            ExtensionsEnum.TXT
    );

    private final DialogRepository dialogRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileStorageService fileStorageService;
    private final MessageRepository messageRepository;
    private final RagCommunicationService ragCommunicationService;
    @Qualifier("dbExecutor")
    private final TaskExecutor dbExecutor;

    public CreateDialogResponse createDialogWithFiles(User user, MultipartFile[] files) throws IOException {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("At least one file must be provided.");
        }

        log.info(
                "event=dialog_create_with_files_start user_id={} file_count={}",
                user.getId(),
                files.length
        );

        Dialog dialog = new Dialog();
        dialog.setOwner(user);
        dialog.setTitle(files[0].getOriginalFilename());
        Dialog savedDialog = dialogRepository.save(dialog);

        try {
            for (MultipartFile file : files) {
                addFileToDialog(savedDialog, file);
            }
            log.info(
                    "event=dialog_create_with_files_success dialog_id={} user_id={} file_count={}",
                    savedDialog.getId(),
                    user.getId(),
                    files.length
            );
        } catch (RuntimeException | IOException ex) {
            log.warn(
                    "event=dialog_create_with_files_failed dialog_id={} user_id={} message={}",
                    savedDialog.getId(),
                    user.getId(),
                    ex.getMessage(),
                    ex
            );
            cleanupDialogArtifacts(savedDialog);
            throw ex instanceof RuntimeException runtimeException
                    ? runtimeException
                    : new FileUploadException("Failed to upload files for dialog creation", ex);
        }

        return new CreateDialogResponse(savedDialog.getId(), savedDialog.getTitle());
    }

    public CompletableFuture<CreateDialogResponse> createDialogWithFilesAsync(User user, MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("At least one file must be provided.");
        }

        log.info(
                "event=dialog_create_with_files_async_start user_id={} file_count={}",
                user.getId(),
                files.length
        );

        Dialog dialog = new Dialog();
        dialog.setOwner(user);
        dialog.setTitle(files[0].getOriginalFilename());
        Dialog savedDialog = dialogRepository.save(dialog);

        try {
            return addFilesToDialogAsync(savedDialog.getId(), user, files)
                    .thenApply(unused -> new CreateDialogResponse(savedDialog.getId(), savedDialog.getTitle()))
                    .whenComplete((unused, ex) -> {
                        if (ex != null) {
                            log.warn(
                                    "event=dialog_create_with_files_async_failed dialog_id={} user_id={} message={}",
                                    savedDialog.getId(),
                                    user.getId(),
                                    ex.getMessage()
                            );
                            cleanupDialogArtifacts(savedDialog);
                        } else {
                            log.info(
                                    "event=dialog_create_with_files_async_success dialog_id={} user_id={} file_count={}",
                                    savedDialog.getId(),
                                    user.getId(),
                                    files.length
                            );
                        }
                    });
        } catch (RuntimeException ex) {
            log.warn(
                    "event=dialog_create_with_files_async_failed dialog_id={} user_id={} message={}",
                    savedDialog.getId(),
                    user.getId(),
                    ex.getMessage(),
                    ex
            );
            cleanupDialogArtifacts(savedDialog);
            throw ex;
        }
    }

    public List<DialogFileResponse> addFilesToDialog(Long dialogId, User currentUser, MultipartFile[] files) throws IOException {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("At least one file must be provided.");
        }

        log.info(
                "event=dialog_add_files_start dialog_id={} user_id={} file_count={}",
                dialogId,
                currentUser.getId(),
                files.length
        );

        Dialog dialog = getDialog(dialogId);
        assertDialogOwner(dialog, currentUser);

        List<DialogFileResponse> storedFiles = new ArrayList<>();
        List<FileMetadata> uploadedFiles = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                FileMetadata savedFile = addFileToDialog(dialog, file);
                uploadedFiles.add(savedFile);
                storedFiles.add(toFileResponse(savedFile, dialog.getId()));
            }
            log.info(
                    "event=dialog_add_files_success dialog_id={} user_id={} file_count={}",
                    dialogId,
                    currentUser.getId(),
                    files.length
            );
        } catch (RuntimeException | IOException ex) {
            log.warn(
                    "event=dialog_add_files_failed dialog_id={} user_id={} message={}",
                    dialogId,
                    currentUser.getId(),
                    ex.getMessage(),
                    ex
            );
            cleanupFiles(uploadedFiles);
            throw ex instanceof RuntimeException runtimeException
                    ? runtimeException
                    : new FileUploadException("Failed to upload files to dialog", ex);
        }

        return storedFiles;
    }

    private FileMetadata addFileToDialog(Dialog dialog, MultipartFile file) throws IOException {
        ExtensionsEnum parsedExtension = resolveSupportedExtension(file.getOriginalFilename());
        String extractedText = extractTextOrThrow(file, parsedExtension);
        String storageFileName = UUID.randomUUID() + "." + parsedExtension.value();

        uploadToStorage(storageFileName, file);

        FileMetadata saved = saveFileMetadata(dialog, file, storageFileName);
        RagFileRequest ragFile = toRagFileRequest(saved, extractedText);
        try {
            ragCommunicationService.loadFileToRag(dialog.getId(), List.of(ragFile));
            return saved;
        } catch (RuntimeException ex) {
            cleanupPersistedFile(saved.getId(), storageFileName);
            throw ex;
        }
    }

    //ToDo: Мб переделать через статусы, помечать в handle что файл битый, а не делать cleanup
    private CompletableFuture<FileMetadata> addFileToDialogAsync(Dialog dialog, MultipartFile file) throws IOException {
        ExtensionsEnum parsedExtension = resolveSupportedExtension(file.getOriginalFilename());
        String extractedText = extractTextOrThrow(file, parsedExtension);
        String storageFileName = UUID.randomUUID() + "." + parsedExtension.value();

        uploadToStorage(storageFileName, file);

        FileMetadata saved = saveFileMetadata(dialog, file, storageFileName);
        RagFileRequest ragFile = toRagFileRequest(saved, extractedText);

        return ragCommunicationService.loadFileToRagAsync(dialog.getId(), List.of(ragFile))
                .handle((unused, ex) -> {
                    if (ex != null) {
                        cleanupPersistedFile(saved.getId(), storageFileName);
                        throw propagateAsyncFailure(ex);
                    }
                    return saved;
                });
    }

    public CompletableFuture<List<DialogFileResponse>> addFilesToDialogAsync(
            Long dialogId,
            User currentUser,
            MultipartFile[] files
    ) {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("At least one file must be provided.");
        }

        log.info(
                "event=dialog_add_files_async_start dialog_id={} user_id={} file_count={}",
                dialogId,
                currentUser.getId(),
                files.length
        );

        Dialog dialog = getDialog(dialogId);
        assertDialogOwner(dialog, currentUser);

        List<CompletableFuture<FileMetadata>> futures = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                futures.add(addFileToDialogAsync(dialog, file));
            } catch (IOException e) {
                cleanupFiles(completedSuccessfully(futures));
                throw new FileUploadException("Failed to upload file: " + file.getOriginalFilename(), e);
            }
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .handle((unused, ex) -> {
                    if (ex != null) {
                        log.warn(
                                "event=dialog_add_files_async_failed dialog_id={} user_id={} message={}",
                                dialogId,
                                currentUser.getId(),
                                ex.getMessage()
                        );
                        cleanupFiles(completedSuccessfully(futures));
                        throw propagateAsyncFailure(ex);
                    }

                    log.info(
                            "event=dialog_add_files_async_success dialog_id={} user_id={} file_count={}",
                            dialogId,
                            currentUser.getId(),
                            files.length
                    );
                    return futures.stream()
                            .map(CompletableFuture::join)
                            .map(saved -> toFileResponse(saved, dialog.getId()))
                            .toList();
                });
    }

    private String validateExtension(String filename) {
        String extension = getFileExtension(filename);
        if (extension.isBlank()) {
            throw new FileUploadException("File must have an extension");
        }
        return extension;
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase(Locale.ROOT);
    }

    private ExtensionsEnum resolveSupportedExtension(String filename) {
        String extension = validateExtension(filename);
        return ExtensionsEnum.fromValue(extension)
                .filter(TEXT_BASED_EXTENSIONS::contains)
                .orElseThrow(() -> new UnsupportedFileExtension(
                        String.format("File extension '%s' is not supported", extension)
                ));
    }

    private String extractTextOrThrow(MultipartFile file, ExtensionsEnum extension) throws IOException {
        String text = FileParserFactory.getParser(extension).parse(file);
        if (text == null || text.isBlank()) {
            throw new TextExtractionException(
                    String.format("Unable to extract text from file '%s'", file.getOriginalFilename())
            );
        }
        return text;
    }

    private void uploadToStorage(String storageFileName, MultipartFile file) {
        try {
            fileStorageService.uploadFile(storageFileName, file.getInputStream(), file.getSize());
        } catch (IOException e) {
            throw new FileUploadException("Could not store file " + file.getOriginalFilename(), e);
        }
    }

    private FileMetadata saveFileMetadata(Dialog dialog, MultipartFile file, String storageFileName) {
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setDialog(dialog);
        fileMetadata.setOriginalFileName(file.getOriginalFilename());
        fileMetadata.setStorageFileName(storageFileName);
        fileMetadata.setFileSize(file.getSize());
        fileMetadata.setMimeType(file.getContentType());

        try {
            return fileMetadataRepository.save(fileMetadata);
        } catch (RuntimeException ex) {
            cleanupStorageFile(storageFileName);
            throw ex;
        }
    }

    private RagFileRequest toRagFileRequest(FileMetadata metadata, String extractedText) {
        return new RagFileRequest(
                metadata.getId().toString(),
                metadata.getOriginalFileName(),
                extractedText
        );
    }

    private void cleanupPersistedFile(Long fileId, String storageFileName) {
        try {
            fileMetadataRepository.deleteById(fileId);
        } catch (RuntimeException ignored) {
        }
        cleanupStorageFile(storageFileName);
    }

    private void cleanupFiles(List<FileMetadata> files) {
        for (FileMetadata file : files) {
            cleanupPersistedFile(file.getId(), file.getStorageFileName());
        }
    }

    private void cleanupStorageFile(String storageFileName) {
        try {
            fileStorageService.deleteFile(storageFileName);
        } catch (RuntimeException ignored) {
        }
    }

    private void cleanupDialogArtifacts(Dialog dialog) {
        try {
            List<FileMetadata> files = fileMetadataRepository.findByDialogId(dialog.getId());
            for (FileMetadata file : files) {
                cleanupStorageFile(file.getStorageFileName());
            }
            dialogRepository.deleteById(dialog.getId());
        } catch (RuntimeException ignored) {
        }
    }

    private RuntimeException propagateAsyncFailure(Throwable ex) {
        if (ex instanceof CompletionException completionException && completionException.getCause() != null) {
            Throwable cause = completionException.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                return runtimeException;
            }
            return new CompletionException(cause);
        }
        if (ex instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new CompletionException(ex);
    }

    private List<FileMetadata> completedSuccessfully(List<CompletableFuture<FileMetadata>> futures) {
        return futures.stream()
                .filter(CompletableFuture::isDone)
                .filter(future -> !future.isCompletedExceptionally())
                .map(CompletableFuture::join)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DialogFileResponse> getFilesFromDialog(Long dialogId, User currentUser) throws IOException {
        Dialog dialog = getDialog(dialogId);
        assertDialogOwner(dialog, currentUser);

        List<FileMetadata> files = dialog.getFiles();

        return files.stream()
                .map(file -> new DialogFileResponse(
                        file.getId(),
                        file.getOriginalFileName(),
                        dialog.getId()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GetDialogMessagesResponse getMessagesFromDialog(Long dialogId, User currentUser) {
        Dialog dialog = dialogRepository.findById(dialogId)
                .orElseThrow(() -> new ResourceNotFoundException("Dialog", "id", dialogId));

        if (!dialog.getOwner().getId().equals(currentUser.getId())) {
            throw new SecurityException("User does not have permission to access this dialog");
        }

        List<DialogMessageResponse> messages = messageRepository.findByDialogIdOrderByCreatedAt(dialogId)
                .stream()
                .map(message -> new DialogMessageResponse(message.getContent(), message.getRole()))
                .collect(Collectors.toList());

        return new GetDialogMessagesResponse(dialog.getId(), messages);
    }

    @Transactional(readOnly = true)
    public List<DialogSummaryResponse> getAllDialogsForUser(User currentUser) {

        return dialogRepository.findByOwnerIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(dialog -> new DialogSummaryResponse(dialog.getId(), dialog.getTitle(), dialog.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public SendMessageResponse sendQuestion(SendMessageRequest messageRequest, User currentUser, Long dialogId) throws IOException {

        log.info(
                "event=dialog_send_question_start dialog_id={} user_id={}",
                dialogId,
                currentUser.getId()
        );

        Message question = new Message();
        question.setRole(Message.MessageRole.USER);
        question.setDialog(getDialog(dialogId));
        question.setContent(messageRequest.question());

        SendMessageResponse messageResponse = ragCommunicationService.sendQuestionToRag(dialogId, question);

        messageRepository.save(question);

        if (messageResponse.answer() != null) {
            Message answer = new Message();
            answer.setRole(Message.MessageRole.BOT);
            answer.setDialog(getDialog(dialogId));
            answer.setContent(messageResponse.answer());
            messageRepository.save(answer);
        }

        log.info(
                "event=dialog_send_question_success dialog_id={} user_id={} has_answer={} answer_len={}",
                dialogId,
                currentUser.getId(),
                messageResponse.answer() != null,
                messageResponse.answer() == null ? 0 : messageResponse.answer().length()
        );

        return messageResponse;
    }

    public CompletableFuture<SendMessageResponse> sendQuestionAsync(
            SendMessageRequest messageRequest,
            User currentUser,
            Long dialogId
    ) {
        Dialog dialog = getDialog(dialogId);
        assertDialogOwner(dialog, currentUser);

        log.info(
                "event=dialog_send_question_async_start dialog_id={} user_id={}",
                dialogId,
                currentUser.getId()
        );

        Message question = new Message();
        question.setRole(Message.MessageRole.USER);
        question.setDialog(dialog);
        question.setContent(messageRequest.question());

        return ragCommunicationService.sendQuestionToRagAsync(dialogId, question)
                .thenApplyAsync(messageResponse -> {
                    messageRepository.save(question);

                    if (messageResponse.answer() != null) {
                        Message answer = new Message();
                        answer.setRole(Message.MessageRole.BOT);
                        answer.setDialog(dialog);
                        answer.setContent(messageResponse.answer());
                        messageRepository.save(answer);
                    }

                    log.info(
                            "event=dialog_send_question_async_success dialog_id={} user_id={} has_answer={} answer_len={}",
                            dialogId,
                            currentUser.getId(),
                            messageResponse.answer() != null,
                            messageResponse.answer() == null ? 0 : messageResponse.answer().length()
                    );
                    return messageResponse;
                }, dbExecutor);
    }

    public void deleteDialog(Long dialogId, User currentUser) {

        Dialog dialog = getDialog(dialogId);
        assertDialogOwner(dialog, currentUser);

        List<FileMetadata> files = fileMetadataRepository.findByDialogId(dialogId);
        log.info(
                "event=dialog_delete dialog_id={} user_id={} file_count={}",
                dialogId,
                currentUser.getId(),
                files.size()
        );

        for (FileMetadata file : files) {
            fileStorageService.deleteFile(file.getStorageFileName());
        }

        dialogRepository.deleteById(dialogId);
    }

    @Transactional
    public DialogSummaryResponse changeDialogTitle(Long dialogId, User currentUser, String newTitle) {
        Dialog dialog = getDialog(dialogId);
        assertDialogOwner(dialog, currentUser);
        log.info(
                "event=dialog_title_change dialog_id={} user_id={} new_title_len={}",
                dialogId,
                currentUser.getId(),
                newTitle == null ? 0 : newTitle.length()
        );
        dialog.setTitle(newTitle);
        Dialog saved = dialogRepository.save(dialog);
        return new DialogSummaryResponse(saved.getId(), saved.getTitle(), saved.getCreatedAt());
    }

    private Dialog getDialog(Long dialogId) {
        return dialogRepository.findById(dialogId)
                .orElseThrow(() -> new ResourceNotFoundException("Dialog", "id", dialogId));
    }

    private void assertDialogOwner(Dialog dialog, User currentUser) {
        if (!dialog.getOwner().getId().equals(currentUser.getId())) {
            throw new SecurityException("User does not have permission to access this dialog");
        }
    }

    private DialogFileResponse toFileResponse(FileMetadata file, Long dialogId) {
        return new DialogFileResponse(file.getId(), file.getOriginalFileName(), dialogId);
    }
}
