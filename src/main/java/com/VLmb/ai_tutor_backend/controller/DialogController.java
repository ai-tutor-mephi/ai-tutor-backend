package com.VLmb.ai_tutor_backend.controller;

import com.VLmb.ai_tutor_backend.dto.*;
import com.VLmb.ai_tutor_backend.dto.MessageRequest;
import com.VLmb.ai_tutor_backend.service.CustomUserDetails;
import com.VLmb.ai_tutor_backend.service.DialogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(path = "/{dialogId}/send-question")
    public ResponseEntity<MessageResponse> sendMessageToDialog(
            @PathVariable Long dialogId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody MessageRequest messageRequest) throws IOException {

        MessageResponse response = dialogService.sendQuestion(messageRequest, principal.getUser(), dialogId);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    @GetMapping(path = "/{dialogId}/files")
    public ResponseEntity<List<FileResponse>> getFilesFromDialog(
            @PathVariable Long dialogId,
            @AuthenticationPrincipal CustomUserDetails principal) throws IOException {

        List<FileResponse> response = dialogService.getFilesFromDialog(dialogId, principal.getUser());

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
