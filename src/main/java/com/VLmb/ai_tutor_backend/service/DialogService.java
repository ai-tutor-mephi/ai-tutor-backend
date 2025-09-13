package com.VLmb.ai_tutor_backend.service;

import com.VLmb.ai_tutor_backend.dto.DialogResponse;
import com.VLmb.ai_tutor_backend.dto.FileResponse;
import com.VLmb.ai_tutor_backend.entity.Dialog;
import com.VLmb.ai_tutor_backend.entity.FileMetadata;
import com.VLmb.ai_tutor_backend.entity.User;
import com.VLmb.ai_tutor_backend.repository.DialogRepository;
import com.VLmb.ai_tutor_backend.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.beans.Transient;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DialogService {

    private final DialogRepository dialogRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public DialogResponse createDialogWithFile(User user, MultipartFile file) throws IOException {

        Dialog dialog = new Dialog();
        dialog.setOwner(user);
        dialog.setTitle(file.getOriginalFilename());
        Dialog savedDialog = dialogRepository.save(dialog);

        addFileToDialog(savedDialog, file);

        return new DialogResponse(savedDialog.getId(), savedDialog.getTitle());
    }

    @Transactional
    public FileResponse addFileToDialog(Long dialogId, User currentUser, MultipartFile file) throws IOException {
        Dialog dialog = dialogRepository.findById(dialogId)
                .orElseThrow(() -> new RuntimeException("Dialog not found"));

        if (!dialog.getOwner().getId().equals(currentUser.getId())) {
            throw new SecurityException("User does not have permission to access this dialog");
        }

        FileMetadata savedFile = addFileToDialog(dialog, file);

        return new FileResponse(savedFile.getId(), savedFile.getOriginalFileName(), dialog.getId());
    }

    private FileMetadata addFileToDialog(Dialog dialog, MultipartFile file) throws IOException {
        String extension = getFileExtension(file.getOriginalFilename());
        String storageFileName = UUID.randomUUID() + "." + extension;

        fileStorageService.uploadFile(storageFileName, file.getInputStream(), file.getSize());

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

}
