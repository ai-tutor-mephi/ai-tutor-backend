package com.VLmb.ai_tutor_backend.feature.dialog.api;

import com.VLmb.ai_tutor_backend.feature.auth.application.CustomUserDetails;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.ChangeDialogTitleRequest;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.CreateDialogRequest;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.DialogSummaryResponse;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.GetDialogMessagesResponse;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.CreateDialogResponse;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.SendMessageRequest;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.SendMessageResponse;
import com.VLmb.ai_tutor_backend.feature.dialog.application.DialogService;
import com.VLmb.ai_tutor_backend.feature.file.application.DialogFileResponse;
import jakarta.validation.Valid;
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
    public ResponseEntity<CreateDialogResponse> createDialogWithFiles(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestPart(name = "request", required = false) @Valid CreateDialogRequest request,
            @RequestPart("files") MultipartFile[] files) throws IOException {

        String title = request == null ? null : request.title();
        CreateDialogResponse response = dialogService.createDialogWithFiles(principal.getUser(), files, title);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(path = "/{dialogId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<DialogFileResponse>> addFileToDialog(
            @PathVariable Long dialogId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("files") MultipartFile[] files) throws IOException {

        List<DialogFileResponse> response = dialogService.addFilesToDialog(dialogId, principal.getUser(), files);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(path = "/{dialogId}/send-question")
    public ResponseEntity<SendMessageResponse> sendMessageToDialog(
            @PathVariable Long dialogId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody SendMessageRequest messageRequest) throws IOException {

        SendMessageResponse response = dialogService.sendQuestion(messageRequest, principal.getUser(), dialogId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    @GetMapping(path = "/{dialogId}/files")
    public ResponseEntity<List<DialogFileResponse>> getFilesFromDialog(
            @PathVariable Long dialogId,
            @AuthenticationPrincipal CustomUserDetails principal) throws IOException {

        List<DialogFileResponse> response = dialogService.getFilesFromDialog(dialogId, principal.getUser());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(path = "/{dialogId}/messages")
    public ResponseEntity<GetDialogMessagesResponse> getMessagesFromDialog(
            @PathVariable Long dialogId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        GetDialogMessagesResponse response = dialogService.getMessagesFromDialog(dialogId, principal.getUser());

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<DialogSummaryResponse>> getAllDialogs(@AuthenticationPrincipal CustomUserDetails principal) {

        List<DialogSummaryResponse> dialogs = dialogService.getAllDialogsForUser(principal.getUser());

        return ResponseEntity.ok(dialogs);
    }

    @PatchMapping("/{dialogId}/change-title")
    public ResponseEntity<DialogSummaryResponse> changeDialogTitle(
            @PathVariable Long dialogId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ChangeDialogTitleRequest request) {

        DialogSummaryResponse dialogInfo = dialogService.changeDialogTitle(dialogId, principal.getUser(), request.title());
        return ResponseEntity.ok(dialogInfo);
    }

    @DeleteMapping("/{dialogId}")
    public ResponseEntity<Void> deleteDialog(
            @PathVariable Long dialogId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        dialogService.deleteDialog(dialogId, principal.getUser());
        return ResponseEntity.noContent().build();

    }

}
