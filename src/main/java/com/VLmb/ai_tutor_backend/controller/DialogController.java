package com.VLmb.ai_tutor_backend.controller;

import com.VLmb.ai_tutor_backend.dto.DialogResponse;
import com.VLmb.ai_tutor_backend.dto.FileResponse;
import com.VLmb.ai_tutor_backend.entity.User;
import com.VLmb.ai_tutor_backend.repository.DialogRepository;
import com.VLmb.ai_tutor_backend.service.DialogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@RestController
@RequestMapping("/api/dialogs")
@RequiredArgsConstructor
public class DialogController {

    private final DialogService dialogService;

    @PostMapping(path = "/with-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DialogResponse> createDialogWithFile(
            @AuthenticationPrincipal User currentUser,
            @RequestParam("file") MultipartFile file) {
        try {
            DialogResponse response = dialogService.createDialogWithFile(currentUser, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file", e);
        }
    }

    @PostMapping(path = "/{dialogId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileResponse> addFileToDialog(
            @PathVariable Long dialogId,
            @AuthenticationPrincipal User currentUser,
            @RequestParam("file") MultipartFile file) {
        try {
            FileResponse response = dialogService.addFileToDialog(dialogId, currentUser, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file", e);
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }
}
