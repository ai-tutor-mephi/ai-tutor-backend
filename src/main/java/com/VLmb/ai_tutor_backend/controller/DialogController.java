package com.VLmb.ai_tutor_backend.controller;

import com.VLmb.ai_tutor_backend.dto.DialogInfo;
import com.VLmb.ai_tutor_backend.dto.DialogResponse;
import com.VLmb.ai_tutor_backend.dto.FileResponse;
import com.VLmb.ai_tutor_backend.entity.User;
import com.VLmb.ai_tutor_backend.repository.UserRepository;
import com.VLmb.ai_tutor_backend.service.CustomUserDetails;
import com.VLmb.ai_tutor_backend.service.DialogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/dialogs")
@RequiredArgsConstructor
public class DialogController {

    private final DialogService dialogService;

    @PostMapping(path = "/with-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DialogResponse> createDialogWithFiles(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("files") MultipartFile[] files) throws IOException {

        DialogResponse response = dialogService.createDialogWithFiles(principal.getUser(), files);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(path = "/{dialogId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<FileResponse>> addFileToDialog(
            @PathVariable Long dialogId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("files") MultipartFile[] files) throws IOException {

        List<FileResponse> response = dialogService.addFilesToDialog(dialogId, principal.getUser(), files);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<DialogInfo>> getAllDialogs(@AuthenticationPrincipal CustomUserDetails principal) {

        List<DialogInfo> dialogs = dialogService.getAllDialogsForUser(principal.getUser());

        return ResponseEntity.ok(dialogs);
    }

    @DeleteMapping("/{dialogId}")
    public ResponseEntity<Void> deleteDialog(
            @PathVariable Long dialogId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        dialogService.deleteDialog(dialogId, principal.getUser());
        return ResponseEntity.noContent().build();

    }

}
