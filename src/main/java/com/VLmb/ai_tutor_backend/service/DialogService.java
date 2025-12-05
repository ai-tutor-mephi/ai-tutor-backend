package com.VLmb.ai_tutor_backend.service;

import com.VLmb.ai_tutor_backend.dto.DialogInfo;
import com.VLmb.ai_tutor_backend.dto.DialogResponse;
import com.VLmb.ai_tutor_backend.dto.FileInf;
import com.VLmb.ai_tutor_backend.dto.FileResponse;
import com.VLmb.ai_tutor_backend.dto.MessageRequest;
import com.VLmb.ai_tutor_backend.dto.MessageResponse;
import com.VLmb.ai_tutor_backend.entity.Dialog;
import com.VLmb.ai_tutor_backend.entity.FileMetadata;
import com.VLmb.ai_tutor_backend.entity.Message;
import com.VLmb.ai_tutor_backend.entity.User;
import com.VLmb.ai_tutor_backend.exception.FileUploadException;
import com.VLmb.ai_tutor_backend.exception.ResourceNotFoundException;
import com.VLmb.ai_tutor_backend.repository.DialogRepository;
import com.VLmb.ai_tutor_backend.repository.FileMetadataRepository;
import com.VLmb.ai_tutor_backend.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DialogService {

    private static final String PDF_EXTENSION = "pdf";
    private static final String DOCX_EXTENSION = "docx";
    private static final Set<String> TEXT_BASED_EXTENSIONS = Set.of(PDF_EXTENSION, DOCX_EXTENSION);

    private final DialogRepository dialogRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileStorageService fileStorageService;
    private final MessageRepository messageRepository;
    private final RagCommunicationService ragCommunicationService;
    private final PdfParsingService pdfParsingService;
    private final DocxParsingService docxParsingService;

    @Transactional
    public DialogResponse createDialogWithFiles(User user, MultipartFile[] files) throws IOException {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("At least one file must be provided.");
        }

        Dialog dialog = new Dialog();
        dialog.setOwner(user);
        dialog.setTitle(files[0].getOriginalFilename());
        Dialog savedDialog = dialogRepository.save(dialog);

        for (MultipartFile file : files) {
            try {
                addFileToDialog(savedDialog, file);
            } catch (IOException e) {
                throw new FileUploadException("Failed to upload file: " + file.getOriginalFilename(), e);
            }
        }

        return new DialogResponse(savedDialog.getId(), savedDialog.getTitle());
    }

    @Transactional
    public List<FileResponse> addFilesToDialog(Long dialogId, User currentUser, MultipartFile[] files) throws IOException {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("At least one file must be provided.");
        }

        Dialog dialog = getDialog(dialogId);
        assertDialogOwner(dialog, currentUser);

        List<FileResponse> storedFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                FileMetadata savedFile = addFileToDialog(dialog, file);
                storedFiles.add(toFileResponse(savedFile, dialog.getId()));
            } catch (IOException e) {
                throw new FileUploadException("Failed to upload file: " + file.getOriginalFilename(), e);
            }
        }

        return storedFiles;
    }

    private FileMetadata addFileToDialog(Dialog dialog, MultipartFile file) throws IOException {
        String extension = validateExtension(file.getOriginalFilename());
        String storageFileName = UUID.randomUUID() + "." + extension;

        try {
            fileStorageService.uploadFile(storageFileName, file.getInputStream(), file.getSize());
        } catch (IOException e) {
            throw new FileUploadException("Could not store file " + file.getOriginalFilename(), e);
        }

        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setDialog(dialog);
        fileMetadata.setOriginalFileName(file.getOriginalFilename());
        fileMetadata.setStorageFileName(storageFileName);
        fileMetadata.setFileSize(file.getSize());
        fileMetadata.setMimeType(file.getContentType());

        FileMetadata saved = fileMetadataRepository.save(fileMetadata);
        if (TEXT_BASED_EXTENSIONS.contains(extension)) {
            sendFileToRag(dialog.getId(), saved, file, extension);
        }

        return saved;
    }

    private void sendFileToRag(Long dialogId, FileMetadata metadata, MultipartFile file, String extension) throws IOException {
        String text = extractText(file, extension);
        if (text == null || text.isBlank()) {
            return;
        }
        FileInf fileInf = new FileInf(metadata.getId(), metadata.getOriginalFileName(), text);
        ragCommunicationService.loadFileToRag(dialogId, List.of(fileInf));
    }

    private String extractText(MultipartFile file, String extension) throws IOException {
        return switch (extension) {
            case PDF_EXTENSION -> pdfParsingService.parsePdf(file);
            case DOCX_EXTENSION -> docxParsingService.parseDocx(file);
            default -> "";
        };
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

    @Transactional(readOnly = true)
    public List<FileResponse> getFilesFromDialog(Long dialogId, User currentUser) throws IOException {
        Dialog dialog = getDialog(dialogId);
        assertDialogOwner(dialog, currentUser);

        List<FileMetadata> files = dialog.getFiles();

        return files.stream()
                .map(file -> new FileResponse(
                        file.getId(),
                        file.getOriginalFileName(),
                        dialog.getId()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DialogInfo> getAllDialogsForUser(User currentUser) {

        return dialogRepository.findByOwnerIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(dialog -> new DialogInfo(dialog.getId(), dialog.getTitle(), dialog.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional
    public MessageResponse sendQuestion(MessageRequest messageRequest, User currentUser, Long dialogId) throws IOException {

        Message question = new Message();
        question.setRole(Message.MessageRole.USER);
        question.setDialog(getDialog(dialogId));
        question.setContent(messageRequest.question());

        MessageResponse messageResponse = ragCommunicationService.sendQuestionToRag(dialogId, question);

        messageRepository.save(question);

        if (messageResponse.answer() != null) {
            Message answer = new Message();
            answer.setRole(Message.MessageRole.BOT);
            answer.setDialog(getDialog(dialogId));
            answer.setContent(messageResponse.answer());
            messageRepository.save(answer);
        }

        return messageResponse;
    }

    public void deleteDialog(Long dialogId, User currentUser) {

        Dialog dialog = getDialog(dialogId);
        assertDialogOwner(dialog, currentUser);

        for (FileMetadata file : dialog.getFiles()) {
            fileStorageService.deleteFile(file.getStorageFileName());
        }

        dialogRepository.delete(dialog);
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

    private FileResponse toFileResponse(FileMetadata file, Long dialogId) {
        return new FileResponse(file.getId(), file.getOriginalFileName(), dialogId);
    }
}
