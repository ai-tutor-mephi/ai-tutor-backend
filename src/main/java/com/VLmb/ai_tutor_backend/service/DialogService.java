package com.VLmb.ai_tutor_backend.service;

import com.VLmb.ai_tutor_backend.client.RagRestClient;
import com.VLmb.ai_tutor_backend.dto.*;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DialogService {

    private final DialogRepository dialogRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileStorageService fileStorageService;
    private final MessageRepository messageRepository;
    private final RagRestClient ragRestClient;

    @Transactional
    public DialogResponse createDialogWithFiles(User user, MultipartFile[] files) throws IOException {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("At least one file must be provided.");
        }

        Dialog dialog = new Dialog();
        dialog.setOwner(user);
        dialog.setTitle(files[0].getOriginalFilename());
        Dialog savedDialog = dialogRepository.save(dialog);

        Arrays.stream(files).forEach(file -> {
            try {
                addFileToDialog(savedDialog, file);
            } catch (IOException e) {
                throw new FileUploadException("Failed to upload file: " + file.getOriginalFilename(), e);
            }
        });

        return new DialogResponse(savedDialog.getId(), savedDialog.getTitle());
    }

    @Transactional
    public List<FileResponse> addFilesToDialog(Long dialogId, User currentUser, MultipartFile[] files) throws IOException {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("At least one file must be provided.");
        }

        Dialog dialog = dialogRepository.findById(dialogId)
                .orElseThrow(() -> new ResourceNotFoundException("Dialog", "id", dialogId));

        if (!dialog.getOwner().getId().equals(currentUser.getId())) {
            throw new SecurityException("User does not have permission to access this dialog");
        }

        return Arrays.stream(files).map(file -> {
            try {
                FileMetadata savedFile = addFileToDialog(dialog, file);
                return new FileResponse(savedFile.getId(), savedFile.getOriginalFileName(), dialog.getId());
            } catch (IOException e) {
                throw new FileUploadException("Failed to upload file: " + file.getOriginalFilename(), e);
            }
        }).collect(Collectors.toList());
    }

    private FileMetadata addFileToDialog(Dialog dialog, MultipartFile file) throws IOException {
        String extension = getFileExtension(file.getOriginalFilename());
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

        return fileMetadataRepository.save(fileMetadata);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    @Transactional(readOnly = true)
    public List<FileResponse> getFilesFromDialog(Long dialogId, User currentUser) throws IOException {
        Dialog dialog = dialogRepository.findById(dialogId)
                .orElseThrow(() -> new ResourceNotFoundException("Dialog", "id", dialogId));

        if (!dialog.getOwner().getId().equals(currentUser.getId())) {
            throw new SecurityException("User does not have permission to access this dialog");
        }

        List<FileMetadata> files = dialog.getFiles();

        return files.stream()
                .map(file -> new FileResponse(
                        file.getId(),
                        file.getOriginalFileName(),
                        dialog.getId()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DialogMessagesResponse getMessagesFromDialog(Long dialogId, User currentUser) {
        Dialog dialog = dialogRepository.findById(dialogId)
                .orElseThrow(() -> new ResourceNotFoundException("Dialog", "id", dialogId));

        if (!dialog.getOwner().getId().equals(currentUser.getId())) {
            throw new SecurityException("User does not have permission to access this dialog");
        }

        List<DialogMessagesDto> messages = messageRepository.findByDialogIdOrderByCreatedAtDesc(dialogId)
                .stream()
                .map(message -> new DialogMessagesDto(message.getContent(), message.getRole()))
                .collect(Collectors.toList());

        return new DialogMessagesResponse(dialog.getId(), messages);
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
        question.setDialog(dialogRepository.findById(dialogId)
                .orElseThrow(() -> new ResourceNotFoundException("Dialog", "id", dialogId)));
        question.setContent(messageRequest.question());

        List<DialogMessagesDto> dialogMessages = new ArrayList<>();
        for (Message message: messageRepository.findByDialogId(dialogId)) {
            dialogMessages.add(new DialogMessagesDto(message.getContent(), message.getRole()));
        }

        MessageResponse messageResponse = ragRestClient.current(new RagRequestDto(
                dialogId,
                dialogMessages,
                question.getContent()
        ));

        messageRepository.save(question);

        if (messageResponse.answer() != null) {
            Message answer = new Message();
            answer.setRole(Message.MessageRole.BOT);
            answer.setDialog(dialogRepository.findById(dialogId)
                    .orElseThrow(() -> new ResourceNotFoundException("Dialog", "id", dialogId)));
            answer.setContent(messageResponse.answer());
            messageRepository.save(answer);
        }

        return messageResponse;
    }

    public void deleteDialog(Long dialogId, User currentUser) {

        Dialog dialog = dialogRepository.findById(dialogId)
                .orElseThrow(() -> new ResourceNotFoundException("Dialog", "id", dialogId));

        if (!dialog.getOwner().getId().equals(currentUser.getId())) {
            throw new SecurityException("User does not have permission to delete this dialog");
        }

        for (FileMetadata file : dialog.getFiles()) {
            fileStorageService.deleteFile(file.getStorageFileName());
        }

        dialogRepository.delete(dialog);
    }

}
